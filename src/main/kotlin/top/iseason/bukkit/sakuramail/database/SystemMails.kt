package top.iseason.bukkit.sakuramail.database

import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IdTable
import org.jetbrains.exposed.sql.javatime.duration
import org.jetbrains.exposed.sql.select
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.StringEntity
import top.iseason.bukkit.bukkittemplate.config.StringEntityClass
import top.iseason.bukkit.bukkittemplate.config.StringIdTable
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkit.sakuramail.config.SystemMailYml

object SystemMails : StringIdTable() {
    /**
     * 邮件类型，默认为系统邮件，不会被删除
     */
    val type = varchar("type", 255).default("system")

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

    /**
     * 有效时间
     */
    val expire = duration("expire").nullable()

    /**
     * 判断Table是否有某个id的记录
     */
    fun <T : Comparable<T>> IdTable<T>.has(id: T): Boolean {
        return try {
            transaction {
                !this@has.slice(this@has.id).select { this@has.id eq id }.limit(1).empty()
            }
        } catch (e: Exception) {
            false
        }
    }
}

class SystemMail(
    id: EntityID<String>
) : StringEntity(id) {
    companion object : StringEntityClass<SystemMail>(SystemMails)

    var icon by SystemMails.icon
    var type by SystemMails.type
    var title by SystemMails.title
    var items by SystemMails.items
    var commands by SystemMails.commands
    var expire by SystemMails.expire

    /**
     * 转为yml本地对象
     */
    fun toYml(): SystemMailYml {
        val systemMailYml = SystemMailYml(id.value, ItemUtils.fromByteArray(icon.bytes), title)
        if (items != null) {
            systemMailYml.items = ItemUtils.fromByteArraysToMap(items!!.bytes) as MutableMap<Int, ItemStack>
        }
        if (commands != null) {
            systemMailYml.commands = commands!!.split(";").toMutableList()
        }
        if (expire != null) {
            systemMailYml.expire = expire
        }
        systemMailYml.type = type
        return systemMailYml
    }

}