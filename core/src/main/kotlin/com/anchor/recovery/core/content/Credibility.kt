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
 */
@Serializable(with = CredibilitySerializer::class)
enum class Credibility(val label: String, val rank: Int) {
    HIGH("高", 2),
    MEDIUM("中", 1),
    LOW("低", 0),
    ;

    companion object {
        fun fromRaw(raw: String?): Credibility = when (raw?.trim()) {
            "高" -> HIGH
            "中" -> MEDIUM
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
