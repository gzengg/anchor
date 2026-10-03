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
 * v1 只造库并按导出的 schema 校验 v1；v2 用例写入 v1 数据后跑迁移，验证
 * 「旧四表数据逐行保留 + 新表建立且为空 + 新表可写入」。
 * 迁移写错列名 / 漏字段 / 主键顺序不对，都会在这里失败。
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

    @Test
    fun migration1To2KeepsV1RowsAndCreatesMilestoneTable() {
        helper.createDatabase(TEST_DB, 1).use { db ->
            db.execSQL(
                "INSERT INTO check_in (date, note, createdAt) VALUES ('2024-05-01', '第一天', 1714521600000)",
            )
            db.execSQL(
                "INSERT INTO relapse (occurredAt, situation, emotions, triggers, note) " +
                    "VALUES (1714608000000, '深夜一个人', 'stress', '熬夜', '触发源')",
            )
            db.execSQL(
                "INSERT INTO urge_episode (startedAt, durationSec, peakIntensity, endIntensity, tool) " +
                    "VALUES (1714694400000, 600, 7, 3, 'BREATHING')",
            )
            db.execSQL(
                "INSERT INTO assessment_result (type, takenAt, totalScore, level, answersJson) " +
                    "VALUES ('CSBD', 1714780800000, 12, 'low', '[1,2,3]')",
            )
        }

        val migrated = helper.runMigrationsAndValidate(TEST_DB, 2, true, MIGRATION_1_2)
        migrated.use { db ->
            assertEquals(2, db.version)

            // 旧数据一行不能少、不能变。
            db.query("SELECT date, note, createdAt FROM check_in").use { cursor ->
                assertEquals(1, cursor.count)
                cursor.moveToFirst()
                assertEquals("2024-05-01", cursor.getString(0))
                assertEquals("第一天", cursor.getString(1))
                assertEquals(1714521600000L, cursor.getLong(2))
            }
            db.query("SELECT COUNT(*) FROM relapse").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT COUNT(*) FROM urge_episode").use { cursor ->
                cursor.moveToFirst()
                assertEquals(1, cursor.getInt(0))
            }
            db.query("SELECT level FROM assessment_result").use { cursor ->
                cursor.moveToFirst()
                assertEquals("low", cursor.getString(0))
            }

            // 新表：迁移后为空，且字段与主键可直接使用。
            db.query("SELECT COUNT(*) FROM milestone_achievement").use { cursor ->
                cursor.moveToFirst()
                assertEquals(0, cursor.getInt(0))
            }
            db.execSQL(
                "INSERT INTO milestone_achievement (milestoneDays, eraStartDate, achievedDate, recordedAt) " +
                    "VALUES (7, '2024-05-01', '2024-05-07', 1715040000000)",
            )
            db.query(
                "SELECT milestoneDays, eraStartDate, achievedDate FROM milestone_achievement",
            ).use { cursor ->
                cursor.moveToFirst()
                assertEquals(7, cursor.getInt(0))
                assertEquals("2024-05-01", cursor.getString(1))
                assertEquals("2024-05-07", cursor.getString(2))
            }
        }
    }

    private companion object {
        const val TEST_DB = "migration-test.db"
    }
}
