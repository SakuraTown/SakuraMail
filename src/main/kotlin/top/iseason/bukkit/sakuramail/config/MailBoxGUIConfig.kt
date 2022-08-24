package top.iseason.bukkit.sakuramail.config

import com.cryptomorin.xseries.XItemStack
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta

@FilePath("ui/mailbox.yml")
object MailBoxGUIConfig : SimpleYAMLConfig() {

    @Key
    var title = "&a我的邮箱"

    @Key
    var row = 6

    var icons = mutableMapOf<ItemStack, List<Int>>()
    var mails = mutableMapOf<ItemStack, List<Int>>()
    var nextPage = mutableMapOf<ItemStack, List<Int>>()
    var lastPage = mutableMapOf<ItemStack, List<Int>>()
    var getAll = mutableMapOf<ItemStack, List<Int>>()
    var clearAccepted = mutableMapOf<ItemStack, List<Int>>()

    var pageMailSize = mails.values.sumOf { it.size }
    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        icons = readSlots(config.getConfigurationSection("icons"))
        mails = readSlots(config.getConfigurationSection("mails"))
        nextPage = readSlots(config.getConfigurationSection("nextPages"))
        lastPage = readSlots(config.getConfigurationSection("lastPages"))
        getAll = readSlots(config.getConfigurationSection("getAlls"))
        clearAccepted = readSlots(config.getConfigurationSection("clearAccepted"))
        test()
        pageMailSize = mails.values.sumOf { it.size }
    }

    fun readSlots(section: ConfigurationSection?): MutableMap<ItemStack, List<Int>> {
        val mutableMapOf = mutableMapOf<ItemStack, List<Int>>()
        if (section == null) return mutableMapOf
        section.getKeys(false).forEach {
            val section2 = section.getConfigurationSection(it) ?: return@forEach
            val slots = (section2.getString("slots") ?: "").trim().split(",").mapNotNull {
                runCatching { it.toInt() }.getOrNull()
            }
            val itemSection = section2.getConfigurationSection("item")
            val icon = if (itemSection == null) ItemStack(Material.AIR) else XItemStack.deserialize(itemSection)
            mutableMapOf[icon] = slots
        }
        return mutableMapOf
    }

    fun test() {
        mails[ItemStack(Material.AIR)] = (0..row * 9 - 10).toList()
        lastPage[ItemStack(Material.PAPER).applyMeta { setDisplayName("上一页") }] = listOf(45)
        nextPage[ItemStack(Material.PAPER).applyMeta { setDisplayName("下一页") }] = listOf(46)
        getAll[ItemStack(Material.CHEST).applyMeta { setDisplayName("全部领取") }] = listOf(52)
        clearAccepted[ItemStack(Material.ANVIL).applyMeta { setDisplayName("清除已领取") }] = listOf(53)
    }
}