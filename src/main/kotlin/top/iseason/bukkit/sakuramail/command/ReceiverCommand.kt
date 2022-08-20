package top.iseason.bukkit.sakuramail.command

import org.bukkit.command.CommandSender
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.bukkittemplate.command.CommandNode
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.Params
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.config.MailReceiversYml
import top.iseason.bukkit.sakuramail.config.TimeMailReceiver


object ReceiverCommand : CommandNode(
    name = "receiver",
    description = "邮件接收者相关操作",
    default = PermissionDefault.OP,
) {
    private val op1 = listOf("--and", "--or", "--andNot", "--orNot")
    private val op2 = listOf("loginTime", "quitTime")
    private val op3 = listOf("before.time", "after.time", "between.time1.time2")
    private val op4 = listOf("greater.time", "less.time")
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
        val strings = MailReceiversYml.timeReceivers[id] ?: throw ParmaException("&cid不存在!")
        println(TimeMailReceiver(strings).getReceivers())
        true
    }
}