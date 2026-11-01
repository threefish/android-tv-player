package com.tvplayer.player

import com.tvplayer.core.repository.FileRepository
import com.tvplayer.core.util.SmartFileNameComparator
import java.text.Normalizer
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 字幕匹配：优先同名，其次带语言/清晰度后缀，最后按名称相似度兜底。
 * 带集数的视频会严格校验集数，避免把相邻集或续集的字幕挂到当前视频上。
 */
@Singleton
class SubtitleFinder @Inject constructor(
    private val fileRepository: FileRepository
) {

    suspend fun findSubtitles(videoPath: String): List<SubtitleInfo> {
        val videoFile = fileRepository.getByPath(videoPath) ?: return emptyList()
        if (videoFile.isDirectory) return emptyList()
        val videoTitle = videoFile.name.substringBeforeLast('.', videoFile.name)
        if (videoTitle.isBlank()) return emptyList()

        val videoParts = NameParts.parse(videoTitle)
        val parentPath = videoPath.substringBeforeLast("/", "/").ifEmpty { "/" }

        return fileRepository.getFilesByExtensions(parentPath, SUPPORTED_SUBTITLE_EXTS)
            .mapNotNull { file ->
                val format = file.name.substringAfterLast('.', "").lowercase(Locale.ROOT)
                val subtitleTitle = file.name.substringBeforeLast('.', file.name)
                val subtitleParts = NameParts.parse(subtitleTitle)
                val score = matchScore(videoParts, subtitleParts) ?: return@mapNotNull null
                SubtitleInfo(
                    path = file.path,
                    name = file.name,
                    language = subtitleParts.language,
                    format = format,
                    isExact = score >= EXACT_SCORE,
                    matchScore = score
                )
            }
            .sortedWith(
                compareByDescending<SubtitleInfo> { it.matchScore }
                    .thenByDescending { if (it.language?.startsWith("中文") == true) 1 else 0 }
                    .thenComparator { a, b -> SmartFileNameComparator.compareNames(a.name, b.name) }
            )
    }

    private fun matchScore(video: NameParts, subtitle: NameParts): Int? {
        val videoEpisode = video.episode
        val subtitleEpisode = subtitle.episode
        if (videoEpisode != null && subtitleEpisode != null) {
            if (videoEpisode.episode != subtitleEpisode.episode) return null
            // 两边都带季数时才比较季，字幕只写“第 N 集”时按集数匹配
            if (videoEpisode.season != null && subtitleEpisode.season != null &&
                videoEpisode.season != subtitleEpisode.season
            ) {
                return null
            }
        } else if (videoEpisode == null && subtitleEpisode != null) {
            // 视频没有集数、字幕却带集数，多半是别的剧集，不做匹配
            return null
        }

        val videoName = video.normalized
        val subtitleName = subtitle.core
        if (videoName.isEmpty() || subtitleName.isEmpty()) return null

        if (videoName == subtitleName) {
            return if (subtitle.hasLanguageSuffix) EXACT_SCORE else 100
        }

        // 视频名更长：多出的部分只允许年份、清晰度等标签，避免把续集字幕挂错
        if (videoName.startsWith("$subtitleName ")) {
            if (isSafeExtra(videoName.removePrefix(subtitleName))) return 90
        }
        // 字幕名更长：多出的部分只允许语言、清晰度等标签
        if (subtitleName.startsWith("$videoName ")) {
            if (isSafeExtra(subtitleName.removePrefix(videoName))) return 92
        }

        // 集数已经核对过，去掉集数写法（S01E02 / 第01集 / EP02）后再比片名
        if (videoEpisode != null && subtitleEpisode != null) {
            val videoBase = stripEpisode(videoName)
            val subtitleBase = stripEpisode(subtitleName)
            if (videoBase.isNotEmpty() && subtitleBase.isNotEmpty()) {
                if (videoBase == subtitleBase) return 88
                if (videoBase.startsWith("$subtitleBase ") &&
                    isSafeExtra(videoBase.removePrefix(subtitleBase))
                ) {
                    return 86
                }
                if (subtitleBase.startsWith("$videoBase ") &&
                    isSafeExtra(subtitleBase.removePrefix(videoBase))
                ) {
                    return 86
                }
                val baseSimilarity = similarity(videoBase, subtitleBase)
                val baseLengthRatio = minOf(videoBase.length, subtitleBase.length).toFloat() /
                    maxOf(videoBase.length, subtitleBase.length)
                if (baseSimilarity >= FUZZY_SIMILARITY && baseLengthRatio >= 0.6f) {
                    return 50 + (baseSimilarity * 20).toInt()
                }
            }
        }

        // 数字（年份、频道号等）不同视为不同内容
        if (video.numbers != subtitle.numbers) return null

        val similarity = similarity(videoName, subtitleName)
        val lengthRatio = minOf(videoName.length, subtitleName.length).toFloat() /
            maxOf(videoName.length, subtitleName.length)
        if (similarity >= FUZZY_SIMILARITY && lengthRatio >= 0.6f) {
            return 50 + (similarity * 20).toInt()
        }
        return null
    }

    private fun isSafeExtra(extra: String): Boolean {
        val tokens = extra.trim().split(' ').filter { it.isNotEmpty() }
        if (tokens.isEmpty()) return true
        return tokens.all { token -> token in SAFE_TAGS || YEAR_REGEX.matches(token) }
    }

    private fun similarity(a: String, b: String): Float {
        val left = bigrams(a)
        val right = bigrams(b)
        if (left.isEmpty() || right.isEmpty()) return 0f
        val overlap = left.count { it in right }
        return 2f * overlap / (left.size + right.size)
    }

    private fun bigrams(text: String): Set<String> {
        val compact = text.filterNot { it == ' ' }
        if (compact.length < 2) return setOf(compact)
        return buildSet {
            for (i in 0 until compact.length - 1) add(compact.substring(i, i + 2))
        }
    }

    private class NameParts(
        val normalized: String,
        val core: String,
        val hasLanguageSuffix: Boolean,
        val language: String?,
        val episode: EpisodeInfo?,
        val numbers: Set<String>
    ) {
        companion object {
            fun parse(raw: String): NameParts {
                val normalized = normalizeTitle(raw)
                val tokens = normalized.split(' ').filter { it.isNotEmpty() }
                val languageTokens = mutableListOf<String>()
                var end = tokens.size
                while (end > 0 && tokens[end - 1] in LANGUAGE_TAGS) {
                    languageTokens.add(0, tokens[end - 1])
                    end--
                }
                val coreTokens = tokens.subList(0, end)
                return NameParts(
                    normalized = normalized,
                    core = coreTokens.joinToString(" "),
                    hasLanguageSuffix = languageTokens.isNotEmpty(),
                    language = languageLabel(languageTokens),
                    episode = extractEpisode(normalized),
                    numbers = tokens.asSequence()
                        .filterNot { it in QUALITY_TAGS }
                        .mapNotNull { it.toLongOrNull()?.toString() }
                        .toSet()
                )
            }

            private fun languageLabel(tokens: List<String>): String? {
                if (tokens.isEmpty()) return null
                val hasChinese = tokens.any { it in CHINESE_TAGS || it in TRADITIONAL_TAGS }
                val hasEnglish = tokens.any { it in ENGLISH_TAGS }
                return when {
                    tokens.any { it in BILINGUAL_TAGS } || (hasChinese && hasEnglish) -> "中英双语"
                    tokens.any { it in TRADITIONAL_TAGS } -> "中文（繁体）"
                    hasChinese -> "中文"
                    hasEnglish -> "English"
                    tokens.any { it in JAPANESE_TAGS } -> "日本語"
                    tokens.any { it in KOREAN_TAGS } -> "한국어"
                    else -> tokens.joinToString(" ")
                }
            }
        }
    }
}

