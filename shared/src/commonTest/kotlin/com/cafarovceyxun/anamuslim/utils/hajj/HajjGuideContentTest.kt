package com.cafarovceyxun.anamuslim.utils.hajj

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Bələdçinin addımları əl ilə yazılıb, dəlillər isə bazada mövzu **açarı** ilə saxlanır
 * (`hajj_evidence.topic`). Açar səhv yazılsa və ya bazanın CHECK formasına uymasa dəlil səssizcə
 * heç bir addımda görünmür — nə kompilyator, nə ekran bunu göstərir.
 */
class HajjGuideContentTest {

    @Test
    fun topicKeysAreUniqueAndMatchTheDatabaseCheck() {
        val keys = HajjTopic.entries.map { it.key }
        assertEquals(keys.size, keys.distinct().size, "Təkrar mövzu açarı")
        // `hajj_evidence_topic_shape`: ^[a-z][a-z0-9_]{0,39}$
        val shape = Regex("^[a-z][a-z0-9_]{0,39}$")
        keys.forEach { assertTrue(shape.matches(it), "Açar bazanın CHECK-inə uymur: $it") }
        keys.forEach { assertEquals(it, HajjTopic.of(it)?.key) }
    }

    @Test
    fun everyStepHasATopic() {
        (HajjGuideContent.steps + HajjGuideContent.umrahSteps).forEach { step ->
            assertTrue(step.topics.isNotEmpty(), "${step.id}: mövzu yoxdur")
            assertEquals(step.topics.size, step.topics.distinct().size, "${step.id}: təkrar mövzu")
        }
    }

    @Test
    fun everyTypeHasStepsOnEveryHajjDay() {
        HajjType.entries.forEach { type ->
            val days = HajjGuideContent.stepsFor(type).map { it.day }.toSet()
            assertEquals(HajjDay.entries.toSet(), days, "${type.title}: boş gün var")
        }
    }

    @Test
    fun everyTypeReachesSayAndTawaf() {
        HajjType.entries.forEach { type ->
            val topics = HajjGuideContent.stepsFor(type).flatMap { it.topics }.toSet()
            assertTrue(HajjTopic.SAY in topics, "${type.title}: səy addımı yoxdur")
            assertTrue(HajjTopic.TAWAF in topics, "${type.title}: tavaf addımı yoxdur")
        }
    }

    @Test
    fun stepIdsAreUnique() {
        // `done` dəsti id ilə saxlanır — ümrə və Həcc addımları eyni dəstə düşür.
        val ids = (HajjGuideContent.steps + HajjGuideContent.umrahSteps).map { it.id }
        assertEquals(ids.size, ids.distinct().size)
    }

    @Test
    fun sayAlternatesAndEndsAtMarwa() {
        assertEquals("Səfadan Mərvəyə", HajjGuideContent.sayDirection(1))
        assertEquals("Mərvədən Səfaya", HajjGuideContent.sayDirection(2))
        // Yeddinci gediş Səfadan başlayır, yəni Mərvədə bitir.
        assertEquals("Səfadan Mərvəyə", HajjGuideContent.sayDirection(7))
    }
}
