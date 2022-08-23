package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.dao.id.EntityID
import top.iseason.bukkit.bukkittemplate.config.StringEntity
import top.iseason.bukkit.bukkittemplate.config.StringEntityClass
import top.iseason.bukkit.bukkittemplate.config.StringIdTable
import top.iseason.bukkit.sakuramail.config.MailSenderYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml

/**
 * 邮递员数据结构
 */
object MailSenders : StringIdTable() {
    val type = varchar("type", 255)
    val param = text("param")
    val receivers = text("receivers")
    val mails = text("mails")
}

/**
 * 邮递员记录
 */
class MailSender(
    id: EntityID<String>
) : StringEntity(id) {
    companion object : StringEntityClass<MailSender>(MailSenders)

    var type by MailSenders.type
    var param by MailSenders.param
    var receivers by MailSenders.receivers
    var mails by MailSenders.mails

    /**
     * 转yml
     */
    fun toMailSenderYml(): MailSenderYml? {
        val receivers = receivers.split(',').toList()
        val mails = mails.split(',').mapNotNull { SystemMailsYml.getMailYml(it) }.toList()
        return MailSenderYml(id.value, type, param, receivers, mails)
    }
}