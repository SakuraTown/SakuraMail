package top.iseason.bukkit.sakuramail.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.bukkittemplate.command.CommandNode

object SenderCommand : CommandNode(
    name = "sender",
    description = "邮件发送者相关操作",
    default = PermissionDefault.OP,
)
//TODO:完善命令