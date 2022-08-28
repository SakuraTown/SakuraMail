package top.iseason.bukkit.sakuramail.command

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.*
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkit.sakuramail.database.MailRecord
import top.iseason.bukkit.sakuramail.database.MailRecords

object AdminCommand : CommandNode(
    name = "admin",
    description = "管理玩家邮件",
    default = PermissionDefault.OP,
)

object AdminRemoveCommand : CommandNode(
    "remove",
    description = "删除玩家某种邮件(全部、领取的、过期的)",
    async = true,
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("<type>", suggest = listOf("all", "accepted", "expired")),
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        val player = getParam<Player>(0).uniqueId
        val type = getParam<String>(1)
        transaction {
            when (type.lowercase()) {
                "all" -> MailRecords.deleteWhere { MailRecords.player eq player }
                "accepted" -> MailRecords.deleteWhere { MailRecords.player eq player and (MailRecords.acceptTime neq null) }
                "expired" -> {
                    MailRecord.find { MailRecords.player eq player }
                        .forEach { record -> if (record.isExpired()) record.delete() }
                }

                else -> {
                    throw ParmaException("&6类型 $type 不存在!")
                }
            }
        }
        it.sendColorMessages("&a删除成功!")
        true
    }
}

object AdminRemoveAllCommand : CommandNode(
    "removeAll",
    description = "删除所有玩家某种邮件(全部、领取的、过期的)",
    async = true,
    default = PermissionDefault.OP,
    params = arrayOf(
        Param("<type>", suggest = listOf("all", "accepted", "expired")),
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        val type = getParam<String>(0).lowercase()
        transaction {
            when (type.lowercase()) {
                "all" -> MailRecords.deleteAll()
                "accepted" -> MailRecords.deleteWhere { MailRecords.acceptTime neq null }
                "expired" -> {
                    MailRecord.all().forEach { record -> if (record.isExpired()) record.delete() }
                }

                else -> {
                    throw ParmaException("&6类型 $type 不存在!")
                }
            }
        }
        it.sendColorMessages("&a删除成功!")
        true
    }
}

object AdminOpenCommand : CommandNode(
    "open",
    description = "查看玩家的邮箱，你可以替他操作",
    async = true,
    default = PermissionDefault.OP,
    isPlayerOnly = true,
    params = arrayOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
    )
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        val player = getParam<Player>(0)
        val playerUI = MailBoxGUIYml.getPlayerUI(player)
        playerUI.update()
        playerUI.openFor(it as Player)
        true
    }
}