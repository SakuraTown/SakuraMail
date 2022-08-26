package top.iseason.bukkit.sakuramail.command

import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.CommandNode
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.Params
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.Lang
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
    async = true,
    params = arrayOf(
        Param("<id>"),
        Param("<type>", listOf("login", "onTime", "period", "manual")),
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
    async = true,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailSendersYml.senders.keys })
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        val id = getParam<String>(0)
        if (transaction { MailSenders.deleteWhere { MailSenders.id eq id } } == 0 && MailSendersYml.senders.remove(id) == null)
            it.sendColorMessages("&cID不存在!")
        else it.sendColorMessages("&a创建成功，细节请前往配置文件修改!")
        MailSendersYml.saveAll()
        true
    }
}

object SenderSendCommand : CommandNode(
    name = "send",
    description = "手动发送一封邮件,login类型的无效",
    default = PermissionDefault.OP,
    async = true,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailSendersYml.senders.keys })
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        val id = getParam<String>(0)
        val sender = MailSendersYml.getSender(id) ?: throw ParmaException("&cID不存在!")
        if (sender.type.lowercase() == "login") throw ParmaException("&clogin类型的邮件无法手动触发!")
        sender.onSend(sender.getAllReceivers(sender.receivers), it)
        it.sendColorMessages("&a发送成功!")
        true
    }
}

object SenderUploadCommand : CommandNode(
    name = "upload",
    description = "上传邮件发送者到数据库",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        runCatching {
            MailSendersYml.upload()
        }.getOrElse { throw ParmaException("&cMailSender数据上传异常!") }
        it.sendColorMessages("&aMailSender数据上传成功!")
        MailSendersYml.saveAll()
        true
    }
}

object SenderDownloadCommand : CommandNode(
    name = "download",
    description = "从数据库下载邮件发送者到本地",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        runCatching {
            MailSendersYml.download()
        }.getOrElse { throw ParmaException("&cMailSender数据下载失异常!") }
        it.sendColorMessages("&aMailSender数据下载成功!")
        MailSendersYml.saveAll()
        true
    }
}

