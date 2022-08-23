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
import top.iseason.bukkit.sakuramail.config.MailSendersYml
import top.iseason.bukkit.sakuramail.config.SystemMailsYml
import top.iseason.bukkit.sakuramail.database.*
import top.iseason.bukkit.sakuramail.listener.PlayerListener
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.util.*


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
        DatabaseConfig.initTables(PlayerTimes, SystemMails, MailReceivers, MailSenders, MailRecords)

        SystemMailsYml.load(false)
        MailReceiversYml.load(false)
        MailSendersYml.load(false)
        registerListeners(PlayerListener)

        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.onLogin(it) }
        }.getOrElse { it.printStackTrace() }
        command()
        CommandBuilder.updateCommands()
    }

    override fun onDisable() {
        MailSendersYml.executor.shutdown()
        MailSendersYml.scheduler.shutdown()
        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.onQuit(it) }
        }.getOrElse { it.printStackTrace() }
        info("&6插件已卸载! ")
    }


    fun loadOrCopyQuartzProperties(): Properties {
        val quartzProperties = File(javaPlugin.dataFolder, "quartz.properties")
        if (!quartzProperties.exists()) {
            javaPlugin.getResource("quartz.properties")?.use {
                Files.copy(it, quartzProperties.toPath())
            }
        }
        val prop = Properties()
        val targetStream: InputStream = FileInputStream(quartzProperties)
        prop.load(targetStream)
        return prop
    }
}