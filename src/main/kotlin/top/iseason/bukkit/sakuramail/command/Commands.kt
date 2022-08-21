package top.iseason.bukkit.sakuramail.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.bukkittemplate.command.commandRoot

fun command() {
    commandRoot(
        "sakuramail",
        alias = arrayOf("smail", "mail"),
        default = PermissionDefault.TRUE,
        async = true,
        description = "系统邮件根节点"
    ) {
        node(SystemMailCommand)
        SystemMailCommand.apply {
            addSubNode(SystemMailCreateCommand)
            addSubNode(SystemMailEditCommand)
            addSubNode(SystemMailUploadCommand)
            addSubNode(SystemMailDownloadCommand)
            addSubNode(SystemMailURemoveCommand)
        }
        node(ReceiverCommand)
        ReceiverCommand.apply {
            addSubNode(ReceiverSetCommand)
            addSubNode(ReceiverAddCommand)
            addSubNode(ReceiverRemoveCommand)
            addSubNode(ReceiverTestCommand)
            addSubNode(ReceiverUploadCommand)
            addSubNode(ReceiverDownloadCommand)
            addSubNode(ReceiverExportCommand)
        }
    }
}