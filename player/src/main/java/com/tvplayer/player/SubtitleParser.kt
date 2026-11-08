package com.tvplayer.player

import java.util.Locale

data class SubtitleCue(
    val startMs: Long,
    val endMs: Long,
    val text: String,
)

/** 解析外挂字幕文本；主字幕仍由播放引擎渲染，副字幕用这里的 cue 在 UI 层叠加 */
object SubtitleParser {

    fun parse(format: String, content: String): List<SubtitleCue> {
        if (content.isBlank()) return emptyList()
        val cues = when (format.lowercase(Locale.ROOT)) {
            "ass", "ssa" -> parseAss(content)
            else -> parseTimedText(content)
        }
        return cues.filter { it.endMs > it.startMs && it.text.isNotBlank() }
            .sortedBy { it.startMs }
    }

    /** 二分查找当前时刻的 cue */
    fun cueAt(cues: List<SubtitleCue>, positionMs: Long): SubtitleCue? {
        var low = 0
        var high = cues.lastIndex
        while (low <= high) {
            val mid = (low + high) ushr 1
            val cue = cues[mid]
            when {
                positionMs < cue.startMs -> high = mid - 1
                positionMs >= cue.endMs -> low = mid + 1
                else -> return cue
            }
        }
        return null
    }

    private fun parseTimedText(content: String): List<SubtitleCue> {
        val blocks = normalize(content).split(BLOCK_SEPARATOR)
        val cues = ArrayList<SubtitleCue>(blocks.size)
        for (block in blocks) {
            val lines = block.lines().map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.isEmpty()) continue
            val first = lines.first()
            if (first.startsWith("WEBVTT", true) || first.startsWith("NOTE", true) ||
                first.startsWith("STYLE", true) || first.startsWith("REGION", true)
            ) {
                // VTT 头部块里也可能带 --> 之外的注释，整块跳过
                if (lines.none { it.contains("-->") }) continue
            }
            val timingIndex = lines.indexOfFirst { it.contains("-->") }
            if (timingIndex < 0) continue
            val match = TIMING_REGEX.find(lines[timingIndex]) ?: continue
            val start = toMillis(match.groupValues, 1) ?: continue
            val end = toMillis(match.groupValues, 5) ?: continue
            val text = lines.drop(timingIndex + 1).joinToString("\n")
            val cleaned = cleanText(text)
            if (cleaned.isNotBlank()) cues.add(SubtitleCue(start, end, cleaned))
        }
        return cues
    }

    private fun parseAss(content: String): List<SubtitleCue> {
        var inEvents = false
        var fields: List<String> = emptyList()
        val cues = ArrayList<SubtitleCue>()
        for (rawLine in normalize(content).lines()) {
            val line = rawLine.trim()
            if (line.isEmpty()) continue
            if (line.startsWith("[")) {
                inEvents = line.equals("[Events]", ignoreCase = true)
                continue
            }
            if (!inEvents) continue
            if (line.startsWith("Format:", ignoreCase = true)) {
                fields = line.substringAfter(':').split(',').map { it.trim().lowercase(Locale.ROOT) }
                continue
            }
            if (!line.startsWith("Dialogue:", ignoreCase = true)) continue
            if (fields.isEmpty()) continue
            val values = line.substringAfter(':').split(',', limit = fields.size)
            if (values.size != fields.size) continue
            val startIndex = fields.indexOf("start")
            val endIndex = fields.indexOf("end")
            val textIndex = fields.indexOf("text")
            if (startIndex < 0 || endIndex < 0 || textIndex < 0) continue
            val start = parseAssTime(values[startIndex]) ?: continue
            val end = parseAssTime(values[endIndex]) ?: continue
            val cleaned = cleanText(values[textIndex])
            if (cleaned.isNotBlank()) cues.add(SubtitleCue(start, end, cleaned))
        }
        return cues
    }

    private fun normalize(content: String): String =
        content.removePrefix("\uFEFF")
            .replace("\r\n", "\n")
            .replace('\r', '\n')

    private fun toMillis(groups: List<String>, offset: Int): Long? {
        val hours = groups.getOrNull(offset)?.takeIf { it.isNotEmpty() }?.toLongOrNull() ?: 0L
        val minutes = groups.getOrNull(offset + 1)?.toLongOrNull() ?: return null
        val seconds = groups.getOrNull(offset + 2)?.toLongOrNull() ?: return null
        val millis = groups.getOrNull(offset + 3)
            ?.padEnd(3, '0')
            ?.take(3)
            ?.toLongOrNull() ?: 0L
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + millis
    }

    private fun parseAssTime(raw: String): Long? {
        val match = ASS_TIME_REGEX.find(raw.trim()) ?: return null
        val hours = match.groupValues[1].toLongOrNull() ?: return null
        val minutes = match.groupValues[2].toLongOrNull() ?: return null
        val seconds = match.groupValues[3].toLongOrNull() ?: return null
        val centis = match.groupValues[4].padEnd(2, '0').take(2).toLongOrNull() ?: 0L
        return ((hours * 60 + minutes) * 60 + seconds) * 1000 + centis * 10
    }

    private fun cleanText(raw: String): String {
        var text = raw.replace(ASS_OVERRIDE_REGEX, "")
            .replace("\\N", "\n")
            .replace("\\n", "\n")
            .replace("\\h", " ")
        text = HTML_TAG_REGEX.replace(text, "")
        text = text.replace("&nbsp;", " ")
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
        return text.lines().joinToString("\n") { it.trim() }.trim()
    }

    private val BLOCK_SEPARATOR = Regex("\\n\\s*\\n")
    private val TIMING_REGEX = Regex(
        """(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[.,](\d{1,3})\s*-->\s*(?:(\d{1,2}):)?(\d{1,2}):(\d{2})[.,](\d{1,3})"""
    )
    private val ASS_TIME_REGEX = Regex("""(\d+):(\d{1,2}):(\d{2})[.:](\d{1,2})""")
    // Android 的 ICU 正则要求右花括号转义，否则整个 Pattern 编译失败
    private val ASS_OVERRIDE_REGEX = Regex("""\{[^}]*\}""")
    private val HTML_TAG_REGEX = Regex("""<[^>]+>""")
}
