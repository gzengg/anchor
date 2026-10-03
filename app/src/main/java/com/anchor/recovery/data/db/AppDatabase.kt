package com.anchor.recovery.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.anchor.recovery.data.db.dao.AssessmentResultDao
import com.anchor.recovery.data.db.dao.CheckInDao
import com.anchor.recovery.data.db.dao.MilestoneAchievementDao
import com.anchor.recovery.data.db.dao.RelapseDao
import com.anchor.recovery.data.db.dao.UrgeEpisodeDao
import com.anchor.recovery.data.db.entity.AssessmentResultEntity
import com.anchor.recovery.data.db.entity.CheckInEntity
import com.anchor.recovery.data.db.entity.MilestoneAchievementEntity
import com.anchor.recovery.data.db.entity.RelapseEntity
import com.anchor.recovery.data.db.entity.UrgeEpisodeEntity

/**
 * 本地数据库。数据全部保存在本机，不做云同步。
 *
 * 应用从 v0.2.0 起实际打开的是 [ENCRYPTED_NAME]（SQLCipher 加密库），装配在 [AnchorDatabaseFactory]：
 * [NAME] 只是 v1 的明文库文件名，与 [ENCRYPTED_NAME] 同目录，打开 [NAME] 等于降级成明文。
 *
 * schema 版本与 app 版本无关：v2 只多了 `milestone_achievement`（见 [MIGRATION_1_2]）。
 *
 * `exportSchema = true`：schema JSON 产出到 `app/schemas/`（随 git 入库），
 * 供迁移测试（`MigrationTestHelper`）与后续加表版本对比使用。
 */
@Database(
    entities = [
        CheckInEntity::class,
        RelapseEntity::class,
        UrgeEpisodeEntity::class,
        AssessmentResultEntity::class,
        MilestoneAchievementEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun checkInDao(): CheckInDao

    abstract fun relapseDao(): RelapseDao

    abstract fun urgeEpisodeDao(): UrgeEpisodeDao

    abstract fun assessmentResultDao(): AssessmentResultDao

    abstract fun milestoneAchievementDao(): MilestoneAchievementDao

    companion object {
        /** v1 明文库文件名。只在迁移与降级路径上出现。 */
        const val NAME = "anchor.db"

        /** v0.2.0 起的加密库文件名。 */
        const val ENCRYPTED_NAME = "anchor-enc.db"
    }
}
