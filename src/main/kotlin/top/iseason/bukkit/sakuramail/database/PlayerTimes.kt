package top.iseason.bukkit.sakuramail.database

import org.bukkit.entity.Player
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.SqlExpressionBuilder.between
import org.jetbrains.exposed.sql.SqlExpressionBuilder.eq
import org.jetbrains.exposed.sql.SqlExpressionBuilder.greater
import org.jetbrains.exposed.sql.SqlExpressionBuilder.less
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.sakuramail.utils.TimeUtils
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

/**
 * 记录玩家一次服务器游玩的记录
 * 下线时间 quitTime 为 null 时表示还在服务器
 */
object PlayerTimes : IntIdTable() {
    /**
     * 玩家uuid
     */
    val player = uuid("player")

    /**
     * 登录时间
     */
    val loginTime = datetime("login_time")

    /**
     * 退出时间
     */
    val quitTime = datetime("quit_time").nullable()

    /**
     * 一次游玩时间
     */
    val playTime = duration("play_time").nullable()

    /**
     * 获取总共游玩的时间
     */
    fun getTotalTime(
        uuid: UUID,
        start: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0),
        end: LocalDateTime = LocalDateTime.of(100000, 1, 1, 0, 0)
    ): Duration {
        var duration = Duration.ZERO
        val playerTimes =
            PlayerTime.find { player eq uuid and (loginTime.between(start, end) or loginTime.between(start, end)) }
        val now = LocalDateTime.now()
        for (playerTime in playerTimes) {
            val dura = when {
                playerTime.loginTime.isBefore(start) -> {
                    if (playerTime.playTime == null) Duration.between(start, now)
                    else
                        playerTime.playTime!!.minus(Duration.between(playerTime.loginTime, start))
                }

                playerTime.quitTime == null -> Duration.between(playerTime.loginTime, now)
                playerTime.quitTime!!.isAfter(end) -> Duration.between(playerTime.loginTime, end)
                else -> playerTime.playTime!!
            }
            duration = duration.plus(dura)
        }
        return duration
    }

    fun getTotalTimes(
        start: LocalDateTime = LocalDateTime.of(0, 1, 1, 0, 0),
        end: LocalDateTime = LocalDateTime.of(100000, 1, 1, 0, 0)
    ): Map<UUID, Duration> {
        val mutableMapOf = mutableMapOf<UUID, Duration>()
        val now = LocalDateTime.now()
        transaction {
            for (playerTime in PlayerTime.find { loginTime.between(start, end) or loginTime.between(start, end) }) {
                val duration = when {
                    playerTime.loginTime.isBefore(start) -> {
                        if (playerTime.playTime == null) Duration.between(start, now)
                        else
                            playerTime.playTime!!.minus(Duration.between(playerTime.loginTime, start))
                    }

                    playerTime.quitTime == null -> Duration.between(playerTime.loginTime, now)
                    playerTime.quitTime!!.isAfter(end) -> Duration.between(playerTime.loginTime, end)
                    else -> playerTime.playTime!!
                }
                val rawDuration = mutableMapOf[playerTime.player]
                if (rawDuration == null) mutableMapOf[playerTime.player] = duration
                else mutableMapOf[playerTime.player] = rawDuration.plus(duration)
            }
        }
        return mutableMapOf
    }

    /**
     * 获取所有数据库记录的玩家
     */
    fun getAllPlayers() = transaction {
        PlayerTimes.slice(player).selectAll().distinctBy { player }.map { it[player] }
    }


    /**
     * 解析字符串为sql条件
     * 例子:
     * loginTime before time
     * quitTime after time
     * loginTime between time1 time2
     * quitTime between time1 time2
     * playTime greater time
     * playTime less time
     * 相对时间为 PnYnMnDTnHnMnS
     * 绝对时间为 2011-12-03T10:15:30
     * playTime 不支持绝对时间
     */
    fun parseOP(str: String): Op<Boolean>? {
        val split = str.split(' ')
        val type = split.getOrNull(0)?.lowercase() ?: return null
        val op = split.getOrNull(1)?.lowercase() ?: return null
        when (type) {
            // 登录、退出时间
            "logintime", "quittime" -> {
                val timeStr = split.getOrNull(2) ?: return null
                val time = TimeUtils.parseTime(timeStr)
                val timeColum = if (type == "logintime") loginTime else quitTime
                if ("before" == op) return timeColum.between(LocalDateTime.of(0, 1, 1, 0, 0), time)
                if ("after" == op) return timeColum.between(time, LocalDateTime.of(100000, 1, 1, 0, 0))
                if ("between" == op) {
                    val timeStr2 = split.getOrNull(3) ?: return null
                    val time2 = TimeUtils.parseTime(timeStr2)
                    return timeColum.between(time, time2)
                }
            }
            //一次游戏时间
            "playtime" -> {
                val durationStr = split.getOrNull(2) ?: return null
                val duration = TimeUtils.parseDuration(durationStr) ?: return null
                if ("greater" == op) return playTime.greater(duration)
                if ("less" == op) return playTime.less(duration)
                if ("between" == op) {
                    val durationStr2 = split.getOrNull(3) ?: return null
                    val duration2 = TimeUtils.parseDuration(durationStr2) ?: return null
                    return playTime.between(duration, duration2)
                }
            }

        }
        return null
    }

    /**
     * 解析参数为sql组合条件
     */
    fun parseArgs(args: List<String>, player: Player? = null): Op<Boolean>? {
        var build: OpBuilder? = null
        if (player != null) {
            build = OpBuilder(PlayerTimes.player eq player.uniqueId)
        }
        for (arg in args) {
            val split = arg.removePrefix("--").split(',', limit = 2)
            //默认and
            var ex: Op<Boolean>? = null
            var op = "and"
            if (split.size == 1) {
                ex = parseOP(split[0])
            } else if (split.size >= 2) {
                ex = parseOP(split[1])
                op = split[0]
            }
            if (ex == null) continue
            if (build == null) {
                build = OpBuilder(ex)
            } else {
                build.addOp(op, ex)
            }
        }
        return build?.build()
    }

}

/**
 * 链式构建条件
 */
class OpBuilder(private var ex: Op<Boolean>) {

    /**
     * 添加组合雕件
     */
    fun addOp(op: String, ex: Op<Boolean>): OpBuilder {
        this.ex = when (op) {
            "and" -> this.ex and ex
            "or" -> this.ex or ex
            "andNot" -> this.ex.andNot { ex }
            "orNot" -> this.ex.orNot { ex }
            else -> this.ex
        }
        return this
    }

    /**
     * 生存最后的条件
     */
    fun build() = ex
}

/**
 * 记录玩家一次服务器游玩的记录
 * 下线时间 quitTime 为 null 时表示还在服务器
 */
class PlayerTime(
    id: EntityID<Int>
) : IntEntity(id) {
    companion object : IntEntityClass<PlayerTime>(PlayerTimes)

    /**
     * 玩家uuid
     */
    var player by PlayerTimes.player

    /**
     * 登录时间
     */
    var loginTime by PlayerTimes.loginTime

    /**
     * 退出时间
     */
    var quitTime by PlayerTimes.quitTime

    /**
     * 一次游玩时间
     */
    var playTime by PlayerTimes.playTime


}