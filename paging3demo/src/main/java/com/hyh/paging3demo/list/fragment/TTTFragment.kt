package com.hyh.paging3demo.list.fragment

import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.SystemClock
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.os.postDelayed
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import kotlin.random.Random

class TTTFragment : Fragment() {

    companion object {
        private const val TAG = "TTTFragment"

        var withItemAnimator = false
    }


    val list = listOf(
        "条目：0",
        "条目：1",
        "条目：2",
        "条目：3",
        "条目：4",
        "条目：5",
        "条目：6",
        "条目：7",
        "条目：8",
        "条目：9",
        "条目：10",
        "条目：11",
        "条目：12",
        "条目：13",
        "条目：14",
        "条目：15",
        "条目：16",
        "条目：17",
        "条目：18",
        "条目：19",
    )

    val random = Random(System.currentTimeMillis())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_trade_tab_page, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view)

        val tttAdapter = TTTTAdapter()

        view.findViewById<View>(R.id.btn_refresh).setOnClickListener {

            //val tttAdapter = TTTAdapter()
            //recyclerView.adapter = tttAdapter

            //val start = random.nextInt(0, 5)
            //val end = random.nextInt(10, 20)

            //Log.d(TAG, "onViewCreated: $start - $end")

            //tttAdapter.setData(list.subList(start, end))

            tttAdapter.list.removeAt(0)


        }
        recyclerView.layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
        if (!withItemAnimator) {
            //recyclerView.itemAnimator = null
        }



        recyclerView.adapter = tttAdapter

        tttAdapter.setData(
            listOf(
                "条目：0",
                "条目：1",
                "条目：2",
                "条目：3",
                "条目：4",
                "条目：5",
                "条目：6",
                "条目：7",
                "条目：8",
                "条目：9",
                "条目：10",
                "条目：11",
                "条目：12",
                "条目：13",
                "条目：14",
                "条目：15",
                "条目：16",
                "条目：17",
                "条目：18",
                "条目：19",
            )
        )
        tttAdapter.notifyDataSetChanged()
        /*tttAdapter.setData(
            listOf(
                "条目：1",
                "条目：2",
                "条目：3",
                "条目：4",
                "条目：5",
                "条目：6",
                "条目：7",
                "条目：8",
                "条目：9",
                "条目：10",
                "条目：11",
                "条目：12",
                "条目：13",
                "条目：14",
                "条目：15",
                "条目：16",
                "条目：17",
                "条目：18",
                "条目：19",
                "条目：20",
            )
        )
        recyclerView.adapter = TTTAdapter()*/
    }


    class TTTAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        private val mDiffer: AsyncListDiffer<String?> = AsyncListDiffer(this, object : DiffUtil.ItemCallback<String?>() {
            override fun areItemsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }

            override fun areContentsTheSame(oldItem: String, newItem: String): Boolean {
                return oldItem == newItem
            }
        })


        fun setData(list: List<String?>) {
            mDiffer.submitList(list)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val textView = TextView(parent.context)
            textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 50)
            textView.gravity = Gravity.CENTER
            textView.textSize = 10F
            textView.setTextColor(Color.BLACK)
            return object : RecyclerView.ViewHolder(textView) {}
        }

        override fun getItemCount(): Int {
            return mDiffer.currentList.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val string = mDiffer.currentList[position]
            (holder.itemView as TextView).text = string
        }
    }

    class TTTTAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

        var list: MutableList<String> = mutableListOf()

        fun setData(list: List<String>) {
            this.list = ArrayList(list)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            val textView = TextView(parent.context)
            textView.layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 200)
            textView.gravity = Gravity.CENTER
            textView.textSize = 10F
            textView.setTextColor(Color.BLACK)
            return object : RecyclerView.ViewHolder(textView) {}
        }

        override fun getItemCount(): Int {
            return list.size
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            val string = list[position]
            (holder.itemView as TextView).text = string
        }
    }
}
