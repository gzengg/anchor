package com.anchor.recovery.data.db.crypto

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.io.File
import java.io.IOException
import java.security.GeneralSecurityException
import java.security.KeyStore
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Keystore 里裹着口令的密钥没了 / 口令文件坏了：加密库已经打不开。 */
class DatabaseKeyUnavailableException(message: String, cause: Throwable? = null) :
    IllegalStateException(message, cause)

/**
 * SQLCipher 的口令来源。
 *
 * 返回值是十六进制字符串（只含 `0-9a-f`）：拼进 `ATTACH DATABASE ... KEY '<口令>'`
 * 这种 SQL 字面量时不需要转义；Room 打开库时用的是同一个字符串，两边派生出的密钥才一致。
 */
interface DatabasePassphraseStore {
    fun passphrase(): String
}

/**
 * 口令 = 随机 32 字节，用 Android Keystore 里不可导出的 AES-256 密钥 AES-GCM 包裹，
 * 密文（IV ‖ 密文）落在 `filesDir/anchor-key.bin`。
 *
 * 为什么不直接拿 Keystore 密钥当口令：Keystore 不导出对称密钥字节，SQLCipher 要字节。
 * 为什么不把口令明文存文件：口令文件和数据库同在一个私有目录，一起被拷走就等于没加密；
 * 包裹密钥不出 Keystore（有 TEE / StrongBox 时不出安全环境），单拷文件解不开。
 */
class KeystorePassphraseStore(private val keyFile: File) : DatabasePassphraseStore {

    constructor(context: Context) :
        this(File(context.applicationContext.filesDir, KEY_FILE_NAME))

    @Synchronized
    override fun passphrase(): String {
        try {
            val wrapped = keyFile.takeIf { it.isFile }?.readBytes()
            if (wrapped != null) return unwrap(wrapped).toHex()

            val raw = ByteArray(RAW_KEY_LENGTH).also { SecureRandom().nextBytes(it) }
            writeAtomically(wrap(raw))
            return raw.toHex()
        } catch (e: IOException) {
            throw DatabaseKeyUnavailableException("读写字库口令文件失败：${keyFile.path}", e)
        }
    }

    /** Keystore 里的包裹密钥，第一次调用时生成（不可导出）。 */
    private fun wrappingKey(): SecretKey {
        val keyStore = try {
            KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
        } catch (e: GeneralSecurityException) {
            throw DatabaseKeyUnavailableException("打开 Android Keystore 失败", e)
        }
        (keyStore.getEntry(KEY_ALIAS, null) as? KeyStore.SecretKeyEntry)?.let { return it.secretKey }

        return try {
            KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, KEYSTORE_PROVIDER).run {
                init(
                    KeyGenParameterSpec.Builder(
                        KEY_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
                    )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build(),
                )
                generateKey()
            }
        } catch (e: GeneralSecurityException) {
            throw DatabaseKeyUnavailableException("生成 Keystore 包裹密钥失败", e)
        }
    }

    private fun wrap(raw: ByteArray): ByteArray {
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, wrappingKey())
        return cipher.iv + cipher.doFinal(raw)
    }

    private fun unwrap(wrapped: ByteArray): ByteArray {
        if (wrapped.size <= IV_LENGTH) {
            throw DatabaseKeyUnavailableException("口令文件已损坏（${wrapped.size} 字节）")
        }
        val raw = try {
            val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
            cipher.init(
                Cipher.DECRYPT_MODE,
                wrappingKey(),
                GCMParameterSpec(TAG_BITS, wrapped, 0, IV_LENGTH),
            )
            cipher.doFinal(wrapped, IV_LENGTH, wrapped.size - IV_LENGTH)
        } catch (e: GeneralSecurityException) {
            // 认证标签对不上：Keystore 密钥被系统清过（换锁屏密码、恢复出厂、密钥失效）。
            throw DatabaseKeyUnavailableException("解开口令失败，Keystore 密钥可能已失效", e)
        }
        if (raw.size != RAW_KEY_LENGTH) {
            throw DatabaseKeyUnavailableException("口令长度异常（${raw.size} 字节）")
        }
        return raw
    }

    /** 先写临时文件再改名，避免进程被杀时留下半个口令文件。 */
    private fun writeAtomically(bytes: ByteArray) {
        val temp = File(keyFile.path + TEMP_SUFFIX)
        temp.writeBytes(bytes)
        if (keyFile.exists() && !keyFile.delete()) {
            temp.delete()
            throw DatabaseKeyUnavailableException("无法覆盖口令文件：${keyFile.path}")
        }
        if (!temp.renameTo(keyFile)) {
            temp.delete()
            throw DatabaseKeyUnavailableException("无法写入口令文件：${keyFile.path}")
        }
    }
}

private const val KEYSTORE_PROVIDER = "AndroidKeyStore"
private const val KEY_ALIAS = "anchor.database.passphrase"
private const val KEY_FILE_NAME = "anchor-key.bin"
private const val TEMP_SUFFIX = ".tmp"
private const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
private const val RAW_KEY_LENGTH = 32
private const val IV_LENGTH = 12
private const val TAG_BITS = 128
private const val HEX_DIGITS = "0123456789abcdef"

private fun ByteArray.toHex(): String = buildString(size * 2) {
    for (byte in this@toHex) {
        val value = byte.toInt() and 0xFF
        append(HEX_DIGITS[value ushr 4])
        append(HEX_DIGITS[value and 0x0F])
    }
}
