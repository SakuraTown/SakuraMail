package top.iseason.bukkit.sakuramail.hook

import me.clip.placeholderapi.PlaceholderAPI
import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import top.iseason.bukkittemplate.hook.BaseHook
import top.iseason.bukkittemplate.utils.bukkit.ItemUtils.applyMeta
import top.iseason.bukkittemplate.utils.bukkit.MessageUtils.toColor

object PlaceHolderHook : BaseHook("PlaceholderAPI") {

    /**
     * 设置 placeholder 并转换颜色代码
     */
    fun setPlaceHolder(str: String, player: Player? = null): String {
        if (!hasHooked) return str.toColor()
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