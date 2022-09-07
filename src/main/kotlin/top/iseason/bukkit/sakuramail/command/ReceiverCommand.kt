package top.iseason.bukkit.sakuramail.command

import org.bukkit.Bukkit
import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import top.iseason.bukkit.bukkittemplate.command.CommandNode
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.Params
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.config.dbTransaction
import top.iseason.bukkit.bukkittemplate.utils.MessageUtils.sendColorMessage
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.SakuraMail
import top.iseason.bukkit.sakuramail.config.MailReceiversYml
import top.iseason.bukkit.sakuramail.database.MailReceivers
import java.io.BufferedWriter
import java.io.File
import java.io.FileWriter
import java.util.*


object ReceiverCommand : CommandNode(
    name = "receiver",
    description = "邮件接收者相关操作",
    default = PermissionDefault.OP,
) {
    private val op1 = listOf("--and", "--or", "--andNot", "--orNot")
    private val op2 = listOf("loginTime", "quitTime", "totaltime")
    private val op3 = listOf("before_time", "after_time", "between_time1_time2")
    private val op4 = listOf("greater_time", "less_time", "between_time1_time2")
    private val op5 = listOf("online", "offline", "all", "uuids", "names", "permission_xxx", "gamemode_xxx")
    val listOf = mutableListOf<String>()

    init {
        for (s1 in op1) {
            for (s2 in op2) {
                for (s3 in op3) {
                    listOf.add("$s1,${s2}_$s3")
                }
            }
            for (s4 in op4) {
                listOf.add("$s1,playTime_$s4")
            }
            for (s5 in op5) {
                listOf.add("$s1,$s5")
            }
        }
    }
}

object ReceiverAddCommand : CommandNode(
    name = "add",
    description = "设置邮件接收者参数",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys }),
        Param("[parms]", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
        Param("", suggest = ReceiverCommand.listOf),
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = {
        val id = getParam<String>(0)
        val copyOfRange = params.copyOfRange(1, params.size)
        copyOfRange.forEachIndexed { index, s ->
            copyOfRange[index] = s.removePrefix("--").replace('_', ' ')
        }
        val orDefault = MailReceiversYml.timeReceivers.getOrDefault(id, mutableListOf())
        orDefault.addAll(copyOfRange)
        MailReceiversYml.timeReceivers[id] = orDefault
        MailReceiversYml.save()
        it.sendColorMessage("$id 的参数为")
        it.sendColorMessage(orDefault)
    }
}

object ReceiverSetCommand : CommandNode(
    name = "set",
    description = "设置邮件接收者参数",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys }),
        Param("[index]"),
        Param("[parms]", suggest = ReceiverCommand.listOf)
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = {
        val id = getParam<String>(0)
        val strings = MailReceiversYml.timeReceivers[id] ?: throw ParmaException("&cid不存在!")
        val index = getParam<Int>(1)
        strings[index] = getParam<String>(2).removePrefix("--").replace('_', ' ')
        MailReceiversYml.save()
        it.sendColorMessage("$id 的参数为")
        it.sendColorMessage(strings)
    }
}

object ReceiverRemoveCommand : CommandNode(
    name = "remove",
    description = "删除邮件接收者",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val id = getParam<String>(0)
        val remove = MailReceiversYml.timeReceivers.remove(id)
        MailReceiversYml.save()
        val count = dbTransaction {
            MailReceivers.deleteWhere { MailReceivers.id eq id }
        }
        if (remove == null && count == 0) throw ParmaException("&cid不存在!")
        it.sendColorMessage("&a删除成功")
    }
}

object ReceiverTestCommand : CommandNode(
    name = "test",
    description = "测试邮件接收者",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val id = getParam<String>(0)
        val receivers = MailReceiversYml.getReceivers(id) ?: throw ParmaException("&cid不存在!")
        if (receivers.isEmpty()) it.sendColorMessage("&6没有符合条件的接收者!")
        else it.sendColorMessage(
            "&a找到 ${receivers.size} 个接收者,前5个为: &6${
                receivers.joinToString(
                    ", ",
                    limit = 5,
                    truncated = "..."
                ) { uid ->
                    (Bukkit.getPlayer(uid) ?: Bukkit.getOfflinePlayer(uid)).name.toString()
                }
            }"
        )
    }
}

object ReceiverExportCommand : CommandNode(
    name = "export",
    description = "导出符合邮件接收者的玩家",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys }),
        Param("[type]", suggest = listOf("name", "uuid"))
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{ it ->
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val id = getParam<String>(0)
        val receivers: List<UUID> =
            MailReceiversYml.getReceivers(id) ?: throw ParmaException("&cid不存在!")
        if (receivers.isEmpty()) it.sendColorMessage("&6没有符合条件的接收者!")
        else it.sendColorMessage("&a找到 ${receivers.size} 个接收者")
        val type = getOptionalParam<String>(1) ?: "name"
        val results: List<String> = if (type == "name")
            receivers.mapNotNull { (Bukkit.getPlayer(it) ?: Bukkit.getOfflinePlayer(it)).name }
        else receivers.map { it.toString() }
        val file = File(SakuraMail.javaPlugin.dataFolder, "export${File.separatorChar}$id-$type.txt")
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        BufferedWriter(FileWriter(file)).use { bw ->
            for (receiver in results) {
                bw.write(receiver)
                bw.newLine()
            }
        }
        it.sendColorMessage("&a文件已输出: $file")
    }
}

object ReceiverUploadCommand : CommandNode(
    name = "upload",
    description = "上传邮件接收者数据",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        kotlin.runCatching {
            MailReceiversYml.upload()
        }.getOrElse { e ->
            e.printStackTrace()
            throw ParmaException("&cMailReceiver数据上传异常!")
        }
        it.sendColorMessage("&aMailReceiver数据上传成功!")
    }
}

object ReceiverDownloadCommand : CommandNode(
    name = "download",
    description = "下载邮件接收者数据",
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<id>", suggestRuntime = { MailReceiversYml.timeReceivers.keys })
    ),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        kotlin.runCatching {
            MailReceiversYml.download()
        }.getOrElse { e ->
            e.printStackTrace()
            throw ParmaException("&cMailReceiver数据下载异常!")
        }
        it.sendColorMessage("&aMailReceiver数据下载成功!")
    }
}