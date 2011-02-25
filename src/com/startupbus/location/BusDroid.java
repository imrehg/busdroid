
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

import com.simplegeo.client.SimpleGeoPlacesClient;
import com.simplegeo.client.SimpleGeoStorageClient;
import com.simplegeo.client.callbacks.FeatureCollectionCallback;
import com.simplegeo.client.types.Feature;
import com.simplegeo.client.types.FeatureCollection;
import com.simplegeo.client.types.Point;
import com.simplegeo.client.types.Geometry;
import com.simplegeo.client.types.Record;

// import com.simplegeo.android.cache.CommitLog;
import com.simplegeo.client.handler.GeoJSONHandler;
// import com.simplegeo.client.encoder.GeoJSONEncoder;
// import com.simplegeo.client.geojson.GeoJSONObject;
// import com.simplegeo.client.model.DefaultRecord;
// import com.simplegeo.client.model.GeoJSONRecord;
// import com.simplegeo.client.model.IRecord;
// import com.simplegeo.client.model.Region;

// import com.android.location.LocationManager;

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


public class BusDroid extends Activity implements OnClickListener {
    public static final String PREFS_NAME = "BusdroidPrefs";

    private static final String TAG = "BusDroid";
    private LocationManager myManager;
    FeatureCollection collection;
    Button buttonStart, buttonStop;
    TextView debugArea;
    EditText sglayeredit;
    String buslayer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	// Restore preferences
	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	String buslayer = settings.getString("BusLayer", "");

	buttonStart = (Button) findViewById(R.id.buttonStart);
	buttonStop = (Button) findViewById(R.id.buttonStop);

	debugArea = (TextView) findViewById(R.id.debugArea);

	sglayeredit = (EditText) findViewById(R.id.sglayeredit);
	sglayeredit.setText(buslayer);

	buttonStart.setOnClickListener(this);
	buttonStop.setOnClickListener(this);

	// LocationManager locator = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
	myManager = (LocationManager) getSystemService(LOCATION_SERVICE); 
	Location l = myManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	if (l == null) {
	    // Fall back to coarse location.
	    l = myManager.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	}
	// Start with fine location.
	// Location l = locator.getLastKnownLocation(LocationManager.GPS_PROVIDER);
	// if (l == null) {
	//     // Fall back to coarse location.
	//     l = locator.getLastKnownLocation(LocationManager.NETWORK_PROVIDER);
	// }
	// // locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE); Location location = locationManager.getCurrentLocation("gps");

	SimpleGeoStorageClient client = SimpleGeoStorageClient.getInstance();
	client.getHttpClient().setToken("CrQ8RDznnjEhwUCGn5Uv9G3h9kR4xcLK", "MtLzaKmMP8C2DfYBDWUemZ6pRLZQe2cT");

	Point loc = new Point(l.getLatitude(), l.getLongitude());
	// debugArea.setText(String.format("Last location:\n%.7f, %.7f\n(from %s)",
	// 				loc.getLat(),
	// 				loc.getLon(),
	// 				l.getProvider()
	// 				)
	// 		  );
	// // try {
	// //     collection = client.search(37.7787, -122.3896, "",  "", 25.0);
	// // } catch (IOException e) {
	// //     debugArea.setText(e.getMessage());
	// // }
	// String text = "";
	// // try {
	// //     text = collection.toJSONString();
	// // } catch(JSONException e) {
	// //     debugArea.setText(e.getMessage());
	// // }


	String recordId = String.format("bus_%d", 1234);
	String layer = "com.startupbus.test";
	String rectype= "Location";
	Record update = new Record();

	HashMap hm = new HashMap();
	hm.put("testing", true);
	Record statusupdate = new Record(recordId, layer, rectype, loc.getLon(), loc.getLat());

	// newplace.setGeometry(new Geometry(loc));
	// newplace.setType("StartupBusTest01");
	// newplace.setProperties(hm);
	// newplace.setSimpleGeoId("ABC");
	String text = "";
	try {
	    text = statusupdate.toJSONString();
	} catch(JSONException e) {
	    debugArea.setText(e.getMessage());
	}
	debugArea.setText(text);

	ArrayList al = new ArrayList(); 
	al.add(statusupdate);

	// try {
	//     client.addOrUpdateRecords(al, buslayer);
	// } catch(IOException e) {
	//     debugArea.setText("IO>"+e.getMessage()+"\n");
	// } catch(JSONException e) {
	//     debugArea.setText("JSON>"+e.getMessage()+"\n");
	// }
	
	// // HashMap ret = new HashMap();
	// try {
	//     client.addOrUpdateRecord(statusupdate);
	// } catch(IOException e) {
	//     debugArea.setText("IO>"+e.getMessage()+"\n");
	// } catch(JSONException e) {
	//     debugArea.setText("JSON>"+e.getMessage()+"\n");
	// }

	// // Set set = ret.entrySet();
	// // Iterator i = set.iterator();
	// // while(i.hasNext()){
	// //     Map.Entry me = (Map.Entry)i.next();
	// //     debugArea.append(">"+me.getKey() + "< : " + me.getValue() );
	// // }
	
    }

   // class MyData {
   //      public MyData( String spinnerText, String value ) {
   //          this.spinnerText = spinnerText;
   //          this.value = value;
   //      }

   //      public String getSpinnerText() {
   //          return spinnerText;
   //      }

   //      public String getValue() {
   //          return value;
   //      }

   //      public String toString() {
   //          return spinnerText;
   //      }

   //      String spinnerText;
   //      String value;
   //  }

    public void startGPS() {
	startService(new Intent(BusDroid.this,
				GPSLoggerService.class));
    }

    public void stopGPS() {
	stopService(new Intent(BusDroid.this,
				GPSLoggerService.class));
    }


    public void onClick(View src) {
	switch (src.getId()) {
	case R.id.buttonStart:
	    Log.d(TAG, "onClick: starting srvice");
	    startService(new Intent(BusDroid.this,
				    GPSLoggerService.class));
	    debugArea.setText("Yeah");
	    break;
	case R.id.buttonStop:
	    Log.d(TAG, "onClick: stopping srvice");
	    stopService(new Intent(BusDroid.this,
				   GPSLoggerService.class));
   	    debugArea.setText("Noeh");
	    break;

	}

    }

    public void saveSettings(){
	// Save Shared Preferences

	SharedPreferences settings = getSharedPreferences(PREFS_NAME, 0);
	SharedPreferences.Editor editor = settings.edit();

	// Name of bus' layer on SimpleGeo
	buslayer = sglayeredit.getText().toString();
	editor.putString("BusLayer", buslayer);
	
	// Commit the edits!
	editor.commit();	
    }

    protected void onStop(){
	super.onStop();
	saveSettings();
    }

}