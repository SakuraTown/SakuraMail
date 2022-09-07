package top.iseason.bukkit.sakuramail.ui

import org.bukkit.Material
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.ui.container.ChestUI
import top.iseason.bukkit.bukkittemplate.ui.slot.*
import top.iseason.bukkit.bukkittemplate.utils.MessageUtils.sendColorMessage
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.sakuramail.Lang
import top.iseason.bukkit.sakuramail.config.MailBoxGUIYml
import top.iseason.bukkit.sakuramail.config.MailContentYml
import top.iseason.bukkit.sakuramail.database.MailRecordCache
import top.iseason.bukkit.sakuramail.database.PlayerMailRecordCaches
import top.iseason.bukkit.sakuramail.hook.PlaceHolderHook

class MailContent(
    val player: Player,
    private val mail: MailRecordCache,
    private val lastUI: MailBoxPage? = null
) : ChestUI(
    mail.mailYml.title,
    row = MailBoxGUIYml.row,
    clickDelay = 500L
) {

    private val back = Button(ItemStack(Material.AIR)).onClicked {
        if (lastUI != null) {
            player.openInventory(lastUI.inventory)
        } else player.closeInventory()

    }
    private val accept = Button(ItemStack(Material.AIR)).onClicked(true) {
        if (transaction { mail.getKit() }) {
            lastUI?.updateMails()
            //切换状态
            MailContentYml.accepts.values.flatten().forEach {
                (slots[it] as? Button)?.itemStack = null
                slots[it] = null
            }
            MailContentYml.accepteds.forEach { (item, slots) ->
                for (slot in slots) {
                    baseInventory?.setItem(slot, item)
                }
            }

        }
    }
    private val accepted = Icon(ItemStack(Material.AIR), 0)

    private val delete = Button(ItemStack(Material.AIR)).onClicked(true) {
        if (mail.canGetKit()) {
            player.sendColorMessage(Lang.ui_delete_not_accept)
            return@onClicked
        }
        PlayerMailRecordCaches.getPlayerCache(player).removeCache(mail)
        transaction { mail.remove() }
        lastUI?.updateMails()
        submit {
            if (lastUI != null) {
                player.openInventory(lastUI.inventory)
            } else player.closeInventory()
        }
    }

    init {
        setUpSlots(back, MailContentYml.backs)
        setUpSlots(delete, MailContentYml.delete)
        if (mail.canGetKit())
            setUpSlots(accept, MailContentYml.accepts)
        else
            setUpSlots(accepted, MailContentYml.accepteds)
        mail.mailYml.items.forEach { (t, u) ->
            Icon(u, t).setup()
        }
    }

    private fun setUpSlots(slot: BaseSlot, map: MutableMap<ItemStack, List<Int>>) {
        map.forEach { (item, list) ->
            for (i in list) {
                val clone = slot.clone(i)
                if (clone is ClickSlot) {
                    clone.rawItemStack = PlaceHolderHook.setPlaceHolder(item, player)
                } else if (clone is Icon) {
                    clone.rawItemStack = PlaceHolderHook.setPlaceHolder(item, player)
                }
                clone.setup()
            }
        }
    }
}