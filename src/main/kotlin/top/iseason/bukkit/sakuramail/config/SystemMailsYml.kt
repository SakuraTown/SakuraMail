package top.iseason.bukkit.sakuramail.config

import org.bukkit.Bukkit
import org.bukkit.Material
import org.bukkit.NamespacedKey
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.persistence.PersistentDataType
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.bukkit.checkAir
import top.iseason.bukkit.bukkittemplate.utils.bukkit.giveItems
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.sakuramail.SakuraMail
import top.iseason.bukkit.sakuramail.database.SystemMail

@FilePath("mails.yml")
object SystemMailsYml : SimpleYAMLConfig() {
    /**
     * 本地缓存
     */
    var mails = mutableMapOf<String, SystemMailYml>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        mails.clear()
        for (key in getKeys(false)) {
            val section = getConfigurationSection(key) ?: continue
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
    fun uploadDatabase() {
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
            for (systemMail in SystemMail.all()) {
                mails[systemMail.id.value] = systemMail.toYml()
            }
        }
        saveToYml()
    }

    /**
     * 数据保存至yml
     */
    fun saveToYml() {
        config.getKeys(false).forEach { config.set(it, null) }
        for (mail in mails) {
            config[mail.key] = mail.value.toSection()
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
    var items: MutableMap<Int, ItemStack> = mutableMapOf(),
    var commands: MutableList<String> = mutableListOf()
) {
    /**
     * 将礼包给予玩家
     */
    fun apply(player: Player): Boolean {
        val filter = items.values.filter { !it.isFakeItem() }
        val emptySlot = player.openInventory.bottomInventory.filter { it == null || it.type.checkAir() }.count()
        if (filter.size > emptySlot) return false
        player.giveItems(filter)
        submit {
            for (command in commands) {
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
        section["icon"] = icon
        section["title"] = title
        if (items.isNotEmpty()) {
            section["items"] = ItemUtils.toBase64(items)
            section["fakeItems"] = items.mapNotNull {
                if (it.value.isFakeItem()) it.key
                else null
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
        this.icon = ExposedBlob(ItemUtils.toByteArray(this@SystemMailYml.icon))
        this.title = this@SystemMailYml.title
        if (this@SystemMailYml.items.isNotEmpty()) {
            this.items = ExposedBlob(ItemUtils.toByteArrays(this@SystemMailYml.items))
        }
        if (this@SystemMailYml.commands.isNotEmpty()) {
            this.commands = this@SystemMailYml.commands.joinToString(";")
        }
    }

    companion object {

        /**
         * 反序列化
         */
        fun of(section: ConfigurationSection): SystemMailYml? {
            val id = section.getString("id") ?: return null
            val icon = section.getItemStack("icon") ?: ItemStack(Material.STONE)
            val title = section.getString("title") ?: ""
            val systemMailYml = SystemMailYml(id, icon, title)
            val items = section.getString("items")
            if (items != null) {
                val fromBase64ToMap = ItemUtils.fromBase64ToMap(items)
                val string = section.getString("fakeItems")
                if (string != null) {
                    for (s in string.split(',')) {
                        runCatching {
                            fromBase64ToMap[s.toInt()]?.applyMeta {
                                persistentDataContainer.set(FAKE_ITEM, PersistentDataType.BYTE, 1)
                            }
                        }
                    }
                }
                systemMailYml.items = fromBase64ToMap
            }
            systemMailYml.commands = section.getStringList("commands")
            return systemMailYml
        }

        /**
         * 检测此Item是否是虚拟物品，将不会给予玩家
         */
        fun ItemStack.isFakeItem() = itemMeta?.persistentDataContainer?.has(FAKE_ITEM, PersistentDataType.BYTE) == true

        private val FAKE_ITEM = NamespacedKey(SakuraMail.javaPlugin, "sakura_mail_fake_item")
    }
}