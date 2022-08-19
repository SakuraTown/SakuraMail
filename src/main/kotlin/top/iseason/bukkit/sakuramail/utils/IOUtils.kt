package top.iseason.bukkit.sakuramail.utils

import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.event.EventHandler
import org.bukkit.event.HandlerList
import org.bukkit.event.Listener
import org.bukkit.event.inventory.InventoryCloseEvent
import org.bukkit.inventory.Inventory
import top.iseason.bukkit.bukkittemplate.utils.Submitter
import top.iseason.bukkit.bukkittemplate.utils.submit
import top.iseason.bukkit.bukkittemplate.utils.toColor
import top.iseason.bukkit.sakuramail.SakuraMail

object IOUtils {
    /**
     * 打开一个界面，让玩家输入物品，当界面关闭时回调方法
     */
    fun Player.onItemInput(
        inv: Inventory = Bukkit.createInventory(null, 54, "&a请输入物品".toColor()),
        async: Boolean = false,
        onFinish: (Inventory) -> Unit
    ) {
        openInventory(inv)
        var submit: Submitter? = null
        val listener = object : Listener {
            @EventHandler
            fun onClose(event: InventoryCloseEvent) {
                if (event.inventory == inv) {
                    submit(async = async) {
                        onFinish(inv)
                    }
                    submit?.cancel()
                    HandlerList.unregisterAll(this)
                }
            }
        }
        submit = submit(delay = 12000) {
            if (openInventory.topInventory == inv) {
                closeInventory()
            } else {
                onFinish(inv)
                HandlerList.unregisterAll(listener)
            }
        }
        SakuraMail.registerListeners(listener)
    }
}