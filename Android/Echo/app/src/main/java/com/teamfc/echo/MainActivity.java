package com.teamfc.echo;

import android.app.Activity;
import android.content.Context;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Environment;
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
    public static long mOffset = 0l;
    public static MediaPlayer mMediaPlayer;
    private static boolean mPrepared = false;

    private Button mButton1;
    private Button mButton2;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mButton1 = (Button) findViewById(R.id.button1);
        mButton2 = (Button) findViewById(R.id.button2);
        mMediaPlayer = new MediaPlayer();

        try {
//            mSocket = new SocketIO("http://10.0.0.84:3000/");
//            mSocket = new SocketIO("http://192.168.2.6:3000/");
            mSocket = new SocketIO("http://10.42.0.1:3000/");
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
                    if(event.equals("sync")) {
                        long t1 = SystemClock.uptimeMillis();
                        JSONObject body = (JSONObject) args[0];
                        long t0 = 0l;
                        if(body != null) {
                            try {
                                t0 = body.getLong("t0");
                            } catch (JSONException e) {
                                e.printStackTrace();
                            }
                        }
                        JSONObject response = new JSONObject();
                        try {
                            response.put("t0", t0);
                            response.put("t1", t1);
                            long t2 = SystemClock.uptimeMillis();
                            response.put("t2", t2);
                            MainActivity.mSocket.emit("client_sync_callback", response);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                    if(event.equals("offset")) {
                        JSONObject body = (JSONObject) args[0];
                        if (body != null) {
                            try {
                                MainActivity.mOffset = body.getLong("offset");
                                Log.d("SocketIO", "Offset = " + MainActivity.mOffset);
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

        mButton1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mSocket.emit("client_sync");
                long correctedTime = SystemClock.uptimeMillis() - mOffset;
                Toast.makeText(getApplicationContext(), "Time = " + correctedTime + " Offset = " + mOffset, Toast.LENGTH_SHORT).show();
            }
        });

        mButton2.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                play();
            }
        });

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
                mMediaPlayer.setDataSource("http://mp3dos.com/assets/songs/18000-18999/18615-niggas-in-paris-jay-z-kanye-west--1411570006.mp3");
//                mMediaPlayer.setDataSource(Environment.getExternalStorageDirectory().toString() + "/Stuff/testsong.mp3");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        mMediaPlayer.prepareAsync();
    }

    public void play() {
        if(mPrepared) {
            long time = SystemClock.uptimeMillis() - mOffset;
            long futureTime = (time + 5000l) / 10000l * 10000l;
            if (futureTime - time < 5000) {
                futureTime += 10000;
            }
            Handler handler = new Handler();
            handler.postAtTime(new Runnable() {
                @Override
                public void run() {
                    MainActivity.mMediaPlayer.start();
                }
            }, futureTime + mOffset);
        } else {
            Toast.makeText(getApplicationContext(), "Not ready yet!", Toast.LENGTH_SHORT).show();
        }
    }


//    @Override
//    public boolean onCreateOptionsMenu(Menu menu) {
//        // Inflate the menu; this adds items to the action bar if it is present.
//        getMenuInflater().inflate(R.menu.main, menu);
//        return true;
//    }
//
//    @Override
//    public boolean onOptionsItemSelected(MenuItem item) {
//        // Handle action bar item clicks here. The action bar will
//        // automatically handle clicks on the Home/Up mButton1, so long
//        // as you specify a parent activity in AndroidManifest.xml.
//        int id = item.getItemId();
//        if (id == R.id.action_settings) {
//            return true;
//        }
//        return super.onOptionsItemSelected(item);
//    }

    @Override
    protected void onDestroy() {
        if (MainActivity.mMediaPlayer != null) {
            MainActivity.mMediaPlayer.release();
            MainActivity.mMediaPlayer = null;
        }
        super.onDestroy();
    }
}
