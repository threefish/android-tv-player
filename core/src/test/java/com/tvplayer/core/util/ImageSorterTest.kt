package com.tvplayer.core.util

import com.tvplayer.core.model.FileInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ImageSorterTest {

    @Test
    fun nameModeSortsNumbersByValue() {
        val images = listOf(
            image("/a/10.jpg", "10.jpg"),
            image("/a/2.jpg", "2.jpg"),
            image("/a/01.jpg", "01.jpg")
        )

        assertEquals(
            listOf("01.jpg", "2.jpg", "10.jpg"),
            ImageSorter.sort(images, ImageSortMode.NAME).map { it.name }
        )
    }

    @Test
    fun nameModeSortsChineseNumeralsByValue() {
        val images = listOf(
            image("/a/第十一集.jpg", "第十一集.jpg"),
            image("/a/第十集.jpg", "第十集.jpg"),
            image("/a/第二集.jpg", "第二集.jpg"),
            image("/a/第一集.jpg", "第一集.jpg")
        )

        assertEquals(
            listOf("第一集.jpg", "第二集.jpg", "第十集.jpg", "第十一集.jpg"),
            ImageSorter.sort(images, ImageSortMode.NAME).map { it.name }
        )
    }

    @Test
    fun nameModeHandlesArabicAndChineseEpisodeNames() {
        val images = listOf(
            image("/a/AAAA-第一集-视频.mp4.jpg", "AAAA-第一集-视频.mp4.jpg"),
            image("/a/AAAA-第一集.mp4.jpg", "AAAA-第一集.mp4.jpg"),
            image("/a/AAAA-01.MP4.jpg", "AAAA-01.MP4.jpg")
        )

        assertEquals(
            listOf("AAAA-01.MP4.jpg", "AAAA-第一集.mp4.jpg", "AAAA-第一集-视频.mp4.jpg"),
            ImageSorter.sort(images, ImageSortMode.NAME).map { it.name }
        )
    }

    @Test
    fun directoryModeGroupsByParent() {
        val images = listOf(
            image("/video/第2章/b.jpg", "b.jpg"),
            image("/video/第10章/a.jpg", "a.jpg"),
            image("/video/第1章/b.jpg", "b.jpg"),
            image("/video/第1章/a.jpg", "a.jpg"),
            image("/video/第2章/a.jpg", "a.jpg")
        )

        assertEquals(
            listOf(
                "/video/第1章/a.jpg",
                "/video/第1章/b.jpg",
                "/video/第2章/a.jpg",
                "/video/第2章/b.jpg",
                "/video/第10章/a.jpg"
            ),
            ImageSorter.sort(images, ImageSortMode.DIRECTORY).map { it.path }
        )
    }

    @Test
    fun timeModePutsNewestFirst() {
        val images = listOf(
            image("/a/old.jpg", "old.jpg", modifiedTime = 100),
            image("/a/new.jpg", "new.jpg", modifiedTime = 300),
            image("/a/mid.jpg", "mid.jpg", modifiedTime = 200)
        )

        assertEquals(
            listOf("new.jpg", "mid.jpg", "old.jpg"),
            ImageSorter.sort(images, ImageSortMode.TIME).map { it.name }
        )
    }

    @Test
    fun onlyDecodableExtensionsAreImages() {
        assertTrue(image("/a/photo.JPG", "photo.JPG").isImage)
        assertTrue(image("/a/photo.jpeg", "photo.jpeg").isImage)
        assertTrue(image("/a/photo.webp", "photo.webp").isImage)
        assertTrue(image("/a/photo.png", "photo.png").isImage)
        assertTrue(image("/a/photo.bmp", "photo.bmp").isImage)
        assertTrue(image("/a/photo.gif", "photo.gif").isImage)
        assertFalse(image("/a/photo.heic", "photo.heic").isImage)
        assertFalse(image("/a/photo.avif", "photo.avif").isImage)
        assertFalse(image("/a/photo.svg", "photo.svg").isImage)
        assertFalse(
            FileInfo(
                path = "/a/photo.jpg",
                name = "photo.jpg",
                isDirectory = true,
                size = 0,
                modifiedTime = 0
            ).isImage
        )
    }

    private fun image(path: String, name: String, modifiedTime: Long = 0) = FileInfo(
        path = path,
        name = name,
        isDirectory = false,
        size = 0,
        modifiedTime = modifiedTime
    )
}
