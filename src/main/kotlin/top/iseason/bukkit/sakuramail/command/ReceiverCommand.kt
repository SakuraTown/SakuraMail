package top.iseason.bukkit.sakuramail.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.CommandNode
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.Params
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.config.MailReceiversYml
import top.iseason.bukkit.sakuramail.database.MailReceivers


object ReceiverCommand : CommandNode(
    name = "receiver",
    description = "邮件接收者相关操作",
    default = PermissionDefault.OP,
) {
    private val op1 = listOf("--and", "--or", "--andNot", "--orNot")
    private val op2 = listOf("loginTime", "quitTime", "totaltime")
    private val op3 = listOf("before.time", "after.time", "between.time1.time2")
    private val op4 = listOf("greater.time", "less.time", "between.time1.time2")
    private val op5 = listOf("online", "offline", "all", "uuids", "names", "permission.xxx", "gamemode.xxx")
    val listOf = mutableListOf<String>()

    init {
        for (s1 in op1) {
            for (s2 in op2) {
                for (s3 in op3) {
                    listOf.add("$s1:$s2.$s3")
                }
            }
            for (s4 in op4) {
                listOf.add("$s1:playTime.$s4")
            }
            for (s5 in op5) {
                listOf.add("$s1:$s5")
            }
        }
    }
}

object ReceiverAddCommand : CommandNode(
    name = "add",
    description = "设置邮件接收者参数",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailReceiversYml.timeReceivers.keys }),
        Param("<parms>", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
    ),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val id = getParam<String>(0)
        val copyOfRange = params.copyOfRange(1, params.size)
        copyOfRange.forEachIndexed { index, s ->
            copyOfRange[index] = s.removePrefix("--")
        }
        val orDefault = MailReceiversYml.timeReceivers.getOrDefault(id, mutableListOf())
        orDefault.addAll(copyOfRange)
        MailReceiversYml.timeReceivers[id] = orDefault
        MailReceiversYml.save()
        it.sendColorMessages("$id 的参数为")
        it.sendColorMessages(orDefault)
        true
    }
}

object ReceiverSetCommand : CommandNode(
    name = "set",
    description = "设置邮件接收者参数",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailReceiversYml.timeReceivers.keys }),
        Param("<index>"),
        Param("<parms>", suggest = ReceiverCommand.listOf)
    ),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val id = getParam<String>(0)
        val strings = MailReceiversYml.timeReceivers[id] ?: throw ParmaException("&cid不存在!")
        val index = getParam<Int>(1)
        strings[index] = getParam<String>(2).removePrefix("--")
        MailReceiversYml.save()
        it.sendColorMessages("$id 的参数为")
        it.sendColorMessages(strings)
        true
    }
}

object ReceiverRemoveCommand : CommandNode(
    name = "remove",
    description = "删除邮件接收者",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val id = getParam<String>(0)
        val remove = MailReceiversYml.timeReceivers.remove(id)
        MailReceiversYml.save()
        val count = transaction {
            MailReceivers.deleteWhere { MailReceivers.id eq id }
        }
        if (remove == null && count == 0) throw ParmaException("&cid不存在!")
        it.sendColorMessages("&a删除成功")
        true
    }
}

object ReceiverTestCommand : CommandNode(
    name = "test",
    description = "测试邮件接收者",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val id = getParam<String>(0)
        val receivers = MailReceiversYml.getReceivers(id) ?: throw ParmaException("&cid不存在!")
        if (receivers.isEmpty()) it.sendColorMessages("&6没有符合条件的接收者!")
        else it.sendColorMessages(
            "&a找到 ${receivers.size} 个接收者,前5个为: &6${
                receivers.joinToString(
                    ", ",
                    limit = 5,
                    truncated = "..."
                ) {
                    (Bukkit.getPlayer(it) ?: Bukkit.getOfflinePlayer(it)).name.toString()
                }
            }"
        )
        true
    }
}

object ReceiverUploadCommand : CommandNode(
    name = "upload",
    description = "上传邮件接收者数据",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        kotlin.runCatching {
            MailReceiversYml.upload()
        }.getOrElse {
            it.printStackTrace()
            throw ParmaException("&c数据上传异常!")
        }
        it.sendColorMessages("&a数据上传成功!")
        true
    }
}

object ReceiverDownloadCommand : CommandNode(
    name = "download",
    description = "下载邮件接收者数据",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("[id]", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        kotlin.runCatching {
            MailReceiversYml.download()
        }.getOrElse {
            it.printStackTrace()
            throw ParmaException("&c数据下载异常!")
        }
        it.sendColorMessages("&a数据下载成功!")
        true
    }
}