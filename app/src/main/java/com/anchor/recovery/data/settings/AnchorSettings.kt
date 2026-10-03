package com.anchor.recovery.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "anchor_settings")

/** DataStore 里的全部设置项。 */
data class AnchorSettingsSnapshot(
    val reminderEnabled: Boolean = false,
    /** 用户是否要求「准点」提醒（Android 12+ 需要 SCHEDULE_EXACT_ALARM）；默认关，维持不精确提醒。 */
    val exactReminderEnabled: Boolean = false,
    val reminderTime: String = DEFAULT_REMINDER_TIME,
    val onboardingDone: Boolean = false,
    val disclaimerAckVersion: Int = 0,
    /** 用户自写的「戒断理由 / 人生价值」提示语，供 F4 轮播；空列表时使用默认文案。 */
    val motivationPrompts: List<String> = emptyList(),
) {
    /** 实际用于轮播的提示语：用户没写就用默认三条。 */
    val effectivePrompts: List<String>
        get() = motivationPrompts.ifEmpty { DEFAULT_PROMPTS }

    companion object {
        const val DEFAULT_REMINDER_TIME = "21:00"

        /**
         * 默认提示语。刻意写成中性、无功效宣称的句式，只描述"此刻的选择"。
         */
        val DEFAULT_PROMPTS: List<String> = listOf(
            "先不做决定，十分钟之后再看这件事。",
            "我想要的是自己说了算，而不是被这一分钟牵着走。",
            "这一条记录是写给我自己的，不是写给任何人看的。",
        )
    }
}

/**
 * 设置项读写（`anchor_settings`）。
 *
 * 只做读写与默认值兜底，任何判断逻辑都不放在这里。
 */
class AnchorSettings(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    val snapshot: Flow<AnchorSettingsSnapshot> = dataStore.data.map { preferences ->
        AnchorSettingsSnapshot(
            reminderEnabled = preferences[KEY_REMINDER_ENABLED] ?: false,
            exactReminderEnabled = preferences[KEY_EXACT_REMINDER_ENABLED] ?: false,
            reminderTime = preferences[KEY_REMINDER_TIME] ?: AnchorSettingsSnapshot.DEFAULT_REMINDER_TIME,
            onboardingDone = preferences[KEY_ONBOARDING_DONE] ?: false,
            disclaimerAckVersion = preferences[KEY_DISCLAIMER_VERSION] ?: 0,
            motivationPrompts = preferences[KEY_PROMPTS]
                ?.split(PROMPT_SEPARATOR)
                ?.filter { it.isNotBlank() }
                .orEmpty(),
        )
    }

    suspend fun setReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_REMINDER_ENABLED] = enabled }
    }

    suspend fun setReminderTime(hhmm: String) {
        dataStore.edit { it[KEY_REMINDER_TIME] = hhmm }
    }

    suspend fun setExactReminderEnabled(enabled: Boolean) {
        dataStore.edit { it[KEY_EXACT_REMINDER_ENABLED] = enabled }
    }

    suspend fun setOnboardingDone(done: Boolean) {
        dataStore.edit { it[KEY_ONBOARDING_DONE] = done }
    }

    suspend fun setDisclaimerAckVersion(version: Int) {
        dataStore.edit { it[KEY_DISCLAIMER_VERSION] = version }
    }

    /** 保存提示语：去掉首尾空白与空行，顺序保持用户输入顺序（按换行拼接存储）。 */
    suspend fun setMotivationPrompts(prompts: List<String>) {
        val cleaned = prompts.map { it.trim() }.filter { it.isNotEmpty() }.joinToString(PROMPT_SEPARATOR)
        dataStore.edit { preferences ->
            if (cleaned.isEmpty()) {
                preferences.remove(KEY_PROMPTS)
            } else {
                preferences[KEY_PROMPTS] = cleaned
            }
        }
    }

    private companion object {
        const val PROMPT_SEPARATOR = "\n"

        val KEY_REMINDER_ENABLED = booleanPreferencesKey("reminder_enabled")
        val KEY_EXACT_REMINDER_ENABLED = booleanPreferencesKey("exact_reminder_enabled")
        val KEY_REMINDER_TIME = stringPreferencesKey("reminder_time")
        val KEY_ONBOARDING_DONE = booleanPreferencesKey("onboarding_done")
        val KEY_DISCLAIMER_VERSION = intPreferencesKey("disclaimer_ack_version")
        val KEY_PROMPTS = stringPreferencesKey("motivation_prompts")
    }
}
