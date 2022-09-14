package top.iseason.bukkit.sakuramail.command

import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.deleteWhere
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkit.sakuramail.database.MailRecord
import top.iseason.bukkit.sakuramail.database.MailRecords
import top.iseason.bukkittemplate.command.*
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage

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
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
        Param("<type>", suggest = listOf("all", "accepted", "expired")),
    )
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val player = getParam<Player>(0).uniqueId
        val type = getParam<String>(1)
        dbTransaction {
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
        it.sendColorMessage("&a删除成功!")
    }
}

object AdminRemoveAllCommand : CommandNode(
    "removeAll",
    description = "删除所有玩家某种邮件(全部、领取的、过期的)",
    async = true,
    default = PermissionDefault.OP,
    params = listOf(
        Param("<type>", suggest = listOf("all", "accepted", "expired")),
    )
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val type = getParam<String>(0).lowercase()
        dbTransaction {
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
        it.sendColorMessage("&a删除成功!")
    }
}

object AdminOpenCommand : CommandNode(
    "open",
    description = "查看玩家的邮箱，你可以替他操作",
    async = true,
    default = PermissionDefault.OP,
    isPlayerOnly = true,
    params = listOf(
        Param("<player>", suggestRuntime = ParamSuggestCache.playerParam),
    )
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val player = getParam<Player>(0)
        val playerUI = MailBoxGUIYml.getPlayerUI(player)
        playerUI.update()
        playerUI.openFor(it as Player)
    }
}
