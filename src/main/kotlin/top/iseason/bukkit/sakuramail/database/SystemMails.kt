package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.dao.id.EntityID
import top.iseason.bukkit.bukkittemplate.config.StringEntity
import top.iseason.bukkit.bukkittemplate.config.StringEntityClass
import top.iseason.bukkit.bukkittemplate.config.StringIdTable
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkit.sakuramail.config.SystemMailYml

object SystemMails : StringIdTable() {
    /**
     * 显示的图标
     */
    val icon = blob("icon")

    /**
     * 预览时的标题
     */
    val title = varchar("title", 255)

    /**
     * 附件物品
     */
    val items = blob("items").nullable()

    /**
     * 附件 命令
     */
    val commands = text("commands").nullable()
}

class SystemMail(
    id: EntityID<String>
) : StringEntity(id) {
    companion object : StringEntityClass<SystemMail>(SystemMails)

    var icon by SystemMails.icon
    var title by SystemMails.title
    var items by SystemMails.items
    var commands by SystemMails.commands

    /**
     * 转为yml本地对象
     */
    fun toYml(): SystemMailYml {
        val systemMailYml = SystemMailYml(id.value, ItemUtils.fromByteArray(icon.bytes), title)
        if (items != null) {
            systemMailYml.items = ItemUtils.fromByteArraysToMap(items!!.bytes)
        }
        if (commands != null) {
            systemMailYml.commands = commands!!.split(";").toMutableList()
        }
        return systemMailYml
    }
}