package com.hyh.toast;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.ImageView;
import android.widget.Toast;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

    }

    public void showToast(View view) {
        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                ImageView imageView = new ImageView(MainActivity.this);
                imageView.setImageResource(R.drawable.ic_launcher_background);
                imageView.setClickable(true);
                imageView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        Toast.makeText(MainActivity.this, "自定义Toast被点击了", Toast.LENGTH_SHORT).show();
                    }
                });
                Toast toast = new Toast(new MyContext(getApplication()));
                toast.setDuration(Toast.LENGTH_LONG);
                toast.setView(imageView);
                toast.show();
            }
        }, 3000);
    }
}