package top.iseason.bukkit.sakuramail.config

import org.bukkit.Bukkit
import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.bukkit.entity.Player
import org.jetbrains.exposed.sql.deleteAll
import org.jetbrains.exposed.sql.transactions.transaction
import org.quartz.*
import org.quartz.impl.StdSchedulerFactory
import top.iseason.bukkit.bukkittemplate.config.DatabaseConfig
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.bukkittemplate.debug.info
import top.iseason.bukkit.sakuramail.SakuraMail
import top.iseason.bukkit.sakuramail.database.MailRecord
import top.iseason.bukkit.sakuramail.database.MailSender
import top.iseason.bukkit.sakuramail.database.MailSenders
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.*
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit


@FilePath("senders.yml")
object MailSendersYml : SimpleYAMLConfig() {
    val executor = ScheduledThreadPoolExecutor(10).apply {
        removeOnCancelPolicy = true
    }
    val scheduler: Scheduler = StdSchedulerFactory(SakuraMail.loadOrCopyQuartzProperties()).scheduler
    val schedules = mutableListOf<ScheduledFuture<*>>()
    var senders = mutableMapOf<String, MailSenderYml>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        senders.clear()
        config.getKeys(false).forEach { id ->
            val section = config.getConfigurationSection(id) ?: return@forEach
            val fromConfig = fromConfig(id, section) ?: return@forEach
            senders[id] = fromConfig
        }
        updateSender()
    }

    /**
     * 更新发送器计时
     */
    fun updateSender() {
        //取消所有
        schedules.forEach { it.cancel(false) }
        schedules.clear()
        scheduler.clear()
        senders.values.forEach {
            when (it.type) {
                "ontime" -> setupOnTimeTask(it)
                "period" -> setupPeriodTask(it)
            }
        }
        scheduler.start()
    }

    /**
     * 设置准时任务
     */
    fun setupOnTimeTask(sender: MailSenderYml) {
        val now = LocalDateTime.now()
        val time = runCatching {
            LocalDateTime.parse(sender.param, DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        }.getOrNull() ?: return
        if (time.isBefore(now)) {
            info("&6OnTime 任务 ${sender.id} 已过期: $time")
            return
        }
        val between = Duration.between(now, time)
        schedules += executor.schedule(sender, between.toMillis(), TimeUnit.MILLISECONDS)
        info("&aOnTime &a任务 &6${sender.id} &a将在: &6$time &a执行")
    }

    /**
     * 设置定时任务
     */
    fun setupPeriodTask(sender: MailSenderYml) {
        val cronSchedule = runCatching { CronScheduleBuilder.cronSchedule(sender.param) }.getOrElse {
            info("&6period 任务 &7${sender.id} &6的 cron表达式 不正确:&7 ${sender.param}")
            return
        }
        val trigger = TriggerBuilder.newTrigger()
            .withIdentity(sender.id, "sakura-mail-sender")
            .withSchedule(cronSchedule)
            .build()
        val job = JobBuilder.newJob(PeriodJob::class.java)
            .withIdentity(sender.id, "sakura-mail-sender")
            .usingJobData("id", sender.id)
            .build()
        scheduler.scheduleJob(job, trigger);
        info("&aOnTime &a任务 &6${sender.id} 已启动: &6${sender.param}")
    }


    /**
     * 获取邮件发送者并缓存
     */
    fun getSender(id: String) =
        senders[id] ?: transaction { MailSender.findById(id)?.toMailSenderYml()?.apply { senders[id] = this } }


    /**
     * 保存数据到本地
     */
    fun saveAll() {
        config.getKeys(false).forEach { config[it] = null }
        senders.forEach { (key, sender) -> config[key] = sender.toSection() }
        (config as YamlConfiguration).save(configPath)
    }

    /**
     * 从配置读取单个sender
     */
    private fun fromConfig(id: String, section: ConfigurationSection): MailSenderYml? {
        var type = section.getString("type")?.lowercase() ?: return null
        if (type !in setOf("login", "ontime", "period")) type = "manual"
        var param = section["param"] ?: ""
        param = if (param is Date)
            LocalDateTime.ofInstant(param.toInstant(), ZoneId.of("Z")).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)
        else
            param.toString()
        val receivers = section.getStringList("receivers")
        val mails = section.getStringList("mails").mapNotNull { SystemMailsYml.getMailYml(it) }
        return MailSenderYml(id, type, param, receivers, mails)
    }

    /**
     * 上传数据
     */
    fun upload() {
        transaction {
            MailSenders.deleteAll()
            senders.values.forEach { sender ->
                MailSender.new(sender.id) {
                    this.type = sender.type
                    this.param = sender.param
                    this.receivers = sender.receivers.joinToString(",")
                    this.mails = sender.mails.joinToString(",") { it.id }
                }
            }
        }
    }

    /**
     * 下载数据
     */
    fun download() {
        senders.clear()
        transaction {
            MailSender.all()
        }.forEach {
            val mailSenderYml = it.toMailSenderYml() ?: return@forEach
            senders[mailSenderYml.id] = mailSenderYml
        }
        saveAll()
    }
}

