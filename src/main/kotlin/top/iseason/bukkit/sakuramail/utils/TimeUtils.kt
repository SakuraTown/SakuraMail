package top.iseason.bukkit.sakuramail.utils

import java.time.Duration
import java.time.LocalDateTime

object TimeUtils {

    //获取一定时间间隔的时间，例如 "PnYnMnDTnHnMnS" 可随意组合
    fun parseTime(str: String): LocalDateTime {
        val now = LocalDateTime.now()
        val parse = kotlin.runCatching { Duration.parse(str) }.getOrElse { return now }
        return now.plus(parse)
    }

}