package top.iseason.bukkit.sakuramail.hook

import dev.lone.itemsadder.api.ItemsAdder
import org.bukkit.Bukkit
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.debug.info

object ItemsAdderHook {
    val placeholder = Bukkit.getPluginManager().getPlugin("ItemsAdder")

    init {
        if (placeholder != null) info("&a检测到 ItemsAdder")
    }

    fun getItemsAdderItem(name: String): ItemStack? {
        if (placeholder == null) return null
        return ItemsAdder.getCustomItem(name)
    }
}