package top.iseason.bukkit.sakuramail.database

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.javatime.datetime
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.config.MailBoxGUIConfig
import top.iseason.bukkit.sakuramail.config.SystemMailYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkit.sakuramail.hook.PlaceHolderHook
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.*

object MailRecords : IntIdTable() {
    /**
     * 玩家uid
     */
    val player = uuid("player")

    /**
     * 关联的邮件
     */
    val mail = varchar("mail", 255)

    /**
     * 发送时间
     */
    val sendTime = datetime("sendTime")

    /**
     * 领取时间
     */
    val acceptTime = datetime("acceptTime").nullable()

}

class MailRecord(id: EntityID<Int>) : IntEntity(id) {

    var player by MailRecords.player
    var mail by MailRecords.mail
    var sendTime by MailRecords.sendTime
    var acceptTime by MailRecords.acceptTime

    companion object : IntEntityClass<MailRecord>(MailRecords)
}

object MailRecordCaches {
    private val playerCaches = mutableMapOf<UUID, PlayerMailRecordCaches>()

    fun getPlayerCache(player: Player) =
        playerCaches.computeIfAbsent(player.uniqueId) { PlayerMailRecordCaches(player) }

    fun getPlayerCache(uuid: UUID) = playerCaches[uuid]

    fun remove(player: Player) = playerCaches.remove(player.uniqueId)?.caches?.clear()
}

class PlayerMailRecordCaches(val player: Player) {
    /**
     * 邮件缓存，key为 mail id
     */
    var caches: MutableMap<Int, MailRecordCache> = mutableMapOf()

    var records = transaction { MailRecord.find { MailRecords.player eq player.uniqueId }.toMutableSet() }

    /**
     * 规划好的页面
     */
    var pages = records.sortedByDescending { it.sendTime }.chunked(MailBoxGUIConfig.pageMailSize)

    //页数
    var page = if (pages.isEmpty()) 1 else pages.size

    /**
     * 获取缓存的一页
     */
    fun getCache(page: Int): List<MailRecordCache>? {
        val content = pages.getOrNull(page) ?: return null
        return content.map { record -> caches.computeIfAbsent(record.id.value) { MailRecordCache(record, player) } }
    }

    fun insertRecord(record: MailRecord) {
        records.add(record)
        update()
    }

    fun removeRecord(record: MailRecord) {
        records.remove(record)
        update()
    }

    fun removeCache(cache: MailRecordCache) {
        caches.remove(cache.record.id.value)
        removeRecord(cache.record)
    }

    fun update() {
        pages = records.sortedByDescending { it.sendTime }.chunked(MailBoxGUIConfig.pageMailSize)
        page = if (pages.isEmpty()) 1 else pages.size
    }

}

class MailRecordCache(
    val record: MailRecord,
    val player: Player
) {
    lateinit var icon: ItemStack
    lateinit var title: String
    val mailYml by lazy { getYml() }

    init {
        setIconAndTitle()
    }

    /**
     * 设置邮件已领取
     */
    fun setAccepted() {
        icon.setGlow(false)
        record.acceptTime = LocalDateTime.now()
    }

    /**
     * 删除该邮件
     */
    fun remove() {
        record.delete()
    }

    /**
     * 领取邮件
     */
    fun getKit(): Boolean {
        if (!canGetKit()) {
            player.sendColorMessages("&6该邮件已领取!")
            return false
        }
        if (!mailYml.apply(player)) {
            player.sendColorMessages("&6你的背包没有足够的空间!")
            return false
        } else {
            player.sendColorMessages("&a领取成功")
            setAccepted()
            return true
        }
    }

    /**
     * 领取邮件
     */
    fun getKitSliently(): Boolean {
        return if (!canGetKit()) true
        else if (!mailYml.apply(player)) {
            false
        } else {
            setAccepted()
            true
        }
    }

    fun canGetKit(): Boolean = record.acceptTime == null

    private fun getYml(): SystemMailYml {
        val yml = SystemMailsYml.getMailYml(record.mail)!!
        val mutableMapOf = mutableMapOf<Int, ItemStack>()
        yml.items.forEach { (t, u) -> mutableMapOf[t] = PlaceHolderHook.setPlaceHolder(u.clone(), player) }
        val copy = yml.copy(
            id = record.mail,
            icon = icon,
            title = title,
            items = mutableMapOf,
            commands = yml.commands.map { PlaceHolderHook.setPlaceHolder(it, player) }.toMutableList()
        )
        return copy
    }

    fun setIconAndTitle() {
        transaction {
            val systemMailYml = SystemMailsYml.getMailYml(record.mail)!!
            val itemStack = systemMailYml.icon.clone()

            itemStack.applyMeta {
                if (hasDisplayName())
                    setDisplayName(setPlaceHolder(displayName))
                if (hasLore())
                    lore = lore!!.map { setPlaceHolder(it) }
            }
            icon = PlaceHolderHook.setPlaceHolder(itemStack, player)
            //未领取的发光
            if (record.acceptTime == null) {
                icon.setGlow()
            }
            title = PlaceHolderHook.setPlaceHolder(systemMailYml.title, player)
        }
    }

    private fun setPlaceHolder(str: String): String {
        var temp = str
        temp =
            temp.replace("%sakura_mail_sendtime%", record.sendTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME), true)
        temp = temp.replace(
            "%sakura_mail_accepttime%",
            record.acceptTime?.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) ?: "未领取",
            true
        )
        temp = temp.replace("%sakura_mail_id%", record.mail, true)
        return temp
    }

    private fun ItemStack.setGlow(glow: Boolean = true) {
        if (glow)
            applyMeta {
                addEnchant(Enchantment.DURABILITY, 1, false)
                addItemFlags(ItemFlag.HIDE_ENCHANTS)
            }
        else applyMeta {
            removeEnchant(Enchantment.DURABILITY)
            removeItemFlags(ItemFlag.HIDE_ENCHANTS)
        }
    }
}