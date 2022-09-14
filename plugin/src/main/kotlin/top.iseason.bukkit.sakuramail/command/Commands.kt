package top.iseason.bukkit.sakuramail.command

import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessages

fun mainCommand() {
    command("sakuramail") {
        alias = arrayOf("smail", "mail")
        default = PermissionDefault.TRUE
        async = true
        description = "系统邮件根节点"
        node(SystemMailCommand).apply {
            addSubNode(SystemMailCreateCommand)
            addSubNode(SystemMailEditCommand)
            addSubNode(SystemMailUploadCommand)
            addSubNode(SystemMailDownloadCommand)
            addSubNode(SystemMailURemoveCommand)
        }
        node(ReceiverCommand).apply {
            addSubNode(ReceiverSetCommand)
            addSubNode(ReceiverAddCommand)
            addSubNode(ReceiverRemoveCommand)
            addSubNode(ReceiverTestCommand)
            addSubNode(ReceiverUploadCommand)
            addSubNode(ReceiverDownloadCommand)
            addSubNode(ReceiverExportCommand)
        }
        node(SenderCommand).apply {
            addSubNode(SenderCreateCommand)
            addSubNode(SenderRemoveCommand)
            addSubNode(SenderSendCommand)
            addSubNode(SenderUploadCommand)
            addSubNode(SenderDownloadCommand)
        }
        node(AdminCommand).apply {
            addSubNode(AdminRemoveCommand)
            addSubNode(AdminRemoveAllCommand)
            addSubNode(AdminOpenCommand)
        }
        node("upload") {
            description = "上传数据到数据库"
            default = PermissionDefault.OP
            async = true
            param("<type>", listOf("all", "sender", "systemMail", "receiver"))
            executor {
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@executor
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
            }
        }
        node(
            "download"
        ) {
            default = PermissionDefault.OP
            description = "从数据库下载数据"
            async = true
            param("<type>", listOf("all", "sender", "systemMail", "receiver"))
            executor {
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@executor
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
            }
        }
        node("open") {
            description = "打开邮箱"
            async = true
            isPlayerOnly = true
            executor {
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@executor
                }
                val player = it as Player
                val playerUI = MailBoxGUIYml.getPlayerUI(player)
                playerUI.update()
                playerUI.openFor(player)
            }
        }
        node(
            "openFor"
        ) {
            description = "为玩家打开邮箱"
            async = true
            default = PermissionDefault.OP
            param("<player>", suggestRuntime = ParamSuggestCache.playerParam)
            onExecute = onExecute@{
                if (!DatabaseConfig.isConnected) {
                    it.sendColorMessages(Lang.database_error)
                    return@onExecute
                }
                val player = getParam<Player>(0)
                val playerUI = MailBoxGUIYml.getPlayerUI(player)
                playerUI.update()
                playerUI.openFor(player)
            }
        }
        node("debug") {
            description = "切换调试模式"
            async = true
            default = PermissionDefault.OP
            onExecute = {
                SimpleLogger.isDebug = !SimpleLogger.isDebug
                it.sendColorMessages("&a调试模式: &6${SimpleLogger.isDebug}")
            }
        }
        node("reConnect") {
            description = "重新链接数据库"
            async = true
            default = PermissionDefault.OP
            onExecute = {
                DatabaseConfig.reConnected()
                it.sendColorMessages("&a操作完成!")
            }
        }

    }
}