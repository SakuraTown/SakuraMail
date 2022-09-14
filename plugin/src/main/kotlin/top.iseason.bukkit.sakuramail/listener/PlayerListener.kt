package top.iseason.bukkit.sakuramail.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerQuitEvent
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.and
import org.jetbrains.exposed.sql.select
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkit.sakuramail.config.MailSendersYml
import top.iseason.bukkit.sakuramail.database.MailRecords
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCaches
import top.iseason.bukkit.sakuramail.database.PlayerTime
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import top.iseason.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkittemplate.config.dbTransaction
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.formatBy
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.sendColorMessage
import top.iseason.bukkittemplate.utils.other.submit
import java.time.Duration
import java.time.LocalDateTime

object PlayerListener : Listener {

    /**
     * 玩家登录时调用，处理邮件接受者类型 login
     */
    fun onLogin(player: Player) {
        if (!DatabaseConfig.isConnected) return
        submit(async = true) {
            // login 类型发生在更新玩家登陆时间之前
            MailSendersYml.senders.values.forEach {
                if (it.type != "login") return@forEach
                it.onSend(it.getAllReceivers(it.receivers, player))
            }
            // 更新玩家登陆时间
            updateLoginTime(player)
            if (Lang.login_tip.trim().isEmpty()) return@submit
            dbTransaction {
                val count = MailRecords.slice(MailRecords.id)
                    .select { MailRecords.player eq player.uniqueId and (MailRecords.acceptTime eq null) }
                    .count()
                if (count == 0L) return@dbTransaction
                player.sendColorMessage(Lang.login_tip.formatBy(count))
            }
        }
    }

    /**
     * 更新玩家登录时间
     */
    fun updateLoginTime(player: Player) {
        if (!DatabaseConfig.isConnected) return
        dbTransaction {
            PlayerTime.new {
                this.player = player.uniqueId
                this.loginTime = LocalDateTime.now()
            }
        }
    }

    /**
     * 更新玩家退出时间
     */
    fun updateQuitTime(player: Player) {
        if (!DatabaseConfig.isConnected) return
        dbTransaction {
            val login = PlayerTime.find { PlayerTimes.player eq player.uniqueId }
                .orderBy(PlayerTimes.id to SortOrder.DESC)
                .limit(1).firstOrNull() ?: return@dbTransaction
            if (login.quitTime != null) return@dbTransaction
            login.quitTime = LocalDateTime.now()
            login.playTime = Duration.between(login.loginTime, login.quitTime)
        }
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        if (!DatabaseConfig.isConnected) return
        submit(async = true) {
            updateQuitTime(event.player)
            PlayerMailRecordCaches.remove(event.player)
            MailBoxGUIYml.guiCaches.remove(event.player.uniqueId)
        }
    }
}