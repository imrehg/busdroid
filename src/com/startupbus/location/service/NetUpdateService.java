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

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.database.sqlite.SQLiteDatabase;
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
import org.json.JSONArray;

// import com.startupbus.location.service.GPSLoggerService;

public class NetUpdateService extends Service {

    public static final String DATABASE_NAME = "GPSLOGGERDB";
    public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";

    public static final String REMOTE_TABLE_NAME = "startupbus";

    private final DecimalFormat sevenSigDigits = new DecimalFormat("0.#######");
    private final DateFormat timestampFormat = new SimpleDateFormat("yyyyMMddHHmmss");

    private LocationManager lm;
    private LocationListener locationListener;
    private SQLiteDatabase db;

    private static long minTimeMillis = 2000;
    private static long minDistanceMeters = 10;
    private static float minAccuracyMeters = 35;

    private int lastStatus = 0;
    private static boolean showingDebugToast = false;

    private static final String tag = "NetUpdateService";

    public static final String PREFS_NAME = "BusdroidPrefs";

    private final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    private final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";

    private com.cleardb.app.Client cleardbClient;

    private Timer testTimer;

   // private static final String INSERT = "insert into " 
   //    + REMOTE_TABLE_NAME + "(bus_id, timestamp, longitude, latitude) values ('?', '?', '?', '?')";


    public void sendUpdate() {
	try {
	    cleardbClient.startTransaction();
	} catch (ClearDBInTransactionException e) {
	    return;
	}
	double now = (double) (System.currentTimeMillis() / 1000.0);
	String query = String.format("INSERT INTO startupbus (bus_id, timestamp, longitude, latitude) VALUES ('%s', '%f', '%f', '%f')",
			      "Taipei",
			      now,
			      25.033333,
			      121.633333
			      );
	try {
	    cleardbClient.query(query);
	} catch (ClearDBQueryException e) {
	    Toast.makeText(getBaseContext(), "Query fail, cdb", Toast.LENGTH_SHORT).show();
	} catch (Exception e) {
	    Toast.makeText(getBaseContext(), "Query fail, other", Toast.LENGTH_SHORT).show();
	}

	try {
	    JSONObject payload = cleardbClient.sendTransaction();
	} catch (ClearDBQueryException clearE) {
	    System.out.println("ClearDB Exception: " + clearE.getMessage());
	} catch (Exception e) {
	    System.out.println("General Exception: " + e.getMessage());
	}
    	Toast.makeText(getBaseContext(), "NetUpdate run:"+query, Toast.LENGTH_SHORT).show();
    }

    ////////// Timer
    class testTask extends TimerTask {
	public void run() {
	    Log.i(tag, "repeat");

        }
    }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public void onCreate() {
    	super.onCreate();
    	Toast.makeText(getBaseContext(), "Start NetUpdate", Toast.LENGTH_SHORT).show();
	// cleardbClient = new com.cleardb.app.Client(API_KEY, APP_ID);
	// testTimer = new Timer();
	// testTimer.scheduleAtFixedRate(new testTask(), 10L, 5*1000L);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
	// sendUpdate();
	testTimer.cancel();
    }

    @Override
    	public IBinder onBind(Intent intent) {
    	return mBinder;
    }

    public void showToast(String msg) {
	Toast.makeText(getBaseContext(), "SGmessage" + msg,
		       Toast.LENGTH_SHORT).show();
    }

    /**
     * Class for clients to access. Because we know this service always runs in
     * the same process as its clients, we don't need to deal with IPC.
     */
    public class LocalBinder extends Binder {
    	NetUpdateService getService() {
    	    return NetUpdateService.this;
    	}
    }

}
