package com.startupbus.location.webserver;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

import org.apache.http.HttpEntity;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentProducer;
import org.apache.http.entity.EntityTemplate;
import org.apache.http.protocol.HttpContext;
import org.apache.http.protocol.HttpRequestHandler;

import android.content.Context;

import android.util.Log;

import org.json.JSONObject;
import org.json.JSONException;
import org.json.JSONArray;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;

public class HomepageHandler implements HttpRequestHandler {
    private Context context = null;
    private final String tag = "BusDroid:HomepageHandler";

    public static final String DATABASE_NAME = "GPSLOGGERDB";
    public static final String POINTS_TABLE_NAME = "LOCATION_POINTS";

    public HomepageHandler(Context context){
	this.context = context;
    }
    
    @Override
    public void handle(HttpRequest request, HttpResponse response, HttpContext httpContext) throws HttpException, IOException {
	Log.i(tag, "Handling");
	String contentType = "application/json";
	HttpEntity entity = new EntityTemplate(new ContentProducer() {
    		public void writeTo(final OutputStream outstream) throws IOException {
		    OutputStreamWriter writer = new OutputStreamWriter(outstream, "UTF-8");
		    String resp = getLocation();
		    
		    writer.write(resp);
		    writer.flush();
    		}
	    });
	
	((EntityTemplate)entity).setContentType(contentType);
	
	response.setEntity(entity);
    }

    public String getLocation() {
	// Query the database to get the latest location

    	SQLiteDatabase db = context.openOrCreateDatabase(DATABASE_NAME, SQLiteDatabase.OPEN_READONLY, null);
    	String query = String.format("SELECT * from %s ORDER BY timestamp DESC LIMIT 1;",
    				     POINTS_TABLE_NAME);
    	Cursor cur = db.rawQuery(query, new String [] {});
	float lat = (float) 37.826667;
	float lon = (float) -122.423333;
	float altitude = (float) 0.0;
	float speed = (float) 0.0;
	float bearing = (float) 0.0;
	float accuracy = (float) 1000.0;

	try {
	    cur.moveToFirst();
	    lon = cur.getFloat(cur.getColumnIndex("LONGITUDE"));
	    lat = cur.getFloat(cur.getColumnIndex("LATITUDE"));
	    altitude = cur.getFloat(cur.getColumnIndex("ALTITUDE"));
	    accuracy = cur.getFloat(cur.getColumnIndex("ACCURACY"));
	    speed = cur.getFloat(cur.getColumnIndex("SPEED"));
	    bearing = cur.getFloat(cur.getColumnIndex("BEARING"));
	} catch (Exception e) {
	    Log.i(tag, "No location found?");
	}

	// Construct response object
	JSONObject response = new JSONObject();
	try {
	    JSONObject location = new JSONObject();
	    location.put("latitude", lat);
	    location.put("longitude", lon);
	    location.put("altitude", altitude);
	    location.put("accuracy", accuracy);
	    location.put("speed", speed);
	    location.put("bearing", speed);

	    response.put("location", location);
	} catch(JSONException e) {
	    Log.i(tag, "JSON error");
	}
	return response.toString();
    }
}