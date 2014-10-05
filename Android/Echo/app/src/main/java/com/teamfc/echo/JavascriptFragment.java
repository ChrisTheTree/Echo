package com.teamfc.echo;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;

/**
 * Created by Jacky on 04/03/14.
 */
public class JavascriptFragment extends Fragment {

    public static final String FILE_URL = "FILE_URL";
    public WebView mWebView;

    public JavascriptFragment(){};
    public static JavascriptFragment newInstance(String fileUrl){
        Bundle bundle = new Bundle();
        bundle.putString(FILE_URL, fileUrl);
        JavascriptFragment fragment = new JavascriptFragment();
        fragment.setArguments(bundle);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View root = inflater.inflate(R.layout.fragment_main, container, false);
        mWebView = (WebView) root.findViewById(R.id.main_web_view);
        final Bundle arguments = getArguments();
        if(arguments!=null){
            final String url = arguments.getString(FILE_URL);
            Utility.setWebkitSettings(mWebView);
            mWebView.loadUrl(url);
        }
        return root;
    }


}
