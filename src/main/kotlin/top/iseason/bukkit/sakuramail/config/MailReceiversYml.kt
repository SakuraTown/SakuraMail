package top.iseason.bukkit.sakuramail.config

import org.bukkit.Bukkit
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.selectAll
import org.jetbrains.exposed.sql.sum
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.sakuramail.database.MailReceiver
import top.iseason.bukkit.sakuramail.database.PlayerTime
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import java.time.Duration
import java.util.*

@FilePath("receiver.yml")
object MailReceiversYml : SimpleYAMLConfig() {

    @Comment("邮件目标选择器")
    @Key("receivers")
    var timeReceivers = mutableMapOf<String, MutableList<String>>()

    /**
     * 获取某个id的目标选择器并缓存
     */
    fun getReceiver(id: String): MutableList<String>? = timeReceivers[id] ?: transaction {
        val findById = MailReceiver.findById(id) ?: return@transaction null
        val list = findById.params.split("$").toMutableList()
        timeReceivers[id] = list
        save(false)
        list
    }

    /**
     * 将数据上传至数据库
     */
    fun upload() {
        transaction {
            timeReceivers.forEach { (key, list) ->
                val findById = MailReceiver.findById(key)
                if (findById != null) {
                    findById.params = list.joinToString("$")
                } else MailReceiver.new(key) {
                    params = list.joinToString("$")
                }
            }
        }
    }

    /**
     * 从数据库下载数据
     */
    fun download() {
        transaction {
            for (mailReceiver in MailReceiver.all()) {
                timeReceivers[mailReceiver.id.value] = mailReceiver.params.split("$").toMutableList()
            }
        }
        save()
    }

    /**
     * 解析某个id的接收者
     */
    fun getReceivers(id: String): List<UUID>? {
        val strings = timeReceivers[id] ?: return null
        return getReceivers(strings)
    }

    /**
     * 由参数解析出接收者
     */
    fun getReceivers(params: List<String>): List<UUID> {
        val operation = PlayerTimes.parseArgs(params)
        //限制个数
        val limitStr = params.find { it.startsWith("limit.") }
        var limit: Int? = null
        if (limitStr != null) {
            limit = runCatching { limitStr.removePrefix("limit.").toInt() }.getOrNull()
        }
        //先处理sql查询
        var resultSet = runCatching {
            transaction {
                var resultSet = setOf<UUID>()
                //数量限制
                //常规查询
                if (operation != null) {
                    var query = PlayerTime.find { operation }
                    //限制
                    if (limit != null) query = query.limit(limit)
                    //符合条件的UUID
                    resultSet = query.orderBy(PlayerTimes.id to SortOrder.DESC).map { it.player }.toSet()
                }
                resultSet
            }
        }.getOrNull() ?: setOf()
        var totalMap: Map<UUID, Duration>? = null
        //处理其他的
        params.forEach { param ->
            runCatching {
                val args = param.removePrefix("--").split(':', limit = 2)
                val op = if (args.size >= 2) args[0] else "and"
                val split = args.last().split('.')
                var duration: Duration? = null
                var setStr = split[0]
                if (param.contains("totaltime", true)) {
                    duration = runCatching { Duration.parse(split[2]) }.getOrNull() ?: return@forEach
                    val operation = PlayerTimes.playTime.sum()
                    if (totalMap == null) {
                        var query =
                            PlayerTimes.slice(PlayerTimes.player, operation).selectAll().groupBy(PlayerTimes.player)
                        if (limit != null) query = query.limit(limit)
                        totalMap = query.associate {
                            it[PlayerTimes.player] to it[operation]!!
                        }
                    }
                    setStr = split[1]
                }
                val set: Set<UUID> = when (setStr) {
                    "greater" -> {
                        totalMap!!.mapNotNull { (u, d) -> if (d > duration) u else null }.toSet()
                    }

                    "less" -> {
                        totalMap!!.mapNotNull { (u, d) -> if (d < duration) u else null }.toSet()
                    }

                    "between" -> {
                        val duration2 = runCatching { Duration.parse(split[3]) }.getOrNull() ?: Duration.ofSeconds(0)
                        totalMap!!.mapNotNull { (u, d) -> if (d > duration && d < duration2) u else null }.toSet()
                    }

                    "online" -> Bukkit.getOnlinePlayers().map { p -> p.uniqueId }.toSet()

                    "offline" -> Bukkit.getOfflinePlayers().mapNotNull { p -> if (!p.isOnline) p.uniqueId else null }
                        .toSet()

                    "all" -> Bukkit.getOfflinePlayers()
                        .mapNotNull { p -> if (p.hasPlayedBefore()) p.uniqueId else null }.toSet()

                    "permission" -> Bukkit.getOnlinePlayers()
                        .mapNotNull { p -> if (p.hasPermission(split[1])) p.uniqueId else null }.toSet()

                    "gamemode" -> Bukkit.getOnlinePlayers()
                        .mapNotNull { p -> if (p.gameMode.name.equals(split[1], true)) p.uniqueId else null }.toSet()

                    "uuids" -> split[1].split(";").mapNotNull { runCatching { UUID.fromString(split[1]) }.getOrNull() }
                        .toSet()

                    "names" -> split[1].split(";").mapNotNull { name ->
                        (Bukkit.getPlayer(name) ?: Bukkit.getOfflinePlayer(name).let {
                            if (it.hasPlayedBefore()) {
                                it
                            } else null
                        })?.uniqueId
                    }.toSet()

                    else -> return@forEach
                }
                when (op) {
                    "and" -> resultSet = if (resultSet.isEmpty()) set else resultSet.intersect(set)
                    "andNot" -> resultSet = resultSet.toMutableSet().apply { removeAll(set) }
                    "or" -> resultSet = resultSet.union(set)
                    "orNot" -> resultSet =
                        resultSet.union(set).toMutableSet().apply { removeAll(resultSet.intersect(set)) }
                }

            }
        }
        return resultSet.toList()
    }

}