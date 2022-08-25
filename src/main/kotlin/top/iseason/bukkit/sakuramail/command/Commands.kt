package top.iseason.bukkit.sakuramail.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.ParamSuggestCache
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml

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
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@onExecute true
                }
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
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@onExecute true
                }
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
        node(
            "open",
            description = "打开邮箱",
            async = true,
            isPlayerOnly = true
        ) {
            onExecute {
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@onExecute true
                }
                val player = it as Player
                val playerUI = MailBoxGUIYml.getPlayerUI(player)
                playerUI.update()
                playerUI.openFor(player)
                true
            }
        }
        node(
            "openFor",
            description = "为玩家打开邮箱",
            async = true,
            default = PermissionDefault.OP,
            params = arrayOf(Param("[player]", suggestRuntime = ParamSuggestCache.playerParam))
        ) {
            onExecute {
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@onExecute true
                }
                val player = getParam<Player>(0)
                val playerUI = MailBoxGUIYml.getPlayerUI(player)
                playerUI.update()
                playerUI.openFor(player)
                true
            }
        }
    }
}