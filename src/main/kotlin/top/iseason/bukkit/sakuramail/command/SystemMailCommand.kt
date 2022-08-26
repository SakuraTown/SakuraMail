package top.iseason.bukkit.sakuramail.command

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.command.CommandSender
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.CommandNode
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.Params
import top.iseason.bukkit.bukkittemplate.command.ParmaException
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.ui.UIListener
import top.iseason.bukkit.bukkittemplate.utils.bukkit.getHeldItem
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.bukkittemplate.utils.toColor
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.SystemMailYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkit.sakuramail.database.MailRecordCaches
import top.iseason.bukkit.sakuramail.database.MailRecords
import top.iseason.bukkit.sakuramail.database.SystemMails
import top.iseason.bukkit.sakuramail.database.SystemMails.has
import top.iseason.bukkit.sakuramail.utils.IOUtils.onItemInput

object SystemMailCommand : CommandNode(
    name = "systemMail",
    description = "系统邮件相关操作",
    default = PermissionDefault.OP,
)

object SystemMailCreateCommand : CommandNode(
    name = "create",
    description = "创建系统邮件,手上的是图标",
    default = PermissionDefault.OP,
    params = arrayOf(Param("<id>"), Param("[title]")),
    async = true
) {
    override var onExecute: (Params.(CommandSender) -> Boolean)? = onExecute@{
        val id = getParam<String>(0)
        if (SystemMails.has(id)) throw ParmaException("&cid已存在!")
        val title = getOptionalParam<String>(1)?.toColor() ?: ""
        val heldItem = (it as? Player)?.inventory?.getHeldItem() ?: ItemStack(Material.STONE)
        val systemMailYml = SystemMailYml(id, heldItem, title)
        if (it !is Player) return@onExecute true
        submit {
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
        true
    }
}

object SystemMailEditCommand : CommandNode(
    name = "edit",
    description = "编辑系统邮件的物品,其他操作请从yml修改",
    default = PermissionDefault.OP,
    params = arrayOf(Param("<id>", suggestRuntime = { SystemMailsYml.mails.keys })),
    isPlayerOnly = true,
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = {
        val player = it as Player
        val id = getParam<String>(0)
        val mailYml = SystemMailsYml.getMailYml(id) ?: throw ParmaException("邮件不存在!")
        val createInventory = Bukkit.createInventory(null, 54, "&a请输入物品".toColor())
        mailYml.items.forEach { (i, itemStack) -> createInventory.setItem(i, itemStack) }
        submit {
            player.onItemInput(createInventory, true) { inv ->
                val mutableMapOf = mutableMapOf<Int, ItemStack>()
                inv.contents.forEachIndexed { index, itemStack ->
                    if (itemStack == null) return@forEachIndexed
                    mutableMapOf[index] = itemStack
                }
                mailYml.items = mutableMapOf
                SystemMailsYml.saveToYml()
                player.sendColorMessages("&a邮件已保存!")
            }
        }
        true
    }
}

object SystemMailURemoveCommand : CommandNode(
    name = "remove",
    description = "删除邮件",
    default = PermissionDefault.OP,
    params = arrayOf(Param("<id>", suggestRuntime = { SystemMailsYml.mails.keys })),
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        val param = getParam<String>(0)
        val result = transaction {
            MailRecords.deleteWhere(1) { MailRecords.mail eq param }
            SystemMails.deleteWhere(1) { SystemMails.id eq param }
        }
        if (result == 0 && SystemMailsYml.mails.remove(param) == null)
            it.sendColorMessages("&a邮件不存在!")
        else {
            it.sendColorMessages("&a邮件已删除!")
            MailRecordCaches.clear()
            //关闭打开的UI，强制刷新
            UIListener.onDisable()
        }
        SystemMailsYml.saveToYml()
        true
    }
}


object SystemMailUploadCommand : CommandNode(
    name = "upload",
    description = " 上传邮件数据至数据库",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        runCatching { SystemMailsYml.upload() }.getOrElse { error ->
            error.printStackTrace()
            throw ParmaException("&cSystemMail数据上传异常!")
        }
        it.sendColorMessages("&aSystemMail数据上传成功!")
        true
    }
}

object SystemMailDownloadCommand : CommandNode(
    name = "download",
    description = " 从数据库下载邮件数据至本地",
    default = PermissionDefault.OP,
    async = true
) {
    override var onExecute: (Params.(sender: CommandSender) -> Boolean)? = onExecute@{
        if (!DatabaseConfig.isConnected) {
            it.sendColorMessages(Lang.database_error)
            return@onExecute true
        }
        runCatching { SystemMailsYml.downloadFromDatabase() }.getOrElse { error ->
            error.printStackTrace()
            throw ParmaException("&cSystemMail数据下载异常!")
        }
        it.sendColorMessages("&aSystemMail数据下载成功!")
        true
    }
}