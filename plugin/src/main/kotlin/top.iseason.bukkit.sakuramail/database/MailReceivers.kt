package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.dao.id.EntityID
import top.iseason.bukkittemplate.config.StringEntity
import top.iseason.bukkittemplate.config.StringEntityClass
import top.iseason.bukkittemplate.config.StringIdTable

object MailReceivers : StringIdTable() {
    val params = text("params")
}

class MailReceiver(
    id: EntityID<String>
) : StringEntity(id) {
    companion object : StringEntityClass<MailReceiver>(MailReceivers)

    var params by MailReceivers.params

}