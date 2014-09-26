package com.sauravtom.zenreddit;

import android.app.ActionBar;
import android.app.DownloadManager;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.ShareActionProvider;
import android.widget.Toast;
import com.sauravtom.zenreddit.R;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class WebViewFragment extends Fragment {

    public static WebView webView;
    public static WebViewClient webViewClient;
    private ProgressBar spinner;
    private String[] menus;
    private int position;
    private View v;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        position = getArguments().getInt("position");
        String url = getArguments().getString("url");
        menus = getResources().getStringArray(R.array.menus);

        v = inflater.inflate(R.layout.fragment_layout, container, false);

        webView = (WebView)v.findViewById(R.id.webView);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(false);


        spinner = (ProgressBar)v.findViewById(R.id.progressbar);
        spinner.setIndeterminate(true);
        spinner.setVisibility(View.GONE);

        webView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                //Toast.makeText(getBaseContext(), "On create touch", Toast.LENGTH_LONG).show();
                Log.d("1f1", "1f1 onCreated called");
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_UP:
                        if (!v.hasFocus()) {
                            v.requestFocus();
                        }
                        break;
                }
                return false;
            }
        });

        webViewClient = new WebViewClient(){

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                spinner.setVisibility(View.VISIBLE);
            }

            @Override
            public void onPageFinished(WebView view, final String url) {

                spinner.setVisibility(View.INVISIBLE);
                try {
                    if (typeUrl(url) == "comments/external")
                        getActivity().getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                }
                catch (Exception e){
                    e.printStackTrace();
                    Log.e("Fdrag3","getActionBar returned null");
                }

            }

            //Intercept and change urls to comply with reddit mobile view
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                //if(!url.endsWith(".compact") && url.startsWith("http://www.reddit.com/r/")) url += "/.compact";
                String type = typeUrl(url);
                try {
                    if(type == "youtube") {
                        url = changeYoutubeUrl(url);
                        //getActivity().getActionBar().setTitle("Youtube Player");
                        getActivity().getActionBar().hide();
                    }
                    else if(type == "vimeo") {
                        url = changeVimeoUrl(url);
                        getActivity().getActionBar().hide();
                        //getActivity().getActionBar().setTitle("Vimeo Player");
                    }
                    else if(type == "subreddit") {
                        url = url + ".compact";
                    }
                    else if (type == "comments/external") {
                        getActivity().getActionBar().setTitle("");
                    }
                    else if (type == "image") {
                        //downloadFile(url);
                    }

                }
                catch (Exception e){
                    e.printStackTrace();
                    Log.e("Fdrag2","getActionBar returned null");
                }
                view.loadUrl(url);
                return true;
            }
        };
        webView.getSettings().setJavaScriptEnabled(true);
        webView.getSettings().setBuiltInZoomControls(true);
        webView.getSettings().setAppCacheEnabled(true);
        webView.setWebViewClient(webViewClient);
        webView.loadUrl(url);

        return v;
    }

    public static String typeUrl(String url){
        if(url.contains("youtube.com/") || url.contains("youtu.be")) return "youtube";
        else if(url.contains("vimeo.com/")) return "vimeo";

        else if(url.startsWith("mailto:")){
            //share that shit
        }

        //Subreddits
        else if(url.startsWith("http://www.reddit.com") && !url.contains("/comments/") && !url.endsWith(".compact") && url != "http://www.reddit.com/.compact"){
            return "subreddit";
        }
        // comments or external links
        else if(url.contains("/comments/") || !url.startsWith("http://www.reddit.com")){
            if(url.endsWith(".jpg") || url.endsWith(".png") || url.endsWith(".gif")){
                return "image";
            }
            return "comments/external";
        }
        //Exceptional case
        else if(url == "http://www.reddit.com/.compact"){
            return "home";
        }
        return "unknown";
    }

    public String changeYoutubeUrl(String url){
        String pattern = "(?<=watch\\?v=|/videos/|embed\\/)[^#\\&\\?]*";

        Pattern compiledPattern = Pattern.compile(pattern);
        Matcher matcher = compiledPattern.matcher(url);

        if(matcher.find()){
            String id = matcher.group();
            String new_url = "http://www.youtube.com/embed/"+ id + "?autoplay=1";
            return new_url;
        }
        else{
            return url;
        }
    }

    public String changeVimeoUrl(String url){
        String id = url.substring(url.lastIndexOf('/') + 1);
        String new_url = "http://player.vimeo.com/video/"+ id + "?autoplay=1";
        return new_url;
    }



}