
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

import android.app.AlertDialog;
import android.content.DialogInterface;


public class BusDroid extends Activity implements OnClickListener {
    public static final String PREFS_NAME = "BusdroidPrefs";
    private static final String tag = "BusDroid:Main";

    public static final String DATABASE_NAME = "GPSLOGGERDB";
    public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";

    private LocationManager myManager;
    Button buttonStart, buttonStop;
    private TextView cityview;
    TextView debugArea;
    EditText sglayeredit;
    private int bus_id;
    private int refresh_interval;
    private String remote_server;
    private TextView push_location_view;

    final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";

    private HashMap<String, Integer> city_map;
    private HashMap<Integer, String> city_map_reverse;

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
	refresh_interval = settings.getInt("refresh_interval", 5);

	buttonStart = (Button) findViewById(R.id.buttonStart);
	buttonStop = (Button) findViewById(R.id.buttonStop);

	cityview = (TextView) findViewById(R.id.cityview);
	push_location_view = (TextView) findViewById(R.id.push_location_view);
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

	city_map_reverse = new HashMap(6);
	Set s= city_map.keySet();
	Iterator i=s.iterator();
	String city;
	while(i.hasNext()) {
	    city = (String) i.next();
	    city_map_reverse.put(city_map.get(city), city);
	}
	cityview.setText(city_map_reverse.get(bus_id));

	remote_server = settings.getString("remote_server", "http://startupbus.com/api/locations");
	settingsEditor.putString("remote_server", remote_server);
	settingsEditor.commit();

	push_location_view.setText(remote_server);
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
    }

    /*
     * GPS logging related service
     */
    public void startGPS() {
	startService(new Intent(BusDroid.this,
				GPSLoggerService.class));
	Log.i(tag, "Started GPS service.");
    }

    public boolean stopGPS() {
	boolean res = stopService(new Intent(BusDroid.this,
				     GPSLoggerService.class));
	if (res == false) {
	    Log.i(tag, "Tried to stop GPS service that wasn't running.");
	    return(false);
	} else {
	    Log.i(tag, "Stopped GPS service.");
	    return(true);
	}
    }

    public void restartGPS() {
	boolean res = stopGPS();
	if (res) {
	    startGPS();
	}
    }

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
	    saveSettings(false);
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

    public void saveSettings(boolean clearOutstanding){
	// Save Shared Preferences

	// Sometimes it's better to throw away some stuff we haven't handled yet
	if (clearOutstanding) {
	    settingsEditor.putString("outstanding_updates", "");
	}

	// If the city name is changed, ignore all previous points when
	// checking for new locations
	long now = (long) (System.currentTimeMillis() / 1000L);
	settingsEditor.putLong("last_update", now);

	// Name of bus in Database
	settingsEditor.putInt("bus_id", bus_id);

	// Refresh_interval
	settingsEditor.putInt("refresh_interval", refresh_interval);

	// Remote server to push to
	settingsEditor.putString("remote_server", remote_server);

	// Commit the edits!
	settingsEditor.commit();
    }

    protected void onStop(){
	super.onStop();
	saveSettings(false);
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
	saveSettings(false);
	restartGPS();
    }

    public void setBusID(int new_bus_id) {
	// Update bus id in database
	Log.i(tag, String.format("New Bus ID set: %d (was %d)", new_bus_id, bus_id));
	bus_id = new_bus_id;
	cityview.setText(city_map_reverse.get(bus_id));
	saveSettings(true);
	restartGPS();
    }

    public void set_location_dialog() {
	AlertDialog.Builder alert = new AlertDialog.Builder(this);

	alert.setTitle("Remote server address");
	alert.setMessage("Careful with this option!");

	// Set an EditText view to get user input 
        final EditText input = new EditText(this);
	alert.setView(input);

	input.setText(remote_server);

	alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    remote_server = input.getText().toString();
		    saveSettings(true);
		    restartGPS();
		    push_location_view.setText(remote_server);
		}
	    });

	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		}
	    });

	alert.show();
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

	case R.id.push_location:
	    set_location_dialog();
	    return true;

	default:
	    return super.onOptionsItemSelected(item);
	}
    }

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