package top.iseason.bukkit.sakuramail.ui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkit.bukkittemplate.ui.slot.*
import top.iseason.bukkit.bukkittemplate.utils.MessageUtils.formatBy
import top.iseason.bukkit.bukkittemplate.utils.MessageUtils.sendColorMessage
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCache
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCaches
import top.iseason.bukkit.sakuramail.hook.PlaceHolderHook

class MailBoxPage(
    val player: Player,
    private val page: Int = 0,
    private val mailCache: PlayerMailRecordCache = PlayerMailRecordCaches.getPlayerCache(player)
) : ChestUI(
    PlaceHolderHook.setPlaceHolder(
        MailBoxGUIYml.title
            .replace("%sakura_mail_current_page%", (page + 1).toString())
            .replace("%sakura_mail_total_page%", mailCache.page.toString()), player
    ), row = MailBoxGUIYml.row,
    clickDelay = 500L
) {
    private var mails = mailCache.getPageCache(page)
    private var mailIndex = mutableMapOf<Int, Int>()

    private val icon = Icon(ItemStack(Material.AIR), 0)

    private val mail = MailSlot().onClicked(true) {
        val mailIndex = mailIndex[index] ?: return@onClicked
        val mailRecordCache = mails?.getOrNull(mailIndex) ?: return@onClicked
        val build = MailContent(player, mailRecordCache, this@MailBoxPage).build()
        submit {
            player.openInventory(build)
        }
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
                PlayerMailRecordCaches.getPlayerCache(player).removeCache(it)
                it.remove()
            }
        }
        updateMails()
        player.sendColorMessage(Lang.ui_clear_success)
    }
    private val getAll = Button(ItemStack(Material.PAPER), 0).onClicked(true) {
        if (mails == null) return@onClicked
        var count = 0
        transaction {
            for (mail in mails!!) {
                if (!mail.canGetKit()) continue
                if (!mail.getKitSliently()) {
                    player.sendColorMessage(Lang.ui_getAll_no_space)
                    break
                }
                count++
            }
        }
        if (count == 0) {
            player.sendColorMessage(Lang.ui_getAll_no_mail)
            return@onClicked
        }
        updateMails()
        player.sendColorMessage(Lang.ui_getAll_success.formatBy(count))
    }

    init {
        setUpSlots(icon, MailBoxGUIYml.icons)
        setUpSlots(nextPage, MailBoxGUIYml.nextPage)
        setUpSlots(lastPage, MailBoxGUIYml.lastPage)
        setUpSlots(mail, MailBoxGUIYml.mails)
        setUpSlots(getAll, MailBoxGUIYml.getAll)
        setUpSlots(clear, MailBoxGUIYml.clearAccepted)
        updateMails()
    }

    private fun setUpSlots(slot: BaseSlot, map: MutableMap<ItemStack, List<Int>>) {
        map.forEach { (item, list) ->
            for (i in list) {
                val clone = slot.clone(i)
                if (slot is MailSlot) {
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


    fun updateMails() {
        mails = mailCache.getPageCache(page)
        mailIndex.clear()
        val iterator = mails?.iterator()
        var index = 0
        slots.forEach {
            if (it !is MailSlot) return@forEach
            if (iterator != null && iterator.hasNext()) {
                val stack = iterator.next().icon
                it.rawItemStack = stack
                it.itemStack = stack
                mailIndex[it.index] = index
                index++
            } else {
                it.rawItemStack = null
                it.itemStack = null
            }

        }
        player.updateInventory()
    }

    class MailSlot : Button(ItemStack(Material.AIR), 0) {
        override fun clone(index: Int): MailSlot {
            return MailSlot().also {
                it.baseInventory = baseInventory
                it.onClick = onClick
                it.onClicked = onClicked
                it.asyncClick = asyncClick
            }
        }
    }

}

