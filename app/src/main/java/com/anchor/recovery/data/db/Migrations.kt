package com.anchor.recovery.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/**
 * v1 → v2：新增里程碑达成表（P2 徽章墙）。
 *
 * 只加表、不动 v1 的四张旧表，用户既有记录原样保留；`AppDatabaseMigrationTest` 会写入 v1
 * 数据后跑这条迁移并逐列校验。建表语句必须与 Room 依 `MilestoneAchievementEntity` 生成的
 * v2 schema 等价（列名、NOT NULL、主键顺序），否则迁移测试会以 schema mismatch 失败。
 */
val MIGRATION_1_2: Migration = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `milestone_achievement` (" +
                "`milestoneDays` INTEGER NOT NULL, " +
                "`eraStartDate` TEXT NOT NULL, " +
                "`achievedDate` TEXT NOT NULL, " +
                "`recordedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`milestoneDays`, `eraStartDate`))",
        )
    }
}

/** 全部迁移，按版本升序；装配在 [AnchorDatabaseFactory] 的两个 builder 上。 */
val ALL_MIGRATIONS: Array<Migration> = arrayOf(MIGRATION_1_2)
