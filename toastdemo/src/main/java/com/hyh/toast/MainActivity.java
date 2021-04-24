package com.hyh.toast;

import android.graphics.Rect;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.hyh.toast.utils.DisplayUtil;

import kotlin.Unit;
import kotlin.jvm.functions.Function0;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        /*FtToast.regularToast()
                .text("测试一下")
                .show();*/
    }

    public void showToast(View view) {
        View test_view = findViewById(R.id.text_view);

        test_view.setTranslationX(400);


        int[] location = new int[2];
        test_view.getLocationOnScreen(location);

        Rect rect = new Rect();

        boolean localVisibleRect = test_view.getGlobalVisibleRect(rect);


        int screenWidth = DisplayUtil.INSTANCE.getScreenWidth(this);
        int screenHeight = DisplayUtil.INSTANCE.getScreenHeight(this);


        Toast.makeText(this, localVisibleRect + "--" + rect.toString(), Toast.LENGTH_SHORT).show();


        /*FtToast.multipleToast()
                .duration(2000)
                .anchorView(view)
                .text("测试一下")
                .clickAction(new Function0<Unit>() {
                    @Override
                    public Unit invoke() {
                        Toast.makeText(MainActivity.this, "xxxxxxxx", Toast.LENGTH_SHORT).show();
                        return null;
                    }
                })
                .show();





        PopupWindow popupWindow = new PopupWindow(MainActivity.this);

        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
               *//* PopupWindow popupWindow = new PopupWindow(MainActivity.this);
                View imageView = new View(MainActivity.this);
                imageView.setBackgroundColor(Color.RED);
                popupWindow.setWidth(400);
                popupWindow.setHeight(400);
                popupWindow.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
                popupWindow.setContentView(imageView);
                popupWindow.showAsDropDown(view);*//*
                //popupWindow.showAtLocation(view, Gravity.CENTER, 0, 0);


                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("测试")
                        .show();
            }
        }, 1000);*/




        /*new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                *//*ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageResource(R.drawable.ic_launcher_background);
                imageView.setClickable(true);
                imageView.setOnClickListener(v -> Toast.makeText(MainActivity.this, "自定义Toast被点击了", Toast.LENGTH_SHORT).show());*//*
                View toastView = new View(MainActivity.this);
                toastView.setBackgroundColor(Color.RED);
                toastView.setLayoutParams(new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

                Toast toast = new Toast(new MyContext(getApplication()));
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setGravity(Gravity.TOP, 0, 0);
                toast.setView(toastView);
                toast.show();
            }
        }, 1000);*/
    }
}