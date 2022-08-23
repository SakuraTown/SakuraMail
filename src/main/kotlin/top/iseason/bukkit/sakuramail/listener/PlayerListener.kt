package top.iseason.bukkit.sakuramail.listener

import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerLoginEvent
import org.bukkit.event.player.PlayerQuitEvent
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.utils.runAsync
import top.iseason.bukkit.sakuramail.config.MailSendersYml
import top.iseason.bukkit.sakuramail.database.PlayerTime
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import java.time.Duration
import java.time.LocalDateTime

object PlayerListener : Listener {
    /**
     * 更新玩家登录时间
     */
    fun onLogin(player: Player) {
        if (!DatabaseConfig.isConnected) return
        transaction {
            PlayerTime.new {
                this.player = player.uniqueId
                this.loginTime = LocalDateTime.now()
            }
        }
    }

    /**
     * 更新玩家退出时间
     */
    fun onQuit(player: Player) {
        if (!DatabaseConfig.isConnected) return
        transaction {
            val login = PlayerTime.find { PlayerTimes.player eq player.uniqueId }
                .orderBy(PlayerTimes.id to SortOrder.DESC)
                .limit(1).firstOrNull() ?: return@transaction
            if (login.quitTime != null) return@transaction
            login.quitTime = LocalDateTime.now()
            login.playTime = Duration.between(login.loginTime, login.quitTime)
        }
    }

    @EventHandler
    fun onPlayerLoginEvent(event: PlayerLoginEvent) {
        runAsync {
            onLogin(event.player)
            MailSendersYml.senders.values.forEach {
                if (it.type != "login") return@forEach
                it.onSend(it.getAllReceivers(it.receivers, event.player))
            }
        }
    }

    @EventHandler
    fun onPlayerQuitEvent(event: PlayerQuitEvent) {
        runAsync { onQuit(event.player) }
    }
}