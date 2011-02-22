
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

public class Logger extends Activity implements OnClickListener {
    private static final String TAG = "Logger";
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