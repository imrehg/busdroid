
package com.startupbus.location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.ToggleButton;
import android.widget.Toast;

import com.startupbus.location.service.GPSLoggerService;
import com.startupbus.location.service.NetUpdateService;

// import com.simplegeo.client.SimpleGeoPlacesClient;
// import com.simplegeo.client.SimpleGeoStorageClient;
// import com.simplegeo.client.callbacks.FeatureCollectionCallback;
// import com.simplegeo.client.types.Feature;
// import com.simplegeo.client.types.FeatureCollection;
// import com.simplegeo.client.types.Point;
// import com.simplegeo.client.types.Geometry;
// import com.simplegeo.client.types.Record;

// import com.simplegeo.client.handler.GeoJSONHandler;

import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;

import java.io.IOException;
import org.json.JSONException;

import java.util.*;

import android.widget.ArrayAdapter;
import android.widget.Spinner;

import android.content.SharedPreferences;
import android.text.TextWatcher;

import com.cleardb.app.ClearDBQueryException;
import com.cleardb.app.Client;
import org.json.JSONObject;
import org.json.JSONArray;

import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

import android.provider.Settings;

public class BusDroid extends Activity implements OnClickListener {
    public static final String PREFS_NAME = "BusdroidPrefs";
    private static final String tag = "BusDroid:Main";

    public static final String DATABASE_NAME = "GPSLOGGERDB";
    public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";

    private LocationManager myManager;
    Button buttonStart, buttonStop;
    TextView debugArea;
    EditText sglayeredit;
    String bus_id;

    final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";


    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	// Restore preferences
	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	String bus_id = settings.getString("bus_id", "Test");

	buttonStart = (Button) findViewById(R.id.buttonStart);
	buttonStop = (Button) findViewById(R.id.buttonStop);

	debugArea = (TextView) findViewById(R.id.debugArea);

	sglayeredit = (EditText) findViewById(R.id.sglayeredit);
	sglayeredit.setText(bus_id);

	buttonStart.setOnClickListener(this);
	buttonStop.setOnClickListener(this);

	// // LocationManager locator = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	// myManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
	// Location l = myManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	// if (l == null) {
	//     // Fall back to coarse location.
	//     l = myManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	// }
	// // Start with fine location.
	// // Location l = locator.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	// // if (l == null) {
	// //     // Fall back to coarse location.
	// //     l = locator.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	// // }
	// // // locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); Location location = locationManager.getCurrentLocation("gps");

	// SimpleGeoStorageClient client = SimpleGeoStorageClient.getInstance();
	// client.getHttpClient().setToken("CrQ8RDznnjEhwUCGn5Uv9G3h9kR4xcLK", "MtLzaKmMP8C2DfYBDWUemZ6pRLZQe2cT");

	// Point loc = new Point(l.getLatitude(), l.getLongitude());
	// // debugArea.setText(String.format("Last location:\n%.7f, %.7f\n(from %s)",
	// // 				loc.getLat(),
	// // 				loc.getLon(),
	// // 				l.getProvider()
	// // 				)
	// // 		  );
	// // // try {
	// // //     collection = client.search(37.7787, -122.3896, "",  "", 25.0);
	// // // } catch (IOException e) {
	// // //     debugArea.setText(e.getMessage());
	// // // }
	// // String text = "";
	// // // try {
	// // //     text = collection.toJSONString();
	// // // } catch(JSONException e) {
	// // //     debugArea.setText(e.getMessage());
	// // // }


	// String recordId = String.format("bus_%d", 1234);
	// String layer = "com.startupbus.test";
	// String rectype= "Location";
	// Record update = new Record();

	// HashMap hm = new HashMap();
	// hm.put("testing", true);
	// Record statusupdate = new Record(recordId, layer, rectype, loc.getLon(), loc.getLat());

	// // newplace.setGeometry(new Geometry(loc));
	// // newplace.setType("StartupBusTest01");
	// // newplace.setProperties(hm);
	// // newplace.setSimpleGeoId("ABC");
	// String text = "";
	// try {
	//     text = statusupdate.toJSONString();
	// } catch(JSONException e) {
	//     debugArea.setText(e.getMessage());
	// }
	// debugArea.setText(text);

	// ArrayList al = new ArrayList(); 
	// al.add(statusupdate);

