package top.iseason.bukkit.sakuramail.database

import org.jetbrains.exposed.sql.SqlLogger
import org.jetbrains.exposed.sql.Transaction
import org.jetbrains.exposed.sql.statements.StatementContext
import org.jetbrains.exposed.sql.statements.expandArgs
import top.iseason.bukkit.bukkittemplate.debug.debug

object MySqlLogger : SqlLogger {
    override fun log(context: StatementContext, transaction: Transaction) {
        debug("&6DEBUG SQL: &7${context.expandArgs(transaction)}")
    }
}