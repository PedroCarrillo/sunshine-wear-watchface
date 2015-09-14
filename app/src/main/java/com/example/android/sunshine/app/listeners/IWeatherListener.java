package com.example.android.sunshine.app.listeners;

import com.google.android.gms.wearable.DataMap;

/**
 * Created by pcarrillo on 14/09/2015.
 */
public interface IWeatherListener {

    void getWeatherData(DataMap weatherDataMap);

}
