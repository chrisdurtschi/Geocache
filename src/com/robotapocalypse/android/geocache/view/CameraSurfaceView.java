package com.robotapocalypse.android.geocache.view;

import java.io.IOException;

import android.content.Context;
import android.graphics.PixelFormat;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.location.Location;
import android.util.AttributeSet;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

public class CameraSurfaceView extends SurfaceView {
   Camera mCamera;
   SurfaceHolder mPreviewHolder;
   Boolean mPreviewRunning = false;
   int mPreviewWidth, mPreviewHeight, mPictureWidth, mPictureHeight;
   
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

   public void setPreviewWidth(int width) {
	   mPreviewWidth = width;
   }
   
   public void setPreviewHeight(int height) {
	   mPreviewHeight = height;
   }
   
   public void setPictureWidth(int width) {
	   mPictureWidth = width;
   }
   
   public void setPictureHeight(int height) {
	   mPictureHeight = height;
   }   
   
   
   public void createCamera() {
	   if (mCamera == null) {
		   mCamera = Camera.open();
		   try {
			   Parameters params = mCamera.getParameters();
			   params.setPreviewSize(mPreviewWidth, mPreviewHeight);
			   params.setPreviewFormat(PixelFormat.JPEG);
			   params.setPictureSize(mPictureWidth, mPictureHeight);
			   params.setPictureFormat(PixelFormat.JPEG);
			   mCamera.setParameters(params);
			   mCamera.setPreviewDisplay(mPreviewHolder);
		   } catch (IOException exception) {
			   mCamera.release();
			   mCamera = null;
			   // TODO: add more exception handling logic here
		   }
	   }
   }
   
   public void startPreview() {
	   if (!mPreviewRunning && mCamera != null) {
		   mCamera.startPreview();
		   mPreviewRunning = true;
	   }
   }
   
   public void stopPreview() {
	   if (mPreviewRunning && mCamera != null) {
		   mCamera.stopPreview();
		   mPreviewRunning = false;
	   }
   }
   
   
   // SURFACE HOLDER CALLBACKS
   
   SurfaceHolder.Callback surfaceHolderListener = new SurfaceHolder.Callback() {
	   
	   public void surfaceCreated(SurfaceHolder holder) {
		   CameraSurfaceView.this.createCamera();
	   }
	   
	   public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
		   CameraSurfaceView.this.stopPreview();
		   CameraSurfaceView.this.startPreview();
	   }

	   public void surfaceDestroyed(SurfaceHolder holder) {
		   CameraSurfaceView.this.stopPreview();
		   mCamera.release();
		   mCamera = null;
	   }
   };
}