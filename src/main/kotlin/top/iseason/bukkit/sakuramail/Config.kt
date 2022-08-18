package top.iseason.bukkit.sakuramail

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath

@FilePath("config.yml")
object Config : SimpleYAMLConfig() {

    override val onLoaded: ConfigurationSection.() -> Unit = {
//        println("loaded")
    }

}