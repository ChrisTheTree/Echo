package com.teamfc.echo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentStatePagerAdapter;
import android.support.v4.app.ListFragment;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.teamfc.echo.util.AmazonUtils;
import com.teamfc.echo.util.ColorUtils;
import com.teamfc.echo.util.FileUtils;

import java.util.ArrayList;


public class OnboardingActivity extends FragmentActivity implements QueueFragment.OnFragmentInteractionListener {
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_onboarding);

        //Sexy things
        getActionBar().setDisplayShowTitleEnabled(false);

        //Sexy pager
        mPager = (ViewPager) findViewById(R.id.pager);
        mPagerAdapter = new ScreenSlidePagerAdapter(getSupportFragmentManager());
        mPager.setAdapter(mPagerAdapter);

        //Gangster songs
        makeQueue();
    }

    private static final int READ_REQUEST_CODE = 42;


    public void performAudioSearch() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("audio/*");
        startActivityForResult(intent, READ_REQUEST_CODE);
    }

    @TargetApi(Build.VERSION_CODES.KITKAT)
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent resultData) {
        if (requestCode == READ_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            if (resultData != null) {
                processSong(resultData.getData());
            }
        }
    }

    private void processSong(Uri uri) {
        String path = FileUtils.getPath(this, uri);
        MediaMetadataRetriever myRetriever = new MediaMetadataRetriever();
        myRetriever.setDataSource(this, uri);

        //TODO: emit meta data to server
        int hash = Math.abs(path.hashCode());
        String title = myRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = myRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
        String type = myRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE);

        Context context = getApplicationContext();
        CharSequence text = "E C H O I N G";
        int duration = Toast.LENGTH_SHORT;

        Toast toast = Toast.makeText(context, text, duration);
        toast.show();
        new AmazonUtils.uploadS3(this).execute(path, hash + "");
    }

    private void makeQueue() {
        ArrayList<Song> songs = new ArrayList<Song>();
        ColorUtils colorUtils = new ColorUtils(this);
        songs.add(new Song("Baby","Justin Biebs",colorUtils.getColor()));
        songs.add(new Song("Niggas in Paris","Jay-Z, Kanye West",colorUtils.getColor()));
        songs.add(new Song("The Humpty Dance", "Digital Underground",colorUtils.getColor()));
        songs.add(new Song("Thrift Shop", "Macklemore & Ryan Lewis feat. Wanz",colorUtils.getColor()));
        songs.add(new Song("Tootsee Roll", "69 Boyz",colorUtils.getColor()));
        songs.add(new Song("Baby","Justin Biebs",colorUtils.getColor()));
        songs.add(new Song("Niggas in Paris","Jay-Z, Kanye West",colorUtils.getColor()));
        songs.add(new Song("The Humpty Dance", "Digital Underground",colorUtils.getColor()));
        songs.add(new Song("Thrift Shop", "Macklemore & Ryan Lewis feat. Wanz",colorUtils.getColor()));
        songs.add(new Song("Tootsee Roll", "69 Boyz",colorUtils.getColor()));

        SongAdapter adapter = new SongAdapter(this, songs);
        ((ListFragment)mPagerAdapter.queueFragment).setListAdapter(adapter);
    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.onboarding, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id){
            case R.id.action_upload:
                performAudioSearch();
            case R.id.action_sync:
                mPagerAdapter.javascriptFragment.mWebView.loadUrl("javascript:redraw();javascript:recolor()");
                onSync();
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onSync() {
        Log.d("ACT","sync");
    }

    @Override
    public void onPlay() {
        Log.d("ACT","play");
    }

    @Override
    public void onPlause() {
        Log.d("ACT","pause");
    }

    @Override
    public void onNext() {
        Log.d("ACT","next");
    }


    public static class ScreenSlidePagerAdapter extends FragmentStatePagerAdapter {
        public JavascriptFragment javascriptFragment;
        public Fragment queueFragment;

        public ScreenSlidePagerAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            javascriptFragment = JavascriptFragment.newInstance("file:///android_asset/hex.html");
            queueFragment = QueueFragment.newInstance();

        }

        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return javascriptFragment;
                case 1:
                    return queueFragment;
                default:
                    return javascriptFragment;
            }
        }

        @Override
        public int getCount() {
            return 2;
        }
    }

    public class SongAdapter extends ArrayAdapter<Song> {
        public SongAdapter(Context context, ArrayList<Song> users) {
            super(context, 0, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            Song song = getItem(position);
            if (convertView == null) {
                convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_row, parent, false);
            }
            TextView first = (TextView) convertView.findViewById(R.id.first_line);
            TextView second = (TextView) convertView.findViewById(R.id.second_line);
            ImageView image = (ImageView) convertView.findViewById(R.id.image);

            first.setText(song.title);
            second.setText(song.artist);
            image.setBackgroundColor(song.color);
            return convertView;
        }
    }
}