private const val EXACT_SCORE = 96
private const val FUZZY_SIMILARITY = 0.85f

private data class EpisodeInfo(
    val season: String?,
    val episode: String
)

private val SUPPORTED_SUBTITLE_EXTS = setOf("srt", "ass", "ssa", "vtt")
private val SEPARATOR_REGEX = Regex("[\\s._\\-+&\\[\\]()（）【】{}]+")
private val YEAR_REGEX = Regex("(19|20)\\d{2}")

private val EPISODE_PATTERNS = listOf(
    Regex("s(\\d{1,2})e(\\d{1,3})"),
    Regex("第\\s*([0-9]+|[零〇一二三四五六七八九十百千两]+)\\s*[集话話期]"),
    Regex("\\b(?:ep|e)\\s*([0-9]{1,4})\\b"),
    Regex("\\b([0-9]{1,4})\\s*[集话話期]")
)

private val BILINGUAL_TAGS = setOf("中英", "双语", "中英双语", "中英双字", "简英", "繁英", "bilingual", "chs&eng")
private val TRADITIONAL_TAGS = setOf("cht", "tc", "big5", "繁", "繁体", "繁中", "繁体中文")
private val CHINESE_TAGS = setOf("zh", "chi", "zho", "chs", "cn", "sc", "简", "简体", "简中", "中文", "中字", "中文字幕")
private val ENGLISH_TAGS = setOf("en", "eng", "english", "英文", "英字")
private val JAPANESE_TAGS = setOf("ja", "jp", "jpn", "japanese", "日文", "日语", "日本語")
private val KOREAN_TAGS = setOf("ko", "kor", "korean", "韩文", "韩语", "한국어")
private val LANGUAGE_TAGS = BILINGUAL_TAGS + TRADITIONAL_TAGS + CHINESE_TAGS +
    ENGLISH_TAGS + JAPANESE_TAGS + KOREAN_TAGS +
    setOf("sub", "subs", "subtitle", "subtitles", "字幕")

