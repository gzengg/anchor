package com.anchor.recovery.core.storage

/**
 * 启动时该打开哪个数据库文件。
 *
 * v1 是明文库 `anchor.db`，v2 起换成加密库 `anchor-enc.db`（SQLCipher + Keystore 包裹的口令）。
 * 判断只看两个文件在不在，四种组合都能在纯 JVM 单元测试里穷举，不必上真机。
 */
enum class DatabaseBootstrapAction {
    /** 开加密库：加密库已存在，或全新安装（首次打开时由 Room 建表）。 */
    OPEN_ENCRYPTED,

    /** 只有 v1 明文库：先把数据导出到加密库，再开加密库。 */
    MIGRATE_LEGACY,
}

/**
 * @param legacyPlaintextExists v1 明文库 `anchor.db` 是否存在
 * @param encryptedExists 加密库 `anchor-enc.db` 是否存在
 */
fun decideDatabaseBootstrap(
    legacyPlaintextExists: Boolean,
    encryptedExists: Boolean,
): DatabaseBootstrapAction = when {
    // 加密库存在就说明迁移已经成功过（迁移只在核对行数通过后才改名成正式文件），
    // 此时绝不能再导出一次，否则会拿旧的明文覆盖掉新写入的加密数据。
    encryptedExists -> DatabaseBootstrapAction.OPEN_ENCRYPTED
    legacyPlaintextExists -> DatabaseBootstrapAction.MIGRATE_LEGACY
    else -> DatabaseBootstrapAction.OPEN_ENCRYPTED
}

/** 某张表在两边的行数对不上。 */
data class TableCountMismatch(
    val table: String,
    val legacyRows: Long,
    val encryptedRows: Long,
)

/**
 * 迁移后的一致性核对：逐表比对行数，只在一边出现的表按 0 行计。
 *
 * @return 不一致项；空列表表示导出完整，可以删掉明文库。
 */
fun compareTableCounts(
    legacy: Map<String, Long>,
    encrypted: Map<String, Long>,
): List<TableCountMismatch> =
    (legacy.keys + encrypted.keys).sorted().mapNotNull { table ->
        val legacyRows = legacy[table] ?: 0L
        val encryptedRows = encrypted[table] ?: 0L
        if (legacyRows == encryptedRows) {
            null
        } else {
            TableCountMismatch(table = table, legacyRows = legacyRows, encryptedRows = encryptedRows)
        }
    }
