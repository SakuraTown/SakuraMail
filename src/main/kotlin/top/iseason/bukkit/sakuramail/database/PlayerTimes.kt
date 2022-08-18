package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.javatime.duration

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
     * 游玩时间
     */
    val playTime = duration("play_time").nullable()

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
     * 游玩时间
     */
    var playTime by PlayerTimes.playTime

}