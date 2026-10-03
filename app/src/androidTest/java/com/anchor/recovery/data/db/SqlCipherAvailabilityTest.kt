package com.anchor.recovery.data.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.anchor.recovery.data.db.crypto.KeystorePassphraseStore
import net.zetetic.database.sqlcipher.SupportOpenHelperFactory
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * P0-0 门禁：本机（真机 / 模拟器）能否真的打开 SQLCipher 加密库。
 *
 * 只用测试专用文件名（[TEST_DB_NAME] / [TEST_KEY_FILE_NAME]），不碰生产库
 * （`anchor.db` / `anchor-enc.db`）和正式口令文件（`anchor-key.bin`）。
 */
@RunWith(AndroidJUnit4::class)
class SqlCipherAvailabilityTest {

    private val context: Context = ApplicationProvider.getApplicationContext()

    @After
    fun tearDown() {
        listOf(TEST_DB_NAME, TEST_KEY_FILE_NAME).forEach { name ->
            val file = if (name == TEST_KEY_FILE_NAME) {
                File(context.filesDir, name)
            } else {
                context.getDatabasePath(name)
            }
            listOf("", "-wal", "-shm", "-journal").forEach { suffix ->
                File(file.path + suffix).delete()
            }
        }
    }

    /** AAR 只带 arm64-v8a / armeabi-v7a / x86 / x86_64，ABI 不匹配时这里直接失败。 */
    @Test
    fun sqlCipherNativeLibraryLoads() {
        System.loadLibrary("sqlcipher")
    }

    @Test
    fun encryptedDatabaseIsNotPlaintextOnDiskAndNeedsTheKey() {
        val passphrase = passphraseStore().passphrase()

        open(passphrase).let { db ->
            try {
                db.openHelper.writableDatabase.execSQL(
                    "CREATE TABLE gate_probe (id INTEGER PRIMARY KEY AUTOINCREMENT, value TEXT NOT NULL)",
                )
                db.openHelper.writableDatabase.execSQL("INSERT INTO gate_probe (value) VALUES ('磐石')")
            } finally {
                db.close()
            }
        }

        val file = context.getDatabasePath(TEST_DB_NAME)
        assertTrue("加密库文件没落盘：${file.path}", file.isFile)
        val raw = file.readBytes()
        assertNotEquals("文件头仍是明文 SQLite，加密没生效", "SQLite format 3\u0000", raw.decodeToString(0, 16))
        assertFalse("库里还能直接读到明文内容", iso8859(raw).contains("磐石"))

        open(passphrase).let { db ->
            try {
                db.query("SELECT value FROM gate_probe", null).use { cursor ->
                    assertTrue(cursor.moveToFirst())
                    assertEquals("磐石", cursor.getString(0))
                }
            } finally {
                db.close()
            }
        }

        val openedWithWrongKey = try {
            open("wrong-passphrase").let { db ->
                try {
                    db.openHelper.writableDatabase.query("SELECT count(*) FROM sqlite_master").close()
                } finally {
                    db.close()
                }
            }
            true
        } catch (e: Exception) {
            false
        }
        assertFalse("错误口令不应能打开加密库", openedWithWrongKey)
    }

    /** 口令是随机的 32 字节；同一个 Keystore 密钥重复调用必须拿到同一串，且文件里不存明文。 */
    @Test
    fun passphraseIsStableAndNeverStoredInPlaintext() {
        val first = passphraseStore().passphrase()
        val keyFile = File(context.filesDir, TEST_KEY_FILE_NAME)

        assertEquals(64, first.length)
        assertTrue("口令必须是十六进制（要拼进 SQL 字面量）", first.all { it in "0123456789abcdef" })
        assertEquals(first, passphraseStore().passphrase())

        assertFalse("口令文件里出现了明文口令", iso8859(keyFile.readBytes()).contains(first))
    }

    private fun passphraseStore() = KeystorePassphraseStore(File(context.filesDir, TEST_KEY_FILE_NAME))

    private fun open(passphrase: String): AppDatabase =
        Room.databaseBuilder(context, AppDatabase::class.java, TEST_DB_NAME)
            .allowMainThreadQueries()
            .openHelperFactory(SupportOpenHelperFactory(passphrase.toByteArray(Charsets.UTF_8)))
            .build()

    private fun iso8859(bytes: ByteArray) = bytes.toString(Charsets.ISO_8859_1)

    private companion object {
        const val TEST_DB_NAME = "sqlcipher-gate-test.db"
        const val TEST_KEY_FILE_NAME = "sqlcipher-gate-test-key.bin"
    }
}
