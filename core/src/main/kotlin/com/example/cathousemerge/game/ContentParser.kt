package com.example.cathousemerge.game

import kotlin.math.abs
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

/** Reads the six authored content files and rejects inconsistent references before play starts. */
object ContentParser {
    private val json = Json { ignoreUnknownKeys = true }
    private val cats = setOf("daju", "huahua", "tuanzi", "meiqiu", "ali", "huzi")
    private val rooms = listOf("porch", "entry", "living", "bedroom", "kitchen", "yard", "attic", "whole")

    fun parse(readText: (String) -> String): GameContent {
        val rawTables = read<Map<String, List<List<Double>>>>("gen_tables.json", readText)
        val tables = rawTables.map { (key, rows) ->
            val generatorLevel = key.toIntOrNull()
                ?: throw IllegalArgumentException("gen_tables.json 的生成器等级无效：$key")
            val weights = rows.map { row ->
                require(row.size == 2) { "gen_tables.json 等级 $key 的条目必须是 [物品等级, 概率]" }
                require(row[0].isFinite() && row[0] == row[0].toInt().toDouble()) {
                    "gen_tables.json 等级 $key 的物品等级必须是整数"
                }
                DropWeight(row[0].toInt(), row[1])
            }
            generatorLevel to weights
        }.toMap()
        require(tables.size == rawTables.size) { "gen_tables.json 的生成器等级重复" }

        val content = GameContent(
            chains = read("chains.json", readText),
            items = read("items.json", readText),
            orders = read("orders.json", readText),
            days = read("days.json", readText),
            dialogue = read("dialogue.json", readText),
            dropTables = tables,
        )
        validate(content)
        return content
    }

    private inline fun <reified T> read(name: String, readText: (String) -> String): T =
        try {
            json.decodeFromString<T>(readText(name))
        } catch (error: Exception) {
            throw IllegalArgumentException("无法读取内容文件 $name：${error.message}", error)
        }

