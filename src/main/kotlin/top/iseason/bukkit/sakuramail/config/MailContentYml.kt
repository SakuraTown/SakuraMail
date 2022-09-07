package top.iseason.bukkit.sakuramail.config

import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils.toSection

@FilePath("ui/content.yml")
object MailContentYml : SimpleYAMLConfig() {
    @Key
    @Comment("", "点击间隔，防止操作过快的数据库异常")
    var clickDelay: Long = 500

    @Key
    @Comment("", "容器的行数")
    var row = 6

    @Key("accept")
    @Comment("", "领取邮件组件")
    var acceptSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "49")
        set(
            "1.item", ItemStack(Material.CHEST).applyMeta { setDisplayName("领取") }.toSection()
        )
    }

    @Key("accepted")
    @Comment("", "已经领取邮件组件")
    var acceptedSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "49")
        set(
            "1.item", ItemStack(Material.CHEST).applyMeta { setDisplayName("已领取") }.toSection()
        )
    }

    @Key("back")
    @Comment("", "返回上一级组件")
    var backSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "46")
        set(
            "1.item", ItemStack(Material.PAPER).applyMeta { setDisplayName("返回") }.toSection()
        )
    }

    @Key("delete")
    @Comment("", "删除邮件组件")
    var deleteSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "53")
        set(
            "1.item", ItemStack(Material.ANVIL).applyMeta { setDisplayName("删除") }.toSection()
        )
    }
    var accepts = mutableMapOf<ItemStack, List<Int>>()
    var accepteds = mutableMapOf<ItemStack, List<Int>>()
    var backs = mutableMapOf<ItemStack, List<Int>>()
    var delete = mutableMapOf<ItemStack, List<Int>>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {

        MailBoxGUIYml.guiCaches.clear()
        accepts = MailBoxGUIYml.readSlots(acceptSection)
        accepteds = MailBoxGUIYml.readSlots(acceptedSection)
        backs = MailBoxGUIYml.readSlots(backSection)
        delete = MailBoxGUIYml.readSlots(deleteSection)
    }

}