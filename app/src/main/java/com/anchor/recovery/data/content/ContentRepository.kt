package com.anchor.recovery.data.content

import android.content.res.AssetManager
import com.anchor.recovery.core.content.ContentLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 知识库加载结果：成功时 [library] 非空，失败时 [error] 是可直接展示给用户的原因。
 *
 * 内容资产是构建期已自检的离线 JSON，正常不会失败；这里做兜底是为了让内容瑕疵降级成
 * 一个错误卡片，而不是启动崩溃。
 */
data class ContentSnapshot(
    val library: ContentLibrary?,
    val error: String? = null,
) {
    val isReady: Boolean get() = library != null
}

/**
 * 离线知识库仓库：首次访问时读 assets（IO 线程），之后进程内缓存。
 */
class ContentRepository(private val assets: AssetManager) {

    @Volatile
    private var cached: ContentSnapshot? = null

    suspend fun snapshot(): ContentSnapshot {
        cached?.let { return it }
        val loaded = withContext(Dispatchers.IO) { readAssets() }
        cached = loaded
        return loaded
    }

    private fun readAssets(): ContentSnapshot = try {
        fromStrings(readAsset(ARTICLES_ASSET), readAsset(INDEX_ASSET))
    } catch (error: Exception) {
        ContentSnapshot(null, "内容加载失败：${error.message ?: error::class.java.simpleName}")
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val ARTICLES_ASSET = "content/articles.json"
        const val INDEX_ASSET = "content/index.json"

        /** 纯函数入口：解析失败返回 [ContentSnapshot.error]，不抛异常。 */
        fun fromStrings(articlesJson: String, indexJson: String): ContentSnapshot = try {
            ContentSnapshot(ContentLibrary.parse(articlesJson, indexJson))
        } catch (error: Exception) {
            ContentSnapshot(null, "内容格式错误：${error.message ?: error::class.java.simpleName}")
        }
    }
}
