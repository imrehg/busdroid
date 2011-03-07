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
    private final DateFormat timestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSSZ");
    private final DateFormat timestampFormatShort = new SimpleDateFormat("HH:mm");

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
    private int bus_id;
    private long last_update;

    private final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    private final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";

    private String LocationServerURI;
    // private final String LocationServerURI = "http://startupbus.com:3000/api/locations";
    // // Local debug with netcat
    // private final String LocationServerURI = "http://192.168.2.14:3000";
    // private final String LocationServerURI = "http://192.168.11.20:3000/api/locations";

    private com.cleardb.app.Client cleardbClient;

    private Timer testTimer;

    private String ns;
    private NotificationManager mNM;


   // private static final String INSERT = "insert into " 
   //    + REMOTE_TABLE_NAME + "(bus_id, timestamp, longitude, latitude) values ('?', '?', '?', '?')";

    public static void postToServer(String uri, JSONObject payload) throws Exception {
	int TIMEOUT_MILLISEC = 10000;  // = 10 seconds
	HttpParams httpParams = new BasicHttpParams();
	HttpConnectionParams.setConnectionTimeout(httpParams, TIMEOUT_MILLISEC);
	HttpConnectionParams.setSoTimeout(httpParams, TIMEOUT_MILLISEC);
	HttpClient client = new DefaultHttpClient(httpParams);

	HttpPost request = new HttpPost(uri);
	request.setEntity(new ByteArrayEntity(payload.toString().getBytes("UTF8")));
	request.setHeader("Accept", "application/json");
	request.setHeader("Content-type", "application/json");
	HttpResponse response = client.execute(request);
	Log.i(tag, response.getStatusLine().toString());    }

    public void sendUpdate(int bus_id, long timestamp, float lon, float lat) {
	// Send update to server: both new and outstanding

	// Pull in outstanding updates
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

	if (timestamp > 0) {
	    JSONObject update = new JSONObject();
	    try {
		String sampletime = timestampFormat.format(timestamp*1000L);
		update.put("bus_id", bus_id);
		update.put("sampled_at", sampletime);
		update.put("longitude", lon);
		update.put("latitude", lat);
	    } catch(JSONException e) {
		Log.i(tag, "JSON error in creating update, nothing sent");
		return;
	    }
	    multi_update.put(update);
	}

	// Is there any new update to actually post?
	if (multi_update.length() > 0) {
	    JSONObject payload = new JSONObject();
	    try {
		payload.put("locations", multi_update);
	    } catch(JSONException e) {
		Log.i(tag, "JSON error in creating payload, nothing sent");
		return;
	    }
	    Log.i(tag, "Payload to send: " + payload.toString() + " => " + LocationServerURI);
	    try {
		postToServer(LocationServerURI, payload);
		Log.i(tag, "Update run, JSON format");
		prefedit.putString("outstanding_updates", "");
		prefedit.commit();
		showNotification("Location sent",
				 "Bus location sent okay",
				 true);
	    } catch(Exception e) {
		Log.i(tag, "Sending update failed");
		prefedit.putString("outstanding_updates", multi_update.toString());
		prefedit.commit();
		long oldtime = settings.getLong("last_update", timestamp);
		showNotification("Failed location update",
				 "No location update since "+timestampFormatShort.format(oldtime*1000L),
				 false);
	    }
	}
    }

    public void showNotification(CharSequence tickerText, CharSequence contentText, boolean good) {
	int icon;
	if (good) {
	    icon = R.drawable.nf_icon_well;
	} else {
	    icon = R.drawable.nf_icon_notwell;
	}

	long when = System.currentTimeMillis();         // notification time
	Context context = getApplicationContext();      // application Context
	CharSequence contentTitle = "BusDroid";  // expanded message title

	Intent notificationIntent = new Intent(this, NetUpdateService.class);
	PendingIntent contentIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
	Notification notification = new Notification(icon, tickerText, when);
	notification.setLatestEventInfo(context, contentTitle, contentText, contentIntent);
	notification.flags |= Notification.FLAG_ONGOING_EVENT;

	mNM.notify(1, notification);
	Log.i(tag, "Notification set: "+contentText);
    }

    ////////// Timer
    class testTask extends TimerTask {
	public void run() {
	    last_update = settings.getLong("last_update", 0);
	    // // Force to update
	    // last_update = 0;
	    Log.i(tag, String.format("Got last update: %d", last_update));
	    SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READONLY, null);
	    String query = String.format("SELECT * from %s WHERE TIMESTAMP > %d ORDER BY timestamp DESC LIMIT 1;",
					 POINTS_TABLE_NAME,
					 last_update);
	    Cursor cur = db.rawQuery(query, new String [] {});
	    long timestamp = 0;
	    float lon = 0;
	    float lat = 0;

	    try {
		cur.moveToFirst();
		lon = cur.getFloat(cur.getColumnIndex("LONGITUDE"));
		lat = cur.getFloat(cur.getColumnIndex("LATITUDE"));
		timestamp = cur.getLong(cur.getColumnIndex("TIMESTAMP"));
		Log.i(tag, String.format("%s: %f lon, %f lat at %d (latest since  %d)", bus_id, lon, lat, timestamp, last_update));
		prefedit.putLong("last_update", (long)timestamp);
		prefedit.commit();
	    } catch (Exception e) {
		Log.i(tag, String.format("No new location for %s (since %d / %s)", bus_id, last_update, timestampFormat.format(last_update*1000L)));
	    }
	    sendUpdate(bus_id, timestamp, lon, lat);

        }
    }

    private final IBinder mBinder = new LocalBinder();
    @Override
    public void onCreate() {
    	super.onCreate();

	settings = getSharedPreferences(PREFS_NAME, 0);
	prefedit = settings.edit();
	bus_id = settings.getInt("bus_id", 1);
	LocationServerURI = settings.getString("remote_server", "http://startupbus.com/api/locations");

	int refresh_interval = settings.getInt("refresh_interval", 1);

	testTimer = new Timer();
	testTimer.scheduleAtFixedRate(new testTask(), 10L, refresh_interval*60*1000L);

	ns = Context.NOTIFICATION_SERVICE;
	mNM = (NotificationManager) getSystemService(ns);
	showNotification("Finding location",
			 "Locking on to GPS signal",
			 false);
    }

    @Override
    public void onDestroy() {
    	super.onDestroy();
	// sendUpdate();
	testTimer.cancel();
	mNM.cancelAll();
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
