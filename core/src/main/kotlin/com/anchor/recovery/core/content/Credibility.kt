package com.anchor.recovery.core.content

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * 文章可信度三级徽章。
 *
 * 内容管线只输出「高 / 中 / 低」三种取值（源数据里的「中-高」在上游被保守降级为「中」）。
 * 解析时对任何未知取值兜底为 [LOW]，绝不因为内容数据瑕疵让 App 崩溃。
 *
 * [label] 是**数据 key**，不是界面文案：它既是 articles.json / 导出 JSON 里的取值，
 * 也是 [fromRaw] 的解析依据，所以中文必须留在 :core，不能改成资源或枚举名。
 * 界面上显示的中文另走 `:app` 的 `LegalContentText.credibilityRes` / `strings_legal.xml`。
 */
@Serializable(with = CredibilitySerializer::class)
enum class Credibility(val label: String, val rank: Int) {
    HIGH("高", 2),
    MEDIUM("中", 1),
    LOW("低", 0),
    ;

    companion object {
        /** 解析源数据取值；未知（含 null / 「中-高」）一律兜底为 [LOW]。 */
        fun fromRaw(raw: String?): Credibility = when (raw?.trim()) {
            HIGH.label -> HIGH
            MEDIUM.label -> MEDIUM
            else -> LOW
        }
    }
}

object CredibilitySerializer : KSerializer<Credibility> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("com.anchor.recovery.core.content.Credibility", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Credibility) {
        encoder.encodeString(value.label)
    }

    override fun deserialize(decoder: Decoder): Credibility = Credibility.fromRaw(decoder.decodeString())
}
