package com.anchor.recovery.data.db

import androidx.room.testing.MigrationTestHelper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * 迁移测试骨架（P0-1）：打通「KSP 导出 schema JSON → 资产 → MigrationTestHelper」这条路。
 *
 * v1 还没有迁移，所以这里只验证「造出 v1 库 + 按导出的 schema 校验 v1」。
 * 方案 P2 加 `milestone_achievement` 表时，在本类补 `createDatabase(TEST_DB, 1)` → 写数据 →
 * `runMigrationsAndValidate(TEST_DB, 2, true)` 的用例：迁移写错列名 / 漏字段会在这里失败。
 *
 * schema JSON 从「debug 变体资产」读（见 `app/build.gradle.kts` 的 sourceSets 注释），所以本测试
 * 能本地跑、不需要真机，也因此只能放在 `src/testDebug`（release 变体不打包 schema）：
 * SQLCipher / Keystore 相关的真机验证在 `app/src/androidTest`。
 *
 * 用测试专用库名，不碰生产库（`anchor.db` / `anchor-enc.db`）。
 */
@RunWith(RobolectricTestRunner::class)
class AppDatabaseMigrationTest {

    @get:Rule
    val helper = MigrationTestHelper(InstrumentationRegistry.getInstrumentation(), AppDatabase::class.java)

    @Test
    fun version1SchemaMatchesExportedSchema() {
        helper.createDatabase(TEST_DB, 1).use { database ->
            assertEquals(1, database.version)
        }
        helper.runMigrationsAndValidate(TEST_DB, 1, true).close()
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
