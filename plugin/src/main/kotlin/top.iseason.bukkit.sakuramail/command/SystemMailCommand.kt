package top.iseason.bukkit.sakuramail.command

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.SystemMailYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkit.sakuramail.database.MailRecords
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCaches
import top.iseason.bukkit.sakuramail.database.SystemMails
import top.iseason.bukkit.sakuramail.database.SystemMails.has
import top.iseason.bukkittemplate.command.CommandNode
import top.iseason.bukkittemplate.command.Param
import top.iseason.bukkittemplate.command.Params
import top.iseason.bukkittemplate.command.ParmaException
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.ui.UIListener
import top.iseason.bukkittemplate.utils.bukkit.EntityUtils.getHeldItem
import top.iseason.bukkittemplate.utils.bukkit.IOUtils.onItemInput
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object SystemMailCommand : CommandNode(
    name = "systemMail",
    description = "系统邮件相关操作",
    default = PermissionDefault.OP,
)

object SystemMailCreateCommand : CommandNode(
    name = "create",
    description = "创建系统邮件,手上的是图标",
    default = PermissionDefault.OP,
    params = listOf(Param("<id>"), Param("[title]")),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        val id = getParam<String>(0)
        if (SystemMails.has(id)) throw ParmaException("&cid已存在!")
        val title = getOptionalParam<String>(1)?.toColor() ?: ""
        val heldItem = (it as? Player)?.getHeldItem() ?: ItemStack(Material.STONE)
        val systemMailYml = SystemMailYml(id, heldItem, title)
        if (it !is Player) return@onExecute
        it.onItemInput(async = true) { inv ->
            val mutableMapOf = mutableMapOf<Int, ItemStack>()
            inv.contents.forEachIndexed { index, itemStack ->
                if (itemStack == null) return@forEachIndexed
                mutableMapOf[index] = itemStack
            }
            systemMailYml.items = mutableMapOf
            SystemMailsYml.mails[systemMailYml.id] = systemMailYml
            SystemMailsYml.saveToYml()
        }
    }
}

object SystemMailEditCommand : CommandNode(
    name = "edit",
    description = "编辑系统邮件的物品,其他操作请从yml修改",
    default = PermissionDefault.OP,
    params = listOf(
        Param(
            "<id>",
            suggestRuntime = { SystemMailsYml.mails.mapNotNull { if (it.value.type == "system") it.key else null } })
    ),
    isPlayerOnly = true,
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = {
        val player = it as Player
        val id = getParam<String>(0)
        val mailYml = SystemMailsYml.getMailYml(id) ?: throw ParmaException("邮件不存在!")
        val createInventory = Bukkit.createInventory(null, 54, "&a请输入物品".toColor())
        mailYml.items.forEach { (i, itemStack) -> createInventory.setItem(i, itemStack) }
        player.onItemInput(createInventory, true) { inv ->
            val mutableMapOf = mutableMapOf<Int, ItemStack>()
            inv.contents.forEachIndexed { index, itemStack ->
                if (itemStack == null) return@forEachIndexed
                mutableMapOf[index] = itemStack
            }
            mailYml.items = mutableMapOf
            SystemMailsYml.saveToYml()
            player.sendColorMessage("&a邮件已保存!")
        }
    }
}

object SystemMailURemoveCommand : CommandNode(
    name = "remove",
    description = "删除邮件",
    default = PermissionDefault.OP,
    params = listOf(Param("<id>", suggestRuntime = { SystemMailsYml.mails.keys })),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        val param = getParam<String>(0)
        val result = dbTransaction {
            MailRecords.deleteWhere(1) { MailRecords.mail eq param }
            SystemMails.deleteWhere(1) { SystemMails.id eq param }
        }
        if (result == 0 && SystemMailsYml.mails.remove(param) == null)
            it.sendColorMessage("&a邮件不存在!")
        else {
            it.sendColorMessage("&a邮件已删除!")
            PlayerMailRecordCaches.clear()
            //关闭打开的UI，强制刷新
            UIListener.onDisable()
        }
        SystemMailsYml.saveToYml()
    }
}


object SystemMailUploadCommand : CommandNode(
    name = "upload",
    description = " 上传邮件数据至数据库",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        runCatching { SystemMailsYml.upload() }.getOrElse { error ->
            error.printStackTrace()
            throw ParmaException("&cSystemMail数据上传异常!")
        }
        it.sendColorMessage("&aSystemMail数据上传成功!")
    }
}

object SystemMailDownloadCommand : CommandNode(
    name = "download",
    description = " 从数据库下载邮件数据至本地",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Unit)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessage(Lang.database_error)
            return@onExecute
        }
        runCatching { SystemMailsYml.downloadFromDatabase() }.getOrElse { error ->
            error.printStackTrace()
            throw ParmaException("&cSystemMail数据下载异常!")
        }
        it.sendColorMessage("&aSystemMail数据下载成功!")
    }
}