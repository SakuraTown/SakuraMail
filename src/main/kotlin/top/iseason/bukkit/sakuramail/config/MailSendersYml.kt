package top.iseason.bukkit.sakuramail.config

import org.bukkit.configuration.ConfigurationSection
import org.bukkit.configuration.file.YamlConfiguration
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.config.SimpleYAMLConfig
import top.iseason.bukkit.bukkittemplate.config.annotations.FilePath
import top.iseason.bukkit.sakuramail.database.MailSender
import java.util.*

@FilePath("senders.yml")
object MailSendersYml : SimpleYAMLConfig() {

    var senders = mutableMapOf<String, BaseMailSenderYml>()

    override val onLoaded: (ConfigurationSection.() -> Unit) = {
        config.getKeys(false).forEach { id ->
            val section = config.getConfigurationSection(id) ?: return@forEach
            val fromConfig = fromConfig(id, section) ?: return@forEach
            senders[id] = fromConfig
        }
    }

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
    fun fromConfig(id: String, section: ConfigurationSection): BaseMailSenderYml? {
        val type = section.getString("type") ?: return null
        val receivers = section.getStringList("receivers")
        val mails = section.getStringList("mails").mapNotNull { SystemMailsYml.getMailYml(it) }
        when (type.lowercase()) {
            "login" -> return LoginSenderYml(id, receivers, mails)
        }
        return null
    }

    /**
     * 上传数据
     */
    fun upload() {
        transaction {
            senders.values.forEach { sender ->
                sender.toDatabase()
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

//当玩家登录时发送
class LoginSenderYml(
    id: String,
    receivers: List<String>,
    mails: List<SystemMailYml>
) : BaseMailSenderYml(id, "login", receivers, mails) {

    override fun toSection(): ConfigurationSection {
        val section = super.toSection()
        section["type"] = "login"
        return section
    }

}

abstract class BaseMailSenderYml(
    val id: String,
    val type: String,
    /**
     * 一系列reciever的id
     */
    val receivers: List<String>,

    val mails: List<SystemMailYml>
) {
    /**
     * 上传数据库
     */
    fun toDatabase() {
        val findById = MailSender.findById(id)
        if (findById != null) {
            findById.type = type
            findById.receivers = receivers.joinToString(",")
            findById.mails = mails.joinToString(",") { it.id }
        } else MailSender.new(id) {
            this.type = this@BaseMailSenderYml.type
            this.receivers = this@BaseMailSenderYml.receivers.joinToString(",")
            this.mails = this@BaseMailSenderYml.mails.joinToString(",") { it.id }
        }
    }

    /**
     * 转配置
     */
    open fun toSection(): ConfigurationSection {
        val section = YamlConfiguration()
        section["receivers"] = receivers
        section["mails"] = mails.map { it.id }
        return section
    }

    fun onSend() {
        //TODO:
    }

    /**
     * 获取邮件接收者
     */
    fun getAllReceivers(): List<UUID> {
        var resultSet = setOf<UUID>()
        receivers.forEach {
            val split = it.split(":")
            val op = if (split.size >= 2) split[0] else "and"
            val lastOrNull = split.lastOrNull() ?: return@forEach
            val set = MailReceiversYml.getReceivers(lastOrNull)?.toSet() ?: return@forEach
            when (op) {
                "and" -> resultSet = if (resultSet.isEmpty()) set else resultSet.intersect(set)
                "andNot" -> resultSet = resultSet.toMutableSet().apply { removeAll(set) }
                "or" -> resultSet = resultSet.union(set)
                "orNot" -> resultSet =
                    resultSet.union(set).toMutableSet().apply { removeAll(resultSet.intersect(set)) }
            }
        }
        return resultSet.toList()
    }

    companion object {

    }
}
