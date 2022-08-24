package top.iseason.bukkit.sakuramail.ui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkit.bukkittemplate.ui.slot.*
import top.iseason.bukkit.bukkittemplate.utils.sendColorMessages
import top.iseason.bukkit.sakuramail.config.MailBoxGUIConfig
import top.iseason.bukkit.sakuramail.database.MailRecordCaches
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCaches
import top.iseason.bukkit.sakuramail.hook.PlaceHolderHook

class MailBoxPage(
    val player: Player,
    val page: Int = 0,
    val mailCache: PlayerMailRecordCaches = MailRecordCaches.getPlayerCache(player)
) : ChestUI(
    PlaceHolderHook.setPlaceHolder(
        MailBoxGUIConfig.title
            .replace("%sakura_mail_current_page%", (page + 1).toString())
            .replace("%sakura_mail_total_page%", mailCache.page.toString()), player
    ), row = MailBoxGUIConfig.row,
    clickDelay = 500L
) {
    private var mails = mailCache.getCache(page)
    private val icon = Icon(ItemStack(Material.AIR), 0)

    private val mail = Button(ItemStack(Material.AIR), 0).serializeId("mail").onClicked(true) {
        println("open mail")
    }
    private val nextPage = Button(ItemStack(Material.PAPER), 0).onClicked(true) {
        this.getContainer()?.nextPage(player)
    }
    private val lastPage = Button(ItemStack(Material.PAPER), 0).onClicked(true) {
        this.getContainer()?.lastPage(player)
    }
    private val clear = Button(ItemStack(Material.PAPER), 0).onClicked(true) {
        if (mails == null) return@onClicked
        transaction {
            mails!!.forEach {
                if (it.canGetKit()) return@forEach
                MailRecordCaches.getPlayerCache(player).removeCache(it)
                it.remove()
            }
        }
        updateMails()
        player.sendColorMessages("&a清除成功!")
    }
    private val getAll = Button(ItemStack(Material.PAPER), 0).onClicked(true) {
        if (mails == null) return@onClicked
        var count = 0
        transaction {
            for (mail in mails!!) {
                if (!mail.getKitSliently()) {
                    player.sendColorMessages("&c背包空间不足!")
                    break
                }
                count++
            }
        }
        updateMails()
        player.sendColorMessages("&a已领取 &6$count &a个邮件!")
    }

    init {
        setUpSlots(icon, MailBoxGUIConfig.icons)
        setUpSlots(nextPage, MailBoxGUIConfig.nextPage)
        setUpSlots(lastPage, MailBoxGUIConfig.lastPage)
        setUpSlots(mail, MailBoxGUIConfig.mails)
        setUpSlots(getAll, MailBoxGUIConfig.getAll)
        setUpSlots(clear, MailBoxGUIConfig.clearAccepted)
        updateMails()
    }

    private fun setUpSlots(slot: BaseSlot, map: MutableMap<ItemStack, List<Int>>) {
        map.forEach { (item, list) ->
            for (i in list) {
                val clone = slot.clone(i)
                if ("mail" != clone.serializeId) {
                    if (clone is ClickSlot) {
                        clone.rawItemStack = PlaceHolderHook.setPlaceHolder(item, player)
                    } else if (clone is Icon) {
                        clone.rawItemStack = PlaceHolderHook.setPlaceHolder(item, player)
                    }
                }
                clone.setup()
            }
        }
    }


    private fun updateMails() {
        mails = mailCache.getCache(page)
        val iterator = mails?.iterator()
        slots.forEach {
            if ("mail" != it?.serializeId) return@forEach
            if (it !is Button) return@forEach
            if (iterator != null && iterator.hasNext()) {
                val stack = iterator.next().icon
                it.rawItemStack = stack
                it.itemStack = stack
            } else {
                it.rawItemStack = null
                it.itemStack = null
            }
        }
        player.updateInventory()

    }

}

