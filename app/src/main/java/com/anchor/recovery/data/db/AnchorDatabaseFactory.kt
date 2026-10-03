package com.anchor.recovery.data.db

import android.content.Context
import android.util.Log
import androidx.room.Room
import com.anchor.recovery.core.storage.DatabaseBootstrapAction
import com.anchor.recovery.core.storage.compareTableCounts
import com.anchor.recovery.core.storage.decideDatabaseBootstrap
import com.anchor.recovery.data.db.crypto.DatabaseKeyUnavailableException
import com.anchor.recovery.data.db.crypto.DatabasePassphraseStore
import net.zetetic.database.sqlcipher.SQLiteDatabase
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import java.io.File

/**
 * 加密数据库的打开路径（P0-2）。三种启动情形：
 *
 * 1. 全新安装 → 直接建加密库；
 * 2. 只有 v1 明文库 → 用 SQLCipher 自带的 `sqlcipher_export` 把数据搬进加密库，
 *    逐表核对行数通过后才改名成正式库，最后删掉明文残留（含 `-wal` / `-shm`）；
 * 3. 已有加密库 → 直接用 Keystore 口令打开，顺手清掉明文残留。
 *
 * 导出写在 `<库名>.migrating` 临时文件里：所以「加密库文件存在」就等于「迁移已核对通过」，
 * 半成品不会冒充正式库。任何一步失败都不动明文库，本次退回明文库继续跑（数据优先于好看）。
 */
object AnchorDatabaseFactory {

    /**
     * @throws DatabaseKeyUnavailableException 口令失效且没有明文库可退回
     * @throws IllegalStateException 加密库已存在但 `libsqlcipher.so` 加载失败
     */
    fun open(context: Context, passphraseStore: DatabasePassphraseStore): AppDatabase {
        val appContext = context.applicationContext
        val legacyFile = appContext.getDatabasePath(AppDatabase.NAME)
        val encryptedFile = appContext.getDatabasePath(AppDatabase.ENCRYPTED_NAME)
        deleteWithSidecars(tempFileFor(encryptedFile))

        val passphrase = try {
            passphraseStore.passphrase()
        } catch (e: DatabaseKeyUnavailableException) {
            // 口令没了，加密库再也解不开：只有明文库还能救数据；连明文库都没有就宁可报错，
            // 也不能静默开一个空库让用户以为数据丢了。
            Log.e(TAG, "数据库口令不可用", e)
            if (legacyFile.isFile) return openLegacyPlaintext(appContext)
            throw e
        }

        if (!loadSqlCipherLibrary()) {
            Log.e(TAG, "libsqlcipher.so 加载失败，改用明文库")
            check(!encryptedFile.isFile) { "加密库已存在，但 libsqlcipher.so 加载失败，无法打开" }
            return openLegacyPlaintext(appContext)
        }

        val action = decideDatabaseBootstrap(
            legacyPlaintextExists = legacyFile.isFile,
            encryptedExists = encryptedFile.isFile,
        )
        return when (action) {
            DatabaseBootstrapAction.MIGRATE_LEGACY -> {
                val migrated = try {
                    migratePlaintextToEncrypted(legacyFile, encryptedFile, passphrase)
                    true
                } catch (e: Exception) {
                    Log.e(TAG, "明文库迁移为加密库失败，本次继续用明文库", e)
                    false
                }
                if (!migrated) return openLegacyPlaintext(appContext)

                Log.i(TAG, "明文库已迁移为加密库，删除明文残留")
                deleteWithSidecars(legacyFile)
                openEncrypted(appContext, passphrase)
            }

            DatabaseBootstrapAction.OPEN_ENCRYPTED -> {
                deleteWithSidecars(legacyFile)
                openEncrypted(appContext, passphrase)
            }
        }
    }

