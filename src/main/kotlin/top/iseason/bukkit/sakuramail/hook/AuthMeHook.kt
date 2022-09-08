package top.iseason.bukkit.sakuramail.hook

import org.bukkit.Bukkit
import top.iseason.bukkit.bukkittemplate.debug.info

object AuthMeHook {
    val authMe = Bukkit.getPluginManager().getPlugin("AuthMe")
    val hasHook = authMe != null

    init {
        if (hasHook) info("&a检测到 AuthMe")
    }

}