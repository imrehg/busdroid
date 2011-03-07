package com.startupbus.location.webserver;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import org.apache.http.HttpException;
import org.apache.http.impl.DefaultConnectionReuseStrategy;
import org.apache.http.impl.DefaultHttpResponseFactory;
import org.apache.http.impl.DefaultHttpServerConnection;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.protocol.BasicHttpContext;
import org.apache.http.protocol.BasicHttpProcessor;
import org.apache.http.protocol.HttpRequestHandlerRegistry;
import org.apache.http.protocol.HttpService;
import org.apache.http.protocol.ResponseConnControl;
import org.apache.http.protocol.ResponseContent;
import org.apache.http.protocol.ResponseDate;
import org.apache.http.protocol.ResponseServer;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import android.util.Log;

public class WebServer extends Thread {
    private static final String SERVER_NAME = "BusDroidGeoServer";
    private static final String ALL_PATTERN = "*";

    private boolean isRunning = false;
    private Context context = null;
    private int serverPort = 0;
	
    private BasicHttpProcessor httpproc = null;
    private BasicHttpContext httpContext = null;
    private HttpService httpService = null;
    private HttpRequestHandlerRegistry registry = null;
    
    private static final String tag = "BusDroid:WebServer";
	
    public WebServer(Context context){
	super(SERVER_NAME);
	
	this.setContext(context);
	
	serverPort = 8080;
	httpproc = new BasicHttpProcessor();
	httpContext = new BasicHttpContext();
		
        httpproc.addInterceptor(new ResponseDate());
        httpproc.addInterceptor(new ResponseServer());
        httpproc.addInterceptor(new ResponseContent());
        httpproc.addInterceptor(new ResponseConnControl());

        httpService = new HttpService(httpproc, 
				      new DefaultConnectionReuseStrategy(),
				      new DefaultHttpResponseFactory());
	
        registry = new HttpRequestHandlerRegistry();
	registry.register(ALL_PATTERN, new HomepageHandler(context));
        
        httpService.setHandlerResolver(registry);
	Log.i(tag, "Webserver started");
    }
	
    @Override
    public void run() {
	super.run();
		
	try {
	    ServerSocket serverSocket = new ServerSocket(serverPort);
	    serverSocket.setReuseAddress(true);

	    
	    while(isRunning){
		try {

		    Log.i(tag, "Server isRunning");
		    final Socket socket = serverSocket.accept();
		    DefaultHttpServerConnection serverConnection = new DefaultHttpServerConnection();
		    
		    serverConnection.bind(socket, new BasicHttpParams());
		    
		    httpService.handleRequest(serverConnection, httpContext);
		    
		    serverConnection.shutdown();
		} catch (IOException e) {
		    e.printStackTrace();
		} catch (HttpException e) {
		    e.printStackTrace();
		}
	    }
	    
	    serverSocket.close();
	} 
	catch (IOException e) {
	    e.printStackTrace();
	}
    }
    
    public synchronized void startThread() {
	isRunning = true;
	
	super.start();
    }
    
    public synchronized void stopThread(){
	isRunning = false;
    }
    
    public void setContext(Context context) {
	this.context = context;
    }
    
    public Context getContext() {
	return context;
    }
}