package top.iseason.bukkit.sakuramail.command

import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.bukkittemplate.command.Param
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
        node(SenderCommand)
        SenderCommand.apply {
            addSubNode(SenderCreateCommand)
            addSubNode(SenderRemoveCommand)
            addSubNode(SenderSendCommand)
            addSubNode(SenderUploadCommand)
            addSubNode(SenderDownloadCommand)
        }

        node(
            "upload",
            description = "上传数据到数据库",
            default = PermissionDefault.OP,
            async = true,
            params = arrayOf(Param("[type]", listOf("all", "sender", "systemMail", "receiver")))
        ) {
            onExecute {
                when (getParam<String>(0).lowercase()) {
                    "sender" -> SenderUploadCommand.onExecute?.invoke(this, it)
                    "systemMail" -> SystemMailUploadCommand.onExecute?.invoke(this, it)
                    "receiver" -> ReceiverUploadCommand.onExecute?.invoke(this, it)
                    else -> {
                        SenderUploadCommand.onExecute?.invoke(this, it)
                        SystemMailUploadCommand.onExecute?.invoke(this, it)
                        ReceiverUploadCommand.onExecute?.invoke(this, it)
                    }
                }
                true
            }
        }
        node(
            "download",
            default = PermissionDefault.OP,
            description = "从数据库下载数据",
            async = true,
            params = arrayOf(Param("[type]", listOf("all", "sender", "systemMail", "receiver")))
        ) {
            onExecute {
                when (getParam<String>(0).lowercase()) {
                    "sender" -> SenderDownloadCommand.onExecute?.invoke(this, it)
                    "systemMail" -> SystemMailDownloadCommand.onExecute?.invoke(this, it)
                    "receiver" -> ReceiverDownloadCommand.onExecute?.invoke(this, it)
                    else -> {
                        SenderDownloadCommand.onExecute?.invoke(this, it)
                        SystemMailDownloadCommand.onExecute?.invoke(this, it)
                        ReceiverDownloadCommand.onExecute?.invoke(this, it)
                    }
                }
                true
            }
        }
    }
}