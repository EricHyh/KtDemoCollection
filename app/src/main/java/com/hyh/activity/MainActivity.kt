package com.hyh.activity

import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.view.*
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.hyh.adapter.ChildAdapter
import com.hyh.adapter.MultiAdapter
import com.hyh.demo.R
import kotlinx.coroutines.*

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)



        /* AlertDialog.Builder(this)
             .setPositiveButton("",)*/

        /*NNWindow.with<Runnable>(this)
            .content()
            .eventListener(Runnable {})
            .show()*/



        runBlocking {
            launch(Dispatchers.Unconfined) {
                delay(10)
            }
        }


        val b1: A1 = B(A1()).t
        val b2: A2 = B(A2()).t

        val t1: A1 = C(A1()).t
        val t2: A2 = C(A2()).t
    }


    fun click(view: View) {
        Toast.makeText(applicationContext, "click", Toast.LENGTH_LONG).show()

        val dialogView = LayoutInflater.from(this).inflate(R.layout.list_dialog, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.recycler_view)
        recyclerView.layoutManager = LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false)
        recyclerView.adapter = ItemAdapter()


        val xx = BottomSheetDialog(
            ContextThemeWrapper(
                applicationContext,
                R.style.Theme_AppCompat
            )
        ).apply {

            window?.attributes?.token =
                (view.rootView.layoutParams as WindowManager.LayoutParams).token

            window?.attributes?.windowAnimations = R.style.nnwindow_bottom_anim

            setContentView(RecyclerView(this@MainActivity).apply {
                layoutParams = ViewGroup.LayoutParams(-1, resources.displayMetrics.heightPixels / 2)
                layoutManager =
                    LinearLayoutManager(this@MainActivity, LinearLayoutManager.VERTICAL, false)
                adapter = ItemAdapter()
            })
            show()


        }

        /*xx.behavior.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
            override fun onStateChanged(bottomSheet: View, newState: Int) {

            }

            override fun onSlide(bottomSheet: View, slideOffset: Float) {
            }

        })*/

        val pop =
            PopupWindow(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        pop.contentView = dialogView
        pop.isOutsideTouchable = true
        pop.height = resources.displayMetrics.heightPixels / 2

        //pop.showAtLocation(view, Gravity.BOTTOM, 0, 0)


        val adapter = MultiAdapter()
        //adapter.addChildAdapter(Xxx())
        //adapter.addChildAdapter(Xxxx())

    }

    class Xxx : ChildAdapter<String>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): com.hyh.adapter.ItemHolder<String> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }
    }

    class Xxxx : ChildAdapter<Int>() {
        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): com.hyh.adapter.ItemHolder<Int> {
            TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
        }

    }


    class ItemAdapter : RecyclerView.Adapter<ItemHolder>() {

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ItemHolder {
            val textView = TextView(parent.context)
            textView.layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT
                , ViewGroup.LayoutParams.WRAP_CONTENT
            )
            textView.setBackgroundColor(Color.WHITE)
            textView.gravity = Gravity.CENTER
            textView.setTextColor(Color.BLACK)

            return ItemHolder(textView)
        }

        override fun getItemCount(): Int {
            return 100
        }

        override fun onBindViewHolder(holder: ItemHolder, position: Int) {
            (holder.itemView as TextView).text = "第${position}条"
        }
    }


    class ItemHolder(view: View) : RecyclerView.ViewHolder(view)

}


open class A1 {

    var a1: Int = 1

    override fun toString(): String {
        return "A(a1=$a1)"
    }
}

open class A2 : A1() {

    var a2: Int = 2

    override fun toString(): String {
        return "A2(a2=$a2)"
    }

}

class B<T : A1>(val t: T) {

}

class C<out T : A1>(val t: T) {


}