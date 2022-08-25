package top.iseason.bukkit.sakuramail.config

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.sakuramail.database.MailReceiver
import top.iseason.bukkit.sakuramail.database.MailReceivers
import top.iseason.bukkit.sakuramail.database.MailRecords
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import top.iseason.bukkit.sakuramail.utils.TimeUtils
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

@FilePath("receivers.yml")
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
            if (param.startsWith("limit")) {
                limit = runCatching { param.removePrefix("limit").toInt() }.getOrNull()
            }
        }
        val uuid: UUID? = player?.uniqueId
        //先处理sql查询
        var resultSet = runCatching {
            val operation = PlayerTimes.parseArgs(temp, player)
            transaction {
                var resultSet = setOf<UUID>()
                //常规查询
                if (operation != null) {
                    var query = PlayerTimes.slice(PlayerTimes.player).select { operation }
                    //限制
                    if (limit != null) query = query.limit(limit)
                    //符合条件的UUID
                    resultSet =
                        query.orderBy(PlayerTimes.id, SortOrder.DESC).distinctBy { PlayerTimes.player }
                            .map { it[PlayerTimes.player] }.toSet()
                }
                resultSet
            }
        }.getOrNull() ?: setOf()
        var totalMap: Map<UUID, Duration>? = null
        //处理其他的
        temp.forEach { param ->
            runCatching {
                val args = param.removePrefix("--").split(',', limit = 2)
                val op = if (args.size >= 2) args[0] else "and"
                val split = args.last().split(' ')
                var duration: Duration? = null
                var setStr = split[0]
                //totaltime_greater_PT1H_STime1_ETime2
                if (param.contains("totaltime", true)) {
                    duration = TimeUtils.parseDuration(split[2]) ?: return@forEach
                    var startTime: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0)
                    var endTime: LocalDateTime = LocalDateTime.of(100000, 1, 1, 0, 0)
                    split.getOrNull(3)?.let {
                        if (it.startsWith("s", true))
                            startTime = TimeUtils.parseTime(it.drop(1))
                        else endTime = TimeUtils.parseTime(it.drop(1))
                    }
                    split.getOrNull(4)?.let {
                        if (it.startsWith("s", true))
                            startTime = TimeUtils.parseTime(it.drop(1))
                        else endTime = TimeUtils.parseTime(it.drop(1))
                    }
                    if (totalMap == null) {
                        totalMap = if (uuid == null) PlayerTimes.getTotalTimes(startTime, endTime)
                        else mapOf(uuid to PlayerTimes.getTotalTime(uuid, startTime, endTime))
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
                        val duration2 = TimeUtils.parseDuration(split[3]) ?: Duration.ZERO
                        totalMap!!.mapNotNull { (u, d) -> if (d > duration && d < duration2) u else null }.toSet()
                    }

                    "hasmail" -> {
                        val mailId = split[2]
                        MailRecords.slice(MailRecords.player).select { MailRecords.mail eq mailId }
                            .distinctBy { MailRecords.player }
                            .map { it[MailRecords.player] }.toSet()
                    }

                    "online" -> Bukkit.getOnlinePlayers().map { p -> p.uniqueId }.toSet()

                    "offline" -> Bukkit.getOfflinePlayers()
                        .mapNotNull { p -> if (!p.isOnline) p.uniqueId else null }
                        .toSet()

                    "all" -> PlayerTimes.getAllPlayers().toSet()
                    "localall" -> Bukkit.getOfflinePlayers()
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
                        val all = PlayerTimes.getAllPlayers().toSet().toMutableSet()
                        all.removeAll(set)
                        resultSet = if (resultSet.isEmpty()) all else resultSet.intersect(all)
                    }

                    "or", "add" -> resultSet = resultSet.union(set)
                    "remove" -> resultSet = resultSet.toMutableSet().apply { removeAll(set) }
                    "orNot" -> {
                        val all = PlayerTimes.getAllPlayers().toSet().toMutableSet()
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