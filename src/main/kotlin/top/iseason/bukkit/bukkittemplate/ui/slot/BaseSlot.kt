package top.iseason.bukkit.bukkittemplate.ui.slot

import org.bukkit.inventory.Inventory
import org.bukkit.inventory.ItemStack
import top.iseason.bukkit.bukkittemplate.ui.container.BaseUI
import top.iseason.bukkit.bukkittemplate.ui.container.UIContainer

/**
 * 一个物品槽
 */
interface BaseSlot {
    /**
     * 物品槽的位置
     */
    val index: Int

    /**
     * 存在的物品栏
     */
    var baseInventory: Inventory?


    /**
     * 存在的物品
     */
    var itemStack: ItemStack?

    /**
     * 复制Slot到指定Index
     */
    fun clone(index: Int): BaseSlot

    /**
     * 重置Slot
     */
    fun reset()

    var serializeId: String

}

/**
 * 设置序列化id，用于序列化,必须设置
 */
fun <T : BaseSlot> T.serializeId(serializeId: String): T {
    this.serializeId = serializeId
    return this
}

/**
 * 获取slot的ui
 */
fun <T : BaseSlot> T.getUI(): BaseUI = baseInventory!!.holder as BaseUI

/**
 * 获取slot的container
 */
fun <T : BaseSlot> T.getContainer(): UIContainer? = getUI().container

