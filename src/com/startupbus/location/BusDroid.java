
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

import java.util.HashMap;

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

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;


public class BusDroid extends Activity implements OnClickListener {
    public static final String PREFS_NAME = "BusdroidPrefs";
    private static final String tag = "BusDroid:Main";

    public static final String DATABASE_NAME = "GPSLOGGERDB";
    public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";

    private LocationManager myManager;
    Button buttonStart, buttonStop;
    TextView debugArea;
    EditText sglayeredit;
    private int bus_id;
    private int refresh_interval;

    final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";

    private HashMap<String, Integer> city_map;

    private SharedPreferences settings;
    private SharedPreferences.Editor settingsEditor;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	// Restore preferences
	settings = getSharedPreferences(PREFS_NAME, 0);
	settingsEditor = settings.edit();

	bus_id = settings.getInt("bus_id", 1);
	refresh_interval = settings.getInt("refresh_interval", 1);

	buttonStart = (Button) findViewById(R.id.buttonStart);
	buttonStop = (Button) findViewById(R.id.buttonStop);

	debugArea = (TextView) findViewById(R.id.debugArea);

	buttonStart.setOnClickListener(this);
	buttonStop.setOnClickListener(this);

	// Set city mapping - quick and dirty hard-coding
	city_map = new HashMap(6);
	city_map.put("New York", new Integer(1));
	city_map.put("San Francisco", new Integer(2));
	city_map.put("Silicon Valley", new Integer(3));
	city_map.put("Chicago", new Integer(4));
	city_map.put("Miami", new Integer(5));
	city_map.put("Cleveland", new Integer(6));
	
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

	// If the city name is changed, ignore all previous points when
	// checking for new locations
	long now = (long) (System.currentTimeMillis() / 1000L);
	settingsEditor.putLong("last_update", now);

	// Name of bus in Database
	settingsEditor.putInt("bus_id", bus_id);

	// Refresh_interval
	settingsEditor.putInt("refresh_interval", refresh_interval);

	// Commit the edits!
	settingsEditor.commit();
    }

    protected void onStop(){
	super.onStop();
	saveSettings();
    }

    // Config menu items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.menu, menu);

	// Load defaults and use them
	switch(bus_id) {
	case 1:
	    setThisChecked(menu, R.id.bus_newyork_check);
	    break;
	case 2:
	    setThisChecked(menu, R.id.bus_sanfrancisco_check);
	    break;
	case 3:
	    setThisChecked(menu, R.id.bus_siliconvalley_check);
	    break;
	case 4:
	    setThisChecked(menu, R.id.bus_chicago_check);
	    break;
	case 5:
	    setThisChecked(menu, R.id.bus_miami_check);
	    break;
	case 6:
	default :
	    setThisChecked(menu, R.id.bus_cleveland_check);
	    break;
	}

	switch(refresh_interval) {
	case 10:
	    setThisChecked(menu, R.id.refresh_10);
	    break;
	case 5:
	    setThisChecked(menu, R.id.refresh_5);
	    break;
	case 2:
	    setThisChecked(menu, R.id.refresh_2);
	    break;
	case 1:
	default :
	    setThisChecked(menu, R.id.refresh_1);
	    break;
	}

	return true;
    }

    public void setThisChecked(Menu menu, int id) {
	MenuItem toSet = menu.findItem(id);
	toSet.setChecked(true);
    }

    public void toggleChecked(MenuItem item) {
	if (item.isChecked()) item.setChecked(false);
	else item.setChecked(true);
    }

    public void setRefreshInterval(int minutes) {
	// Update refresh interval in database
	Log.i(tag, String.format("New refresh interval set: %d (was %d)", minutes, refresh_interval));
	refresh_interval = minutes;
	saveSettings();
    }

    public void setBusID(int new_bus_id) {
	// Update bus id in database
	Log.i(tag, String.format("New Bus ID set: %d (was %d)", new_bus_id, bus_id));
	bus_id = new_bus_id;
	saveSettings();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	// Handle item selection
	switch (item.getItemId()) {
	case R.id.bus_id_setting:
	    return true;
	case R.id.refresh_setting:
	    return true;

	case R.id.refresh_1:
	    setRefreshInterval(1);
	    toggleChecked(item);
	    return true;
	case R.id.refresh_2:
	    setRefreshInterval(2);
	    toggleChecked(item);
	    return true;
	case R.id.refresh_5:
	    setRefreshInterval(5);
	    toggleChecked(item);
	    return true;
	case R.id.refresh_10:
	    setRefreshInterval(10);
	    toggleChecked(item);
	    return true;

	case R.id.bus_chicago_check:
	case R.id.bus_cleveland_check:
	case R.id.bus_miami_check:
	case R.id.bus_newyork_check:
	case R.id.bus_sanfrancisco_check:
	case R.id.bus_siliconvalley_check:
	    setBusID(city_map.get(item.getTitle()));
	    toggleChecked(item);
	    return true;

	default:
	    return super.onOptionsItemSelected(item);
	}
    }

    // public void popupClick() {
    //     PopupMenu popup = new PopupMenu(this);
    //     popup.getMenuInflater().inflate(R.menu.refresh, popup.getMenu());

    //     popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
    //         public boolean onMenuItemClick(MenuItem item) {
    //             Toast.makeText(BusDroid.this, "Clicked popup menu item " + item.getTitle(),
    // 			       Toast.LENGTH_SHORT).show();
    //             return true;
    //         }
    // 	});
    //     popup.show();
    // }

   private void CheckEnableGPS(){
       // Check GPS settings and prompt if GPS satellite access  is not enabled

       String provider = Settings.Secure.getString(getContentResolver(),
						   Settings.Secure.LOCATION_PROVIDERS_ALLOWED);
       Log.i(tag, provider);
       if (provider.indexOf("gps") >= 0) {
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