package top.iseason.bukkit.sakuramail.hook

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.Bukkit
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.utils.bukkit.applyMeta
import top.iseason.bukkit.bukkittemplate.utils.toColor

object PlaceHolderHook {
    val placeholder = Bukkit.getPluginManager().getPlugin("PlaceholderAPI")

    init {
        if (placeholder != null) info("&a检测到 PlaceHolderAPI")
    }

    /**
     * 设置 placeholder 并转换颜色代码
     */
    fun setPlaceHolder(str: String, player: Player? = null): String {
        if (placeholder == null) return str.toColor()
        return PlaceholderAPI.setPlaceholders(player, str).toColor()
    }

    fun setPlaceHolder(item: ItemStack, player: Player? = null): ItemStack {
        return item.applyMeta {
            if (hasDisplayName())
                setDisplayName(setPlaceHolder(displayName, player))
            if (hasLore())
                lore = lore!!.map { setPlaceHolder(it, player) }
        }
    }
}