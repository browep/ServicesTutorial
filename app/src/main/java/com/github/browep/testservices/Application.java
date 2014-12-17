package com.github.browep.testservices;

import android.util.Log;

/**
 * Created by paul on 12/17/14.
 */
public class Application extends android.app.Application {

    public static final String TAG = Application.class.getCanonicalName();


    @Override
    public void onCreate() {

        super.onCreate();
        Log.d(TAG, "onCreate");
    }

    @Override
    protected void finalize() throws Throwable {
        Log.d(TAG, "finalize");
        super.finalize();
    }
}
