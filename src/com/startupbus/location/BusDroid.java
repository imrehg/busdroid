
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
import com.startupbus.location.service.GeolocProviderService;


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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import java.io.BufferedReader;

import org.json.JSONTokener;

import android.app.Activity;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SubMenu;
import android.widget.Toast;
import android.app.ProgressDialog;

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
    private String remote_config;
    private TextView push_location_view;

    final String APP_ID = "3bc0af918733f74f08d0b274e7ede7b0";
    final String API_KEY = "82fb3d39213cf1b75717eac4e1dd8c30b32234cb";

    private HashMap<String, Integer> city_map;
    private HashMap<Integer, String> city_map_reverse;

    private SharedPreferences settings;
    private SharedPreferences.Editor settingsEditor;

    private static final int MCONFIG = 5000;
    private static final int MBUSES = 5001;
    private static final int MINTERVAL = 5002;
    private static final int MENDPOINT = 5003;
    private static final int MOAUTH = 5004;

    private static final int GBUSES = 1;

    private JSONArray buses;
    private String busesJSON;
    private String oauth_token;

    /* Called when the activity is first created. */
    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	// Restore preferences
	settings = getSharedPreferences(PREFS_NAME, 0);
	settingsEditor = settings.edit();

	bus_id = settings.getInt("bus_id", -1);
	refresh_interval = settings.getInt("refresh_interval", 5);
	remote_server = settings.getString("remote_server", "");
	remote_config = settings.getString("remote_config", "");
	busesJSON = settings.getString("busesJSON", "[]");
	oauth_token = settings.getString("oauth_token", "");

	buttonStart = (Button) findViewById(R.id.buttonStart);
	buttonStop = (Button) findViewById(R.id.buttonStop);

	cityview = (TextView) findViewById(R.id.cityview);
	push_location_view = (TextView) findViewById(R.id.push_location_view);
	debugArea = (TextView) findViewById(R.id.debugArea);

	buttonStart.setOnClickListener(this);
	buttonStop.setOnClickListener(this);

	push_location_view.setText(remote_server);
	cityview.setText(getBusName(getBusIndex(bus_id)));

    }

    /* Quick toast creation */
    public void makeToast(String text) {
	Toast.makeText(BusDroid.this, text, Toast.LENGTH_LONG).show();
    }

    /* Return a bus' name based on their index in the config list*/
    public String getBusName(int index) {
	JSONArray buses;
	JSONObject bus;
	String busname;
	try {
	    buses = new JSONArray(busesJSON);
	    bus = buses.optJSONObject(index);
	    busname = bus.getString("name");
	} catch(Exception e) {
	    busname = "?";
	}
	return busname;
    }

    /* Matched a bus' index to its id */
    public int getBusIndex(int id) {
	JSONArray buses;
	JSONObject bus;
	String busname = "";
	int index = -1;
	try {
	    buses = new JSONArray(busesJSON);
	    for (int i = 0; i < buses.length(); i++) {
		bus = buses.optJSONObject(i);
		if (id == bus.getInt("id")) {
		    index = i;
		}
	    }
	} catch(JSONException e) {

	} finally {
	    return index;
	}
    }

    /* Get a string from the net */
    public static String getStringContent(String uri) throws Exception {
	try {
	    HttpClient client = new DefaultHttpClient();
	    HttpGet request = new HttpGet();
	    request.setURI(new URI(uri));
	    HttpResponse response = client.execute(request);
	    InputStream ips  = response.getEntity().getContent();
	    BufferedReader buf = new BufferedReader(new InputStreamReader(ips,"UTF-8"));

	    StringBuilder sb = new StringBuilder();
	    String s;
	    while(true )
		{
		    s = buf.readLine();
		    if(s==null || s.length()==0)
			break;
		    sb.append(s);
		}
	    buf.close();
	    ips.close();
	    return sb.toString();
        }
	finally {
	    // any cleanup code...
	}
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
	    if (bus_id < 0) {
		break;
	    }
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

	    // If the city name is changed, ignore all previous points when
	    // checking for new locations
	    long now = (long) (System.currentTimeMillis() / 1000L);
	    settingsEditor.putLong("last_update", now);
	}

	// Name of bus in Database
	settingsEditor.putInt("bus_id", bus_id);

	// Refresh_interval
	settingsEditor.putInt("refresh_interval", refresh_interval);

	// Remote server to push to
	settingsEditor.putString("remote_server", remote_server);

	settingsEditor.putString("remote_config", remote_config);

	settingsEditor.putString("busesJSON", busesJSON);

	settingsEditor.putString("oauth_token", oauth_token);

	// Commit the edits!
	settingsEditor.commit();
	push_location_view.setText(remote_server);
    }

    protected void onStop(){
	super.onStop();
	saveSettings(false);
    }

    // Config menu items
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

	// Create all the menus

	// Remote configuration pull
	SubMenu confpullMenu = menu.addSubMenu(0, MCONFIG, 0, "Get configuration");
	confpullMenu.setIcon(android.R.drawable.ic_menu_manage);

	// Choose a bus
	SubMenu busMenu = menu.addSubMenu(GBUSES, MBUSES, 0, "Choose Bus");
	busMenu.setIcon(android.R.drawable.ic_menu_mylocation);

	// Choose a bus
	SubMenu oauthMenu = menu.addSubMenu(0, MOAUTH, 0, "OAuth Token");
	oauthMenu.setIcon(android.R.drawable.ic_lock_lock);


	// Rest of the stuff....
	MenuInflater inflater = getMenuInflater();
	inflater.inflate(R.menu.menu, menu);

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

    /* Called every time when the menu is shown, dynamic fill of bus list*/
    @Override
    public boolean onPrepareOptionsMenu (Menu menu) {
	MenuItem busMenuItem = menu.findItem(MBUSES);
	if (busMenuItem != null) {
	    SubMenu busMenu = busMenuItem.getSubMenu();
	    busMenu.clear();
	    try {
		JSONArray buslist = new JSONArray(busesJSON);
		for (int x = 0; x < buslist.length(); x++ ) {
		    try {
			JSONObject bus = buslist.getJSONObject(x);
			String name = bus.getString("name");
			busMenu.add(GBUSES, x, x, name);
		} catch(Exception e) {
			Log.i("Error", "Bus list data broken:"+e.toString());
		    }
		}
	    } catch(Exception e) {
		Log.i("Error", "Bus list broken");
	    }
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

    public void setBusID(int index) {
	JSONArray buses;
	JSONObject bus;
	int busid = 0;
	String busname = "";
	try {
	    buses = new JSONArray(busesJSON);
	    bus = buses.optJSONObject(index);
	    busid = bus.getInt("id");
	    busname = bus.getString("name");
	} catch(JSONException e) {
	    //
	}
	bus_id = busid;
	cityview.setText(busname);
	saveSettings(true);
	restartGPS();
    }


    public void get_remote_config_dialog() {
	AlertDialog.Builder alert = new AlertDialog.Builder(this);

	alert.setTitle("Remote config URL");
	alert.setMessage("Pull new configuration from remote server");

	// Set an EditText view to get user input 
        final EditText input = new EditText(this);
	alert.setView(input);

	input.setText(remote_config);

	alert.setPositiveButton("Get config", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    remote_config = input.getText().toString();
		    try {
			String remotedata = getStringContent(remote_config);
			Log.i("DATA", remotedata);
			JSONObject object = (JSONObject) new JSONTokener(remotedata).nextValue();
			// Save settings
			buses = object.getJSONArray("buses");
			busesJSON = object.getJSONArray("buses").toString();
			remote_server = object.getString("endpoint");
			saveSettings(true);
			restartGPS();
			makeToast("Configuration update successful.");
		    } catch (Exception e) {
			makeToast("Something went wrong, please check your network connection and the URL you gave.");
			Log.i("Remote config error:", e.toString());
		    } finally {
		    }
		}
	    });

	alert.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		}
	    });

	alert.show();
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

    public void set_oauth_token_dialog() {
	AlertDialog.Builder alert = new AlertDialog.Builder(this);

	alert.setTitle("OAuth Token");
	alert.setMessage("Enter your token for great good:");

	// Set an EditText view to get user input 
        final EditText input = new EditText(this);
	alert.setView(input);

	input.setText(oauth_token);

	alert.setPositiveButton("Set", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    oauth_token = input.getText().toString();
		    saveSettings(true);
		}
	    });

	alert.setNegativeButton("Back", new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int whichButton) {
		    // Canceled.
		}
	    });

	alert.show();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	int gID = item.getGroupId();
	int iID = item.getItemId();

	// Buses menu
	if (gID == GBUSES) {
	    switch(iID) {
	    case MBUSES:
		return true;

	    default:
		JSONArray buses;
		JSONObject bus;
		int busid;
		String busname;

		try {
		    buses = new JSONArray(busesJSON);
		    bus = buses.optJSONObject(iID);
		    busid = bus.getInt("id");
		    busname = bus.getString("name");
		} catch(JSONException e) {
		    return true;
		}
		setBusID(iID);
		return true;
	    }
	}


	// Handle item selection
	switch (item.getItemId()) {
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

	case R.id.push_location:
	    set_location_dialog();
	    return true;
	case MCONFIG:
	    get_remote_config_dialog();
	    return true;
	case MOAUTH:
	    set_oauth_token_dialog();
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