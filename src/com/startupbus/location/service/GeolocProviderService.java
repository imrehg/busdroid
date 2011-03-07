package com.startupbus.location.service;

import java.text.DateFormat;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.TimeZone;
import java.util.Timer;
import java.util.TimerTask;


import com.startupbus.location.R;

import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import android.content.SharedPreferences;

import com.cleardb.app.ClearDBQueryException;
import com.cleardb.app.ClearDBInTransactionException;
import com.cleardb.app.Client;
import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.entity.ByteArrayEntity;

import com.startupbus.location.webserver.WebServer;

public class GeolocProviderService extends Service {

    private WebServer server = null;
    
    @Override
    public void onCreate() {
	super.onCreate();
		
	server = new WebServer(this);
    }

    @Override
    public void onDestroy() {
	server.stopThread();
	super.onDestroy();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
	server.startThread();
	return START_STICKY;
    }

    @Override
    public boolean onUnbind(Intent intent) {
	return super.onUnbind(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
	return null;
    }
}
