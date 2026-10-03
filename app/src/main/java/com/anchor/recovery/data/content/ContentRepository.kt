package com.anchor.recovery.data.content

import android.content.res.AssetManager
import androidx.annotation.StringRes
import com.anchor.recovery.R
import com.anchor.recovery.core.content.ContentLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * 知识库加载结果：成功时 [library] 非空；失败时 [errorRes] 给出可直接展示的原因资源，
 * [errorArgs] 是它的格式参数（这里是异常说明）。
 *
 * data 层不持 Context，所以只回传 `@StringRes + args`，由消费端（ui/library 下的页面）
 * 用 `stringResource(errorRes, *errorArgs)` 渲染。
 *
 * 内容资产是构建期已自检的离线 JSON，正常不会失败；这里做兜底是为了让内容瑕疵降级成
 * 一个错误卡片，而不是启动崩溃。
 */
data class ContentSnapshot(
    val library: ContentLibrary?,
    @StringRes val errorRes: Int? = null,
    val errorArgs: List<String> = emptyList(),
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
        ContentSnapshot(
            library = null,
            errorRes = R.string.legal_content_load_failed,
            errorArgs = listOf(error.message ?: error::class.java.simpleName),
        )
    }

    private fun readAsset(path: String): String =
        assets.open(path).bufferedReader(Charsets.UTF_8).use { it.readText() }

    companion object {
        const val ARTICLES_ASSET = "content/articles.json"
        const val INDEX_ASSET = "content/index.json"

        /** 纯函数入口：解析失败返回带 [ContentSnapshot.errorRes] 的快照，不抛异常。 */
        fun fromStrings(articlesJson: String, indexJson: String): ContentSnapshot = try {
            ContentSnapshot(ContentLibrary.parse(articlesJson, indexJson))
        } catch (error: Exception) {
            ContentSnapshot(
                library = null,
                errorRes = R.string.legal_content_format_error,
                errorArgs = listOf(error.message ?: error::class.java.simpleName),
            )
        }
    }
}
