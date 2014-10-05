package com.teamfc.echo;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
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
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.teamfc.echo.util.AmazonUtils;
import com.teamfc.echo.util.ColorUtils;
import com.teamfc.echo.util.FileUtils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.ArrayList;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;


public class EchoActivity extends FragmentActivity implements QueueFragment.OnFragmentInteractionListener {
    private ViewPager mPager;
    private ScreenSlidePagerAdapter mPagerAdapter;

    public static SocketIO mSocket;

    //    public static final long NUMBER_OF_OFFSETS = 20l;
    public static final int NUMBER_OF_OFFSETS = 20;
    //    public static final long OFFSET_TOLERANCE = 10l;
    public static final long OFFSET_TOLERANCE = 10000l;
    //    public static final long SYNC_DELAY = 80l;
    public static final long SYNC_DELAY = 50l;

    public static boolean mSynchronized = false;
    public static long mOffset = 0l;
    public static int mOffsetCounter = 0;
    public static long[] mOffsets = new long[NUMBER_OF_OFFSETS];

    public static MediaPlayer mMediaPlayer1;
//    public static MediaPlayer mMediaPlayer2;
    private static boolean mPrepared1 = false;
//    private static boolean mPrepared2 = false;
    private static int mPlayCount = 0;
//    private static boolean mIsPlaying = false;

    public static Handler mHandler;


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

        //Mitcheltree things
        socketStuff();
    }

