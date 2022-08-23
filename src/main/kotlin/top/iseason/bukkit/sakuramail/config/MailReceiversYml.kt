package top.iseason.bukkit.sakuramail.config

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.sakuramail.database.MailReceiver
import top.iseason.bukkit.sakuramail.database.MailReceivers
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
            MailReceivers.deleteAll()
            timeReceivers.forEach { (key, list) ->
                MailReceiver.new(key) {
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
        val strings = getReceiver(id) ?: return null
        return getReceivers(strings)
    }

    /**
     * 由参数解析出接收者
     */
    fun getReceivers(params: List<String>, player: Player? = null): List<UUID> {
        //限制个数
        val temp = params.toMutableList()
        var limit: Int? = if (player == null) null else 1
        for ((index, param) in temp.withIndex()) {
            if (player != null) {
                temp[index] = param.replace("%uuid%", player.uniqueId.toString())
                continue
            }
            if (param.startsWith("limit_")) {
                limit = runCatching { param.removePrefix("limit_").toInt() }.getOrNull()
            }
        }
        val uuid: UUID? = player?.uniqueId
        //先处理sql查询
        var resultSet = runCatching {
            val operation = PlayerTimes.parseArgs(temp, player)
            transaction {
                var resultSet = setOf<UUID>()
                //数量限制
                //常规查询
                if (operation != null) {
                    var query = PlayerTimes.slice(PlayerTimes.player).select {
                        if (uuid != null) operation and (PlayerTimes.player eq uuid) else operation
                    }
                    //限制
                    if (limit != null) query = query.limit(limit)
                    //符合条件的UUID
                    resultSet =
                        query.orderBy(PlayerTimes.id, SortOrder.DESC).distinctBy { PlayerTimes.id }
                            .map { it[PlayerTimes.player] }.toSet()
                }
                resultSet
            }
        }.getOrNull() ?: setOf()
        var totalMap: Map<UUID, Duration>? = null
        //处理其他的
        temp.forEach { param ->
            runCatching {
                val args = param.removePrefix("--").split(':', limit = 2)
                val op = if (args.size >= 2) args[0] else "and"
                val split = args.last().split('_')
                var duration: Duration? = null
                var setStr = split[0]
                if (param.contains("totaltime", true)) {
                    duration = runCatching { Duration.parse(split[2]) }.getOrNull() ?: return@forEach
                    val operation = PlayerTimes.playTime.sum()
                    if (totalMap == null) {
                        transaction {
                            var query = if (uuid == null)
                                PlayerTimes.slice(PlayerTimes.player, operation).selectAll().groupBy(PlayerTimes.player)
                            else PlayerTimes.slice(PlayerTimes.player, operation).select { PlayerTimes.player eq uuid }
                                .groupBy(PlayerTimes.player)
                            if (limit != null) query = query.limit(limit)
                            totalMap = query.associate {
                                it[PlayerTimes.player] to (it[operation] ?: Duration.ofSeconds(0))
                            }
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
                        val duration2 =
                            runCatching { Duration.parse(split[3]) }.getOrNull() ?: Duration.ofSeconds(0)
                        totalMap!!.mapNotNull { (u, d) -> if (d > duration && d < duration2) u else null }.toSet()
                    }

                    "online" -> Bukkit.getOnlinePlayers().map { p -> p.uniqueId }.toSet()

                    "offline" -> Bukkit.getOfflinePlayers()
                        .mapNotNull { p -> if (!p.isOnline) p.uniqueId else null }
                        .toSet()

                    "all" -> Bukkit.getOfflinePlayers()
                        .mapNotNull { p -> if (p.hasPlayedBefore()) p.uniqueId else null }.toSet()

                    "permission" -> (if (player == null) Bukkit.getOnlinePlayers() else setOf(player))
                        .mapNotNull { p -> if (p.hasPermission(split[1])) p.uniqueId else null }.toSet()

                    "gamemode" -> (if (player == null) Bukkit.getOnlinePlayers() else setOf(player))
                        .mapNotNull { p -> if (p.gameMode.name.equals(split[1], true)) p.uniqueId else null }
                        .toSet()

                    "uuids" -> split[1].split(";")
                        .mapNotNull { runCatching { UUID.fromString(split[1]) }.getOrNull() }
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
                    "andNot" -> {
                        val all = Bukkit.getOfflinePlayers()
                            .mapNotNull { p -> if (p.hasPlayedBefore()) p.uniqueId else null }.toMutableSet()
                        all.removeAll(set)
                        resultSet = if (resultSet.isEmpty()) all else resultSet.intersect(all)
                    }

                    "or", "add" -> resultSet = resultSet.union(set)
                    "remove" -> resultSet = resultSet.toMutableSet().apply { removeAll(set) }
                    "orNot" -> {
                        val all = Bukkit.getOfflinePlayers()
                            .mapNotNull { p -> if (p.hasPlayedBefore()) p.uniqueId else null }.toMutableSet()
                        all.removeAll(set)
                        resultSet = resultSet.union(all)
                    }
                }
            }.getOrElse { it.printStackTrace() }
        }
        if (limit != null) {
            return resultSet.take(limit)
        }
        return resultSet.toList()
    }

}