	// // try {
	// //     client.addOrUpdateRecords(al, buslayer);
	// // } catch(IOException e) {
	// //     debugArea.setText("IO>"+e.getMessage()+"\n");
	// // } catch(JSONException e) {
	// //     debugArea.setText("JSON>"+e.getMessage()+"\n");
	// // }
	
	// // // HashMap ret = new HashMap();
	// // try {
	// //     client.addOrUpdateRecord(statusupdate);
	// // } catch(IOException e) {
	// //     debugArea.setText("IO>"+e.getMessage()+"\n");
	// // } catch(JSONException e) {
	// //     debugArea.setText("JSON>"+e.getMessage()+"\n");
	// // }

	// // // Set set = ret.entrySet();
	// // // Iterator i = set.iterator();
	// // // while(i.hasNext()){
	// // //     Map.Entry me = (Map.Entry)i.next();
	// // //     debugArea.append(">"+me.getKey() + "< : " + me.getValue() );
	// // // }
	
    }

    public void testData() {
	debugArea.append("Start query");
	com.cleardb.app.Client cleardbClient = new com.cleardb.app.Client(API_KEY, APP_ID);
	JSONObject payload = null;
	try {
	    payload = cleardbClient.query("SELECT longitude, latitude, timestamp FROM startupbus WHERE bus_id = 'San Francisco'");
	} catch (ClearDBQueryException e) {
	    debugArea.append("ClearDB error");
	} catch (Exception e) {
	    debugArea.append("Some errror..");
	}
	
	if (payload != null) {
	    try {
		debugArea.append(payload.getString("response"));
	    } catch (JSONException e) {
		debugArea.append("Json decoding error");
	    }
	}
	// for (JSONObject row : payload.getArray("response")) {
	//     debugArea.append(String.format("at : %.7f, %.7f",
	// 				row.getDouble("longitude"),
	// 				row.getDouble("latitude")
	// 				   ));
	// }

    }

    /*
     * GPS logging related service
     */
    public void startGPS() {
	startService(new Intent(BusDroid.this,
				GPSLoggerService.class));
    }

    public void stopGPS() {
	stopService(new Intent(BusDroid.this,
				GPSLoggerService.class));
    }

    // /*
    //  * Net related service
    //  */
    // public void startNetUpdate() {
    // 	startService(new Intent(BusDroid.this,
    // 				NetUpdateService.class));
    // }

    // public void stopNetUpdate() {
    // 	stopService(new Intent(BusDroid.this,
    // 				NetUpdateService.class));
    // }

    public void getLastLoc() {
	SQLiteDatabase db = openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READONLY, null);
	String query = "SELECT * from "+POINTS_TABLE_NAME+" ORDER BY timestamp DESC LIMIT 1;";
	Cursor cur = db.rawQuery(query, new String [] {});
	cur.moveToFirst();
	debugArea.append(cur.getString(cur.getColumnIndex("GMTTIMESTAMP")));
    }

    public void onClick(View src) {
	switch (src.getId()) {
	case R.id.buttonStart:
	    saveSettings();
	    CheckEnableGPS();
	    startGPS();
	    // startNetUpdate();
	    debugArea.setText("On the roll");
	    // getLastLoc();
	    break;
	case R.id.buttonStop:
	    stopGPS();
	    // stopNetUpdate();
   	    debugArea.setText("No more rolling");
	    break;

	}

    }

    public void saveSettings(){
	// Save Shared Preferences

	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	SharedPreferences.Editor editor = settings.edit();

	// If the city name is changed, ignore all previous points when
	// checking for new locations
	long now = (long) (System.currentTimeMillis() / 1000L);
	editor.putLong("last_update", now);


	// Name of bus in Database
	bus_id = sglayeredit.getText().toString();
	editor.putString("bus_id", bus_id);

	// Commit the edits!
	editor.commit();	
    }

    protected void onStop(){
	super.onStop();
	saveSettings();
    }


   private void CheckEnableGPS(){
       // Check GPS settings and prompt if GPS satellite access  is not enabled

       String provider = Settings.Secure.getString(getContentResolver(),
						   Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
       if (provider.equals("gps")) {
	   Toast.makeText(BusDroid.this, "GPS Enabled: " + provider,
			  Toast.LENGTH_LONG).show();
       }else{
	   Intent intent = new Intent(Settings.ACTION_SECURITY_SETTINGS);
	   startActivity(intent);
	   Toast.makeText(BusDroid.this, "Please enable GPS satellites",
			  Toast.LENGTH_LONG).show();
       }   
   }

}