    /**
     * 明文库 → 加密库。`sqlcipher_export` 边读明文边写加密，导到临时文件后逐表核对行数，
     * 过了才改名成正式加密库；抛出时明文库原样未动。
     */
    private fun migratePlaintextToEncrypted(legacyFile: File, encryptedFile: File, passphrase: String) {
        val tempFile = tempFileFor(encryptedFile)
        try {
            val mismatches = openDatabase(legacyFile.path, PLAINTEXT_PASSWORD).use { legacyDatabase ->
                legacyDatabase.rawExecSQL(
                    "ATTACH DATABASE ${asSqlLiteral(tempFile.path)} AS encrypted KEY ${asSqlLiteral(passphrase)}",
                )
                legacyDatabase.rawExecSQL("SELECT sqlcipher_export('encrypted')")
                legacyDatabase.rawExecSQL("DETACH DATABASE encrypted")

                compareTableCounts(
                    legacy = readRowCounts(legacyDatabase),
                    encrypted = openDatabase(tempFile.path, passphrase).use(::readRowCounts),
                )
            }
            check(mismatches.isEmpty()) { "导出后行数不一致，放弃迁移：$mismatches" }

            if (encryptedFile.exists() && !encryptedFile.delete()) {
                error("无法替换旧的加密库：${encryptedFile.path}")
            }
            check(tempFile.renameTo(encryptedFile)) {
                "无法把临时库改名成加密库：${tempFile.path} → ${encryptedFile.path}"
            }
        } catch (e: Throwable) {
            deleteWithSidecars(tempFile)
            throw e
        }
    }

    /** 每张表的行数，表名取自 `sqlite_master`（不写死表名，加表后核对自动覆盖）。 */
    private fun readRowCounts(database: SQLiteDatabase): Map<String, Long> {
        val tables = mutableListOf<String>()
        database.rawQuery("SELECT name FROM sqlite_master WHERE type = 'table' ORDER BY name", emptyArray<String>())
            .use { cursor ->
                while (cursor.moveToNext()) tables += cursor.getString(0)
            }
        return tables.associateWith { table ->
            database.rawQuery("SELECT COUNT(*) FROM ${asSqlIdentifier(table)}", emptyArray<String>()).use { cursor ->
                if (cursor.moveToFirst()) cursor.getLong(0) else 0L
            }
        }
    }

    /** 只读打开：迁移过程不许改动明文库。 */
    private fun openDatabase(path: String, password: String): SQLiteDatabase =
        SQLiteDatabase.openDatabase(path, password, null, SQLiteDatabase.OPEN_READONLY, null, null)

    private fun openEncrypted(context: Context, passphrase: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.ENCRYPTED_NAME)
            .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8)))
            .build()

    /** 降级路径：迁移失败或本地库缺失时，先让 app 能用旧库跑起来。 */
    private fun openLegacyPlaintext(context: Context): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, AppDatabase.NAME).build()

    /**
     * sqlcipher-android 4.x 不自己加载本地库（AAR 里没有任何 `System.loadLibrary` 调用），
     * 必须由 app 先载入 `libsqlcipher.so`（AAR 只带了 arm64-v8a / armeabi-v7a / x86 / x86_64 四种）。
     */
    @Synchronized
    private fun loadSqlCipherLibrary(): Boolean {
        if (sqlCipherLoaded) return true
        return try {
            System.loadLibrary(SQLCIPHER_LIBRARY)
            sqlCipherLoaded = true
            true
        } catch (e: UnsatisfiedLinkError) {
            Log.e(TAG, "加载 $SQLCIPHER_LIBRARY 失败", e)
            false
        }
    }

    private fun tempFileFor(file: File): File = File(file.path + TEMP_SUFFIX)

    /** 库文件连着 `-wal` / `-shm` / `-journal` 一起删：留着 `-wal` 就等于留着明文页面。 */
    private fun deleteWithSidecars(file: File) {
        val files = listOf(file) + SIDECAR_SUFFIXES.map { File(file.path + it) }
        files.forEach { sidecar ->
            if (sidecar.exists() && !sidecar.delete()) Log.w(TAG, "删除文件失败：${sidecar.path}")
        }
    }

    private fun asSqlLiteral(value: String): String = "'" + value.replace("'", "''") + "'"

    private fun asSqlIdentifier(value: String): String = "\"" + value.replace("\"", "\"\"") + "\""

    private const val TAG = "AnchorDatabase"
    private const val SQLCIPHER_LIBRARY = "sqlcipher"
    private const val PLAINTEXT_PASSWORD = ""
    private const val TEMP_SUFFIX = ".migrating"

    private val SIDECAR_SUFFIXES = listOf("-wal", "-shm", "-journal")

    @Volatile
    private var sqlCipherLoaded = false
}
