package top.iseason.bukkit.sakuramail

import org.bukkit.Bukkit
import top.iseason.bukkit.bukkittemplate.KotlinPlugin
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import top.iseason.bukkit.sakuramail.listener.PlayerListener

object SakuraMail : KotlinPlugin() {

    override fun onAsyncLoad() {
        SimpleYAMLConfig.notifyMessage = "&7配置文件 &6%s &7已重载!"
    }

    override fun onEnable() {
        info("&a插件已启用!")
    }

    override fun onAsyncEnable() {
        SimpleLogger.isDebug = true
        //使用数据库请取消注释以下2行
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(PlayerTimes)

        registerListeners(PlayerListener)

        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.onLogin(it) }
        }.getOrElse { it.printStackTrace() }
        //如果使用命令模块，取消注释
//        CommandBuilder.updateCommands()
        //如果使用UI模块,取消注释

    }

    override fun onDisable() {
        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.onQuit(it) }
        }.getOrElse { it.printStackTrace() }
        info("&6插件已卸载! ")
    }

}