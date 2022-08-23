package top.iseason.bukkit.sakuramail.command

import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.CommandNode
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.Params
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.config.MailSenderYml
import top.iseason.bukkit.sakuramail.config.MailSendersYml
import top.iseason.bukkit.sakuramail.database.MailSenders

object SenderCommand : CommandNode(
    name = "sender",
    description = "邮件发送者相关操作",
    default = PermissionDefault.OP,
)

object SenderCreateCommand : CommandNode(
    name = "create",
    description = "创建邮件发送者",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]"),
        Param("[type]", listOf("login", "onTime", "period", "manual")),
        Param("[parma]")
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val id = getParam<String>(0)
        var type = getParam<String>(1).lowercase()
        if (type !in setOf("login", "ontime", "period")) type = "manual"
        val sender = MailSendersYml.getSender(id)
        if (sender != null) throw ParmaException("&ciID已存在!")
        val param = getOptionalParam<String>(2) ?: ""
        MailSendersYml.senders[id] = MailSenderYml(id, type, param, emptyList(), emptyList())
        it.sendColorMessages("&a创建成功，细节请前往配置文件修改!")
        MailSendersYml.saveAll()
        true
    }
}

object SenderRemoveCommand : CommandNode(
    name = "remove",
    description = "删除邮件发送者",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailSendersYml.senders.keys })
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val id = getParam<String>(0)
        if (transaction { MailSenders.deleteWhere { MailSenders.id eq id } } == 0 && MailSendersYml.senders.remove(id) == null)
            it.sendColorMessages("&cID不存在!")
        else it.sendColorMessages("&a创建成功，细节请前往配置文件修改!")
        MailSendersYml.saveAll()
        true
    }
}
