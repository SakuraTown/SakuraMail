package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.dao.id.EntityID
import top.iseason.bukkit.bukkittemplate.config.StringEntity
import top.iseason.bukkit.bukkittemplate.config.StringEntityClass
import top.iseason.bukkit.bukkittemplate.config.StringIdTable
import top.iseason.bukkit.sakuramail.config.BaseMailSenderYml
import top.iseason.bukkit.sakuramail.config.LoginSenderYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml

/**
 * 邮递员数据结构
 */
object MailSenders : StringIdTable() {
    val type = varchar("type", 255)
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
    var receivers by MailSenders.receivers
    var mails by MailSenders.mails

    /**
     * 转yml
     */
    fun toMailSenderYml(): BaseMailSenderYml? {
        val receivers = receivers.split(',').toList()
        val mails = mails.split(',').mapNotNull { SystemMailsYml.getMailYml(it) }.toList()
        when (type.lowercase()) {
            "login" -> return LoginSenderYml(id.value, receivers, mails)
        }
        return null
    }
}