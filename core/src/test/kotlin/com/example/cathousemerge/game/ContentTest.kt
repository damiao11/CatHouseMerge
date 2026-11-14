package com.example.cathousemerge.game

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class ContentTest {
    private val contentDirectory = File("docs/data")

    private fun load(overrides: Map<String, String> = emptyMap()): GameContent =
        ContentParser.parse { name -> overrides[name] ?: File(contentDirectory, name).readText() }

    @Test
    fun authoredContentLoadsWithExpectedCounts() {
        val content = load()
        assertEquals(7, content.chains.size)
        assertEquals(39, content.items.size)
        assertEquals(112, content.orders.size)
        assertEquals(24, content.days.size)
        assertEquals(58, content.dialogue.scenes.size)
        assertEquals(6, content.dialogue.almanac.size)
        assertEquals(3, content.dialogue.letters.size)
    }

    @Test
    fun everyDayHasPositiveConstructionBudget() {
        val content = load()
        assertEquals(36_930, content.orders.sumOf(OrderDef::reward))
        assertEquals(26_595, content.days.sumOf { it.build.cost })
        content.days.forEach { day ->
            val income = content.ordersForDay(day.day).sumOf(OrderDef::reward)
            assertTrue("第 ${day.day} 天的收入必须覆盖建造", income > day.build.cost)
        }
    }

    @Test
    fun finalChapterOnlyRequiresRealItems() {
        val content = load()
        val memory = content.orderById.getValue("o21_2")
        assertEquals(listOf(Requirement("relic", 5, 3), Requirement("relic", 4, 2)), memory.req)
        assertEquals(64, memory.equiv)
        assertEquals(400, memory.reward)
        content.orders.filter { it.day >= 22 }.forEach { order ->
            order.req.forEach { req -> assertTrue(req.itemId in content.itemById) }
        }
        assertEquals(13, content.orders.count { it.day >= 22 })
    }

    @Test
    fun referencedArtworkExistsInDeliveredAssets() {
        val content = load()
        content.items.forEach { item -> assertFile("android-assets/drawable/${item.asset}.png") }
        content.chains.forEach { chain ->
            (1..5).forEach { level -> assertFile("android-assets/drawable/gen_${chain.id}_$level.png") }
        }
        content.dialogue.almanac.forEach { cat ->
            listOf("", "_happy", "_turned").forEach { expression ->
                assertFile("android-assets/drawable/cat_${cat.cat}$expression.png")
            }
        }
        content.days.map(DayDef::room).distinct().forEach { room ->
            listOf("before", "after").forEach { state -> assertFile("android-assets/rooms/room_${room}_$state.webp") }
        }
        listOf("splash", "door", "lights", "garden", "ending").forEach { scene ->
            assertFile("android-assets/scenes/scene_$scene.webp")
        }
    }

    @Test
    fun itemValuesAndEnergyMatchUpstreamDefinitions() {
        val values = listOf(2, 5, 12, 28, 65, 150)
        load().items.forEach { item ->
            assertEquals(values[item.level - 1], item.value)
            assertEquals(1 shl (item.level - 1), item.energy)
        }
        val legacy = """{"id":"wood_3","chain":"wood","level":3,"name":"木板","value":12,"asset":"ic_wood_3"}"""
        assertEquals(4, kotlinx.serialization.json.Json.decodeFromString<ItemDef>(legacy).energy)
    }

    @Test
    fun unavailableOrderItemsAreRejected() {
        val orders = File(contentDirectory, "orders.json").readText()
            .replaceFirst("\"chain\": \"wood\"", "\"chain\": \"missing\"")
        assertInvalid(mapOf("orders.json" to orders), "不存在的物品")
    }

    @Test
    fun ordersBeforeChainUnlockAreRejected() {
        val orders = File(contentDirectory, "orders.json").readText()
            .replaceFirst("\"chain\": \"wood\"", "\"chain\": \"plant\"")
        assertInvalid(mapOf("orders.json" to orders), "尚未解锁")
    }

    @Test
    fun invalidDropProbabilitiesAreRejected() {
        val tables = """{"1":[[1,0.5]],"2":[[1,1.0]],"3":[[1,1.0]],"4":[[1,1.0]],"5":[[1,1.0]]}"""
        assertInvalid(mapOf("gen_tables.json" to tables), "概率合计必须为 1")
    }

    @Test
    fun topTierDirectDropsAreRejected() {
        val tables = """{"1":[[6,1.0]],"2":[[1,1.0]],"3":[[1,1.0]],"4":[[1,1.0]],"5":[[1,1.0]]}"""
        assertInvalid(mapOf("gen_tables.json" to tables), "物品等级或概率无效")
    }

    @Test
    fun malformedContentReportsItsFilename() {
        assertInvalid(mapOf("chains.json" to "{"), "chains.json")
    }

    private fun assertFile(path: String) {
        assertTrue("缺少美术资源 $path", File(path).isFile)
    }

    private fun assertInvalid(overrides: Map<String, String>, expectedMessage: String) {
        try {
            load(overrides)
            fail("不一致的内容应在加载时被拒绝")
        } catch (error: IllegalArgumentException) {
            assertTrue(error.message.orEmpty(), error.message.orEmpty().contains(expectedMessage))
        }
    }
}
