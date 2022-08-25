package top.iseason.bukkit.sakuramail.config

import com.cryptomorin.xseries.XItemStack
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.sakuramail.ui.MailBoxContainer
import java.util.*

@FilePath("ui/mailbox.yml")
object MailBoxGUIYml : SimpleYAMLConfig() {

    var guiCaches = mutableMapOf<UUID, MailBoxContainer>()

    @Key
    var title = "&a我的邮箱"

    @Key
    var row = 6

    @Key("icons")
    var iconSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "47,48,49,50")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.GRAY_STAINED_GLASS_PANE).applyMeta { setDisplayName("") })
        )
    }

    @Key("mails")
    var mailSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", (0..row * 9 - 10).joinToString(",") { it.toString() })
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.AIR))
        )
    }

    @Key("nextPages")
    var nextPageSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "46")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.PAPER).applyMeta { setDisplayName("下一页") })
        )
    }

    @Key("lastPages")
    var lastPageSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "45")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.PAPER).applyMeta { setDisplayName("上一页") })
        )
    }

    @Key("getAlls")
    var getAllSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "52")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.CHEST).applyMeta { setDisplayName("全部领取") })
        )
    }

    @Key("clearAccepted")
    var clearAcceptedSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "53")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.ANVIL).applyMeta { setDisplayName("清除已领取") })
        )
    }
    var icons = mutableMapOf<ItemStack, List<Int>>()
    var mails = mutableMapOf<ItemStack, List<Int>>()
    var nextPage = mutableMapOf<ItemStack, List<Int>>()
    var lastPage = mutableMapOf<ItemStack, List<Int>>()
    var getAll = mutableMapOf<ItemStack, List<Int>>()
    var clearAccepted = mutableMapOf<ItemStack, List<Int>>()

    var pageMailSize = mails.values.sumOf { it.size }
    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        guiCaches.clear()
        icons = readSlots(iconSection)
        mails = readSlots(mailSection)
        nextPage = readSlots(nextPageSection)
        lastPage = readSlots(lastPageSection)
        getAll = readSlots(getAllSection)
        clearAccepted = readSlots(clearAcceptedSection)
        pageMailSize = mails.values.sumOf { it.size }
    }

    /**
     * 从配置中读取界面组件
     */
    fun readSlots(section: ConfigurationSection?): MutableMap<ItemStack, List<Int>> {
        val mutableMapOf = mutableMapOf<ItemStack, List<Int>>()
        if (section == null) return mutableMapOf
        runCatching {
            section.getKeys(false).forEach {
                val section2 = section.getConfigurationSection(it) ?: return@forEach
                val slots = (section2.getString("slots") ?: "").trim().split(",").mapNotNull {
                    runCatching { it.toInt() }.getOrNull()
                }
                val itemSection = section2.getConfigurationSection("item")
                val icon =
                    if (itemSection == null) ItemStack(Material.AIR) else runCatching {
                        XItemStack.deserialize(
                            itemSection
                        )
                    }.getOrElse {
                        ItemStack(Material.AIR)
                    }
                mutableMapOf[icon] = slots
            }
        }.getOrElse {
            info("&6配置 ${section.name} 读取错误!")
        }
        return mutableMapOf
    }

    fun getPlayerUI(player: Player) = guiCaches.computeIfAbsent(player.uniqueId) { MailBoxContainer(player) }

}