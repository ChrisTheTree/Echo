package com.teamfc.echo;

import android.annotation.TargetApi;
import android.os.Build;
import android.webkit.WebView;

/**
 * Created by Jacky on 04/03/14.
 */
public class Utility {
    @TargetApi(Build.VERSION_CODES.JELLY_BEAN)
    public static void setWebkitSettings(WebView mWebView) {
        mWebView.getSettings().setJavaScriptEnabled(true);
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.JELLY_BEAN){
            mWebView.getSettings().setAllowFileAccessFromFileURLs(true);
        }
    }
}

