package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime

object MailRecords : IntIdTable() {
    /**
     * 玩家uid
     */
    val player = uuid("player")

    /**
     * 关联的邮件
     */
    val mail = varchar("mail", 255)

    /**
     * 发送时间
     */
    val sendTime = datetime("sendTime")

    /**
     * 领取时间
     */
    val acceptTime = datetime("acceptTime").nullable()
}

class MailRecord(id: EntityID<Int>) : IntEntity(id) {

    var player by MailRecords.player
    var mail by MailRecords.mail
    var sendTime by MailRecords.sendTime
    var acceptTime by MailRecords.acceptTime

    companion object : IntEntityClass<MailRecord>(MailRecords)
}