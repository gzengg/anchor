package com.anchor.recovery.core.streak

import com.anchor.recovery.core.clock.FakeClock
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MilestoneAchievementsTest {

    private val zone: TimeZone = TimeZone.of("Asia/Shanghai")
    private val today: LocalDate = LocalDate.parse("2024-06-30")

    private fun clock(at: LocalDate = today, hour: Int = 12, timeZone: TimeZone = zone): FakeClock =
        FakeClock.at(at, hour = hour, timeZone = timeZone)

    private fun day(offsetFromToday: Int): LocalDate =
        LocalDate.fromEpochDays(today.toEpochDays() + offsetFromToday)

    /** 以 [start] 为起点连续 [count] 天的打卡日期。 */
    private fun run(start: LocalDate, count: Int): List<LocalDate> =
        (0 until count).map { LocalDate.fromEpochDays(start.toEpochDays() + it) }

    private fun relapseAt(date: LocalDate, hour: Int = 22, timeZone: TimeZone = zone): Instant =
        LocalDateTime(date, LocalTime(hour, 0)).toInstant(timeZone)

    private fun currentDays(checkIns: List<LocalDate>, relapses: List<Instant>, clock: FakeClock): Int =
        StreakCalculator(clock).compute(checkIns, relapses).currentDays

    @Test
    fun `空历史没有任何达成记录`() {
        assertTrue(MilestoneAchievements.detect(emptyList(), emptySet()).isEmpty())

        val wall = MilestoneAchievements.wall(emptyList(), emptySet(), currentDays = 0)
        assertEquals(listOf(1, 7, 30, 60, 90), wall.map { it.milestone.days })
        wall.forEach { status ->
            assertFalse(status.achieved)
            assertNull(status.latestAchievedDate)
            assertEquals(0, status.eraCount)
            assertFalse(status.currentEraReached)
            assertEquals(status.milestone.days, status.daysRemaining)
            assertEquals(0f, status.progress)
        }
    }

    @Test
    fun `单日打卡即达成第 1 天`() {
        val checkIns = listOf(day(-1))
        val achievements = MilestoneAchievements.detect(checkIns, emptySet())
        assertEquals(1, achievements.size)
        assertEquals(1, achievements.first().days)
        assertEquals(day(-1), achievements.first().eraStartDate)
        assertEquals(day(-1), achievements.first().achievedDate)
    }

    @Test
    fun `连续七天达成 1 天与 7 天且达成日分别为首日与第七日`() {
        val checkIns = run(day(-6), 7)
        val achievements = MilestoneAchievements.detect(checkIns, emptySet())
        assertEquals(listOf(1, 7), achievements.map { it.days })
        assertEquals(day(-6), achievements[0].achievedDate)
        assertEquals(day(0), achievements[1].achievedDate)
        assertEquals(day(-6), achievements[1].eraStartDate)
    }

    @Test
    fun `漏打一整天即断签并开启新纪元`() {
        // 连打三天、漏一天、再连打三天：7 天未达成，1 天徽章出现两次
        val checkIns = run(day(-7), 3) + run(day(-3), 3)
        val achievements = MilestoneAchievements.detect(checkIns, emptySet())
        assertEquals(listOf(1, 1), achievements.map { it.days })
        assertEquals(day(-7), achievements[0].eraStartDate)
        assertEquals(day(-3), achievements[1].eraStartDate)
        assertEquals(2, achievements.map { it.eraStartDate }.distinct().size)
    }

    @Test
    fun `复吸清零后历史达成记录保留并按纪元区分`() {
        // 第一纪元：连打 10 天，第 10 天复吸；第二纪元：次日重新连打 8 天
        val firstEra = run(day(-18), 10)
        val relapse = relapseAt(day(-9))
        val secondEra = run(day(-8), 8)
        val checkIns = firstEra + secondEra
        val clock = clock()

        val achievements = MilestoneAchievements.detect(checkIns, setOf(day(-9)))
        assertEquals(listOf(1, 7, 1, 7), achievements.map { it.days })
        // 第一纪元：1 天与 7 天，达成日分别为纪元首日与第 7 日
        assertEquals(day(-18), achievements[0].eraStartDate)
        assertEquals(day(-12), achievements[1].achievedDate)
        // 第二纪元：复吸次日重新起算
        assertEquals(day(-8), achievements[2].eraStartDate)
        assertEquals(day(-2), achievements[3].achievedDate)

        val days = currentDays(checkIns, listOf(relapse), clock)
        assertEquals(8, days)
        val wall = MilestoneAchievements.wall(checkIns, setOf(day(-9)), days)
        val seven = wall.first { it.milestone.days == 7 }
        assertTrue(seven.achieved)
        assertEquals(2, seven.eraCount)
        assertEquals(day(-2), seven.latestAchievedDate)
        assertTrue(seven.currentEraReached)
        val thirty = wall.first { it.milestone.days == 30 }
        assertFalse(thirty.achieved)
        assertEquals(22, thirty.daysRemaining)
    }

    @Test
    fun `复吸日当天达成的里程碑照常点亮且当前纪元视为已结束`() {
        // 与 StreakCalculator.longestDays 口径一致：复吸前一刻的连续计数有效
        val checkIns = run(day(-7), 7)
        val relapseDate = day(-1)
        val relapses = listOf(relapseAt(relapseDate))
        val clock = clock()
        val state = StreakCalculator(clock).compute(checkIns, relapses)

        val achievements = MilestoneAchievements.detect(checkIns, setOf(relapseDate))
        assertEquals(listOf(1, 7), achievements.map { it.days })
        assertEquals(relapseDate, achievements[1].achievedDate)

        val statuses = MilestoneAchievements.wall(checkIns, setOf(relapseDate), state.currentDays)
        val seven = statuses.first { it.milestone.days == 7 }
        assertTrue(seven.achieved)
        assertFalse(seven.currentEraReached)
        assertEquals(7, seven.daysRemaining)
    }

    @Test
    fun `当前纪元已达成时剩余天数为零`() {
        val checkIns = run(day(-6), 7)
        val clock = clock()
        val days = currentDays(checkIns, emptyList(), clock)
        val wall = MilestoneAchievements.wall(checkIns, emptySet(), days)
        val seven = wall.first { it.milestone.days == 7 }
        assertEquals(0, seven.daysRemaining)
        assertEquals(1f, seven.progress)
        val thirty = wall.first { it.milestone.days == 30 }
        assertEquals(23, thirty.daysRemaining)
        assertEquals(7f / 30f, thirty.progress)
    }

    @Test
    fun `时区边界按本地自然日划分复吸纪元`() {
        val checkIns = run(LocalDate.parse("2024-06-01"), 12)
        // 该时刻在 UTC 是 06-10T16:30，在 Asia/Shanghai 已是 06-11T00:30
        val instant = LocalDateTime(LocalDate.parse("2024-06-10"), LocalTime(16, 30)).toInstant(TimeZone.UTC)

        val shanghai = MilestoneTracker(clock(timeZone = zone))
            .detect(checkIns, listOf(instant))
        // 复吸落在本地 06-11：06-12 起算新纪元
        assertEquals(listOf(1, 7, 1), shanghai.map { it.days })
        assertEquals(LocalDate.parse("2024-06-07"), shanghai[1].achievedDate)
        assertEquals(LocalDate.parse("2024-06-12"), shanghai.last().eraStartDate)
        assertEquals(LocalDate.parse("2024-06-12"), shanghai.last().achievedDate)

        val utc = MilestoneTracker(clock(timeZone = TimeZone.UTC))
            .detect(checkIns, listOf(instant))
        // UTC 视角同一时刻仍是 06-10：06-11 起算新纪元
        assertEquals(listOf(1, 7, 1), utc.map { it.days })
        assertEquals(LocalDate.parse("2024-06-11"), utc.last().eraStartDate)
        assertEquals(LocalDate.parse("2024-06-11"), utc.last().achievedDate)
    }

    @Test
    fun `达成里程碑不超过最长连续天数`() {
        val checkIns = run(day(-45), 46)
        val clock = clock()
        val state = StreakCalculator(clock).compute(checkIns, emptyList())
        assertEquals(46, state.currentDays)
        val wall = MilestoneAchievements.wall(checkIns, emptySet(), state.currentDays)
        val achieved = wall.filter { it.achieved }.map { it.milestone.days }
        assertEquals(listOf(1, 7, 30), achieved)
        assertTrue(achieved.max() <= state.longestDays)
        assertNotNull(wall.first { it.milestone.days == 30 }.latestAchievedDate)
        assertEquals(46f / 60f, wall.first { it.milestone.days == 60 }.progress)
    }

    @Test
    fun `里程碑文案保持中性不承诺疗效`() {
        // 绝对化承诺词一律不得出现
        val banned = listOf("保证", "一定", "必然", "已恢复", "已经恢复", "彻底")
        MilestoneAchievements.wall(emptyList(), emptySet(), 0).forEach { status ->
            val text = status.milestone.title + status.milestone.hint
            banned.forEach { word ->
                assertFalse(text.contains(word), "里程碑文案不得包含「$word」：$text")
            }
            // 「痊愈 / 治愈」只能出现在否定语境（同一句内先出现否定词）
            val negation = Regex("(不是|不代表|并非|并不)[^。！？]*(痊愈|治愈)")
            listOf("痊愈", "治愈").forEach { word ->
                if (text.contains(word)) {
                    assertTrue(negation.containsMatchIn(text), "「$word」只能出现在否定语境中：$text")
                }
            }
        }
    }
}
