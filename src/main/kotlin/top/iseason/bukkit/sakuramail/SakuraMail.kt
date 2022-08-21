package top.iseason.bukkit.sakuramail

import org.bukkit.Bukkit
import top.iseason.bukkit.bukkittemplate.KotlinPlugin
import top.iseason.bukkit.bukkittemplate.command.CommandBuilder
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.sakuramail.command.command
import top.iseason.bukkit.sakuramail.config.MailReceiversYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkit.sakuramail.database.MailReceivers
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import top.iseason.bukkit.sakuramail.database.SystemMails
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
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(PlayerTimes, SystemMails, MailReceivers)
        SystemMailsYml.load(false)
        MailReceiversYml.load(false)
        registerListeners(PlayerListener)

        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.onLogin(it) }
        }.getOrElse { it.printStackTrace() }
        command()
        CommandBuilder.updateCommands()
    }

    override fun onDisable() {
        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.onQuit(it) }
        }.getOrElse { it.printStackTrace() }
        info("&6插件已卸载! ")
    }

}