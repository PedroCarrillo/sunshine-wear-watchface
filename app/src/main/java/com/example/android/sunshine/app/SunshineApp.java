package com.example.android.sunshine.app;

import android.app.Application;
import android.content.Context;
/**
 * Created by pcarrillo on 14/09/2015.
 */
public class SunshineApp extends Application{

    private static Context mContext;

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }

    public static Context getContext(){
        return mContext;
    }
}