private val QUALITY_TAGS = setOf(
    "480p", "720p", "1080p", "1440p", "2160p", "4k", "8k", "uhd",
    "hdr", "hdr10", "dv", "dolby", "vision", "atmos",
    "bluray", "blu", "ray", "bd", "br", "web", "webdl", "webrip", "dl",
    "hdtv", "remux", "x264", "x265", "h264", "h265", "hevc", "avc",
    "aac", "ac3", "eac3", "dts", "dd", "ddp", "flac", "mp3",
    "10bit", "8bit", "hfr", "60fps", "50fps", "30fps", "25fps", "24fps",
    "proper", "repack", "extended", "unrated", "imax", "remastered",
    "complete", "internal", "limited", "dubbed", "multi", "dual", "audio"
)

private val SAFE_TAGS = QUALITY_TAGS + LANGUAGE_TAGS

private fun normalizeTitle(raw: String): String =
    Normalizer.normalize(raw, Normalizer.Form.NFKC)
        .lowercase(Locale.ROOT)
        .replace(SEPARATOR_REGEX, " ")
        .trim()

private fun extractEpisode(normalized: String): EpisodeInfo? {
    for (pattern in EPISODE_PATTERNS) {
        val match = pattern.find(normalized) ?: continue
        val groups = match.groupValues
        return if (groups.size > 2) {
            EpisodeInfo(
                season = normalizeEpisode(groups[1]),
                episode = normalizeEpisode(groups[2])
            )
        } else {
            EpisodeInfo(season = null, episode = normalizeEpisode(groups[1]))
        }
    }
    return null
}

/** 去掉命中的集数标记，剩下的部分用来比较片名 */
private fun stripEpisode(normalized: String): String {
    for (pattern in EPISODE_PATTERNS) {
        val match = pattern.find(normalized) ?: continue
        return normalized.removeRange(match.range)
            .replace(Regex("\\s+"), " ")
            .trim()
    }
    return normalized
}

private fun normalizeEpisode(raw: String): String {
    raw.toLongOrNull()?.let { return it.toString() }
    return parseChineseNumber(raw)?.toString() ?: raw
}

private fun parseChineseNumber(text: String): Long? {
    var total = 0L
    var section = 0L
    var digit = 0L
    for (c in text) {
        when (c) {
            '十' -> {
                section += (if (digit == 0L) 1 else digit) * 10
                digit = 0
            }
            '百' -> {
                section += (if (digit == 0L) 1 else digit) * 100
                digit = 0
            }
            '千' -> {
                section += (if (digit == 0L) 1 else digit) * 1000
                digit = 0
            }
            '万' -> {
                total += (section + digit) * 10_000
                section = 0
                digit = 0
            }
            '零', '〇' -> digit = 0
            '一' -> digit = 1
            '二', '两' -> digit = 2
            '三' -> digit = 3
            '四' -> digit = 4
            '五' -> digit = 5
            '六' -> digit = 6
            '七' -> digit = 7
            '八' -> digit = 8
            '九' -> digit = 9
            else -> return null
        }
    }
    return total + section + digit
}

data class SubtitleInfo(
    val path: String,
    val name: String,
    val language: String?,
    val format: String,
    val isExact: Boolean = false,
    val matchScore: Int = 0
) {
    val mimeType: String
        get() = when (format.lowercase(Locale.ROOT)) {
            "ass", "ssa" -> "text/x-ssa"
            "vtt" -> "text/vtt"
            else -> "application/x-subrip"
        }
}
