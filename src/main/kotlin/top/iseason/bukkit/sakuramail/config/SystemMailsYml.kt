package top.iseason.bukkit.sakuramail.config

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.utils.bukkit.EntityUtils.giveItems
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.canAddItem
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.toBase64
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.toByteArrays
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.toSection
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.sakuramail.SakuraMail
import top.iseason.bukkit.sakuramail.config.SystemMailsYml.isEncrypted
import top.iseason.bukkit.sakuramail.database.SystemMail
import top.iseason.bukkit.sakuramail.database.SystemMails
import top.iseason.bukkit.sakuramail.hook.ItemsAdderHook
import java.time.Duration

@FilePath("mails.yml")
object SystemMailsYml : SimpleYAMLConfig() {
    @Key
    @Comment(
        "",
        "是否对邮件的 items 加密压缩 ",
        "如果为true，只能使用命令/sakuramail systemMail edit 命令在游戏内修改物品",
        "请注意，设置为 false 可能不支持某些非原版物品!"
    )
    var isEncrypted: Boolean = true

    @Key("mails")
    var mailSection: MemorySection = YamlConfiguration()

    /**
     * 本地缓存
     */
    var mails = mutableMapOf<String, SystemMailYml>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        mails.clear()
        for (key in mailSection.getKeys(false)) {
            val section = mailSection.getConfigurationSection(key) ?: continue
            mails[key] = SystemMailYml.of(section) ?: continue
        }
    }

    /**
     * 获取指定id的邮件，具有缓存
     */
    fun getMailYml(id: String): SystemMailYml? {
        var systemMailYml: SystemMailYml? = mails[id]
        if (systemMailYml == null) {
            systemMailYml = transaction { SystemMail.findById(id)?.toYml() }
        } else return systemMailYml
        if (systemMailYml == null) return null
        mails[id] = systemMailYml
        return systemMailYml
    }

    /**
     * 将本地数据上传至数据库
     */
    fun upload() {
        transaction {
            for (value in mails.values) {
                value.toDatabase()
            }
        }
    }

    /**
     * 从数据库下载数据至本地
     */
    fun downloadFromDatabase() {
        mails.clear()
        transaction {
            for (systemMail in SystemMail.find { SystemMails.type eq "system" }) {
                mails[systemMail.id.value] = systemMail.toYml()
            }
        }
        saveToYml()
    }

    /**
     * 数据保存至yml
     */
    fun saveToYml() {
        mailSection.getKeys(false).forEach { config.set(it, null) }
        for (mail in mails) {
            mailSection[mail.key] = mail.value.toSection()
        }
        (config as YamlConfiguration).save(configPath)
    }
}

/**
 * 系统邮件执行类
 */
