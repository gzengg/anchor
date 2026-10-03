package com.anchor.recovery

import android.app.Application
import com.anchor.recovery.data.db.AppDatabase
import com.anchor.recovery.data.repo.AnchorRepository

/**
 * 手写依赖装配（无 DI 框架）：数据库与仓库都是进程级单例，懒加载避免拖慢冷启动。
 */
class AnchorApplication : Application() {

    val database: AppDatabase by lazy { AppDatabase.build(this) }

    val repository: AnchorRepository by lazy { AnchorRepository(database) }
}
