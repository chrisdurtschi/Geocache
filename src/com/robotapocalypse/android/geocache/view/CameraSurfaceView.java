package com.robotapocalypse.android.geocache.view;

import java.io.IOException;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView {
   Camera mCamera;
   SurfaceHolder mPreviewHolder;
   Boolean mPreviewRunning = false;
   int mPictureWidth, mPictureHeight;
   
   // CONSTRUCTORS

   public CameraSurfaceView(Context context) {
      super(context);
      init();
   }
   
   public CameraSurfaceView(Context context, AttributeSet attrs) {
	   super(context, attrs);
	   init();
   }
   
   void init() {
	   mPreviewHolder = getHolder();
	   mPreviewHolder.setType(SurfaceHolder.SURFACE_TYPE_PUSH_BUFFERS);
	   mPreviewHolder.addCallback(surfaceHolderListener);	   
   }
   
	
	// ACCESSOR METHODS

   public Camera getCamera() {
	   return mCamera;
   }
   
   public void setPictureWidth(int width) {
	   mPictureWidth = width;
   }
   
   public void setPictureHeight(int height) {
	   mPictureHeight = height;
   }
   
   
   // SURFACE HOLDER CALLBACKS
   
   SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {
	   
	   public void surfaceCreated(SurfaceHolder holder) {
		   mCamera = Camera.open();
		   try {
			   mCamera.setPreviewDisplay(mPreviewHolder);
	        } catch (IOException exception) {
	            mCamera.release();
	            mCamera = null;
	            // TODO: add more exception handling logic here
	        }
	   }
	   
	   public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		   if (mPreviewRunning) {
			   mCamera.stopPreview();
		   }
		   Parameters params = mCamera.getParameters();
		   params.setPreviewSize(800, 480);
		   params.setPreviewFormat(PixelFormat.JPEG);
		   params.setPictureSize(mPictureWidth, mPictureHeight);
		   params.setPictureFormat(PixelFormat.JPEG);
		   mCamera.setParameters(params);
		   mCamera.startPreview();
		   mPreviewRunning = true;
	   }

	   public void surfaceDestroyed(SurfaceHolder holder) {
		   mCamera.stopPreview();
		   mCamera.release();
		   mCamera = null;
		   mPreviewRunning = false;
	   }
   };
}