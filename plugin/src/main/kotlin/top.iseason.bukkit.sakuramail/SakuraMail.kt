package top.iseason.bukkit.sakuramail

import fr.xephi.authme.events.LoginEvent
import fr.xephi.authme.events.RegisterEvent
import org.bukkit.Bukkit
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.statements.api.ExposedBlob
import top.iseason.bukkit.sakuramail.command.mainCommand
import top.iseason.bukkit.sakuramail.config.*
import top.iseason.bukkit.sakuramail.database.*
import top.iseason.bukkit.sakuramail.hook.AuthMeHook
import top.iseason.bukkit.sakuramail.hook.ItemsAdderHook
import top.iseason.bukkit.sakuramail.hook.PlaceHolderHook
import top.iseason.bukkit.sakuramail.listener.PlayerListener
import top.iseason.bukkittemplate.KotlinPlugin
import top.iseason.bukkittemplate.command.CommandHandler
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.debug.info
import top.iseason.bukkittemplate.ui.UIListener
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.listen
import top.iseason.bukkittemplate.utils.bukkit.EventUtils.register
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArray
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.toByteArrays
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.Files
import java.time.Duration
import java.time.LocalDateTime
import java.util.*

object SakuraMail : KotlinPlugin() {

    override fun onAsyncLoad() {
        SimpleYAMLConfig.notifyMessage = "&7配置文件 &6%s &7已重载!"
    }

    override fun onAsyncEnable() {
        info("&6插件初始化中...")
        Lang.load(false)
        PlaceHolderHook
        ItemsAdderHook
        DatabaseConfig.load(false)
        DatabaseConfig.initTables(PlayerTimes, SystemMails, MailReceivers, MailSenders, MailRecords)
        SystemMailsYml.load(false)
        MailReceiversYml.load(false)
        MailSendersYml.load(false)
        MailContentYml.load(false)
        MailBoxGUIYml.load(false)
        PlayerListener.register()
        if (AuthMeHook.hasHooked) {
            listen<LoginEvent> {
                PlayerListener.onLogin(player)
            }
            listen<RegisterEvent> {
                MailSendersYml.senders.values.filter { it.type == "register" }.forEach {
                    it.onSend(it.getAllReceivers(it.receivers, player))
                }
            }
        } else {
            listen<PlayerLoginEvent> {
                PlayerListener.onLogin(player)
            }
        }
        UIListener.register()
        mainCommand()
        CommandHandler.updateCommands()
        runCatching {
            Bukkit.getOnlinePlayers().forEach { PlayerListener.updateLoginTime(it) }
        }.getOrElse { it.printStackTrace() }
        info("&a插件初始化完成! ")

    }

    override fun onDisable() {
        MailSendersYml.executor.shutdown()
        MailSendersYml.scheduler.shutdown()
        runCatching {
            Bukkit.getOnlinePlayers().forEach {
                PlayerListener.updateQuitTime(it)
                PlayerMailRecordCaches.remove(it)
                MailBoxGUIYml.guiCaches.remove(it.uniqueId)
            }
        }.getOrElse { it.printStackTrace() }
        info("&6插件已卸载!")
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

    /**
     * 发送一个临时邮件给玩家
     * @param uuid 玩家 uuid
     * @param icon 显示再邮箱里的图标
     * @param title 内容的标题
     * @param items 附件,对 ItemStack 对象使用 setFakeItem 方法可以避免被领取
     * @param commands 执行的命令，玩家名称为 %player%
     * @param expire 有效期
     * @return true 发送成功 ，false 发送失败
     */
    fun sendTempMail(
        uuid: UUID,
        icon: ItemStack,
        title: String,
        items: Map<Int, ItemStack>,
        commands: List<String> = emptyList(),
        expire: Duration = Duration.ofDays(30)
    ): Boolean {
        if (!DatabaseConfig.isConnected) return false
        runCatching {
            dbTransaction {
                val mail = SystemMail.new(UUID.randomUUID().toString()) {
                    this.type = "temp"
                    this.icon = ExposedBlob(icon.toByteArray())
                    this.title = title
                    if (items.isNotEmpty())
                        this.items = ExposedBlob(items.toByteArrays())
                    if (commands.isNotEmpty())
                        this.commands = commands.joinToString(";")
                    this.expire = expire
                }
                val new = MailRecord.new {
                    this.player = uuid
                    this.mail = mail.id.value
                    this.sendTime = LocalDateTime.now()
                }
                PlayerMailRecordCaches.getPlayerCache(uuid)?.insertRecord(new)
                Bukkit.getPlayer(uuid)?.sendColorMessage(Lang.new_mail)
            }
        }.getOrElse { return false }

        return true
    }

}