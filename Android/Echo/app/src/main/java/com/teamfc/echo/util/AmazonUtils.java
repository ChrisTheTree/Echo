package com.teamfc.echo.util;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.mobileconnectors.s3.transfermanager.Download;
import com.amazonaws.mobileconnectors.s3.transfermanager.TransferManager;
import com.amazonaws.mobileconnectors.s3.transfermanager.Upload;

import java.io.File;
import java.lang.ref.WeakReference;

/**
 * Created by jmok on 10/4/14.
 */
public class AmazonUtils {

    final static private String APP_KEY = "AKIAJU2EMLYPUKK3SMIQ";
    final static private String APP_SECRET = "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";
    final static private String BUCKET_NAME = "alstroe";

    static private TransferManager transferManager;
    private static AmazonUtils mInstance = null;

    private AmazonUtils(){
        AWSCredentials awsCredentials = new BasicAWSCredentials(APP_KEY, APP_SECRET);
        transferManager = new TransferManager(awsCredentials);
    }

    public static AmazonUtils getInstance(){
        if(mInstance == null)
        {
            mInstance = new AmazonUtils();
        }
        return mInstance;
    }

    public TransferManager getTransferManager(){
        return transferManager;
    }

    public static class uploadS3 extends AsyncTask<String, Integer, Void> {

        private WeakReference<Activity> weakReference;

        public uploadS3 (Activity context){
            this.weakReference = new WeakReference<Activity>(context);
        }

        protected Void doInBackground(String... params) {
            String path = params[0];
            String hash = params[1];

            File file= new File(path);
            Upload upload = AmazonUtils.getInstance().getTransferManager().upload("alstroe", hash+".mp3", file);

            while (!upload.isDone()) {
                Log.d("Progress", upload.getProgress().getPercentTransferred()+"");
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            Log.d("File", hash+".mp3");

            //TODO: EXTIRPATE; I have no idea what im doing. Don't judge me.
            final Activity activity = weakReference.get();
            if (activity != null) {
                // do your stuff with activity here
                final Context context = activity;
                final CharSequence text = "E C H O E D";
                final int duration = Toast.LENGTH_SHORT;

                activity.runOnUiThread(new Runnable() {
                    public void run() {
                        Toast toast = Toast.makeText(context, text, duration);
                        toast.show();
                    }
                });
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Long result) {
        }
    }

    public static class downloadS3 extends AsyncTask<String, Integer, Void> {
        protected Void doInBackground(String... params) {
            String key = params[0];
            File file = new File("/storage/emulated/0/Echo/"+key);

            Download download = AmazonUtils.getInstance().getTransferManager().download(BUCKET_NAME, key, file);

            while (!download.isDone()) {
                onProgressUpdate((int) download.getProgress().getPercentTransferred());
                try {
                    Thread.sleep(200);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            return null;
        }

        protected void onProgressUpdate(Integer... progress) {
        }

        protected void onPostExecute(Long result) {
        }
    }

}


