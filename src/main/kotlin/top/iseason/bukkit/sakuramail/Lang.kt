package top.iseason.bukkit.sakuramail

import org.bukkit.configuration.ConfigurationSection
import top.iseason.bukkit.bukkittemplate.BukkitTemplate
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.Comment
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.config.annotations.Key
import top.iseason.bukkit.bukkittemplate.debug.SimpleLogger
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.bukkittemplate.utils.MessageUtils
import top.iseason.bukkit.sakuramail.utils.TimeUtils
import java.time.format.DateTimeFormatter

@Key
@FilePath("lang.yml")
object Lang : SimpleYAMLConfig(updateNotify = false) {
    var prefix = "&a[&6${BukkitTemplate.getPlugin().description.name}&a] &f"

    @Comment("", "gui中的时间变量显示格式")
    var time_Format = "yyyy-MM-dd HH:mm:ss"

    var database_error = "&6数据库异常，请联系管理员!"
    var new_mail = "&a你有一封新的邮件!"
    var ui_clear_success = "&a清除成功!"
    var ui_getAll_no_space = "&c背包空间不足!"
    var ui_getAll_no_mail = "&6没有可领取的邮件!"
    var ui_getAll_success = "&a已领取 &6 {0} &a个邮件!"
    var ui_delete_not_accept = "&a该邮件尚未领取!"
    var ui_get_has_accepted = "&6该邮件已领取!"
    var ui_get_no_space = "&6你的背包没有足够的空间!"
    var ui_get_success = "&a领取成功"
    var login_tip = "&a你有 {0} 个未领取的邮件, 请输入/smail open 领取!"

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        TimeUtils.timeFormat = DateTimeFormatter.ofPattern(time_Format)
        SimpleLogger.prefix = prefix
        MessageUtils.defaultPrefix = prefix
        info("&a语言文件已重载!")
    }
}