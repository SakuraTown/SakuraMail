package top.iseason.bukkit.sakuramail.config

import com.cryptomorin.xseries.XItemStack
import org.bukkit.Material
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.MemorySection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta

@FilePath("ui/content.yml")
object MailContentYml : SimpleYAMLConfig() {

    @Key("accept")
    var acceptSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "49")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.CHEST).applyMeta { setDisplayName("领取") })
        )
    }

    @Key("back")
    var backSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "46")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.PAPER).applyMeta { setDisplayName("返回") })
        )
    }

    @Key("delete")
    var deleteSection: MemorySection = YamlConfiguration().apply {
        set("1.slots", "53")
        set(
            "1.item",
            XItemStack.serialize(ItemStack(Material.ANVIL).applyMeta { setDisplayName("删除") })
        )
    }
    var accepts = mutableMapOf<ItemStack, List<Int>>()
    var backs = mutableMapOf<ItemStack, List<Int>>()
    var delete = mutableMapOf<ItemStack, List<Int>>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        MailBoxGUIYml.guiCaches.clear()
        accepts = MailBoxGUIYml.readSlots(acceptSection)
        backs = MailBoxGUIYml.readSlots(backSection)
        delete = MailBoxGUIYml.readSlots(deleteSection)
    }

}