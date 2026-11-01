package com.tvplayer.core.util

import com.tvplayer.core.model.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SmartFileNameComparatorTest {

    @Test
    fun numericSegmentsSortByValue() {
        val names = arrayOf(
            "01-A.mp4", "010-A.mp4", "011-A.mp4", "012-A.mp4",
            "02-A.mp4", "03-A.mp4", "04-A.mp4", "05-A.mp4",
            "06-A.mp4", "07-A.mp4", "08-A.mp4", "09-A.mp4"
        )
        assertEquals(
            listOf(
                "01-A.mp4", "02-A.mp4", "03-A.mp4", "04-A.mp4",
                "05-A.mp4", "06-A.mp4", "07-A.mp4", "08-A.mp4",
                "09-A.mp4", "010-A.mp4", "011-A.mp4", "012-A.mp4"
            ),
            sortedVideos(*names)
        )
    }

    @Test
    fun chineseNumeralsSortByValue() {
        assertEquals(
            listOf("第一集.mp4", "第二集.mp4", "第十集.mp4", "第十一集.mp4"),
            sortedVideos("第十一集.mp4", "第十集.mp4", "第二集.mp4", "第一集.mp4")
        )
    }

    @Test
    fun arabicAndChineseEpisodeNamesShareNumberOrder() {
        assertEquals(
            listOf("AAAA-01.MP4", "AAAA-第一集.mp4", "AAAA-第一集-视频.mp4"),
            sortedVideos("AAAA-第一集-视频.mp4", "AAAA-第一集.mp4", "AAAA-01.MP4")
        )
    }

    @Test
    fun directoriesComeBeforeVideos() {
        val result = listOf(
            FileInfo(path = "/01-A.mp4", name = "01-A.mp4", isDirectory = false, size = 0, modifiedTime = 0),
            FileInfo(path = "/Z目录", name = "Z目录", isDirectory = true, size = 0, modifiedTime = 0)
        ).sortedWith(SmartFileNameComparator).map { it.name }

        assertEquals(listOf("Z目录", "01-A.mp4"), result)
    }

    @Test
    fun onlyVideoExtensionsAreTreatedAsVideo() {
        assertTrue(video("01-A.MKV").isVideo)
        assertTrue(video("01-A.rmvb").isVideo)
        assertTrue(video("01-A.MP4").isVideo)
        assertFalse(video("01-A.srt").isVideo)
        assertFalse(video("01-A.jpg").isVideo)
        assertFalse(video("01-A.zip").isVideo)
    }

    private fun sortedVideos(vararg names: String): List<String> =
        names.map { video(it) }.sortedWith(SmartFileNameComparator).map { it.name }

    private fun video(name: String) =
        FileInfo(path = "/$name", name = name, isDirectory = false, size = 0, modifiedTime = 0)
}
