package com.doring.stickyheader

import android.graphics.Canvas
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.RelativeLayout
import androidx.core.view.NestedScrollingParent
import androidx.core.view.forEach
import androidx.recyclerview.widget.RecyclerView
import com.doring.stickyheader.StickyHeaderAdapter.Companion.NO_HEADER_ID

class StickyHeaderDecoration constructor(
    private val parentAdapter: StickyHeaderAdapter<RecyclerView.ViewHolder>,
    private var headerVisiblePolicy: HeaderVisiblePolicy = HeaderVisiblePolicy.ViewTop
) : RecyclerView.ItemDecoration() {

    /**
     * define when sticky header view appear.
     */
    enum class HeaderVisiblePolicy {
        /**
         * appear when sticky view top has gone from screen.
         */
        ViewTop,
        /**
         * appear when sticky view bottom has gone from screen.
         */
        ViewBottom,
        /**
         * appear when sticky view's height match with currently scrolling child view.
         */
        HeaderHeight
    }

    private val headerCache = hashMapOf<Long, RecyclerView.ViewHolder>()
    private var stickyHeaderParent: ViewGroup? = null

    private val visibilityGone: (RecyclerView.ViewHolder) -> Unit = {
        if (it.itemView.visibility != View.GONE) {
            it.itemView.visibility = View.GONE
        }
    }

    /**
     * get currently showing sticky header
     */
    var currentStickyHeader: RecyclerView.ViewHolder? = null
        private set

    override fun onDrawOver(canvas: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        val stickyHeader = latestStickyHeader(parent) ?: hideAllStickyExceptOne(parent)

        currentStickyHeader = stickyHeader
        if (stickyHeader != null) {
            drawStickyHeader(parent, stickyHeader, calcStickyHeaderY(parent, stickyHeader.itemView))
        } else {
            hideAllCachedHeaders()
        }
    }

    private fun latestStickyHeader(parent: RecyclerView): RecyclerView.ViewHolder? {
        parent.forEach { childView ->
            val adapterPos = parent.getChildAdapterPosition(childView)
            if (isStickyHeaderHolder(adapterPos)) {
                val header = getOrCreateStickyHeader(parent, adapterPos) ?: return@forEach
                val y = when (headerVisiblePolicy) {
                    HeaderVisiblePolicy.ViewBottom -> childView.bottom.toFloat()
                    HeaderVisiblePolicy.HeaderHeight -> (childView.bottom - header.itemView.height).toFloat()
                    // HeaderVisiblePolicy.ViewTop
                    else -> childView.y
                }

                if (y < 0) {
                    return header
                } else {
                    header.apply(visibilityGone)
                }
            }
        }

        return null
    }

    /**
     * Hide all passed sticky headers and return currently visible(latest) sticky header.
     */
    private fun hideAllStickyExceptOne(parent: RecyclerView): RecyclerView.ViewHolder? {
        if (parent.childCount == 0) {
            return null
        }
        // the sticky header position would be skipped when we scrolled the RecyclerView very fast.
        // so we must check all passed sticky headers.
        val adapterPos = parent.getChildAdapterPosition(parent.getChildAt(0))
        val passedAllStickyHeaders = (0 until adapterPos)
            .filter(this::isStickyHeaderHolder)
            .mapNotNull { getOrCreateStickyHeader(parent, it) }

        return if (passedAllStickyHeaders.isEmpty()) null else {
            passedAllStickyHeaders.reduce { acc, viewHolder ->
                acc.apply(visibilityGone)

                // viewHolder is the next accumulator value.
                // it's important to do not apply gone visibility if when viewHolder has EditText widgets.
                viewHolder
            }
        }
    }

    /**
     * sticky header will be added to RecyclerView's parent (exclude refreshable layout like SwipeRefreshLayout)
     */
    private fun drawStickyHeader(
        parent: RecyclerView,
        stickyHeader: RecyclerView.ViewHolder,
        y: Float
    ) {
        val stickyView = stickyHeader.itemView

        if (stickyView.visibility != View.VISIBLE) {
            stickyView.visibility = View.VISIBLE
        }

        if (stickyView.parent == null) {
            // sticky header 가 추가될 view group 을 찾아놓음
            if (stickyHeaderParent == null) {
                var parentView = parent.parent as View
                while (!(parentView is RelativeLayout || parentView is FrameLayout) || parentView is NestedScrollingParent) {
                    parentView = parentView.parent as View
                }
                stickyHeaderParent = parentView as ViewGroup
            }

            stickyHeaderParent?.addView(
                stickyView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
        }

        stickyView.translationY = parent.y + y
    }

    private fun isStickyHeaderHolder(position: Int): Boolean {
        return position != RecyclerView.NO_POSITION && parentAdapter.getHeaderItemId(position) != NO_HEADER_ID
    }

    private fun calcStickyHeaderY(parent: RecyclerView, target: View): Float {
        return (0 until parent.childCount)
            .map {
                val childView = parent.getChildAt(it)
                val childAdapterPos = parent.getChildAdapterPosition(childView)

                childAdapterPos to childView
            }
            .filter { isStickyHeaderHolder(it.first) && it.second.y >= 0 }
            .map { it.second.y - target.height }
            .firstOrNull { it < 0 } ?: 0f
    }

    private fun getOrCreateStickyHeader(
        parent: RecyclerView,
        position: Int
    ): RecyclerView.ViewHolder? {
        val key = parentAdapter.getHeaderItemId(position)

        var holder = headerCache[key]

        if (holder == null) {
            val viewType = parent.adapter?.getItemViewType(position) ?: return null

            holder = parentAdapter.onCreateHeaderViewHolder(parent, viewType)
            headerCache[key] = holder
        }

        val headerView = holder.itemView

        parentAdapter.onBindHeaderViewHolder(holder, position)

        val widthSpec =
            View.MeasureSpec.makeMeasureSpec(parent.measuredWidth, View.MeasureSpec.EXACTLY)
        val heightSpec =
            View.MeasureSpec.makeMeasureSpec(parent.measuredHeight, View.MeasureSpec.UNSPECIFIED)

        val childWidth = ViewGroup.getChildMeasureSpec(
            widthSpec,
            parent.paddingLeft + parent.paddingRight, headerView.layoutParams?.width ?: 0
        )
        val childHeight = ViewGroup.getChildMeasureSpec(
            heightSpec,
            parent.paddingTop + parent.paddingBottom, headerView.layoutParams?.height ?: 0
        )

        headerView.measure(childWidth, childHeight)
        headerView.layout(0, 0, headerView.measuredWidth, headerView.measuredHeight)

        return holder
    }

    private fun hideAllCachedHeaders() {
        headerCache.values.forEach(visibilityGone)
    }

    /**
     * remove all sticky header view from parent layout
     */
    fun clearHeaderCache() {
        headerCache.values.forEach { stickyHeaderParent?.removeView(it.itemView) }
        headerCache.clear()
    }
}