package com.hyh.paging3demo.anim

import android.animation.ObjectAnimator
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.TranslateAnimation
import androidx.core.view.forEach
import androidx.core.view.forEachIndexed
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.hyh.paging3demo.R
import com.hyh.paging3demo.fragment.CommonBaseFragment

/**
 * TODO: Add Description
 *
 * @author eriche 2022/1/13
 */
class Mode1Fragment : CommonBaseFragment() {
    companion object {
        private const val TAG = "Mode1Fragment"
    }

    private val anim: Boolean by lazy { arguments?.getBoolean("anim", false) ?: false }

    private var contentView: RecyclerView? = null

    override fun initData() {
    }

    override fun getContentView(inflater: LayoutInflater, container: ViewGroup?): View {
        val context = container?.context ?: inflater.context
        return RecyclerView(context).apply {
            setBackgroundColor(Color.WHITE)
            //setPadding(0, 1000, 0, 0)
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            layoutManager = LinearLayoutManager(context, LinearLayoutManager.VERTICAL, false)
            adapter = Mode1ListAdapter()
        }
    }

    override fun initView(contentView: View?) {
        this.contentView = contentView as RecyclerView
    }


    fun animOut(action: () -> Unit) {
        var isAction = false
        contentView?.apply {
            forEachIndexed { index, view ->
                if ( getChildAdapterPosition(view) == 0) return@forEachIndexed
                val animationSet = AnimationSet(true)
                animationSet.setAnimationListener(object : Animation.AnimationListener {
                    override fun onAnimationRepeat(animation: Animation?) {
                    }

                    override fun onAnimationEnd(animation: Animation?) {
                        if (!isAction) {
                            isAction = true
                            action()
                        }
                    }

                    override fun onAnimationStart(animation: Animation?) {
                    }
                })
                animationSet.addAnimation(AlphaAnimation(1.0F, 0.0F))
                animationSet.addAnimation(TranslateAnimation(0F, 0F, 0F, 100F))
                animationSet.duration = 200
                view.startAnimation(animationSet)
            }
        }
    }
}


class Mode1ListAdapter : RecyclerView.Adapter<Mode1Holder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Mode1Holder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_anim_test_mode1_1, parent, false)
        return Mode1Holder(view)
    }

    override fun getItemCount(): Int {
        return 15
    }

    override fun onBindViewHolder(holder: Mode1Holder, position: Int) {

    }
}

class Mode1Holder(view: View) : RecyclerView.ViewHolder(view) {

}