//--- Mitcheltree things ---------------------------------------------------------------------------
    public static long getOffset(long[] offsets) {
        int[] counters = new int[offsets.length];
        long[] results = new long[offsets.length];
        for(int i = 0; i < offsets.length; ++i) {
            int counter = 0;
            long sum = 0;
            for(long otherOffset : offsets) {
                if(Math.abs(offsets[i] - otherOffset) <= OFFSET_TOLERANCE) {
                    ++counter;
                    sum += otherOffset;
                }
            }
            counters[i] = counter;
            if(counter != 0) {
                results[i] = Math.round(((double) sum) / ((double) counter));
            }
        }
        int max = 0;
        int maxIndex = 0;
        for(int i = 0; i < counters.length; ++i) {
            if(counters[i] > max) {
                max = counters[i];
                maxIndex = i;
//                Log.d("SocketIO", "max " + max);
            }
        }
        if(max == 0) { // TODO: this is shit
            return 0;
        } else {
            return results[maxIndex];
        }
    }

    private void socketStuff() {
        mHandler = new Handler();

        mMediaPlayer1 = new MediaPlayer();
//        mMediaPlayer2 = new MediaPlayer();

        try {
            mSocket = new SocketIO("http://10.42.0.1:3030/");
        } catch (MalformedURLException e) {
            e.printStackTrace();
        }
        if (mSocket != null) {
            mSocket.connect(new IOCallback() {
                @Override
                public void onMessage(JSONObject json, IOAcknowledge ack) {
                    try {
                        Log.d("SocketIO", "Server said: " + json.toString(2));
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }

                @Override
                public void onMessage(String data, IOAcknowledge ack) {
                    Log.d("SocketIO", "Server said: " + data);
                }

                @Override
                public void onError(SocketIOException socketIOException) {
                    Log.d("SocketIO", "Error");
                    socketIOException.printStackTrace();
                }

                @Override
                public void onDisconnect() {
                    Log.d("SocketIO", "Connection terminated.");
                }

                @Override
                public void onConnect() {
                    Log.d("SocketIO", "Connection created.");
                }

                @Override
                public void on(String event, IOAcknowledge ack, Object... args) {
                    long t1 = SystemClock.uptimeMillis();
                    if(event.equals("sync")) {
                        JSONObject body = (JSONObject) args[0];
                        try {
                            body.put("t1", t1);
                            long t2 = SystemClock.uptimeMillis();
                            body.put("t2", t2);
                            EchoActivity.mSocket.emit("client_sync_callback", body);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if(event.equals("offset")) {
                        JSONObject body = (JSONObject) args[0];
                        if (body != null) {
                            try {
//                                MainActivity.mOffset = body.getLong("offset");
                                processOffsets(body.getLong("offset"));
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(event.equals("play")) {
//                        Log.d("SocketIO", "play");
                        JSONObject body = (JSONObject) args[0];
                        if (body != null) {
                            try {
//                                MainActivity.mOffset = body.getLong("offset");
                                long startAt = body.getLong("startAt");
                                EchoActivity.play(startAt);
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                    }
                    if(event.equals("stop")) {
//                        Log.d("SocketIO", "stop");
                        EchoActivity.pause();
                    }
                }
            });
        }

        if(mMediaPlayer1 != null) {
            mMediaPlayer1.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mPrepared1 = true;
                    Toast.makeText(getApplicationContext(), "Ready 1!", Toast.LENGTH_SHORT).show();
                }
            });
        }
//        if(mMediaPlayer2 != null) {
//            mMediaPlayer2.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
//                public void onPrepared(MediaPlayer mp) {
//                    mPrepared2 = true;
//                    Toast.makeText(getApplicationContext(), "Ready 2!", Toast.LENGTH_SHORT).show();
//                }
//            });
//        }

        try {
            if(mMediaPlayer1 != null) {
//                mMediaPlayer1.setDataSource(Environment.getExternalStorageDirectory().toString() + "/Stuff/testsong.mp3");
//                mMediaPlayer1.setDataSource("http://mp3dos.com/assets/songs/18000-18999/18615-niggas-in-paris-jay-z-kanye-west--1411570006.mp3");
                mMediaPlayer1.setDataSource("https://s3.amazonaws.com/alstroe/1468664291.mp3");
            }
//            if(mMediaPlayer2 != null) {
//                mMediaPlayer2.setDataSource("https://s3.amazonaws.com/alstroe/850920812.mp3"); // TODO: change!
//            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if(mMediaPlayer1 != null) {
            mMediaPlayer1.prepareAsync();
        }
//        if(mMediaPlayer2 != null) {
//            mMediaPlayer2.prepareAsync();
//        }

    }

    public void processOffsets(long offset) {
        if(mOffsetCounter < NUMBER_OF_OFFSETS) {
            mOffsets[mOffsetCounter] = offset;
            ++mOffsetCounter;
            if(mOffsetCounter < NUMBER_OF_OFFSETS) { // TODO: this is shit
                try {
                    Thread.sleep(SYNC_DELAY);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                EchoActivity.mSocket.emit("client_sync");
            }
        }
        if(mOffsetCounter >= NUMBER_OF_OFFSETS) {
            mOffset = getOffset(mOffsets);
            mSynchronized = true;
//            Toast.makeText(getApplicationContext(), "Synchronized!", Toast.LENGTH_SHORT).show();
            Log.d("SocketIO", "synced" + offset);
        }
    }

    public static void play(long serverStartTime) {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                if(EchoActivity.mPrepared1 && EchoActivity.mPlayCount == 0) {
                    if(EchoActivity.mMediaPlayer1 != null) {
                        EchoActivity.mMediaPlayer1.start();
                    }
                }
//                else if(EchoActivity.mPrepared2 && EchoActivity.mPlayCount == 1) {
//                    if(EchoActivity.mMediaPlayer2 != null) {
//                        EchoActivity.mMediaPlayer2.start();
//                    }
//                }
            }
        };

        if(mPrepared1 && /*mPrepared2 &&*/ mSynchronized) {
//            long time = SystemClock.uptimeMillis() - mOffset;
//            long futureTime = (time + 5000l) / 10000l * 10000l;
//            if (futureTime - time < 5000) {
//                futureTime += 10000;
//            }
//            long delay = (futureTime + mOffset) - SystemClock.uptimeMillis();
//            handler.postDelayed(runnable, delay);
//            handler.postAtTime(runnable, futureTime + mOffset);
//            handler.postAtTime(runnable, serverStartTime + mOffset);
            if(mHandler != null) {
                mHandler.postAtTime(runnable, serverStartTime + mOffset);
            }
//            long temp = futureTime + mOffset;
//            Log.d("SocketIO", "Futuretime client: " + temp);
        }
//        else {
//            Toast.makeText(getApplicationContext(), "Not ready yet!", Toast.LENGTH_SHORT).show();
//        }
    }

    public static void pause() {
        if(mMediaPlayer1 != null && mMediaPlayer1.isPlaying()) {
            mMediaPlayer1.pause();
            mMediaPlayer1.seekTo(0);
        }
//        if(mMediaPlayer2 != null && mMediaPlayer2.isPlaying()) {
//            mMediaPlayer2.pause();
//        }
    }

    @Override
    protected void onDestroy() {
        if (EchoActivity.mMediaPlayer1 != null) {
            EchoActivity.mMediaPlayer1.release();
            EchoActivity.mMediaPlayer1 = null;
        }
//        if (EchoActivity.mMediaPlayer2 != null) {
//            EchoActivity.mMediaPlayer2.release();
//            EchoActivity.mMediaPlayer2 = null;
//        }
        super.onDestroy();
    }


//--- Mok things ---------------------------------------------------------------------------
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

        songs.add(new Song("The Humpty Dance", "Digital Underground",colorUtils.getColor()));
        songs.add(new Song("Gangsta's Paradise", "Coolio feat. L.V.",colorUtils.getColor()));
        songs.add(new Song("Flava In Ya Ear", "Craig Mack",colorUtils.getColor()));
        songs.add(new Song("Thrift Shop", "Macklemore & Ryan Lewis feat. Wanz",colorUtils.getColor()));
        songs.add(new Song("Can't Hold Us", "Macklemore & Ryan Lewis feat. Wanz",colorUtils.getColor()));
        songs.add(new Song("Tootsee Roll", "69 Boyz",colorUtils.getColor()));
        songs.add(new Song("No Hands","Waka Flocka Flame feat. Roscoe Dash & Wale",colorUtils.getColor()));
        songs.add(new Song("Big Poppa/Warning","The Notorious B.I.G.",colorUtils.getColor()));
        songs.add(new Song("Expression","Salt-N-Pepa",colorUtils.getColor()));


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
                break;
            case R.id.action_sync:
                mPagerAdapter.javascriptFragment.mWebView.loadUrl("javascript:redraw();javascript:recolor()");
                onSync();
                break;
            default:
                break;
        }

        return super.onOptionsItemSelected(item);
    }

    private void onSync() {
        Log.d("ACT","sync");
        mSynchronized = false;
        mOffset = 0l;
        mOffsetCounter = 0;
        mSocket.emit("client_sync");
        long correctedTime = SystemClock.uptimeMillis() - mOffset;
        Toast.makeText(getApplicationContext(), "Time = " + correctedTime, Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onPlay() {
        Log.d("ACT","play");
        mSocket.emit("client_play");
    }

    @Override
    public void onPlause() {
        Log.d("ACT","pause");
        mSocket.emit("client_stop");
        pause();
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
