package top.iseason.bukkit.sakuramail.command

import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailSenderYml
import top.iseason.bukkit.sakuramail.config.MailSendersYml
import top.iseason.bukkit.sakuramail.database.MailSenders
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.Params
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

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
    params = listOf(
        Param("<id>"),
        Param("<type>", listOf("login", "onTime", "period", "manual")),
        Param("[parma]")
    )
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = {
        val id = getParam<String>(0)
        var type = getParam<String>(1).lowercase()
        if (type !in setOf("login", "ontime", "period")) type = "manual"
        val sender = MailSendersYml.getSender(id)
        if (sender != null) throw ParmaException("&ciID已存在!")
        val param = getOptionalParam<String>(2) ?: ""
        MailSendersYml.senders[id] = MailSenderYml(id, type, param, emptyList(), emptyList())
        it.sendColorMessage("&a创建成功，细节请前往配置文件修改!")
        MailSendersYml.saveAll()
        true
    }
}

object SenderRemoveCommand : CommandNode(
    name = "remove",
    description = "删除邮件发送者",
    default = PermissionDefault.OP,
    async = true,
    params = listOf(
        Param("<id>", suggestRuntime = { MailSendersYml.senders.keys })
    )
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val id = getParam<String>(0)
        if (dbTransaction { MailSenders.deleteWhere { MailSenders.id eq id } } == 0 && MailSendersYml.senders.remove(id) == null)
            it.sendColorMessage("&cID不存在!")
        else it.sendColorMessage("&a创建成功，细节请前往配置文件修改!")
        MailSendersYml.saveAll()
    }
}

object SenderSendCommand : CommandNode(
    name = "send",
    description = "手动发送一封邮件,login类型的无效",
    default = PermissionDefault.OP,
    async = true,
    params = listOf(
        Param("<id>", suggestRuntime = { MailSendersYml.senders.keys })
    )
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val id = getParam<String>(0)
        val sender = MailSendersYml.getSender(id) ?: throw ParmaException("&cID不存在!")
        if (sender.type.lowercase() == "login") throw ParmaException("&clogin类型的邮件无法手动触发!")
        sender.onSend(sender.getAllReceivers(sender.receivers), it)
        it.sendColorMessage("&a发送成功!")
    }
}

object SenderUploadCommand : CommandNode(
    name = "upload",
    description = "上传邮件发送者到数据库",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        runCatching {
            MailSendersYml.upload()
        }.getOrElse { throw ParmaException("&cMailSender数据上传异常!") }
        it.sendColorMessage("&aMailSender数据上传成功!")
        MailSendersYml.saveAll()
    }
}

object SenderDownloadCommand : CommandNode(
    name = "download",
    description = "从数据库下载邮件发送者到本地",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        runCatching {
            MailSendersYml.download()
        }.getOrElse { throw ParmaException("&cMailSender数据下载异常!") }
        it.sendColorMessage("&aMailSender数据下载成功!")
        MailSendersYml.saveAll()
    }
}

