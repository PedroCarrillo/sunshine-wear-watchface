package com.example.android.sunshine.app.sync;

import android.util.Log;

import com.example.android.sunshine.app.SunshineApp;
import com.example.android.sunshine.app.listeners.IWeatherListener;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;

/**
 * Created by pcarrillo on 14/09/2015.
 */
public class WeatherWearableListener extends WearableListenerService implements IWeatherListener {

    public static final String TAG = WeatherWearableListener.class.getSimpleName();
    public static final String PATH_WEATHER_UPDATE = "/WeatherWatchFace/WeatherUpdate";

    private static GoogleApiClient mGoogleApiClient;

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        super.onDataChanged(dataEvents);
        Log.e("Data get", dataEvents.getStatus().toString());
    }

    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        super.onMessageReceived(messageEvent);
        if (messageEvent.getPath().equalsIgnoreCase(PATH_WEATHER_UPDATE)) {
            if(mGoogleApiClient == null) mGoogleApiClient = new GoogleApiClient.Builder( this ).addApi( Wearable.API ).build();
            TodayWeatherAsyncTask todayWeatherAsyncTask = new TodayWeatherAsyncTask(this);
            todayWeatherAsyncTask.execute();
        }
    }

    public static void updateConnectedWearDevices() {
        if(mGoogleApiClient == null)  mGoogleApiClient = new GoogleApiClient.Builder(SunshineApp.getContext()).addApi( Wearable.API ).build();
        TodayWeatherAsyncTask todayWeatherAsyncTask = new TodayWeatherAsyncTask(new IWeatherListener() {
            @Override
            public void getWeatherData(DataMap weatherDataMap) {
                sendResponseToDevices(weatherDataMap);
            }
        });
        todayWeatherAsyncTask.execute();
    }

    @Override
    public void getWeatherData(final DataMap weatherDataMap) {
        sendResponseToDevices(weatherDataMap);
    }

    private static void sendResponseToDevices(final DataMap weatherDataMap) {
        if (!mGoogleApiClient.isConnected()) mGoogleApiClient.connect();
        Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult getConnectedNodesResult) {
                for(Node node: getConnectedNodesResult.getNodes()){
                    Wearable.MessageApi.sendMessage(mGoogleApiClient, node.getId(), PATH_WEATHER_UPDATE, weatherDataMap.toByteArray())
                            .setResultCallback(
                                    new ResultCallback<MessageApi.SendMessageResult>() {
                                        @Override
                                        public void onResult(MessageApi.SendMessageResult sendMessageResult) {
                                            Log.d(TAG, "SendUpdateMessage: " + sendMessageResult.getStatus());
                                        }
                                    }
                            );
                }
            }
        });
    }
}
