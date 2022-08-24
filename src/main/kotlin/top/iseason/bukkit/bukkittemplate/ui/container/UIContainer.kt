package top.iseason.bukkit.bukkittemplate.ui.container

import org.bukkit.entity.HumanEntity
import top.iseason.bukkit.bukkittemplate.utils.submit

/**
 * 多页UI
 */
@Suppress("unused")
open class UIContainer(
    /**
     * 储存多页数组
     */
    protected val pages: Array<Pageable?>
) {

    // 页码
    val size = pages.size
    protected val viewers = mutableMapOf<HumanEntity, Int>()

    /**
     * 翻页时调用
     * @param from 源页码
     * @param to 目标页码
     */
    open var onPageChanged: ((from: Int, to: Int) -> Unit)? = null

    /**
     * 获取当前页码的UI
     */
    open fun getCurrentPage(player: HumanEntity): BaseUI? {
        val index = viewers[player] ?: 0
        val pageable = pages[index] ?: return null
        pageable.container = this
        val ui = pageable.getUI()
        if (!ui.hasBuilt) {
            ui.build()
        }
        return ui
    }

    /**
     * 定位玩家视图到下一页
     */
    open fun nextPage(player: HumanEntity) {
        val next = ((viewers[player] ?: 0) + 1) % size
        val inventory = setPage(next, player)?.inventory ?: return
        submit {
            player.openInventory(inventory)
        }
    }

    /**
     * 定位玩家视图到上一页
     */
    open fun lastPage(player: HumanEntity) {
        var last = (viewers[player] ?: 0) - 1
        if (last < 0) last += size
        val inventory = setPage(last, player)?.inventory ?: return
        submit {
            player.openInventory(inventory)
        }
    }

    /**
     * 定位到第 page 页
     */
    open fun setPage(page: Int, player: HumanEntity): BaseUI? {
        require(page in 0..size) { "page $page is not exist!" }
        onPageChanged?.invoke(viewers[player] ?: 0, page)
        viewers[player] = page
        return getCurrentPage(player)
    }

    /**
     * 为某个玩家打开UI
     */
    open fun openFor(player: HumanEntity) {
        require(pages.isNotEmpty()) { "Your pageable ui must possess at lease 1 page" }
        val currentPage = getCurrentPage(player) ?: return
        player.openInventory(currentPage.inventory)
    }


    /**
     * 复制
     */
    open fun clone(): UIContainer {
        val copyOf = pages.copyOf()
        copyOf.forEachIndexed { index, pageable ->
            if (pageable == null) return@forEachIndexed
            val clone = pageable.getUI().clone()
            copyOf[index] = clone
        }
        return UIContainer(copyOf).also { it.onPageChanged = onPageChanged }
    }
}