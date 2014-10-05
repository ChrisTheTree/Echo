package com.teamfc.echo;

import android.app.Activity;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.net.MalformedURLException;

import io.socket.IOAcknowledge;
import io.socket.IOCallback;
import io.socket.SocketIO;
import io.socket.SocketIOException;


public class MainActivity extends Activity {

    public static SocketIO mSocket;

//    public static final long NUMBER_OF_OFFSETS = 20l;
    public static final int NUMBER_OF_OFFSETS = 100;
//    public static final long OFFSET_TOLERANCE = 10l;
    public static final long OFFSET_TOLERANCE = 10000l;
//    public static final long SYNC_DELAY = 80l;
    public static final long SYNC_DELAY = 50l;

    public static boolean mSynchronized = false;
    public static long mOffset = 0l;
    public static int mOffsetCounter = 0;
    public static long[] mOffsets = new long[NUMBER_OF_OFFSETS];

    public static MediaPlayer mMediaPlayer;
    private static boolean mPrepared = false;

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
                Log.d("SocketIO", "max " + max);
            }
        }
        if(max == 0) { // TODO: this is shit
            return 0;
        } else {
            return results[maxIndex];
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMediaPlayer = new MediaPlayer();
        Button button1 = (Button) findViewById(R.id.button1);
        Button button2 = (Button) findViewById(R.id.button2);

        try {
//            mSocket = new SocketIO("http://10.0.0.84:3000/");
//            mSocket = new SocketIO("http://192.168.2.6:3000/");
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
                            MainActivity.mSocket.emit("client_sync_callback", body);
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
                        // TODO!
                    }
                }
            });
        }

        if(mMediaPlayer != null) {
            mMediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                public void onPrepared(MediaPlayer mp) {
                    mPrepared = true;
                    Toast.makeText(getApplicationContext(), "Ready!", Toast.LENGTH_SHORT).show();
                }
            });
        }

        try {
            if(mMediaPlayer != null) {
//                mMediaPlayer.setDataSource(Environment.getExternalStorageDirectory().toString() + "/Stuff/testsong.mp3");
//                mMediaPlayer.setDataSource("http://mp3dos.com/assets/songs/18000-18999/18615-niggas-in-paris-jay-z-kanye-west--1411570006.mp3");
                mMediaPlayer.setDataSource("https://s3.amazonaws.com/alstroe/850920812.mp3");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();

        button1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSynchronized = false;
                mOffset = 0l;
                mOffsetCounter = 0;
                mSocket.emit("client_sync");
                long correctedTime = SystemClock.uptimeMillis() - mOffset;
                Toast.makeText(getApplicationContext(), "Time = " + correctedTime + " Offset = " + mOffset, Toast.LENGTH_SHORT).show();
            }
        });

        button2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });
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
                MainActivity.mSocket.emit("client_sync");
            }
        }
        if(mOffsetCounter >= NUMBER_OF_OFFSETS) {
            mOffset = getOffset(mOffsets);
            mSynchronized = true;
            Log.d("SocketIO", "merpy" + offset);
        }
    }

    public void play() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                MainActivity.mMediaPlayer.start();
            }
        };
        Handler handler = new Handler();

        if(mPrepared && mSynchronized) {
            long time = SystemClock.uptimeMillis() - mOffset;
            long futureTime = (time + 5000l) / 10000l * 10000l;
            if (futureTime - time < 5000) {
                futureTime += 10000;
            }
//            long delay = (futureTime + mOffset) - SystemClock.uptimeMillis();
//            handler.postDelayed(runnable, delay);
            handler.postAtTime(runnable, futureTime + mOffset);
//            long temp = futureTime + mOffset;
//            Log.d("SocketIO", "Futuretime client: " + temp);
        } else {
            Toast.makeText(getApplicationContext(), "Not ready yet!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onDestroy() {
        if (MainActivity.mMediaPlayer != null) {
            MainActivity.mMediaPlayer.release();
            MainActivity.mMediaPlayer = null;
        }
        super.onDestroy();
    }
}