class MailSenderYml(
    val id: String,
    val type: String,
    val param: String,
    var receivers: List<String>,
    var mails: List<SystemMailYml>
) : Runnable {
    /**
     * 上传数据库
     */
    fun toDatabase() {
        val findById = MailSender.findById(id)
        if (findById != null) {
            findById.type = type
            findById.param = param
            findById.receivers = receivers.joinToString(",")
            findById.mails = mails.joinToString(",") { it.id }
        } else MailSender.new(id) {
            this.type = this@MailSenderYml.type
            this.param = this@MailSenderYml.param
            this.receivers = this@MailSenderYml.receivers.joinToString(",")
            this.mails = this@MailSenderYml.mails.joinToString(",") { it.id }
        }
    }

    /**
     * 转配置
     */
    fun toSection(): ConfigurationSection {
        val section = YamlConfiguration()
        section["param"] = param
        section["type"] = type
        section["receivers"] = receivers
        section["mails"] = mails.map { it.id }
        return section
    }

    fun onSend(receivers: List<UUID>) {
        if (!DatabaseConfig.isConnected) {
            error("&c邮件 ${id} 发送失败，数据库未链接!")
        }
        if (mails.isEmpty()) return
        transaction {
            for (m in mails) {
                if (receivers.isEmpty()) continue
                for (receiver in receivers) {
                    MailRecord.new {
                        player = receiver
                        mail = m.id
                        sendTime = LocalDateTime.now()
                    }
                }
                info("&a已发送&6 ${m.id} ${receivers.size} &a份!")
            }
        }
    }

    /**
     * 获取邮件接收者
     */
    fun getAllReceivers(receivers: List<String>, player: Player? = null): List<UUID> {
        var resultSet = setOf<UUID>()
        receivers.forEach {
            val split = it.split(":")
            val op = if (split.size >= 2) split[0] else "and"
            val lastOrNull = split.lastOrNull() ?: return@forEach
            val receiver = MailReceiversYml.getReceiver(lastOrNull) ?: return@forEach
            val set = MailReceiversYml.getReceivers(receiver, player).toSet()
            when (op.lowercase()) {
                "and" -> resultSet = if (resultSet.isEmpty()) set else resultSet.intersect(set)
                "andnot" -> {
                    val all = Bukkit.getOfflinePlayers()
                        .mapNotNull { p -> if (p.hasPlayedBefore()) p.uniqueId else null }.toMutableSet()
                    all.removeAll(set)
                    resultSet = if (resultSet.isEmpty()) all else resultSet.intersect(all)
                }

                "or", "add" -> resultSet = resultSet.union(set)
                "remove" -> resultSet = resultSet.toMutableSet().apply { removeAll(set) }
                "ornot" -> {
                    val all = Bukkit.getOfflinePlayers()
                        .mapNotNull { p -> if (p.hasPlayedBefore()) p.uniqueId else null }.toMutableSet()
                    all.removeAll(set)
                    resultSet = resultSet.union(all)
                }
            }
        }
        return resultSet.toList()
    }

    /**
     * 准时任务ontime调用的
     */
    override fun run() {
        onSend(getAllReceivers(receivers))
    }

}

class PeriodJob : Job {
    override fun execute(context: JobExecutionContext) {
        val id = context.jobDetail.jobDataMap.getString("id")
        MailSendersYml.getSender(id)?.run()
    }
}
