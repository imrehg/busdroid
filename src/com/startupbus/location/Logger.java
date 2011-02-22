
package com.startupbus.location;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.TextView;

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
	Intent intent=new Intent("com.startupbus.location.MyService");  
	switch (src.getId()) {
	case R.id.buttonStart:
	    Log.d(TAG, "onClick: starting srvice");
	    this.startService(intent);
	    debugArea.setText("Yeah");
	    break;
	case R.id.buttonStop:
	    Log.d(TAG, "onClick: stopping srvice");
	    this.stopService(intent);
	    debugArea.setText("Noeh");
	    break;
	}
    }
}