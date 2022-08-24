package top.iseason.bukkit.sakuramail.utils

import java.time.*
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
     * DPT2H  ->以当日0点为基准
     */
    fun parseTime(str: String): LocalDateTime =
        runCatching {
            LocalDate.parse(str, DateTimeFormatter.ISO_LOCAL_DATE).atTime(0, 0)
        }.getOrElse {
            runCatching {
                LocalDateTime.parse(str, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
            }.getOrElse {
                val anchor = when (str.first().lowercase()) {
                    "h" -> LocalDate.now().atTime(LocalTime.now().hour, 0)
                    "d" -> {
                        LocalDate.now().atStartOfDay()
                    }

                    "w" -> {
                        val dayOfWeek = LocalDate.now().dayOfWeek
                        if (dayOfWeek == DayOfWeek.SUNDAY) LocalDate.now().atStartOfDay()
                        else LocalDate.now().minusDays(dayOfWeek.value.toLong()).atStartOfDay()
                    }

                    "m" -> {
                        val now = LocalDate.now()
                        LocalDate.of(now.year, now.month, 1).atStartOfDay()
                    }

                    else -> {
                        LocalDateTime.now()
                    }
                }
                parseRelativeTime(str.drop(1), anchor)
            }
        }

    /**
     * 格式化时间间隔
     */
    fun parseDuration(str: String): Duration? = runCatching { Duration.parse(str) }.getOrNull()
}