package top.iseason.bukkit.sakuramail.ui

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.ui.container.ChestUI

@FilePath("myui.yml")
object MyUIConfig : SimpleYAMLConfig() {
    var myUI: ChestUI? = null
    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        myUI = MyUISer.deserialize(config)
    }
}