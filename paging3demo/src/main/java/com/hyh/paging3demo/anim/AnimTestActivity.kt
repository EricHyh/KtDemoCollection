package com.hyh.paging3demo.anim

import android.app.Dialog
import android.graphics.Color
import android.os.*
import android.util.Log
import android.view.*
import android.widget.RadioGroup
import android.widget.TextView
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.hyh.paging3demo.R
import com.hyh.paging3demo.anim.ITestInterface.Companion.readArrayFromParcel
import com.hyh.paging3demo.anim.ITestInterface.Companion.readListFromParcel
import com.hyh.paging3demo.anim.ITestInterface.Companion.writeArrayToParcel
import com.hyh.paging3demo.anim.ITestInterface.Companion.writeListToParcel
import com.hyh.paging3demo.anim.internal.FragmentStarter
import com.hyh.paging3demo.utils.DisplayUtil
import com.hyh.paging3demo.utils.OSUtils

/**
 * TODO: Add Description
 *
 * @author eriche 2022/1/13
 */
class AnimTestActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "AnimTestActivity"
    }


    private val rgMode by lazy {
        findViewById<RadioGroup>(R.id.rg_mode)
    }

    private var mode1Fragment: Mode1Fragment = Mode1Fragment()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.act_anim_test)
        rgMode.setOnCheckedChangeListener { _, _ ->
            switchFragment()
        }
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, mode1Fragment)
            .commitNow()
    }

    private fun switchFragment() {
        /*val mode2Fragment = Mode2Fragment()
        supportFragmentManager.beginTransaction().add(R.id.fragment_container, mode2Fragment).commitNow()
        mode1Fragment.animOut {
            mode2Fragment.animIn()
            Handler(Looper.getMainLooper()).post {
                supportFragmentManager.beginTransaction().remove(mode1Fragment).commitNow()
            }
        }*/


        /*val dialog = Dialog(this, R.style.RightSlideDialog).apply {


            val params: WindowManager.LayoutParams? = window?.attributes
            params?.gravity = Gravity.END or Gravity.BOTTOM
            params?.width = 300
            params?.height = DisplayUtil.getScreenHeight(context)
            params?.y = 0
            params?.x = 0
            window?.attributes = params

            setContentView(TextView(context).apply {
                text = "内容"
                layoutParams = ViewGroup.LayoutParams(-1, -1)
                setBackgroundColor(Color.RED)
            })
            setCancelable(true)
            setCanceledOnTouchOutside(true)

            //防止系统栏隐藏时内容区域大小发生变化
            *//*var uiFlags = View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP && !OSUtils.isEMUI3_x()) {
                fitsNotchScreen()
                //初始化5.0以上，包含5.0
                uiFlags = initBarAboveLOLLIPOP(uiFlags)
                //android 6.0以上设置状态栏字体为暗色
                uiFlags = setStatusBarDarkFont(uiFlags, false)
                //适配android 11以上
                setBarDarkFontAboveR()
            }
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
                uiFlags =
                    uiFlags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or View.INVISIBLE or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
            }
            window?.decorView?.systemUiVisibility = uiFlags*//*


        }

        dialog.show()

        dialog.window?.decorView?.setOnApplyWindowInsetsListener { v, insets ->
            insets
        }*/

        /*dialog.window?.decorView!!.addOnAttachStateChangeListener(object : View.OnAttachStateChangeListener {
            override fun onViewDetachedFromWindow(v: View?) {

            }

            override fun onViewAttachedToWindow(v: View?) {
                val windowInsetsCompat = ViewCompat.getRootWindowInsets(dialog.window?.decorView!!)
                Log.d(TAG, "switchFragment: ")
                Handler().postDelayed({
                    val view = v
                    Log.d(TAG, "onViewAttachedToWindow: ")
                }, 1000)
            }
        })*/


        val listOf = mutableListOf<ITestInterface>(
            TestInterface1(0, "0"),
            TestInterface2(1, "1"),
            TestInterface3(2, "2")
        ) as ArrayList

        /*val listOf1 = mutableListOf<ITestInterface>(
            TestInterface1(0, "0"),
            TestInterface2(1, "1"),
            TestInterface3(2, "2")
        ) as ArrayList*/

        val bundle = Bundle()
        bundle.classLoader = this.classLoader

        bundle.putParcelableArrayList("list", listOf)
        bundle.putParcelable("data", TestInterface1(0, "0"))
        //bundle.putParcelable("TestList", TestList(listOf))
        bundle.putParcelable("TestList", TestList(listOf, listOf.toTypedArray()))

        val toByteArray = bundle.toByteArray()

        val convertToObject = toByteArray?.convertToObject<Bundle>()
        convertToObject?.classLoader = this.classLoader

        val parcelableArrayList = convertToObject?.getParcelableArrayList<ITestInterface>("list")
        val data = convertToObject?.getParcelable<TestInterface1>("data")
        val testList = convertToObject?.getParcelable<TestList>("TestList")

        Log.d(TAG, "switchFragment: ")

    }


    /**
     * 适配刘海屏
     * Fits notch screen.
     */
    private fun fitsNotchScreen() {
        val window = window ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            try {
                val lp: WindowManager.LayoutParams = window.getAttributes()
                lp.layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                window.setAttributes(lp)
            } catch (e: Exception) {
            }
        }
    }

    /**
     * 初始化android 5.0以上状态栏和导航栏
     *
     * @param flags the ui flags
     * @return the int
     */
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private fun initBarAboveLOLLIPOP(flags: Int): Int {
        val window = window ?: return flags
        //获得默认导航栏颜色
        var uiFlags = flags

        //Activity全屏显示，但状态栏不会被隐藏覆盖，状态栏依然可见，Activity顶端布局部分会被状态栏遮住。
        uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        //Activity全屏显示，但导航栏不会被隐藏覆盖，导航栏依然可见，Activity底部布局部分会被导航栏遮住。
        uiFlags = uiFlags or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION

        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS)
        //判断是否存在导航栏
        /*if (mBarConfig.hasNavigationBar()) {
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_NAVIGATION)
        }*/
        //需要设置这个才能设置状态栏和导航栏颜色
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        //设置状态栏颜色
        window.statusBarColor = ColorUtils.blendARGB(
            Color.TRANSPARENT,
            Color.TRANSPARENT, 0.0f
        )
        return uiFlags
    }

    /**
     * Sets status bar dark font.
     * 设置状态栏字体颜色，android6.0以上
     */
    private fun setStatusBarDarkFont(uiFlags: Int, statusBarDarkFont: Boolean): Int {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (statusBarDarkFont) {
                uiFlags or View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                uiFlags
            }
        } else {
            uiFlags
        }
    }

    private fun setBarDarkFontAboveR() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            setStatusBarDarkFontAboveR(false)
            setNavigationIconDarkAboveR(false)
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private fun setStatusBarDarkFontAboveR(statusBarDarkFont: Boolean) {
        val window = window ?: return
        val contentView = window.decorView.findViewById<View>(android.R.id.content) ?: return
        val windowInsetsController: WindowInsetsController =
            contentView.windowInsetsController ?: return
        if (statusBarDarkFont) {
            unsetSystemUiFlag(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR)
            windowInsetsController.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        } else {
            windowInsetsController.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_STATUS_BARS
            )
        }
    }

    @RequiresApi(api = Build.VERSION_CODES.R)
    private fun setNavigationIconDarkAboveR(navigationBarDarkIcon: Boolean) {
        val window = window ?: return
        val contentView = window.decorView.findViewById<View>(android.R.id.content) ?: return
        val controller: WindowInsetsController = contentView.windowInsetsController ?: return
        if (navigationBarDarkIcon) {
            controller.setSystemBarsAppearance(
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        } else {
            controller.setSystemBarsAppearance(
                0,
                WindowInsetsController.APPEARANCE_LIGHT_NAVIGATION_BARS
            )
        }
    }

    protected fun unsetSystemUiFlag(systemUiFlag: Int) {
        val decorView: View = window?.decorView ?: return
        decorView.systemUiVisibility = (
                decorView.systemUiVisibility
                        and systemUiFlag.inv())
    }
}


