package com.tvplayer.webdav

import com.tvplayer.core.model.FileInfo
import com.tvplayer.core.model.WebDavConfig
import okhttp3.Credentials
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.InputStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.xml.parsers.DocumentBuilderFactory
import java.io.ByteArrayInputStream
import android.util.Log

class WebDavClient(private val config: WebDavConfig) {
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val baseUrl = config.url.trimEnd('/')
    private val basePath = try { java.net.URL(baseUrl).path } catch (e: Exception) { "" }

    suspend fun list(path: String): List<FileInfo> {
        val url = "$baseUrl/${path.trimStart('/')}"
        Log.d("WebDavClient", "PROPFIND $url")
        val propfindBody = """
            <?xml version="1.0" encoding="utf-8"?>
            <D:propfind xmlns:D="DAV:">
                <D:prop>
                    <D:displayname/>
                    <D:getcontentlength/>
                    <D:getlastmodified/>
                    <D:getcontenttype/>
                    <D:resourcetype/>
                </D:prop>
            </D:propfind>
        """.trimIndent()

        val request = Request.Builder()
            .url(url)
            .header("Depth", "1")
            .header("Authorization", Credentials.basic(config.username, config.password))
            .method("PROPFIND", propfindBody.toRequestBody("application/xml".toMediaType()))
            .build()

        val body = withContext(Dispatchers.IO) {
            val response = client.newCall(request).execute()
            Log.d("WebDavClient", "PROPFIND response: ${response.code}")
            if (!response.isSuccessful) {
                throw Exception("PROPFIND failed: ${response.code}")
            }
            // 没有响应体不能让调用方误以为该目录是空的，否则同步会误删整棵本地缓存
            response.body?.string() ?: throw Exception("PROPFIND empty body")
        }
        return parsePropfindResponse(body, path)
    }

    private fun parsePropfindResponse(xml: String, parentPath: String): List<FileInfo> {
        val files = mutableListOf<FileInfo>()
        try {
            val factory = DocumentBuilderFactory.newInstance()
            factory.isNamespaceAware = true
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(ByteArrayInputStream(xml.toByteArray()))
            
            val responses = doc.getElementsByTagNameNS("DAV:", "response")
            for (i in 0 until responses.length) {
                val response = responses.item(i) as org.w3c.dom.Element
                
                val hrefNodes = response.getElementsByTagNameNS("DAV:", "href")
                if (hrefNodes.length == 0) continue
                val href = hrefNodes.item(0).textContent ?: continue
                
                // 只用百分号解码；URLDecoder 是表单解码，会把路径里的字面量 + 变成空格
                val decodedHref = android.net.Uri.decode(href)
                var normalizedPath = decodedHref
                if (basePath.isNotEmpty() && normalizedPath.startsWith(basePath)) {
                    normalizedPath = normalizedPath.substring(basePath.length)
                }
                if (!normalizedPath.startsWith("/")) {
                    normalizedPath = "/$normalizedPath"
                }
                
                if (normalizedPath == parentPath || normalizedPath.isEmpty() || normalizedPath == "/") {
                    if (normalizedPath == parentPath) continue
                    if (normalizedPath == "/" && parentPath == "/") continue
                }

                val propNodes = response.getElementsByTagNameNS("DAV:", "prop")
                if (propNodes.length == 0) continue

                val prop = propNodes.item(0) as org.w3c.dom.Element
                val displayName = getTagValue(prop, "displayname") ?: normalizedPath.substringAfterLast("/").trimEnd('/')
                val contentLength = getTagValue(prop, "getcontentlength")?.toLongOrNull() ?: 0
                val lastModified = getTagValue(prop, "getlastmodified")
                val contentType = getTagValue(prop, "getcontenttype")
                val resourceType = getTagValue(prop, "resourcetype")
                
                val isDirectory = resourceType?.contains("collection") == true || normalizedPath.endsWith("/")
                val modifiedTime = parseDate(lastModified) ?: 0

                val finalPath = normalizedPath.trimEnd('/')
                if (finalPath.isEmpty()) continue

                files.add(FileInfo(
                    id = 0,
                    path = finalPath,
                    name = displayName,
                    isDirectory = isDirectory,
                    size = contentLength,
                    modifiedTime = modifiedTime,
                    mimeType = contentType
                ))
            }
        } catch (e: Exception) {
            Log.e("WebDavClient", "parsePropfindResponse failed", e)
            // 解析失败必须抛出：返回空列表会被同步当成「远端目录已空」而误删缓存
            throw e
        }
        return files
    }

    private fun getTagValue(parent: org.w3c.dom.Node, tagName: String): String? {
        val nodes = parent.childNodes
        for (i in 0 until nodes.length) {
            val node = nodes.item(i)
            if (node.localName == tagName) {
                return node.textContent
            }
        }
        return null
    }

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr == null) return null
        return try {
            val format = java.text.SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", java.util.Locale.ENGLISH)
            format.parse(dateStr)?.time
        } catch (e: Exception) {
            null
        }
    }

    suspend fun getFileInfo(path: String): FileInfo? {
        return try {
            val files = list(path)
            files.find { it.path == path }
        } catch (e: Exception) {
            null
        }
    }

    suspend fun download(path: String): InputStream? {
        val url = "$baseUrl/${path.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .get()
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        return if (response.isSuccessful) response.body?.byteStream() else null
    }

    suspend fun move(sourcePath: String, targetPath: String): Boolean {
        val url = "$baseUrl/${sourcePath.trimStart('/')}"
        val destination = "$baseUrl/${targetPath.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .header("Destination", destination)
            .header("Overwrite", "F")
            .method("MOVE", null)
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        return response.isSuccessful
    }

    suspend fun delete(path: String): Boolean {
        val url = "$baseUrl/${path.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .delete()
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        return response.isSuccessful
    }

    suspend fun createDirectory(path: String): Boolean {
        val url = "$baseUrl/${path.trimStart('/')}"
        val request = Request.Builder()
            .url(url)
            .header("Authorization", Credentials.basic(config.username, config.password))
            .method("MKCOL", null)
            .build()

        val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
        return response.isSuccessful
    }

    suspend fun testConnection(): Boolean {
        return try {
            Log.d("WebDavClient", "Testing connection to: $baseUrl")
            val result = list("/")
            Log.d("WebDavClient", "list() returned ${result.size} items")
            true
        } catch (e: Exception) {
            Log.e("WebDavClient", "testConnection failed", e)
            false
        }
    }
}
