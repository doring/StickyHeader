package com.doring.stickyheader

import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView

interface StickyHeaderAdapter<VH : RecyclerView.ViewHolder> {
    companion object {
        const val NO_HEADER_ID = -1L
    }

    fun getHeaderItemId(position: Int): Long
    fun onCreateHeaderViewHolder(parent: ViewGroup, viewType: Int): VH
    fun onBindHeaderViewHolder(holder: VH, position: Int)
}