interface ITestInterface : Parcelable {

    val id: Long

    val text: String


    companion object {

        /**
         * 将账户列表数据写入[Parcel]
         */
        @JvmStatic
        fun Parcel.writeListToParcel(list: List<ITestInterface>, flags: Int) {
            val array = list.toTypedArray()
            this.writeArrayToParcel(array, flags)
        }

        @JvmStatic
        fun Parcel.readListFromParcel(): List<ITestInterface> {
            val array = this.readArrayFromParcel()
            return array.toList()
        }

        @JvmStatic
        fun Parcel.writeArrayToParcel(array: Array<ITestInterface>, flags: Int) {
            this.writeParcelableArray(array, flags)
        }

        @JvmStatic
        fun Parcel.readArrayFromParcel(): Array<ITestInterface> {
            val classLoader = ITestInterface::class.java.classLoader
            val parcelableArray = this.readParcelableArray(classLoader) ?: emptyArray()
            return Array(parcelableArray.size) {
                parcelableArray[it] as ITestInterface
            }
        }
    }
}


class TestInterface1(
    initialId: Long,
    initialText: String
) : ITestInterface {


    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(text)
    }

    override val id: Long = initialId

    override val text: String = initialText

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestInterface1> {
        override fun createFromParcel(parcel: Parcel): TestInterface1 {
            return TestInterface1(parcel)
        }

        override fun newArray(size: Int): Array<TestInterface1?> {
            return arrayOfNulls(size)
        }
    }
}


