package com.android.generateh264;

import java.io.IOException;

import net.youmi.android.banner.AdSize;
import net.youmi.android.banner.AdView;
import net.youmi.android.banner.AdViewListener;
import android.content.Context;
import android.graphics.Color;
import android.hardware.Camera;
import android.os.Bundle;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;
import android.view.Display;
import android.view.SurfaceHolder;
import android.view.SurfaceHolder.Callback;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.generateh264.CustomMediaRecorder.VideoInfor;

public class CameraActivity extends BaseActivity implements Callback {
	private final static String TAG = "CameraActivity";
	private int mPreviewWidth = 0;
	private int mPreviewHeight = 0;
	private int mScreenW = 0;
	private int mScreenH = 0;
	private int mSurfaceW = 0;
	private int mSurfaceH = 0;
	private SurfaceView mSurfaceView;
	// 支持socket传输的MediaRecorder
	private CustomMediaRecorder mCustomMediaRecorder = null;

	private Camera mCamera = null;

	private LinearLayout mLinearLayoutAd;

	private WakeLock wl;

	private TextView mStart;
	private TextView mStop;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		setContentView(R.layout.camera_activity_layout);
		initView();
		super.onCreate(savedInstanceState);
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
	}

	@Override
	protected void onResume() {
		PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);
		wl = pm.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK, "my_wakelock");
		wl.acquire();
		super.onResume();
	}

	@Override
	protected void onPause() {
		wl.release();
		wl = null;
		super.onPause();
	}

	private void initView() {
		mStart = (TextView) findViewById(R.id.start);
		mStart.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				mStop.setClickable(true);

				mStart.setClickable(false);
				
				updateButton();
				
				if (mCustomMediaRecorder != null) {
					mCustomMediaRecorder.stopRecorder();
					mCustomMediaRecorder = null;
				}

				mCustomMediaRecorder = new CustomMediaRecorder("");
				mCustomMediaRecorder
						.startRecorder(mCamera, mSurfaceView, new VideoInfor(
								6000, 20, mPreviewWidth, mPreviewHeight));
			}
		});
		mStart.setClickable(true);
		mStop = (TextView) findViewById(R.id.stop);
		mStop.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {
				
				mStart.setClickable(true);

				mStop.setClickable(false);

				updateButton();
				
				if (mCustomMediaRecorder != null) {
					mCustomMediaRecorder.stopRecorder();
					mCustomMediaRecorder = null;
				}
			}
		});
		mStop.setClickable(false);

		mSurfaceView = (SurfaceView) findViewById(R.id.surfaceview_preview);
		mSurfaceView.getHolder().addCallback(this);

		Display display = getWindowManager().getDefaultDisplay();
		mScreenW = display.getWidth();
		mScreenH = display.getHeight();

		mLinearLayoutAd = (LinearLayout) findViewById(R.id.ad_linearlayout);

		AdView adView = new AdView(this, AdSize.FIT_SCREEN);
		mLinearLayoutAd.addView(adView);
		adView.setAdListener(new AdViewListener() {

			@Override
			public void onSwitchedAd(AdView arg0) {
				Log.i("YoumiAdDemo", "广告条切换");
			}

			@Override
			public void onReceivedAd(AdView arg0) {
				Log.i("YoumiAdDemo", "请求广告成功");

			}

			@Override
			public void onFailedToReceivedAd(AdView arg0) {
				Log.i("YoumiAdDemo", "请求广告失败");
			}
		});
		
		updateButton();
	}

	@Override
	public void surfaceChanged(SurfaceHolder arg0, int arg1, int arg2, int arg3) {
		Log.e(TAG, "surfaceChanged");
	}

	@Override
	public void surfaceCreated(SurfaceHolder arg0) {
		Log.e(TAG, "surfaceCreated");
		if (mCustomMediaRecorder != null) {
			mCustomMediaRecorder.stopRecorder();
			mCustomMediaRecorder = null;
		}
		mCamera = Camera.open();

		// List<Size> list = mCamera.getParameters().getSupportedVideoSizes();
		// Size s;
		// for (int i = 0; i < list.size(); i++) {
		// s = list.get(i);
		// Log.e(TAG, "w:" + s.width);
		// Log.e(TAG, "h:" + s.height);
		// }

		mPreviewWidth = 320;
		mPreviewHeight = 240;

		mCamera.setDisplayOrientation(90);
		mSurfaceW = mScreenW;
		mSurfaceH = (int) (mScreenW * (1.0f * mPreviewWidth / mPreviewHeight));
		if (mSurfaceH > mScreenH) {
			mSurfaceW = (int) (mScreenH * (1.0f * mPreviewHeight / mPreviewWidth));
			mSurfaceH = mScreenH;
		}
		arg0.setFixedSize(mSurfaceW, mSurfaceH);
		try {
			mCamera.setPreviewDisplay(arg0);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		mCamera.startPreview();
	}

	@Override
	public void surfaceDestroyed(SurfaceHolder arg0) {
		Log.e(TAG, "surfaceDestroyed");
		if (mCustomMediaRecorder != null) {
			mCustomMediaRecorder.stopRecorder();
			mCustomMediaRecorder = null;
		}
		mCamera.release();
		mCamera = null;
	}
	
	private void updateButton(){
		if(mStart.isClickable()){
			mStart.setTextColor(Color.parseColor("#ff00ff"));
		}else{
			mStart.setTextColor(Color.parseColor("#838b8b"));
		}

		if(mStop.isClickable()){
			mStop.setTextColor(Color.parseColor("#ff00ff"));
		}else{
			mStop.setTextColor(Color.parseColor("#838b8b"));
		}
	}
}
