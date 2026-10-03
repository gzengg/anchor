package com.anchor.recovery.data.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.anchor.recovery.data.db.dao.AssessmentResultDao
import com.anchor.recovery.data.db.dao.CheckInDao
import com.anchor.recovery.data.db.dao.RelapseDao
import com.anchor.recovery.data.db.dao.UrgeEpisodeDao
import com.anchor.recovery.data.db.entity.AssessmentResultEntity
import com.anchor.recovery.data.db.entity.CheckInEntity
import com.anchor.recovery.data.db.entity.RelapseEntity
import com.anchor.recovery.data.db.entity.UrgeEpisodeEntity

/**
 * 本地数据库（v1）。数据全部保存在本机，不做云同步；exportSchema=false 表示不产出 schema JSON。
 */
@Database(
    entities = [
        CheckInEntity::class,
        RelapseEntity::class,
        UrgeEpisodeEntity::class,
        AssessmentResultEntity::class,
    ],
    version = 1,
    exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun checkInDao(): CheckInDao

    abstract fun relapseDao(): RelapseDao

    abstract fun urgeEpisodeDao(): UrgeEpisodeDao

    abstract fun assessmentResultDao(): AssessmentResultDao

    companion object {
        const val NAME = "anchor.db"

        fun build(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, NAME).build()
    }
}
