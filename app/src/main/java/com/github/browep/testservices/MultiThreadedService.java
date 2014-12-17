package com.github.browep.testservices;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class MultiThreadedService extends Service {

    public static final String _URL = "url";
    private ExecutorService executorService;
    // counter for number of files we have downloaded
    private int downloadsStarted;
    private int downloadsFinished;
    private Callable<Object> shutdownTask;
    private Future shutdownFuture;
    private Handler handler;

    @Override
    public void onCreate() {
        super.onCreate();
        // let's create a thread pool with five threads
        executorService = Executors.newFixedThreadPool(5);
        downloadsStarted = 0;
        downloadsFinished = 0;
        handler = new Handler();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        // we got new work.  if we were previously going to shutdown then cancel that
        if (shutdownFuture != null && !shutdownFuture.isDone()) {
            shutdownFuture.cancel(true);
        }

        // lets say that we are sending the url in the intent
        String url = intent.getStringExtra(_URL);

        // create a runnable for the ExecutorService
        DownloadRunnable task = new DownloadRunnable(getApplicationContext(), url, downloadsStarted++);

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
     * to be called by the dl task.  if we have exhausted our list of dl's let's shutdown.
     */
    private void finished() {
        if (downloadsFinished == downloadsStarted) {
            handler.post(new Runnable() {
                @Override
                public void run() {
                    Log.d("MultiThreadedService", "downloaded " + downloadsFinished + " images, shutting down.");
                    stopSelf();
                }
            });

        }
    }

    /**
     * dl's the image in question to <index>.jpg
     */
    private class DownloadRunnable implements Runnable {

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

            downloadsFinished++;
            finished();

        }
    }
}
