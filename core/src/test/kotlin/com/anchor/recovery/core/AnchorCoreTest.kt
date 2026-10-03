package com.anchor.recovery.core

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AnchorCoreTest {

    @Test
    fun `version is a plain semver string`() {
        val version = AnchorCore.VERSION
        val parts = version.split(".")
        assertEquals(3, parts.size, "VERSION 应为 x.y.z 形式，实际=$version")
        assertTrue(parts.all { it.toIntOrNull() != null }, "VERSION 各段应为数字，实际=$version")
    }

    @Test
    fun `display name is the chinese app name`() {
        assertEquals("磐石", AnchorCore.APP_DISPLAY_NAME)
    }
}
