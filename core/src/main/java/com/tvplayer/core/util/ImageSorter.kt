package com.tvplayer.core.util

import com.tvplayer.core.model.FileInfo

enum class ImageSortMode(val label: String) {
    DIRECTORY("目录排序"),
    TIME("时间排序"),
    NAME("文件名排序"),
}

/** 图片库排序：目录分组、修改时间、文件名三种模式，名称比较复用文件名智能排序 */
object ImageSorter {

    fun sort(images: List<FileInfo>, mode: ImageSortMode): List<FileInfo> = when (mode) {
        ImageSortMode.DIRECTORY -> images.sortedWith(directoryComparator)
        ImageSortMode.TIME -> images.sortedWith(timeComparator)
        ImageSortMode.NAME -> images.sortedWith(nameComparator)
    }

    private val directoryComparator = Comparator<FileInfo> { a, b ->
        val byDirectory = compareDirectoryPaths(parentPath(a.path), parentPath(b.path))
        if (byDirectory != 0) {
            byDirectory
        } else {
            val byName = SmartFileNameComparator.compareNames(a.name, b.name)
            if (byName != 0) byName else a.path.compareTo(b.path)
        }
    }

    private val timeComparator = Comparator<FileInfo> { a, b ->
        val byTime = b.modifiedTime.compareTo(a.modifiedTime)
        if (byTime != 0) byTime else a.path.compareTo(b.path)
    }

    private val nameComparator = Comparator<FileInfo> { a, b ->
        val byName = SmartFileNameComparator.compareNames(a.name, b.name)
        if (byName != 0) byName else a.path.compareTo(b.path)
    }

    private fun parentPath(path: String): String =
        path.substringBeforeLast('/', "/").ifEmpty { "/" }

    /** 逐段比较目录，段内按智能排序，空路径段（根目录）不参与比较 */
    private fun compareDirectoryPaths(a: String, b: String): Int {
        val aParts = a.split('/').filter { it.isNotEmpty() }
        val bParts = b.split('/').filter { it.isNotEmpty() }
        for (i in 0 until minOf(aParts.size, bParts.size)) {
            val result = SmartFileNameComparator.compareNames(aParts[i], bParts[i])
            if (result != 0) return result
        }
        return aParts.size.compareTo(bParts.size)
    }
}
