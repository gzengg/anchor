package com.anchor.recovery

import kotlin.test.Test
import kotlin.test.assertEquals

class AppInfoTest {

    @Test
    fun `display name comes from core module`() {
        assertEquals("磐石", AppInfo.DISPLAY_NAME)
    }

    @Test
    fun `version is not blank`() {
        val version = AppInfo.VERSION
        check(version.isNotBlank()) { "VERSION 不应为空" }
        assertEquals(3, version.split(".").size)
    }
}