data class SystemMailYml(
    val id: String,
    var icon: ItemStack,
    var title: String,
    var type: String = "system",
    var items: MutableMap<Int, ItemStack> = mutableMapOf(),
    var commands: MutableList<String> = mutableListOf(),
    var expire: Duration? = null
) {
    /**
     * 将礼包给予玩家
     */
    fun apply(player: Player): Boolean {
        val filter = items.values.filter { !it.isFakeItem() }
        val canAddItem = player.canAddItem(*filter.toTypedArray())
        if (canAddItem > 0) return false
        player.giveItems(filter)
        submit {
            for (command in commands) {
                if (command.isBlank()) continue
                parseCommand(command, player)
            }
        }
        return true
    }

    private fun parseCommand(command: String, player: Player) {
        var playerCommand = command.replace("%player%", player.name)
        if (playerCommand.startsWith("CMD:", true)) {
            playerCommand = playerCommand.removeRange(0, 3)
            try {
                Bukkit.dispatchCommand(Bukkit.getConsoleSender(), playerCommand)
            } catch (_: Exception) {
            }
        } else if (playerCommand.startsWith("OP:", true)) {
            playerCommand = playerCommand.removeRange(0, 2)
            val setOp = !player.isOp
            if (setOp) {
                player.isOp = true
                try {
                    Bukkit.dispatchCommand(player, playerCommand)
                } catch (_: Exception) {
                } finally {
                    player.isOp = false
                }
            } else {
                Bukkit.dispatchCommand(player, playerCommand)
            }
        } else {
            try {
                Bukkit.dispatchCommand(player, playerCommand)
            } catch (_: Exception) {
            }
        }

    }

    /**
     * 序列化为section
     */
    fun toSection(): ConfigurationSection {
        val section = YamlConfiguration()
        section["id"] = id
        section["icon"] = ItemsAdderHook.getItemsAdderItem(icon)?.id ?: icon.toSection()
        section["title"] = title
        section["expire"] = expire
        if (items.isNotEmpty()) {
            if (isEncrypted) {
                section["items"] = items.toBase64()
            } else {
                section["items"] = items.toSection()
            }
            section["fakeItems"] = items.mapNotNull {
                if (it.value.isFakeItem()) it.key else null
            }.joinToString(",")
        }
        section["commands"] = commands
        return section
    }

    /**
     * 上传至为数据库
     */
    fun toDatabase(): SystemMail = transaction {
        var mail = SystemMail.findById(this@SystemMailYml.id)
        if (mail == null) {
            mail = SystemMail.new(this@SystemMailYml.id) {
                update()
            }
        } else {
            mail.update()
        }
        mail
    }

    /**
     * 更新数据
     */
    private fun SystemMail.update() {
        this.icon = ExposedBlob(this@SystemMailYml.icon.toByteArray())
        this.title = this@SystemMailYml.title
        if (this@SystemMailYml.items.isNotEmpty()) {
            this.items = ExposedBlob(this@SystemMailYml.items.toByteArrays())
        }
        if (this@SystemMailYml.commands.isNotEmpty()) {
            this.commands = this@SystemMailYml.commands.joinToString(";")
        }
        if (this@SystemMailYml.expire != null) {
            this.expire = this@SystemMailYml.expire
        }
    }

    companion object {

        /**
         * 反序列化
         */
        fun of(section: ConfigurationSection): SystemMailYml? {
            val id = section.getString("id") ?: return null
            val iconSection = section["icon"]
            var icon: ItemStack? = null
            if (iconSection is String) icon = ItemsAdderHook.getItemsAdderItem(iconSection)
            else if (iconSection is ConfigurationSection) icon = ItemUtils.fromSection(iconSection)
            if (icon == null) icon = ItemStack(Material.AIR)
            val title = section.getString("title") ?: ""
            val systemMailYml = SystemMailYml(id, icon, title)
            systemMailYml.expire = runCatching { Duration.parse(section.getString("expire") ?: "") }.getOrNull()
            val fakes =
                section.getString("fakeItems")?.split(',')?.mapNotNull { runCatching { it.toInt() }.getOrNull() }
                    ?: emptyList()
            if (isEncrypted) {
                val items = section.getString("items")
                if (items != null) {
                    val fromBase64ToMap = ItemUtils.fromBase64ToMap(items)
                    for (fake in fakes) {
                        fromBase64ToMap[fake]?.setFakeItem()
                    }
                    systemMailYml.items = fromBase64ToMap as MutableMap
                }
            } else {
                val itemSection = section.getConfigurationSection("items")
                if (itemSection != null) {
                    systemMailYml.items = ItemUtils.fromSectionToMap(itemSection) as MutableMap
                }
            }
            systemMailYml.commands = section.getStringList("commands")
            return systemMailYml
        }

        /**
         * 检测此Item是否是虚拟物品，将不会给予玩家
         */
        fun ItemStack.isFakeItem() = itemMeta?.persistentDataContainer?.has(FAKE_ITEM, PersistentDataType.BYTE) == true

        fun ItemStack.setFakeItem() = applyMeta {
            persistentDataContainer.set(FAKE_ITEM, PersistentDataType.BYTE, 1)
        }

        private val FAKE_ITEM = NamespacedKey(SakuraMail.javaPlugin, "sakura_mail_fake_item")
    }
}