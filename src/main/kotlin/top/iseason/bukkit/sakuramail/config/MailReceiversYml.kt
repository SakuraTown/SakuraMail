package top.iseason.bukkit.sakuramail.config

import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.sakuramail.database.PlayerTime
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import java.time.Duration
import java.util.*

@FilePath("receiver.yml")
object MailReceiversYml : SimpleYAMLConfig() {

    @Comment("时间相关选择器")
    @Key("time")
    var timeReceivers = mutableMapOf<String, MutableList<String>>()

}

abstract class BaseMailReceiver(
    val type: String,
    open val params: MutableList<String> = mutableListOf()
) {
    /**
     * 获取接收者的UUID
     */
    abstract fun getReceivers(): List<UUID>
}

class TimeMailReceiver(
    override val params: MutableList<String> = mutableListOf()
) : BaseMailReceiver("time") {
    /**
     * 获取接收者的UUID
     */
    override fun getReceivers(): List<UUID> {
        val operation = PlayerTimes.parseArgs(params)
        return transaction {
            var resultSet = setOf<UUID>()
            //数量限制
            val limitStr = params.find { it.startsWith("limit.") }
            var limit: Int? = null
            if (limitStr != null) {
                limit = runCatching { limitStr.removePrefix("limit.").toInt() }.getOrNull()
            }
            //常规查询
            if (operation != null) {
                var query = PlayerTime.find { operation }
                //限制
                if (limit != null) query = query.limit(limit)
                //符合条件的UUID
                resultSet = query.orderBy(PlayerTimes.id to SortOrder.DESC).map { it.player }.toSet()
            }
            resultSet = parseTotalTime(resultSet, limit)
            resultSet.toList()
        }
    }

    // --and:totalTime.greater.1h
    private fun parseTotalTime(source: Set<UUID>, limit: Int?): Set<UUID> {
        //总游戏时间限制条件
        var temp = source
        runCatching {
            var resultMap: Map<UUID, Duration>? = null
            for (param in params) {
                if (!param.contains("totalTime")) continue
                val args = param.removePrefix("--").split(':', limit = 2)
                val op = if (args.size >= 2) args[0] else "or"
                val split = args.last().split('.')
                val duration = runCatching { Duration.parse(split[2]) }.getOrNull() ?: continue
                val operation = PlayerTimes.playTime.sum()
                if (resultMap == null) {
                    var query =
                        PlayerTimes.slice(PlayerTimes.player, operation).selectAll().groupBy(PlayerTimes.player)
                    if (limit != null) query = query.limit(limit)
                    resultMap = query.associate {
                        it[PlayerTimes.player] to it[operation]!!
                    }
                }
                val totalTimeSet = mutableSetOf<UUID>()
                when (split[1]) {
                    "greater" -> {
                        resultMap.forEach { (u, d) -> if (d > duration) totalTimeSet += u }
                    }

                    "less" -> {
                        resultMap.forEach { (u, d) -> if (d < duration) totalTimeSet += u }
                    }

                    "between" -> {
                        val duration2 = runCatching { Duration.parse(split[3]) }.getOrNull()
                        if (duration2 != null) {
                            resultMap.forEach { (u, d) -> if (d > duration && d < duration2) totalTimeSet += u }
                        }
                    }
                }
                when (op) {
                    "and" -> temp = temp.intersect(totalTimeSet)
                    "andNot" -> temp = temp.intersect(temp.union(totalTimeSet).intersect(totalTimeSet))
                    "or" -> temp = temp.union(totalTimeSet)
                    "orNot" -> temp = temp.union(temp.union(totalTimeSet).intersect(totalTimeSet))
                }
            }
        }
        return temp
    }
}

