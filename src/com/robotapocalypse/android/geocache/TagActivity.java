package com.robotapocalypse.android.geocache;

import java.io.IOException;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

import com.robotapocalypse.android.geocache.view.CameraSurfaceView;

public class TagActivity extends Activity {
	static final int DIALOG_LOADING_LOCATION = 0;
	static final int DIALOG_CREATING_DOCUMENT = 1;
	static final int DIALOG_UPLOADING_IMAGE = 2;
	
	TelephonyManager mTelephonyManager;
	LocationManager mLocationManager = null;
	Location mLocation = null;
	HttpClient mHttpClient;
	
	CameraSurfaceView mCameraSurface;
	
	
	// ACTIVITY LIFECYCLE

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.tag);
        
        mCameraSurface = (CameraSurfaceView)findViewById(R.id.camera_surface);
        mCameraSurface.setPictureWidth(1024);
        mCameraSurface.setPictureHeight(768);
        mCameraSurface.setVisibility(View.INVISIBLE);
        mCameraSurface.setOnClickListener(new View.OnClickListener() {
			
			@Override
			public void onClick(View v) {
    			if (mCameraSurface == v) {
    				tag();
    			}				
			}
		});
        
		mHttpClient = new DefaultHttpClient();
        mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
    }
	
	@Override
	public void onResume() {
		super.onResume();
		mLocation = null;
		mLocationManager.requestLocationUpdates("gps", 0, 0.0f,	onLocationChange);
		showDialog(DIALOG_LOADING_LOCATION);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mLocation = null;
		mLocationManager.removeUpdates(onLocationChange);
	}
	
	
	// DIALOGS
	
	protected Dialog onCreateDialog(int id) {
		Dialog dialog;
		switch(id) {
		case DIALOG_LOADING_LOCATION:
			dialog = ProgressDialog.show(TagActivity.this, "", getString(R.string.dialog_loading_location), true);
			break;
		case DIALOG_CREATING_DOCUMENT:
			dialog = ProgressDialog.show(TagActivity.this, "", getString(R.string.dialog_creating_document), true);
			break;
		case DIALOG_UPLOADING_IMAGE:
			dialog = ProgressDialog.show(TagActivity.this, "", getString(R.string.dialog_uploading_image), false);
	    default:
	        dialog = null;
	    }
	    return dialog;
	}
	
	
	// LISTENERS

	LocationListener onLocationChange = new LocationListener() {
		
		public void onLocationChanged(Location location) {
			mLocation = location;
			removeDialog(DIALOG_LOADING_LOCATION);
			if (!mCameraSurface.isShown()) {
				mCameraSurface.setVisibility(View.VISIBLE);
			}
		}
		
		public void onProviderDisabled(String provider) {
			// required for interface, not used
		}
		
		public void onProviderEnabled(String provider) {
			// required for interface, not used
		}
		
		public void onStatusChanged(String provider, int status, Bundle extras) {
			// required for interface, not used
		}
	};
	
	
	// TAKE A PICTURE
	
	void tag() {
		showDialog(DIALOG_CREATING_DOCUMENT);
		mCameraSurface.getCamera().takePicture(null, null, new Camera.PictureCallback() {
			
			@Override
			public void onPictureTaken(byte[] data, Camera camera) {
				try {
					String id = String.format(getString(R.string.document_id), mTelephonyManager.getDeviceId(), System.currentTimeMillis());
					String rev = createDocument(id);
					sendImage(id, rev, data);
					removeDialog(DIALOG_CREATING_DOCUMENT);
					
					Toast
					.makeText(TagActivity.this, "Successfully created document " + id, 4000)
					.show();		
				}
				catch (Exception ex) {
					Toast
					.makeText(TagActivity.this, "Request failed: " + ex.toString(), 4000)
					.show();		
				}
			}
		});
	}
	
	String createDocument(String id) throws Exception {
		String url = String.format(getString(R.string.document_url), id);
		HttpPut request = new HttpPut(url);
		request.setHeader("Content-Type", "application/json");
		request.setHeader("Accept", "application/json");

		JSONObject coords = new JSONObject();
		coords.put("latitude", mLocation.getLatitude());
		coords.put("longitude", mLocation.getLongitude());
		coords.put("altitude", mLocation.getAltitude());
		coords.put("bearing", mLocation.getBearing());
		coords.put("speed", mLocation.getSpeed());
		coords.put("accuracy", mLocation.getAccuracy());
		coords.put("timestamp", mLocation.getTime());
		
		JSONObject json = new JSONObject();
		json.put("coords", coords);
		json.put("device_id", mTelephonyManager.getDeviceId());
		json.put("type", "tag");
		
		String body = json.toString();
		
		StringEntity e = new StringEntity(body.toString(), "UTF-8");
		request.setEntity(e);
		
		ResponseHandler<String> responseHandler = new BasicResponseHandler();
		String responseBody = mHttpClient.execute(request, responseHandler);

		JSONObject response = new JSONObject(responseBody);
		try {
			return response.getString("rev");
		} catch (Exception ex) {
			// TODO Auto-generated catch block
			throw ex;
		}
	}
	
	void sendImage(String id, String rev, byte[] data) throws ClientProtocolException, IOException, JSONException {
		String url = String.format(getString(R.string.attachment_url), id, rev);
		HttpPut request = new HttpPut(url);
		request.setHeader("Content-Type", "image/jpeg");
		request.setHeader("Accept", "application/json");
		
		ByteArrayEntity e = new ByteArrayEntity(data);
		request.setEntity(e);
		
		mHttpClient.execute(request);
	}
	
}