package com.anchor.recovery.core.storage

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * 数据库启动决策与迁移核对（P0-2：明文库迁到加密库不能丢数据）。
 */
class DatabaseSecurityTest {

    @Test
    fun `全新安装直接开加密库`() {
        assertEquals(
            DatabaseBootstrapAction.OPEN_ENCRYPTED,
            decideDatabaseBootstrap(legacyPlaintextExists = false, encryptedExists = false),
        )
    }

    @Test
    fun `只有明文库时先迁移`() {
        assertEquals(
            DatabaseBootstrapAction.MIGRATE_LEGACY,
            decideDatabaseBootstrap(legacyPlaintextExists = true, encryptedExists = false),
        )
    }

    @Test
    fun `已有加密库时不再二次迁移`() {
        assertEquals(
            DatabaseBootstrapAction.OPEN_ENCRYPTED,
            decideDatabaseBootstrap(legacyPlaintextExists = false, encryptedExists = true),
        )
        // 迁移成功后明文残留没删干净（上次崩在中途）也不能重导，否则会用旧数据覆盖新数据。
        assertEquals(
            DatabaseBootstrapAction.OPEN_ENCRYPTED,
            decideDatabaseBootstrap(legacyPlaintextExists = true, encryptedExists = true),
        )
    }

    @Test
    fun `行数一致时核对通过`() {
        val legacy = mapOf("check_in" to 3L, "relapse" to 0L, "room_master_table" to 0L)
        val encrypted = mapOf("check_in" to 3L, "relapse" to 0L, "room_master_table" to 0L)

        assertTrue(compareTableCounts(legacy, encrypted).isEmpty())
    }

    @Test
    fun `行数变少或变多都要报出来`() {
        val mismatches = compareTableCounts(
            legacy = mapOf("check_in" to 3L, "relapse" to 1L),
            encrypted = mapOf("check_in" to 2L, "relapse" to 1L),
        )

        assertEquals(listOf(TableCountMismatch(table = "check_in", legacyRows = 3L, encryptedRows = 2L)), mismatches)
    }

    @Test
    fun `加密库多出或缺少表都要报出来`() {
        val mismatches = compareTableCounts(
            legacy = mapOf("check_in" to 1L),
            encrypted = mapOf("check_in" to 1L, "urge_episode" to 2L),
        )

        assertEquals(
            listOf(TableCountMismatch(table = "urge_episode", legacyRows = 0L, encryptedRows = 2L)),
            mismatches,
        )

        val missing = compareTableCounts(
            legacy = mapOf("check_in" to 1L, "urge_episode" to 2L),
            encrypted = mapOf("check_in" to 1L),
        )
        assertEquals(
            listOf(TableCountMismatch(table = "urge_episode", legacyRows = 2L, encryptedRows = 0L)),
            missing,
        )
    }

    @Test
    fun `两边都没有表时视为一致`() {
        assertTrue(compareTableCounts(emptyMap(), emptyMap()).isEmpty())
    }
}
