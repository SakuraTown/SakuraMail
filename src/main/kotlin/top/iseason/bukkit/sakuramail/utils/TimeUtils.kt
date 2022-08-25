package top.iseason.bukkit.sakuramail.utils

import java.time.*
import java.time.format.DateTimeFormatter
import kotlin.math.abs

object TimeUtils {
    var timeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

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

    fun formatDuration(duration: Duration): String {
        val seconds = duration.seconds
        val absSeconds = abs(seconds)
        val positive = String.format(
            "%d 天 %02d 时 %02d 分",
            absSeconds / 86400,
            absSeconds % 86400 / 3600,
            absSeconds % 3600 / 60
        )
        return if (seconds < 0) "-$positive" else positive
    }

    fun formatTime(time: LocalDateTime) = time.format(timeFormat)
}