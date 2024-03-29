package com.hyh.paging3demo;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.widget.ProgressBar;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.hyh.paging3demo.web.BaseWebChromeClient;
import com.hyh.paging3demo.web.CustomWebView;
import com.hyh.paging3demo.web.WebClient;


/**
 * @author Administrator
 * @description
 * @data 2020/4/14
 */
public class WebActivity extends AppCompatActivity {

    private WebClient mWebClient;

    public static void start(Context context, String url) {
        Intent intent = new Intent(context, WebActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.putExtra("url", url);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        String url = getIntent().getStringExtra("url");



        Toolbar toolbar = findViewById(R.id.tool_bar);
        setSupportActionBar(toolbar);
        ActionBar actionBar = getSupportActionBar();

        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
            actionBar.setHomeButtonEnabled(true);
            actionBar.setTitle(null);
        }


        ProgressBar progressBar = findViewById(R.id.web_progressbar);
        CustomWebView webView = findViewById(R.id.web_view);

        mWebClient = new WebClient(this, webView, null, null, progressBar);
        mWebClient.setWebChromeClient(new BaseWebChromeClient() {
            @Override
            public void onReceivedTitle(WebView view, String title) {
                super.onReceivedTitle(view, title);
                toolbar.setTitle(title);
            }
        });
        mWebClient.loadUrl(url);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        if (mWebClient != null && mWebClient.onBackPressed()) return;
        super.onBackPressed();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mWebClient.destroy();
    }
}