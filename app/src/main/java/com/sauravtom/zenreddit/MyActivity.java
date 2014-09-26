package com.sauravtom.zenreddit;

import android.annotation.SuppressLint;
import android.app.*;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.res.AssetManager;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.support.v4.app.ActionBarDrawerToggle;
import android.support.v4.widget.DrawerLayout;
import android.util.Log;
import android.view.*;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.*;
import android.widget.AdapterView.OnItemClickListener;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.sauravtom.zenreddit.R;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import in.championswimmer.sfg.lib.SimpleFingerGestures;

public class MyActivity extends Activity {

    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    private ActionBarDrawerToggle mDrawerToggle;
    private String mTitle = "";
    boolean doubleBackToExitPressedOnce;

    @SuppressLint("NewApi")
    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        if (!isNetworkAvailable()) {
            AlertDialog.Builder alert = new AlertDialog.Builder(this);

            alert.setMessage("Cannot find the internet !!");

            alert.setPositiveButton("Try Again", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    finish();
                    startActivity(getIntent());
                }
            });


            alert.show();
        }

        this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().requestFeature(Window.FEATURE_PROGRESS);
        setContentView(R.layout.activity_main);

        mTitle = "";
        getActionBar().setTitle(mTitle);

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);

        mDrawerList = (ListView) findViewById(R.id.drawer_list);
        mDrawerToggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                R.drawable.ic_drawer, R.string.drawer_open,
                R.string.drawer_close) {

            /** Called when drawer is closed */
            public void onDrawerClosed(View view) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

            /** Called when a drawer is opened */
            public void onDrawerOpened(View drawerView) {
                getActionBar().setTitle(mTitle);
                invalidateOptionsMenu();
            }

        };

        // Setting DrawerToggle on DrawerLayout
        mDrawerLayout.setDrawerListener(mDrawerToggle);

        // Creating an ArrayAdapter to add items to the listview mDrawerList
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(getBaseContext(),
                R.layout.drawer_list_item, getResources().getStringArray(R.array.menus));

        mDrawerList.setAdapter(adapter);

        getActionBar().setHomeButtonEnabled(true);
        getActionBar().setDisplayHomeAsUpEnabled(true);

        // Setting item click listener for the listview mDrawerList
        mDrawerList.setOnItemClickListener(new OnItemClickListener() {

            @Override
            public void onItemClick(AdapterView<?> parent, View view,int position, long id) {

                String[] menuItems = getResources().getStringArray(R.array.menus);
                mTitle = menuItems[position];

                WebViewFragment rFragment = new WebViewFragment();

                Bundle data = new Bundle();
                data.putInt("position", position);
                data.putString("url", getUrl(position));
                rFragment.setArguments(data);
                FragmentManager fragmentManager = getFragmentManager();
                FragmentTransaction ft = fragmentManager.beginTransaction();
                ft.replace(R.id.content_frame, rFragment);
                ft.commit();

                getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                mDrawerLayout.closeDrawer(mDrawerList);

            }
        });

        InitFragment("0");
        AddTabs();
        // Look up the AdView as a resource and load a request.
        AdView adView = (AdView)this.findViewById(R.id.adView);
        AdRequest adRequest = new AdRequest.Builder().build();
        adView.loadAd(adRequest);

        /*Not working
        SimpleFingerGestures mySfg = new SimpleFingerGestures();

        mySfg.setOnFingerGestureListener(new SimpleFingerGestures.OnFingerGestureListener()
        {
            @Override
            public boolean onSwipeUp ( int fingers, long gestureDuration){
                Toast.makeText(getBaseContext(), "Swipe Up", Toast.LENGTH_SHORT).show();
                //grtv.setText("swiped " + fingers + " up");
                return false;
            }

            @Override
            public boolean onSwipeDown ( int fingers, long gestureDuration){
                Toast.makeText(getBaseContext(), "Swipe Down", Toast.LENGTH_SHORT).show();
                //grtv.setText("swiped " + fingers + " down");
                return false;
            }

            @Override
            public boolean onSwipeLeft ( int fingers, long gestureDuration){
                //grtv.setText("swiped " + fingers + " left");
                return false;
            }

            @Override
            public boolean onSwipeRight ( int fingers, long gestureDuration){
                //grtv.setText("swiped " + fingers + " right");
                return false;
            }
            @Override
            public boolean onPinch(int fingers, long gestureDuration) {
                //grtv.setText("pinch");
                return false;
            }

            @Override
            public boolean onUnpinch(int fingers, long gestureDuration) {
                //grtv.setText("unpinch");
                return false;
            }
        });
        */
    }

    public void AddTabs(){
        final ActionBar actionBar = getActionBar();
        actionBar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);

        ActionBar.TabListener tabListener = new ActionBar.TabListener() {
            public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
                String url = "http://www.reddit.com/r/"+tab.getText().toString()+"/.compact";
                if (tab.getText().toString() == "Home"){
                    url = "http://www.reddit.com/.compact";
                }
                try{
                    getActionBar().setTitle("/r/"+tab.getText().toString());
                }
                catch (Exception e){

                }
                InitFragment(url);
            }

            public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // hide the given tab
            }

            public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
                // probably ignore this event
            }
        };

        String[] subreddit_arr = loadJSONFromAsset();
        actionBar.addTab(
                actionBar.newTab()
                        .setText("Home")
                        .setTabListener(tabListener));

        for (int i = 0; i < subreddit_arr.length-1; i++) {
            actionBar.addTab(
                    actionBar.newTab()
                            .setText(subreddit_arr[i])
                            .setTabListener(tabListener));
        }

    }

    public String[] loadJSONFromAsset() {
        String json = "Boo";

        try {

            AssetManager assetManager = getResources().getAssets();
            InputStream is = assetManager.open("subreddits.json");
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            json = new String(buffer, "UTF-8");

        } catch (IOException ex) {
            ex.printStackTrace();
            Log.e("Fdrag","Error reading file");
            return null;
        }
        return json.split(",");

    }

    public void InitFragment(String url){
        if (url.length() < 2 ) url = "http://www.reddit.com/.compact";
        WebViewFragment rFragment = new WebViewFragment();
        Bundle data = new Bundle();
        data.putInt("position", 1);
        data.putString("url", url);
        rFragment.setArguments(data);
        FragmentManager fragmentManager = getFragmentManager();
        FragmentTransaction ft = fragmentManager.beginTransaction();
        ft.replace(R.id.content_frame, rFragment);
        ft.commit();
    }

    protected String getUrl(int position) {
        switch (position) {
            case 0:
                GoToDialog();
                return "";
            case 1:return "http://www.reddit.com/subreddits/mine.compact";
            case 2:return "http://www.reddit.com/message/inbox/.compact";
            case 3:return "http://www.reddit.com/message/moderator/.compact";
            case 4:
                SearchDialog();
                return "";
            case 5:return "http://www.reddit.com/message/compose/.compact";
            case 6:return "http://www.reddit.com/submit.compact";
            case 7:return "http://www.reddit.com/r/random/.compact";
            case 8:return "http://www.reddit.com/login/.compact";
            case 9:return "http://sauravtom.com/zenreddit_links/#about";
            case 10:return "http://sauravtom.com/zenreddit_links/#pro";
            default:
                return "";
        }
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        mDrawerToggle.syncState();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mDrawerToggle.onOptionsItemSelected(item)) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** Called whenever we call invalidateOptionsMenu() */
    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        boolean drawerOpen = mDrawerLayout.isDrawerOpen(mDrawerList);

        menu.findItem(R.id.action_reload).setVisible(!drawerOpen);
        menu.findItem(R.id.action_share).setVisible(!drawerOpen);
        //menu.findItem(R.id.action_download).setVisible(false);

        if(drawerOpen) {
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        }
        //menu.findItem(R.id.action_share).setShowAsAction(1);

        return super.onPrepareOptionsMenu(menu);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);

        String current_url = WebViewFragment.webView.getUrl();
        MenuItem icon_share = menu.findItem(R.id.action_share);
        MenuItem icon_reload = menu.findItem(R.id.action_reload);
        MenuItem icon_download = menu.findItem(R.id.action_download);

        icon_download.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                String url = WebViewFragment.webView.getUrl();
                if (WebViewFragment.typeUrl(url) == "image"){
                    downloadFile(url);
                }else{
                    Toast.makeText(getBaseContext(), "No media found in url.", Toast.LENGTH_SHORT).show();
                }

                return false;
            }
        });

        icon_reload.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem menuItem) {
                WebViewFragment.webView.reload();
                return false;
            }
        });

        ShareActionProvider mShareActionProvider = (ShareActionProvider) icon_share.getActionProvider();
        Intent myIntent = new Intent();
        myIntent.setAction(Intent.ACTION_SEND);
        myIntent.putExtra(Intent.EXTRA_TEXT, current_url + " (via @zenreddit)");
        myIntent.setType("text/plain");
        mShareActionProvider.setShareIntent(myIntent);

        return true;
    }

    /*Class that prevents closing tha app on back button press, instead go to the previous page. Double press closes the app.*/
    @Override
    public void onBackPressed() {

        if(doubleBackToExitPressedOnce){
            finish();
        }
        getActionBar().show();

        String url = WebViewFragment.webView.getUrl();

        if (url == "http://www.reddit.com/.compact") {
            this.doubleBackToExitPressedOnce = true;
            Toast.makeText(this, "Please click BACK again to exit", Toast.LENGTH_SHORT).show();
        }

        if(!url.contains("/comments/") && url.startsWith("http://www.reddit.com") && url != "http://www.reddit.com/.compact"){
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        }

        invalidateOptionsMenu();
        if(WebViewFragment.webView.canGoBack()){
            WebViewFragment.webView.goBack();
        }else{
            InitFragment("0");
            //finish();
        }

        new Handler().postDelayed(new Runnable() {

            @Override
            public void run() {
                doubleBackToExitPressedOnce=false;
            }
        }, 2000);
    }

    public void downloadFile(String uRl) {
        String dirName = "ZenReddit";
        String[] bits = uRl.split("/");
        String fileName = bits[bits.length-1];

        File direct = new File(Environment.getExternalStorageDirectory() + "/"+dirName);

        if (!direct.exists()) {
            direct.mkdirs();
        }

        DownloadManager mgr = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);

        Uri downloadUri = Uri.parse(uRl);
        DownloadManager.Request request = new DownloadManager.Request(
                downloadUri);

        request.setAllowedNetworkTypes(
                DownloadManager.Request.NETWORK_WIFI
                        | DownloadManager.Request.NETWORK_MOBILE)
                .setAllowedOverRoaming(false).setTitle(fileName)
                .setDescription("Image downloaded via Zen reddit app.")
                .setDestinationInExternalPublicDir("/"+dirName, fileName);

        Toast.makeText(this, "Downloading Image to ZenReddit folder.", Toast.LENGTH_LONG).show();
        mgr.enqueue(request);

    }

    public void GoToDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Enter subreddit");

        final EditText input = new EditText(this);
        alert.setView(input);

        alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                WebViewFragment.webView.loadUrl("http://www.reddit.com/r/"+value+"/.compact");
                try{
                    getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    getActionBar().setTitle("/r/" + value);
                }
                catch(Exception e){}
            }
        });

        alert.setNeutralButton("Random !!", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                WebViewFragment.webView.loadUrl("http://www.reddit.com/random/.compact");
                try{
                    getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    getActionBar().setTitle("/r/random");
                }
                catch(Exception e){}
            }
        });

        alert.setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    public void SearchDialog(){
        AlertDialog.Builder alert = new AlertDialog.Builder(this);

        alert.setTitle("Enter Keyword");

        final EditText input = new EditText(this);
        //final EditText subreddit = new EditText(this);
        alert.setView(input);
        //alert.setView(subreddit);

        alert.setPositiveButton("Go", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                String value = input.getText().toString();
                //String subreddit = subreddit.getText().toString();

                WebViewFragment.webView.loadUrl("http://www.reddit.com/search.compact?q="+value+"&sort=relevance&t=all");
                try{
                    getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
                    getActionBar().setTitle("Search");
                }
                catch(Exception e){}
            }
        });

        alert.setNegativeButton("Nevermind", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int whichButton) {
                // Canceled.
            }
        });

        alert.show();
    }

    private boolean isNetworkAvailable() {
        ConnectivityManager connectivityManager
                = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
        return activeNetworkInfo != null && activeNetworkInfo.isConnected();
    }

    @Override
    public void onSaveInstanceState(Bundle savedInstanceState) {
        super.onSaveInstanceState(savedInstanceState);
        // Save UI state changes to the savedInstanceState.
        // This bundle will be passed to onCreate if the process is
        // killed and restarted.
        savedInstanceState.putString("url", WebViewFragment.webView.getUrl());
        savedInstanceState.putInt("tab_index", getActionBar().getSelectedNavigationIndex());
        savedInstanceState.putString("subreddit", getActionBar().getTitle().toString());

    }
    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        // Restore UI state from the savedInstanceState.
        // This bundle has also been passed to onCreate.
        int tab_index = savedInstanceState.getInt("tab_index");
        String url = savedInstanceState.getString("url");
        InitFragment(url);
        if (tab_index == -1) {
            String subreddit = savedInstanceState.getString("subreddit");
            getActionBar().setTitle(subreddit);
            getActionBar().setNavigationMode(ActionBar.NAVIGATION_MODE_STANDARD);
        } else {
            getActionBar().setSelectedNavigationItem(tab_index);
        }
    }


}