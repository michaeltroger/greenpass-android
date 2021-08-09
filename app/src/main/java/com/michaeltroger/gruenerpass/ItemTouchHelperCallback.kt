package com.michaeltroger.gruenerpass

import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.RecyclerView
import com.xwray.groupie.GroupieAdapter
import kotlin.math.sign

class ItemTouchHelperCallback(private val adapter: GroupieAdapter) : ItemTouchHelper.Callback()  {

    override fun getMovementFlags(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder
    ): Int {
        val index = viewHolder.adapterPosition
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
        return 3 * direction
    }

    override fun isLongPressDragEnabled(): Boolean {
        return true
    }

    override fun onMove(
        recyclerView: RecyclerView,
        viewHolder: RecyclerView.ViewHolder,
        target: RecyclerView.ViewHolder
    ): Boolean {
        adapter.notifyItemMoved(viewHolder.adapterPosition, target.adapterPosition)
        return true
    }

    override fun onSelectedChanged(viewHolder: RecyclerView.ViewHolder?, actionState: Int) {
        super.onSelectedChanged(viewHolder, actionState)
        // Handle action state changes
    }

    override fun clearView(recyclerView: RecyclerView, viewHolder: RecyclerView.ViewHolder) {
        super.clearView(recyclerView, viewHolder)
        // Called by the ItemTouchHelper when the user interaction with an element is over and it also completed its animation
        // This is a good place to send update to your backend about changes
    }

    override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
        // swiping is not supported
    }
}