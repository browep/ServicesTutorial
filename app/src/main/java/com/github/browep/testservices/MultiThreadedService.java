package com.github.browep.testservices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

import java.awt.font.TextAttribute;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MultiThreadedService extends Service {

    public static final String _URL = "url";
    private ExecutorService executorService;
    // counter for number of files we have downloaded
    private int i;

    @Override
    public void onCreate() {
        super.onCreate();
        // let's create a thread pool with five threads
        executorService = Executors.newFixedThreadPool(5);
        i = 0;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // lets say that we are sending the url in the intent
        String url = intent.getStringExtra(_URL);

        // create a runnable for the ExecutorService
        DownloadRunnable task = new DownloadRunnable(getApplicationContext(), url, i++);

        // submit it to the ExecutorService, this will be put on the queue and run using a thread
        // from the ExecutorService pool
        executorService.submit(task);

        // tells the OS to restart if we get killed after returning
        return START_STICKY;
    }

    /**
     * we don't care about binding right now, returning null is the way to do that
     *
     * @param intent
     * @return
     */
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    /**
     * dl's the image in question to <index>.jpg
     */
    public static class DownloadRunnable implements Runnable {

        private static final int BUFFER_SIZE = 4096;

        private Context context;
        private String url_str;
        private int index;

        public DownloadRunnable(Context context, String url, int index) {
            this.context = context;
            this.url_str = url;
            this.index = index;
        }

        @Override
        public void run() {
            URL url = null;
            HttpURLConnection httpConn = null;
            try {
                url = new URL(url_str);

                httpConn = (HttpURLConnection) url.openConnection();

                // opens input stream from the HTTP connection
                InputStream inputStream = httpConn.getInputStream();
                String saveFilePath = context.getFilesDir() + File.separator + index + ".jpg";

                // opens an output stream to save into file
                FileOutputStream outputStream = new FileOutputStream(saveFilePath);

                int bytesRead = -1;
                byte[] buffer = new byte[BUFFER_SIZE];
                while ((bytesRead = inputStream.read(buffer)) != -1) {
                    outputStream.write(buffer, 0, bytesRead);
                }

                outputStream.close();
                inputStream.close();

                Log.d("MultiThreadedService", "File downloaded to " + saveFilePath);

            } catch (Exception e) {
                Log.e("MultiThreadedService", e.getMessage(), e);
            } finally {
                if (httpConn != null) {
                    httpConn.disconnect();
                }

            }

        }
    }
}
