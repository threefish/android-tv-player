package com.tvplayer.core.model

data class FileInfo(
    val id: Long = 0,
    val path: String,
    val name: String,
    val isDirectory: Boolean,
    val size: Long,
    val modifiedTime: Long,
    val parentId: Long? = null,
    val isDeleted: Boolean = false,
    val deletedTime: Long? = null,
    val mimeType: String? = null,
    val duration: Long? = null,
    val width: Int? = null,
    val height: Int? = null
) {
    val isVideo: Boolean
        get() = mimeType?.startsWith("video/") == true ||
                name.substringAfterLast('.', "").lowercase() in VIDEO_EXTENSIONS

    /** 只认扩展名：库里 mimeType 常常缺失，且 heic/avif/svg 在 API 23 上无法解码 */
    val isImage: Boolean
        get() = !isDirectory && name.substringAfterLast('.', "").lowercase() in IMAGE_EXTENSIONS

    val isAudio: Boolean
        get() = !isDirectory && (mimeType?.startsWith("audio/") == true ||
                name.substringAfterLast('.', "").lowercase() in AUDIO_EXTENSIONS)

    companion object {
        private val VIDEO_EXTENSIONS = setOf(
            "mp4", "mkv", "avi", "mov", "wmv", "flv", "webm",
            "ts", "m2ts", "mts", "mpg", "mpeg", "m4v", "rmvb", "rm",
            "3gp", "3g2", "vob", "ogv", "f4v", "asf", "divx"
        )

        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "bmp", "gif")

        private val AUDIO_EXTENSIONS = setOf(
            "mp3", "flac", "wav", "aac", "m4a", "ogg", "oga", "opus",
            "wma", "ape", "wv", "aiff", "aif", "amr", "mka"
        )
    }
}
