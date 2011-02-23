
package com.startupbus.location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

import com.startupbus.location.service.GPSLoggerService;

import com.simplegeo.client.SimpleGeoPlacesClient;
import com.simplegeo.client.callbacks.FeatureCollectionCallback;
import com.simplegeo.client.types.Feature;
import com.simplegeo.client.types.FeatureCollection;
import com.simplegeo.client.types.Point;
import com.simplegeo.client.types.Geometry;

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


public class Logger extends Activity implements OnClickListener {
    private static final String TAG = "Logger";
    private LocationManager myManager;
    FeatureCollection collection;
    Button buttonStart, buttonStop;
    TextView debugArea;

    @Override
    public void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
	setContentView(R.layout.main);
	
	buttonStart = (Button) findViewById(R.id.buttonStart);
	buttonStop = (Button) findViewById(R.id.buttonStop);

	debugArea = (TextView) findViewById(R.id.debug);

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

	SimpleGeoPlacesClient client = SimpleGeoPlacesClient.getInstance();
	client.getHttpClient().setToken("CrQ8RDznnjEhwUCGn5Uv9G3h9kR4xcLK", "MtLzaKmMP8C2DfYBDWUemZ6pRLZQe2cT");
	// Point loc = new Point(l.getLongitude(), l.getLatitude());
	Point loc = new Point(l.getLatitude(), l.getLongitude());
	debugArea.setText(String.format("Last location:\n%.7f, %.7f\n(from %s)",
					loc.getLat(),
					loc.getLon(),
					l.getProvider()
					)
			  );
	// try {
	//     collection = client.search(37.7787, -122.3896, "",  "", 25.0);
	// } catch (IOException e) {
	//     debugArea.setText(e.getMessage());
	// }
	String text = "";
	// try {
	//     text = collection.toJSONString();
	// } catch(JSONException e) {
	//     debugArea.setText(e.getMessage());
	// }


	HashMap hm = new HashMap();
	hm.put("postcode", "10617");
	Feature newplace = new Feature();
	newplace.setType("Feature");
	newplace.setGeometry(new Geometry(loc));
	newplace.setType("StartupBusTest01");
	newplace.setProperties(hm);
	newplace.setSimpleGeoId("ABC");
	try {
	    text = newplace.toJSONString();
	} catch(JSONException e) {
	    debugArea.setText(e.getMessage());
	}
	debugArea.setText(text);

	
	HashMap ret = new HashMap();
	try {
	    ret = client.addPlace(newplace);
	} catch(IOException e) {
	    debugArea.setText("IO>"+e.getMessage()+"\n");
	} catch(JSONException e) {
	    debugArea.setText("JSON>"+e.getMessage()+"\n");
	}

	Set set = ret.entrySet();
	Iterator i = set.iterator();
	while(i.hasNext()){
	    Map.Entry me = (Map.Entry)i.next();
	    debugArea.append(">"+me.getKey() + "< : " + me.getValue() );
	}
	
    }

    public void onClick(View src) {
	switch (src.getId()) {
	case R.id.buttonStart:
	    Log.d(TAG, "onClick: starting srvice");
	    startService(new Intent(Logger.this,
				    GPSLoggerService.class));
	    debugArea.setText("Yeah");
	    break;
	case R.id.buttonStop:
	    Log.d(TAG, "onClick: stopping srvice");
	    stopService(new Intent(Logger.this,
				   GPSLoggerService.class));
   	    debugArea.setText("Noeh");
	    break;
	}
    }
}