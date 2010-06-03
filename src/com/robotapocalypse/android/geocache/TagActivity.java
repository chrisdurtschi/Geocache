package com.robotapocalypse.android.geocache;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;

import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.FileEntity;
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
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Bitmap.CompressFormat;
import android.hardware.Camera;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import com.robotapocalypse.android.geocache.view.CameraSurfaceView;

public class TagActivity extends Activity {
	static final int DIALOG_LOADING_LOCATION = 0;
	static final int DIALOG_CREATING_DOCUMENT = 1;
	static final int DIALOG_UPLOADING_IMAGE = 2;
	
	LocationManager mLocationManager = null;
	Location mLocation = null;
	Location mPictureLocation = null;
	
	CameraSurfaceView mCameraSurface;
	FrameLayout mPreviewFrame;
	ImageView mPreviewImage;
	Button mSubmit;
	Button mRetry;
	
	
	// ACTIVITY LIFECYCLE

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE);
        
        setContentView(R.layout.tag);
        
        mLocationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        
        mPreviewFrame = (FrameLayout)findViewById(R.id.preview_frame);
        mPreviewImage = (ImageView)findViewById(R.id.preview_image);
        mSubmit = (Button)findViewById(R.id.submit);
        mRetry = (Button)findViewById(R.id.retry);
        
        mCameraSurface = (CameraSurfaceView)findViewById(R.id.camera_surface);
        mCameraSurface.setPreviewHeight(480);
        mCameraSurface.setPreviewWidth(800);
        mCameraSurface.setPictureWidth(1024);
        mCameraSurface.setPictureHeight(768);
        mCameraSurface.setVisibility(View.INVISIBLE);
        mCameraSurface.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				takePicture();
			}
		});
        
        mSubmit.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				new TagTask().execute("");
			}
		});
        
        mRetry.setOnClickListener(new View.OnClickListener() {
			
			public void onClick(View v) {
				TagActivity.this.showCameraSurface();
			}
		});
	}

	@Override
	public void onResume() {
		super.onResume();
		mLocation = null;
		mPictureLocation = null;
		mLocationManager.requestLocationUpdates("gps", 0, 0.0f,	onLocationChange);
		showDialog(DIALOG_LOADING_LOCATION);
	}
	
	@Override
	public void onPause() {
		super.onPause();
		mLocation = null;
		mPictureLocation = null;
		mLocationManager.removeUpdates(onLocationChange);
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		mLocation = null;
		mPictureLocation = null;
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
			if (mLocation == null) {
				dismissDialog(DIALOG_LOADING_LOCATION);
				TagActivity.this.showCameraSurface();
			}
			mLocation = location;
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
	
	public void hideAllViews() {
		mCameraSurface.setVisibility(View.INVISIBLE);
		mPreviewFrame.setVisibility(View.INVISIBLE);
	}
	
	public void showPreviewFrame() {
		if (!mPreviewFrame.isShown()) {
			mCameraSurface.setVisibility(View.INVISIBLE);
			mPreviewFrame.setVisibility(View.VISIBLE);
		}
	}
	
	public void showCameraSurface() {
		if (!mCameraSurface.isShown()) {
			mCameraSurface.setVisibility(View.VISIBLE);
			mPreviewFrame.setVisibility(View.INVISIBLE);
		}
	}
	
	void takePicture() {
		mCameraSurface.getCamera().autoFocus(new Camera.AutoFocusCallback() {
			
			public void onAutoFocus(boolean success, Camera camera) {
				if (success) {
					mPictureLocation = new Location(mLocation);
				   
					camera.takePicture(null, null, new Camera.PictureCallback() {
						
						public void onPictureTaken(byte[] data, Camera camera) {
							TagActivity.this.showPreviewFrame();

							Bitmap original = BitmapFactory.decodeByteArray(data, 0, data.length);
							Bitmap cropped = Bitmap.createBitmap(original, 212, 84, 600, 600);
							
							FileOutputStream fos = null;
							try {
								fos = TagActivity.this.openFileOutput("tag.jpg", Context.MODE_PRIVATE);
								cropped.compress(CompressFormat.JPEG, 100, fos);
								fos.close();
							} catch (FileNotFoundException e) {
								// TODO Auto-generated catch block
								Log.e("Save Cached Tag", e.getMessage(), e);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								Log.e("Save Cached Tag", e.getMessage(), e);
							}							

							mPreviewImage.setImageBitmap(cropped);
						}
					});
				}
			}
		});
	}
	
	
	private class TagTask extends AsyncTask<String, Integer, String> {
		final HttpClient mHttpClient = new DefaultHttpClient();
		final TelephonyManager mTelephonyManager = (TelephonyManager)getSystemService(Context.TELEPHONY_SERVICE);

		@Override
		protected String doInBackground(String... stuff) {
			try {
				String id = String.format(getString(R.string.document_id), mTelephonyManager.getDeviceId(), System.currentTimeMillis());
				String rev = createDocument(id);
				HttpResponse response = sendImage(id, rev);
				if (response.getStatusLine().getStatusCode() >= 300) {
					return "Error uploading image: " + response.getStatusLine().getStatusCode() + " - " + response.getStatusLine().getReasonPhrase();
				}
				return "Successfully created document " + id;
			}
			catch (Exception ex) {
				return "Request failed: " + ex.toString();
			}
		}
		
		@Override
		protected void onPreExecute() {
			showDialog(DIALOG_CREATING_DOCUMENT);	
		}
		
		@Override
		protected void onPostExecute(String result) {
			removeDialog(DIALOG_CREATING_DOCUMENT);
			Toast.makeText(TagActivity.this, result, 4000).show();
		}
		
		String createDocument(String id) throws Exception {
			String url = String.format(getString(R.string.document_url), id);
			HttpPut request = new HttpPut(url);
			request.setHeader("Content-Type", "application/json");
			request.setHeader("Accept", "application/json");

			JSONObject coords = new JSONObject();
			coords.put("latitude", mPictureLocation.getLatitude());
			coords.put("longitude", mPictureLocation.getLongitude());
			coords.put("altitude", mPictureLocation.getAltitude());
			coords.put("bearing", mPictureLocation.getBearing());
			coords.put("speed", mPictureLocation.getSpeed());
			coords.put("accuracy", mPictureLocation.getAccuracy());
			coords.put("timestamp", mPictureLocation.getTime());
			
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
		
		HttpResponse sendImage(String id, String rev) throws ClientProtocolException, IOException, JSONException {
			String url = String.format(getString(R.string.attachment_url), id, rev);
			HttpPut request = new HttpPut(url);
			request.setHeader("Content-Type", "image/jpeg");
			request.setHeader("Accept", "application/json");
			
			File file = TagActivity.this.getFilesDir().listFiles(new TagFilter())[0];
			FileEntity e = new FileEntity(file, "image/jpeg");
			request.setEntity(e);
			
			return mHttpClient.execute(request);
		}
	}
	
	class TagFilter implements FilenameFilter {
	    public boolean accept(File dir, String name) {
	        return (name.endsWith("tag.jpg"));
	    }
	}
}