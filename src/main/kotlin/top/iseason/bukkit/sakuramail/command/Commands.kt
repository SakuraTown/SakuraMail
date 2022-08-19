package top.iseason.bukkit.sakuramail.command

import org.bukkit.entity.Player
import org.bukkit.inventory.ItemStack
import org.bukkit.permissions.PermissionDefault
import org.jetbrains.exposed.sql.SortOrder
import org.jetbrains.exposed.sql.transactions.transaction
import top.iseason.bukkit.bukkittemplate.command.Param
import top.iseason.bukkit.bukkittemplate.command.commandRoot
import top.iseason.bukkit.bukkittemplate.utils.bukkit.ItemUtils
import top.iseason.bukkit.sakuramail.database.PlayerTime
import top.iseason.bukkit.sakuramail.database.PlayerTimes
import top.iseason.bukkit.sakuramail.utils.IOUtils.onItemInput
import java.util.*

fun command() {
    val op1 = listOf("--and", "--or", "--andNot", "--orNot")
    val op2 = listOf("loginTime", "quitTime")
    val op3 = listOf("before.time", "after.time", "between.time1.time2")
    val op4 = listOf("greater.time", "less.time")
    val listOf = mutableListOf<String>()
    for (s1 in op1) {
        for (s2 in op2) {
            for (s3 in op3) {
                listOf.add("$s1:$s2.$s3")
            }
        }
        for (s4 in op4) {
            listOf.add("$s1:playTime.$s4")
        }
    }
    commandRoot(
        "sakuramail",
        alias = arrayOf("smail", "mail"),
        default = PermissionDefault.OP,
        async = true,
        description = "测试命令2",
        params = arrayOf(
            Param("<条件>", listOf),
            Param("", listOf),
            Param("", listOf),
            Param("", listOf),
            Param("", listOf),
            Param("", listOf),
            Param("", listOf),
            Param("", listOf),
        )
    ) {
        node("test", isPlayerOnly = true) {
            onExecute {
                (it as Player).onItemInput {
                    val mutableMapOf = mutableMapOf<Int, ItemStack>()
                    it.contents.forEachIndexed { index, itemStack ->
                        if (itemStack == null) return@forEachIndexed
                        mutableMapOf[index] = itemStack
                    }
                    val toBase64 = ItemUtils.toBase64(mutableMapOf)
                    println(toBase64)
                }
                true
            }
        }

        node("target", isPlayerOnly = true) {
            onExecute {
                val parseArgs = PlayerTimes.parseArgs(this.params) ?: return@onExecute true
                transaction {
                    val mutableSetOf = mutableSetOf<UUID>()
                    PlayerTime.find { parseArgs }.orderBy(PlayerTimes.id to SortOrder.DESC).forEach {
                        if (mutableSetOf.contains(it.player)) return@forEach
                        println(PlayerTimes.getTotalTime(it.player))
                        mutableSetOf.add(it.player)
                    }
                }
                true
            }
        }
    }
}