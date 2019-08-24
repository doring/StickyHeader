package com.doring.demo.stickyheader

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.doring.stickyheader.StickyHeaderAdapter
import com.doring.stickyheader.StickyHeaderDecoration
import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        list.layoutManager = LinearLayoutManager(this)
        val sampleAdapter = SampleAdapter()
        list.adapter = sampleAdapter
        list.addItemDecoration(StickyHeaderDecoration(sampleAdapter))
    }

    class SampleAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>(),
        StickyHeaderAdapter<RecyclerView.ViewHolder> {
        private val items = arrayListOf<String>().apply { repeat(100) { add("list item") } }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            return object : RecyclerView.ViewHolder(TextView(parent.context).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                )
                setBackgroundColor(0xffffffff.toInt())
            }) {}
        }

        override fun getItemCount() = items.size

        @SuppressLint("SetTextI18n")
        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            (holder.itemView as TextView).text = "${items[position]} $position"
        }

        override fun getHeaderItemId(position: Int): Long {
            return if (position.rem(10) == 5) position.toLong() else StickyHeaderAdapter.NO_HEADER_ID
        }

        override fun onCreateHeaderViewHolder(parent: ViewGroup, viewType: Int) =
            onCreateViewHolder(parent, viewType)

        override fun onBindHeaderViewHolder(holder: RecyclerView.ViewHolder, position: Int) =
            onBindViewHolder(holder, position)
    }
}
