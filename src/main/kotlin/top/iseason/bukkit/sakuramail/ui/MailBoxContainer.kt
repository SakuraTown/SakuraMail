package top.iseason.bukkit.sakuramail.ui

import org.bukkit.entity.HumanEntity
import org.bukkit.entity.Player
import top.iseason.bukkit.bukkittemplate.ui.container.BaseUI
import top.iseason.bukkit.bukkittemplate.ui.container.LazyUIContainer
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCaches

class MailBoxContainer(val player: Player) : LazyUIContainer(
    buildList {
        val cl = MailBoxPage::class.java
        repeat(PlayerMailRecordCaches.getPlayerCache(player).page) {
            add(cl)
        }
    }
) {
    override fun getCurrentPage(player: HumanEntity): BaseUI {
        val index = viewers[player] ?: 0
        if (!allowCache || pages[index] == null) {
            pages[index] = MailBoxPage(this@MailBoxContainer.player, index)
        }
        return super.getCurrentPage(player)
    }

    override fun openFor(player: HumanEntity) {
        val currentPage = getCurrentPage(player)
        submit {
            player.openInventory(currentPage.inventory)
        }
    }

    /**
     * 更新所有页面
     */
    fun update() {
        pages.forEach {
            (it as? MailBoxPage)?.updateMails()
        }
    }
}