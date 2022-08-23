package top.iseason.bukkit.sakuramail.utils

import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

object TimeUtils {

    //获取一定时间间隔的时间，例如 "PnYnMnDTnHnMnS" 可随意组合
    fun parseRelativeTime(str: String, base: LocalDateTime = LocalDateTime.now()): LocalDateTime {
        val parse = kotlin.runCatching { Duration.parse(str) }.getOrElse { return base }
        return base.plus(parse)
    }

    /**
     * 格式化时间
     * 支持的格式有:
     * 2022-08-24
     * 2022-08-24T12:00:00
     * P1DT2H ->以当前时间点为基准
     * RPT2H  ->以当日0点为基准
     */
    fun parseTime(str: String): LocalDateTime =
        runCatching {
            LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE).atTime(0, 0)
        }.getOrElse {
            runCatching {
                LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }.getOrElse {
                if (str.startsWith("R", true))
                    parseRelativeTime(str.drop(1), LocalDate.now().atTime(0, 0))
                else parseRelativeTime(str.drop(1))
            }
        }

    /**
     * 格式化时间间隔
     */
    fun parseDuration(str: String): Duration? = runCatching { Duration.parse(str) }.getOrNull()
}