class TestInterface2(
    initialId: Long,
    initialText: String
) : ITestInterface {


    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(text)
    }

    override val id: Long = initialId

    override val text: String = initialText

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestInterface2> {
        override fun createFromParcel(parcel: Parcel): TestInterface2 {
            return TestInterface2(parcel)
        }

        override fun newArray(size: Int): Array<TestInterface2?> {
            return arrayOfNulls(size)
        }
    }
}


class TestInterface3(
    initialId: Long,
    initialText: String
) : ITestInterface {


    constructor(parcel: Parcel) : this(
        parcel.readLong(),
        parcel.readString() ?: ""
    ) {
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeLong(id)
        parcel.writeString(text)
    }

    override val id: Long = initialId

    override val text: String = initialText

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestInterface3> {
        override fun createFromParcel(parcel: Parcel): TestInterface3 {
            return TestInterface3(parcel)
        }

        override fun newArray(size: Int): Array<TestInterface3?> {
            return arrayOfNulls(size)
        }
    }
}


class TestList(
    val list: List<ITestInterface>,
    val array: Array<ITestInterface>
) : Parcelable {

    constructor(parcel: Parcel) : this(
        parcel.readListFromParcel(),
        parcel.readArrayFromParcel(),
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeListToParcel(list, flags)
        parcel.writeArrayToParcel(array, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<TestList> {
        override fun createFromParcel(parcel: Parcel): TestList {
            return TestList(parcel)
        }

        override fun newArray(size: Int): Array<TestList?> {
            return arrayOfNulls(size)
        }
    }

}


inline fun <reified T : Parcelable> T.toByteArray(): ByteArray? {
    return Parcel.obtain().use {
        writeParcelable(this@toByteArray, 0)
        marshall()
    }
}

inline fun <reified T : Parcelable> ByteArray.convertToObject(): T? {
    return Parcel.obtain().use {
        unmarshall(this@convertToObject, 0, this@convertToObject.size)
        setDataPosition(0)
        readParcelable(this@convertToObject.javaClass.classLoader)
    }
}

inline fun <reified T> Parcel.use(action: Parcel.() -> T): T {
    return try {
        action()
    } finally {
        recycle()
    }
}





