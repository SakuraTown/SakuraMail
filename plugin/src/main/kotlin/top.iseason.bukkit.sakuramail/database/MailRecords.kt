package top.iseason.bukkit.sakuramail.database

import org.bukkit.enchantments.Enchantment
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemFlag
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.id.EntityID
import org.jetbrains.exposed.dao.id.IntIdTable
import org.jetbrains.exposed.sql.deleteWhere
import org.jetbrains.exposed.sql.javatime.datetime
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkit.sakuramail.config.SystemMailYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkit.sakuramail.hook.PlaceHolderHook
import top.iseason.bukkit.sakuramail.utils.TimeUtils
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.time.Duration
import java.time.LocalDateTime
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

    /**
     * 邮件是否过期
     */
    fun isExpired(): Boolean {
        val expire = SystemMailsYml.getMailYml(mail)!!.expire ?: return false
        return sendTime.plus(expire).isBefore(LocalDateTime.now())
    }
}

/**
 * 所有玩家的邮件缓存，直到推出服务器才删除
 */
object PlayerMailRecordCaches {
    private val playerCaches = mutableMapOf<UUID, PlayerMailRecordCache>()

    fun getPlayerCache(player: Player) =
        playerCaches.computeIfAbsent(player.uniqueId) { PlayerMailRecordCache(player) }

    fun getPlayerCache(uuid: UUID) = playerCaches[uuid]

    fun remove(player: Player) = playerCaches.remove(player.uniqueId)?.caches?.clear()
    fun clear() = playerCaches.clear()
}

/**
 * 玩家邮件缓存
 */
class PlayerMailRecordCache(val player: Player) {
    /**
     * 邮件缓存，key为 mail id
     */
    var caches: MutableMap<Int, MailRecordCache> = mutableMapOf()

    private var records = dbTransaction { MailRecord.find { MailRecords.player eq player.uniqueId }.toMutableSet() }

    /**
     * 规划好的页面
     */
    var pages = records.sortedByDescending { it.sendTime }.chunked(MailBoxGUIYml.pageMailSize)

    //页数
    var page = if (pages.isEmpty()) 1 else pages.size

    /**
     * 获取缓存的一页
     */
    fun getPageCache(page: Int): List<MailRecordCache>? {
        val content = pages.getOrNull(page) ?: return null
        return content.map { record -> caches.computeIfAbsent(record.id.value) { MailRecordCache(record, player) } }
    }

    /**
     * 插入记录
     */
    fun insertRecord(record: MailRecord) {
        records.add(record)
        update()
    }

    /**
     * 删除记录
     */
    fun removeRecord(record: MailRecord) {
        records.remove(record)
        update()
    }

    /**
     * 删除缓存
     */
    fun removeCache(cache: MailRecordCache) {
        caches.remove(cache.record.id.value)
        removeRecord(cache.record)
    }

    /**
     * 更新缓存
     */
    fun update() {
        pages = records.sortedByDescending { it.sendTime }.chunked(MailBoxGUIYml.pageMailSize)
        page = if (pages.isEmpty()) 1 else pages.size
    }

}

/**
 * 邮件缓存
 */
class MailRecordCache(
    val record: MailRecord,
    val player: Player
) {
    lateinit var icon: ItemStack
    private var expireStr = ""

    init {
        setIconAndTitle()
    }

    val mailYml = getYml()

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
        //删除一次性邮件
        if (mailYml.type != "system") {
            SystemMails.deleteWhere { SystemMails.id eq mailYml.id }
        }
    }

    /**
     * 领取邮件
     */
    fun getKit(): Boolean {
        if (!canGetKit()) {
            player.sendColorMessage(Lang.ui_get_has_accepted)
            return false
        }
        return if (!mailYml.apply(player)) {
            player.sendColorMessage(Lang.ui_get_no_space)
            false
        } else {
            player.sendColorMessage(Lang.ui_get_success)
            setAccepted()
            setIconAndTitle()
            true
        }
    }

    /**
     * 领取邮件
     */
    fun getKitSliently(): Boolean {
        return if (!mailYml.apply(player)) {
            false
        } else {
            setAccepted()
            setIconAndTitle()
            true
        }
    }

    /**
     * 检查领取邮件的必要条件
     * @return true 表示可以领取
     */
    fun canGetKit(): Boolean {
        // 是否领取
        if (record.acceptTime != null) return false
        // 检查过期
        if (mailYml.expire != null && record.sendTime.plus(mailYml.expire).isBefore(LocalDateTime.now())) {
            return false
        }
        return true
    }

    private fun getYml(): SystemMailYml {
        val yml = SystemMailsYml.getMailYml(record.mail)!!
        val mutableMapOf = mutableMapOf<Int, ItemStack>()
        yml.items.forEach { (t, u) -> mutableMapOf[t] = PlaceHolderHook.setPlaceHolder(u.clone(), player) }
        return yml.copy(
            id = record.mail,
            icon = icon,
            title = PlaceHolderHook.setPlaceHolder(yml.title, player),
            items = mutableMapOf,
            commands = yml.commands.map { PlaceHolderHook.setPlaceHolder(it, player) }.toMutableList()
        )
    }

    private fun setIconAndTitle() {
        val systemMailYml = SystemMailsYml.getMailYml(record.mail)!!
        val itemStack = systemMailYml.icon.clone()
        expireStr = getExpireStr(systemMailYml.expire)
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

    }

    /**
     * 设置插件自带的占位符
     */
    private fun setPlaceHolder(str: String): String {
        var temp = str
        if (temp.contains("%sakura_mail_expire%")) {
            temp = temp.replace("%sakura_mail_expire%", expireStr, true)
        }
        return temp.replace(
            "%sakura_mail_sendtime%",
            TimeUtils.formatTime(record.sendTime),
            true
        ).replace(
            "%sakura_mail_accepttime%",
            if (record.acceptTime == null) "未领取" else TimeUtils.formatTime(record.acceptTime!!),
            true
        ).replace("%sakura_mail_id%", record.mail, true)

    }

    private fun getExpireStr(expire: Duration?): String {
        if (expire == null) return "无限"
        val between = Duration.between(LocalDateTime.now(), record.sendTime.plus(expire))
        if (between.isNegative) return "已到期"
        return TimeUtils.formatDuration(between)
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