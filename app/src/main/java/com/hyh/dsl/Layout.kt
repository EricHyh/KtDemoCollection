package com.hyh.dsl

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import kotlin.coroutines.CoroutineContext

/**
 * TODO: Add Description
 *
 * @author eriche
 * @data 2021/5/7
 */


class TestActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        View {
            layout_width = 0
        }
    }

}


inline var View.layout_width: Number
    get() {
        return 0
    }
    set(value) {
        val w = if (value.dp > 0) value.dp else value.toInt()
        val h = layoutParams?.height ?: 0
        updateLayoutParams<ViewGroup.LayoutParams> {
            width = w
            height = h
        }
    }


inline fun Context.View(style: Int? = null, init: View.() -> Unit): View {
    val view =
        if (style != null) View(
            ContextThemeWrapper(this, style)
        ) else View(this)
    return view.apply(init)
}


inline fun ViewGroup.TextView(init: TextView.() -> Unit) =
    TextView(context).apply(init).also { addView(it) }


inline fun <reified T : ViewGroup.LayoutParams> View.updateLayoutParams(block: T.() -> Unit) {
    layoutParams = (layoutParams as? T)?.apply(block) ?: kotlin.run {
        val width = layoutParams?.width ?: 0
        val height = layoutParams?.height ?: 0
        val lp = ViewGroup.LayoutParams(width, height)
        new<T>(lp).apply(block)
    }
}

inline fun <reified T> new(vararg params: Any): T =
    T::class.java.getDeclaredConstructor(*params.map { it::class.java }.toTypedArray()).also { it.isAccessible = true }.newInstance(*params)


val Int.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()


val Float.dp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this,
        Resources.getSystem().displayMetrics
    )

val Float.sp: Float
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_SP,
        this,
        Resources.getSystem().displayMetrics
    )

val Number.dp: Int
    get() = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP,
        this.toFloat(),
        Resources.getSystem().displayMetrics
    ).toInt()




