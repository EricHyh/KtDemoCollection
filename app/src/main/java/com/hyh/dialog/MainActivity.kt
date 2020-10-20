package com.hyh.dialog

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

       /* AlertDialog.Builder(this)
            .setPositiveButton("",)*/

    }

    fun click(view: View) {Toast.makeText(this,"click",Toast.LENGTH_LONG).show()}
}
