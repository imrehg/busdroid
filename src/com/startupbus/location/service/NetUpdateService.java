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

    private static final String tag = "BusDroid:NetUpdateService";

    public static final String PREFS_NAME = "BusdroidPrefs";
    private SharedPreferences settings;
    private SharedPreferences.Editor prefedit;
    private String bus_id;
    private long last_update;

    private final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    private final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";

    private final String LocationServerURI = "http://startupbus.com:3000/api/locations";
    // // Local debug with netcat
    // private final String LocationServerURI = "http://192.168.2.14:3000";

    private com.cleardb.app.Client cleardbClient;

    private Timer testTimer;

   // private static final String INSERT = "insert into " 
   //    + REMOTE_TABLE_NAME + "(bus_id, timestamp, longitude, latitude) values ('?', '?', '?', '?')";

    public static void postToServer(String uri, JSONArray multi_update) throws Exception {
	int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
	HttpParams httpParams = new BasicHttpParams();
	HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
	HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
	HttpClient client = new DefaultHttpClient(httpParams);

	HttpPost request = new HttpPost(uri);
	request.setEntity(new ByteArrayEntity(multi_update.toString().getBytes("UTF8")));
	HttpResponse response = client.execute(request);
	Log.i(tag, response.getStatusLine().toString());    }

    public void sendUpdate(int bus_id, long timestamp, double lon, double lat) {
	// try {
	//     cleardbClient = new com.cleardb.app.Client(API_KEY, APP_ID);
	//     cleardbClient.startTransaction();
	// } catch (ClearDBInTransactionException e) {
	//     return;
	// }
	// String query = String.format("INSERT INTO startupbus (bus_id, timestamp, longitude, latitude) VALUES ('%s', '%d', '%f', '%f')",
	// 		      bus_id,
	// 		      timestamp,
	// 		      lon,
	// 		      lat);
	// try {
	//     cleardbClient.query(query);
	// } catch (ClearDBQueryException e) {
	//     Log.i(tag, "Query fail, ClearDB");
	// } catch (Exception e) {
	//     Log.i(tag, "Query fail, other");
	// }

	// try {
	//     JSONObject payload = cleardbClient.sendTransaction();
	// } catch (ClearDBQueryException clearE) {
	//     System.out.println("ClearDB Exception: " + clearE.getMessage());
	// } catch (Exception e) {
	//     System.out.println("General Exception: " + e.getMessage());
	// }
	// Log.i(tag, "Update run");

	JSONArray multi_update;
	String outstanding_updates = settings.getString("outstanding_updates", "");
	if (outstanding_updates != "") {
	    try {
		multi_update = new JSONArray(outstanding_updates);
	    } catch(JSONException e) {
		Log.i(tag, "Getting outstanding updates didn't work out well");
		multi_update = new JSONArray();
	    }
	} else {
	    multi_update = new JSONArray();
	}

	JSONObject update = new JSONObject();
	try {
	    update.put("bus_id", bus_id);
	    update.put("sampled_at", timestamp);
	    update.put("longitude", lon);
	    update.put("latitude", lat);
	} catch(JSONException e) {
	    Log.i(tag, "JSON error in creating update, nothing sent");
	    return;
	}

	multi_update.put(update);
	Log.i(tag, "Update to send: " + multi_update.toString() + " => " + LocationServerURI);
	try {
	    postToServer(LocationServerURI, multi_update);
	    Log.i(tag, "Update run, JSON format");
	    prefedit.putString("outstanding_updates", "");
	    prefedit.commit();
	} catch(Exception e) {
	    Log.i(tag, "Sending update failed");
	    prefedit.putString("outstanding_updates", multi_update.toString());
	    prefedit.commit();
	}
    }

    ////////// Timer
    class testTask extends TimerTask {
	public void run() {
	    last_update = settings.getLong("last_update", 0);
	    Log.i(tag, String.format("Got last update: %d", last_update));
	    SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READONLY, null);
	    String query = String.format("SELECT * from %s WHERE TIMESTAMP > %d ORDER BY timestamp DESC LIMIT 1;",
					 POINTS_TABLE_NAME,
					 last_update);
	    Cursor cur = db.rawQuery(query, new String [] {});
	    try {
		cur.moveToFirst();
		Double lon = cur.getDouble(cur.getColumnIndex("LONGITUDE"));
		Double lat = cur.getDouble(cur.getColumnIndex("LATITUDE"));
		Long timestamp = cur.getLong(cur.getColumnIndex("TIMESTAMP"));
		Log.i(tag, String.format("%s: %f lon, %f lat at %d (latest since  %d)", bus_id, lon, lat, timestamp, last_update));
		// Only sends when there's a new update, even if there are outstanding ones...
		sendUpdate(2, timestamp, lon, lat);
		prefedit.putLong("last_update", (long)timestamp);
		prefedit.commit();
	    } catch (Exception e) {
		Log.i(tag, String.format("No new location for %s (since %d)", bus_id, last_update));
	    }
        }
    }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public void onCreate() {
    	super.onCreate();

	settings = getSharedPreferences(PREFS_NAME, 0);
	prefedit = settings.edit();
	bus_id = settings.getString("bus_id", "Test");

	testTimer = new Timer();
	testTimer.scheduleAtFixedRate(new testTask(), 10L, 30*1000L);
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