    private fun validate(content: GameContent) {
        requireUnique(content.chains.map(ChainDef::id), "chains.json 链 ID")
        requireUnique(content.items.map(ItemDef::id), "items.json 物品 ID")
        requireUnique(content.orders.map(OrderDef::id), "orders.json 订单 ID")
        require(content.days.map(DayDef::day) == (1..24).toList()) {
            "days.json 必须按顺序定义第 1–24 天"
        }
        require(content.chains.isNotEmpty() && content.items.isNotEmpty()) { "物品与链表不能为空" }

        content.chains.forEach { chain ->
            require(chain.name.isNotBlank()) { "链 ${chain.id} 缺少名称" }
            require(chain.generator == "gen_${chain.id}") { "链 ${chain.id} 的生成器资源名不匹配" }
            require(chain.unlockDay in 1..24) { "链 ${chain.id} 的解锁日无效" }
            val items = content.items.filter { it.chain == chain.id }.sortedBy(ItemDef::level)
            require(items.size in 5..6 && items.map(ItemDef::level) == (1..items.size).toList()) {
                "链 ${chain.id} 必须有连续的 5–6 个物品等级"
            }
            require(chain.itemIds == items.map(ItemDef::id)) { "链 ${chain.id} 的 itemIds 与物品定义不一致" }
        }
        content.items.forEach { item ->
            require(item.chain in content.chainById) { "物品 ${item.id} 引用了不存在的链 ${item.chain}" }
            require(item.id == "${item.chain}_${item.level}") { "物品 ${item.id} 的 ID 与链、等级不匹配" }
            require(item.asset == "ic_${item.id}") { "物品 ${item.id} 的资源名 ${item.asset} 不匹配" }
            require(item.value > 0 && item.name.isNotBlank()) { "物品 ${item.id} 的名称或价值无效" }
        }

        content.orders.forEach { order ->
            require(order.day in 1..24 && order.id.startsWith("o${order.day}_")) {
                "订单 ${order.id} 的所属天数无效"
            }
            require(order.req.size in 1..2) { "订单 ${order.id} 必须要求 1–2 种物品" }
            requireUnique(order.req.map(Requirement::itemId), "订单 ${order.id} 的需求物品")
            require(order.value >= 0 && order.reward >= 0 && order.equiv > 0) {
                "订单 ${order.id} 的价值、奖励或体力当量无效"
            }
            require(order.req.sumOf(Requirement::count) <= BOARD_SIZE) { "订单 ${order.id} 的需求超出棋盘容量" }
            order.req.forEach { req ->
                require(req.count in 1..BOARD_SIZE) { "订单 ${order.id} 的物品数量必须在棋盘容量内" }
                require(req.itemId in content.itemById) { "订单 ${order.id} 要求了不存在的物品 ${req.itemId}" }
                require(content.chainById.getValue(req.chain).unlockDay <= order.day) {
                    "订单 ${order.id} 要求的链 ${req.chain} 尚未解锁"
                }
            }
            val equivalent = order.req.sumOf { (1 shl (it.level - 1)) * it.count }
            require(order.equiv == equivalent) { "订单 ${order.id} 的体力当量与需求不一致" }
        }

        val assignedOrders = content.days.flatMap(DayDef::orderIds)
        requireUnique(assignedOrders, "days.json 的订单引用")
        require(assignedOrders.toSet() == content.orderById.keys) { "订单必须恰好归属一个游戏日" }
        content.days.forEach { day ->
            require(day.room == rooms[(day.day - 1) / 3]) { "第 ${day.day} 天的房间顺序不正确" }
            require(day.build.step == (day.day - 1) % 3 + 1 && day.build.cost > 0 && day.build.name.isNotBlank()) {
                "第 ${day.day} 天的建造步骤无效"
            }
            require(day.orderIds.size in 3..6) { "第 ${day.day} 天必须有 3–6 条订单" }
            day.orderIds.forEach { orderId ->
                require(content.orderById[orderId]?.day == day.day) { "第 ${day.day} 天的订单引用 $orderId 无效" }
            }
            require(day.orderIds.sumOf { content.orderById.getValue(it).reward } > day.build.cost) {
                "第 ${day.day} 天的订单收入必须覆盖建造费用并有结余"
            }
            val newChains = content.chains.filter { it.unlockDay == day.day }
            require(newChains.size <= 1 && day.newGenerator == newChains.singleOrNull()?.generator) {
                "第 ${day.day} 天的新生成器与链解锁日不匹配"
            }
            listOf("intro", "outro").forEach { kind ->
                val sceneId = day.dialogue[kind]
                val scene = sceneId?.let(content.dialogue.scenes::get)
                require(scene != null && scene.day == day.day && scene.kind == kind) {
                    "第 ${day.day} 天的 $kind 对话引用 $sceneId 无效"
                }
            }
        }

        require(content.dropTables.keys == (1..5).toSet()) { "gen_tables.json 必须定义生成器 1–5 级" }
        content.dropTables.forEach { (level, weights) ->
            require(weights.isNotEmpty()) { "生成器 $level 级的产出表不能为空" }
            requireUnique(weights.map(DropWeight::level), "生成器 $level 级的产出等级")
            weights.forEach { weight ->
                require(weight.level in 1..4 && weight.probability.isFinite() && weight.probability > 0.0 && weight.probability <= 1.0) {
                    "生成器 $level 级的物品等级或概率无效"
                }
            }
            require(abs(weights.sumOf(DropWeight::probability) - 1.0) < 0.000001) {
                "生成器 $level 级的产出概率合计必须为 1"
            }
        }

        require(content.dialogue.version == 1) { "dialogue.json 的版本不受支持" }
        requireUnique(content.dialogue.almanac.map(AlmanacEntry::cat), "图鉴猫 ID")
        require(content.dialogue.almanac.map(AlmanacEntry::cat).toSet() == cats) { "图鉴必须包含六只猫的有效 ID" }
        content.dialogue.scenes.forEach { (id, scene) ->
            require(scene.day in 1..24 && scene.lines.isNotEmpty()) { "对话 $id 的天数或台词无效" }
            scene.lines.forEach { line ->
                require(line.who in cats || line.who == "narrator" || line.who == "player") {
                    "对话 $id 使用了未知角色 ${line.who}"
                }
                require(line.text.isNotBlank()) { "对话 $id 存在空台词" }
            }
        }
    }

    private fun <T> requireUnique(values: List<T>, label: String) {
        require(values.toSet().size == values.size) { "$label 存在重复值" }
    }
}
