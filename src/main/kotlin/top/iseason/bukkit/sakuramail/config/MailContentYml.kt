package top.iseason.bukkit.sakuramail.config

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
    var acceptSection: MemorySection = YamlConfiguration()

    @Key("back")
    var backSection: MemorySection = YamlConfiguration()

    @Key("delete")
    var deleteSection: MemorySection = YamlConfiguration()

    var accepts = mutableMapOf<ItemStack, List<Int>>()
    var backs = mutableMapOf<ItemStack, List<Int>>()
    var delete = mutableMapOf<ItemStack, List<Int>>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        accepts = MailBoxGUIYml.readSlots(acceptSection)
        backs = MailBoxGUIYml.readSlots(backSection)
        delete = MailBoxGUIYml.readSlots(deleteSection)
        test()
    }

    private fun test() {
        accepts[ItemStack(Material.CHEST).applyMeta { setDisplayName("领取") }] = listOf(49)
        backs[ItemStack(Material.PAPER).applyMeta { setDisplayName("返回") }] = listOf(46)
        delete[ItemStack(Material.ANVIL).applyMeta { setDisplayName("删除") }] = listOf(53)
    }
}