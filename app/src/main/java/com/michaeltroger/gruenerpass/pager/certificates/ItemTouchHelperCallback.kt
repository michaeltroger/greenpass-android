package com.michaeltroger.gruenerpass.pager.certificates

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import java.util.Collections
import kotlin.math.sign

private const val SCROLL_FACTOR = 5
private const val SELECTED_ITEM_ALPHA = 0.8f

class ItemTouchHelperCallback(
    private val adapter: CertificateAdapter,
    private val onDragFinished: (idList: List<String>) -> Unit
) : ItemTouchHelper.Callback()  {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val index = viewHolder.bindingAdapterPosition
        val indexLastMovableElement = recyclerView.adapter?.itemCount
        fun isFirstElement() = index == 0
        fun isLastElement() = index == indexLastMovableElement?.minus(1)

        val dragFlags = if (isFirstElement() && !isLastElement()) {
            ItemTouchHelper.RIGHT
        } else if (!isFirstElement() && !isLastElement()) {
            ItemTouchHelper.LEFT or ItemTouchHelper.RIGHT
        } else if (!isFirstElement() && isLastElement()) {
            ItemTouchHelper.LEFT
        } else {
            0
        }

        return makeMovementFlags(dragFlags, 0)
    }

    override fun getMoveThreshold(viewHolder: RecyclerView.ViewHolder): Float {
        return 0.15f
    }

    override fun interpolateOutOfBoundsScroll(
        recyclerView: RecyclerView,
        viewSize: Int,
        viewSizeOutOfBounds: Int,
        totalSize: Int,
        msSinceStartScroll: Long
    ): Int {
        val direction = sign(viewSizeOutOfBounds.toDouble()).toInt()
        return SCROLL_FACTOR * direction
    }

    override fun isLongPressDragEnabled(): Boolean {
        return false
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        val fromPosition = viewHolder.bindingAdapterPosition
        val toPosition = target.bindingAdapterPosition
        if (fromPosition < toPosition) {
            for (i in fromPosition until toPosition) {
                Collections.swap(adapter.list, i, i + 1)
            }
        } else {
            for (i in fromPosition downTo toPosition + 1) {
                Collections.swap(adapter.list, i, i - 1)
            }
        }
        adapter.notifyItemMoved(fromPosition, toPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        viewHolder?.itemView?.alpha = SELECTED_ITEM_ALPHA
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        viewHolder.itemView.alpha = 1f
        onDragFinished(adapter.list)
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // swiping is not supported
    }
}
