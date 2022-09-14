package top.iseason.bukkit.sakuramail.hook

import dev.lone.itemsadder.api.CustomStack
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.hook.BaseHook

object ItemsAdderHook : BaseHook("ItemsAdder") {

    fun getItemsAdderItem(name: String): ItemStack? {
        if (!hasHooked) return null
        return CustomStack.getInstance(name)?.itemStack
    }

    fun getItemsAdderItem(itemStack: ItemStack): CustomStack? {
        if (!hasHooked) return null
        return CustomStack.byItemStack(itemStack)
    }

}