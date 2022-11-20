package top.iseason.bukkit.sakuramail.config

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.sakuramail.hook.ItemsAdderHook
import top.iseason.bukkit.sakuramail.ui.MailBoxContainer
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.annotations.Comment
import top.iseason.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkittemplate.config.annotations.Key
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toSection
import java.util.*

@FilePath("ui/mailbox.yml")
object MailBoxGUIYml : SimpleYAMLConfig() {

    var guiCaches = mutableMapOf<UUID, MailBoxContainer>()

    @Key
    @Comment("", "点击间隔，防止操作过快的数据库异常")
    var clickDelay: Long = 500

    @Key
    @Comment("", "邮箱的标题")
    var title = "&a我的邮箱 %sakura_mail_current_page% / %sakura_mail_total_page%"

    @Key
    @Comment("", "邮箱的行数")
    var row = 6

    @Key("icons")
    @Comment("", "展示的图标，占位美化用")
    var iconSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "47,48,50,51")
        set(
            "1.item",
            ItemStack(Material.GLASS).applyMeta { setDisplayName("") }.toSection()
        )
        set("2.slots", "49")
        set(
            "2.item", ItemStack(Material.NETHER_STAR).applyMeta { setDisplayName("你的邮件") }.toSection()
        )
    }

    @Key("mails")
    @Comment("", "邮件应该显示的位置，item可以不写")
    var mailSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", (0..row * 9 - 10).joinToString(",") { it.toString() })
        set(
            "1.item", ItemStack(Material.AIR).toSection()
        )
    }

    @Key("nextPages")
    @Comment("", "下一页按钮")
    var nextPageSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "46")
        set(
            "1.item", ItemStack(Material.PAPER).applyMeta { setDisplayName("下一页") }.toSection()
        )
    }

    @Key("lastPages")
    @Comment("", "上一页按钮")
    var lastPageSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "45")
        set(
            "1.item", ItemStack(Material.PAPER).applyMeta { setDisplayName("上一页") }.toSection()
        )
    }

    @Key("getAlls")
    @Comment("", "领取全部按钮")
    var getAllSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "52")
        set(
            "1.item", ItemStack(Material.CHEST).applyMeta { setDisplayName("全部领取") }.toSection()
        )
    }

    @Key("clearAccepted")
    @Comment("", "清除未领取的邮件")
    var clearAcceptedSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "53")
        set(
            "1.item", ItemStack(Material.ANVIL).applyMeta { setDisplayName("清除已领取") }.toSection()
        )
    }
    var icons = mutableMapOf<ItemStack, List<Int>>()
    var mails = mutableMapOf<ItemStack, List<Int>>()
    var nextPage = mutableMapOf<ItemStack, List<Int>>()
    var lastPage = mutableMapOf<ItemStack, List<Int>>()
    var getAll = mutableMapOf<ItemStack, List<Int>>()
    var clearAccepted = mutableMapOf<ItemStack, List<Int>>()

    var pageMailSize = mails.values.sumOf { it.size }

    override fun onLoaded(section: ConfigurationSection) {
        guiCaches.clear()
        readGui()
    }

    private fun readGui() {
        mails = readSlots(mailSection)
        icons = readSlots(iconSection)
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
                val itemsAdder = section2.getString("item")
                var item: ItemStack? = null
                if (itemsAdder != null) item = ItemsAdderHook.getItemsAdderItem(itemsAdder)
                if (item == null) {
                    val section3 = section2.getConfigurationSection("item")
                    item = runCatching { ItemUtils.fromSection(section3!!)!! }.getOrElse { ItemStack(Material.AIR) }
                }
                mutableMapOf[item] = slots
            }
        }.getOrElse {
            info("&6配置 ${section.name} 读取错误!")
        }
        return mutableMapOf
    }

    fun getPlayerUI(player: Player) = guiCaches.computeIfAbsent(player.uniqueId) { MailBoxContainer(player) }

}