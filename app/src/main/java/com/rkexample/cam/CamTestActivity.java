package com.rkexample.cam;

/**
 * @author Jose Davis Nidhin
 */


import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import com.example.cam.R;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.PictureCallback;
import android.hardware.Camera.ShutterCallback;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.os.StatFs;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.Toast;
import android.media.MediaRecorder;
import android.media.CamcorderProfile;
public class CamTestActivity extends Activity {
	private static final String TAG = "CamTestActivity";
	private final int CAM_STATUS_IDLE = 0;
	private final int CAM_STATUS_CAPTUREING = 1;
	private final int CAM_STATUS_RECORDING = 2;
	Preview[] mPreview;
	Button mRecordBtn;
	Button mCaptureBtn;
	Camera[] mCamera;
	Activity act;
	Context ctx;
	private MediaRecorder[] mMediaRecorders;
	private int captureCount = 0;
	private int recorderCount = 0;
	private int mCameraNum;


	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		ctx = this;
		act = this;
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
		mCameraNum = Camera.getNumberOfCameras();

		setContentView(R.layout.main);
		
		SurfaceView[] surfaViews = new SurfaceView[mCameraNum];
		mPreview = new Preview[mCameraNum];
		mCamera = new Camera[mCameraNum];
		mMediaRecorders = new MediaRecorder[mCameraNum];
		for (int i = 0; i < mCameraNum; i++) {
		    surfaViews[i] = new SurfaceView(this);
		    LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
		            LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
		    lp.setMargins(10, 10, 10, 10);
		    surfaViews[i].setLayoutParams(lp);
		    ((LinearLayout) findViewById(R.id.layout1)).addView(surfaViews[i]);
		    
		    mPreview[i] = new Preview(this, surfaViews[i]);
		    mPreview[i].setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT));
		    ((FrameLayout) findViewById(R.id.layout)).addView(mPreview[i]);
		}
		
		mCaptureBtn = (Button)findViewById(R.id.capture);
		mCaptureBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Log.d(TAG,"==============take picture============");
				if (captureCount != 0) {
				    Log.d(TAG, "=====last capturing still work");
				    return;
				}
				captureCount = mCameraNum;
				for (int i = 0; i < mCameraNum; i++) {
				    if(mCamera[i] != null)
				        mCamera[i].takePicture(null, null, new JpegCallBack(i));
				}
			}

			
		});
		mRecordBtn = (Button) findViewById(R.id.record);
		mRecordBtn.setOnClickListener(new View.OnClickListener() {
            
            @Override
            public void onClick(View v) {
                // TODO Auto-generated method stub
                
                if (recorderCount > 0) {
                    for (int i = 0; i < mCameraNum; i++) 
                        stopMediaRecorder(i);
                    mRecordBtn.setText(R.string.front_record);
                } else {
                    for (int i = 0; i < mCameraNum; i++) 
                        startRecording(i, mCamera[i], true);
                    mRecordBtn.setText(R.string.stop_record);
                }
            }
        });
		
	
	}



	private void startRecording(int id, Camera mCamera,boolean isFront) {
		// TODO Auto-generated method stub
		
		// BEGIN_INCLUDE (configure_preview)
		 
		// We need to make sure that our preview and recording video size are supported by the
		// camera. Query camera to find all the sizes and choose the optimal size given the
		// dimensions of our preview surface.
		
		Camera.Parameters parameters = mCamera.getParameters();
		//List<Camera.Size> mSupportedPreviewSizes = parameters.getSupportedPreviewSizes();
		//Camera.Size optimalSize = CameraHelper.getOptimalPreviewSize(mSupportedPreviewSizes,
		//mPreview.getWidth(), mPreview.getHeight());
		
		// Use the same size for recording profile.                                                                                                                
		CamcorderProfile profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
		// BEGIN_INCLUDE (configure_media_recorder)

			mMediaRecorders[id] = new MediaRecorder();
			
			// Step 1: Unlock and set camera to MediaRecorder
			mCamera.unlock();
			mMediaRecorders[id].setCamera(mCamera);
	
			// Step 2: Set sources
			//mMediaRecorders[id].setAudioSource(MediaRecorder.AudioSource.CAMCORDER );
			mMediaRecorders[id].setVideoSource(MediaRecorder.VideoSource.CAMERA);
			
			// Step 3: Set a CamcorderProfile (requires API Level 8 or higher)
			mMediaRecorders[id].setProfile(profile);
			mMediaRecorders[id].setVideoSize(profile.videoFrameWidth, profile.videoFrameHeight);
			
			// Step 4: Set output file
			mMediaRecorders[id].setOutputFile(initFile(id).getAbsolutePath());
			// END_INCLUDE (configure_media_recorder)
			 
			// Step 5: Prepare configured MediaRecorder
			try {
			    mMediaRecorders[id].prepare();
			} catch (IllegalStateException e) {
			    Log.d(TAG, "IllegalStateException preparing MediaRecorder: " + e.getMessage());
			    stopMediaRecorder(id);
			} catch (IOException e) {
			    Log.d(TAG, "IOException preparing MediaRecorder: " + e.getMessage());
			    stopMediaRecorder(id);
			}
			mMediaRecorders[id].start();
			recorderCount++;
	}

	private File initFile(int id) {
		// TODO Auto-generated method stub
		File file;
		File dir = new File(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), this
                        .getClass().getPackage().getName());
        if (!dir.exists() && !dir.mkdirs()) {
            Log.wtf(TAG, "Failed to create storage directory: " + dir.getAbsolutePath());
            file = null;
        } else {
            file = new File(dir.getAbsolutePath(), new SimpleDateFormat(
                    "'VIDEO" + id+ "_'yyyyMMddHHmmss'.mp4'").format(new Date()));
        }
        return file;
	}

	private void stopMediaRecorder(int id) {
		// TODO Auto-generated method stub
	    if (mMediaRecorders[id] != null) {
	        mMediaRecorders[id].stop();
	        mMediaRecorders[id].reset();
	        mMediaRecorders[id].release();
	        recorderCount--;
	    }
	}
	
	@Override
	protected void onResume() {
		super.onResume();
		for (int i = 0; i < mCameraNum; i++) {
		    try{
    		    mCamera[i] = Camera.open(i);
    		    mPreview[i].setCamera(mCamera[i]);
		    } catch (RuntimeException ex) {
		        Toast.makeText(ctx, getString(R.string.camera_not_found), Toast.LENGTH_LONG).show();
		    }
		}
	}

	@Override
	protected void onPause() {
	    for (int i = 0; i < mCameraNum; i++) {
	        if (recorderCount > 0) {
	            stopMediaRecorder(i);
	        }
	        if (mCamera[i] != null) {
	            mCamera[i].stopPreview();
	            mPreview[i].setCamera(null);
	            mCamera[i].release();
	            mCamera[i] = null;
	        }
	    }
		super.onPause();
	}

	private void refreshGallery(File file) {
		Intent mediaScanIntent = new Intent( Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
		mediaScanIntent.setData(Uri.fromFile(file));
		sendBroadcast(mediaScanIntent);
	}

	ShutterCallback shutterCallback = new ShutterCallback() {
		public void onShutter() {
			//			 Log.d(TAG, "onShutter'd");
		}
	};

	PictureCallback rawCallback = new PictureCallback() {
		public void onPictureTaken(byte[] data, Camera camera) {
			//			 Log.d(TAG, "onPictureTaken - raw");
		}
	};
	
	private class JpegCallBack implements PictureCallback {
	    private int mCameraId;
	    
	    public JpegCallBack(int id) {
            // TODO Auto-generated constructor stub
	        mCameraId = id;
        }
	    
	    
        @Override
        public void onPictureTaken(byte[] data, Camera camera) {
            // TODO Auto-generated method stub
            new SaveImageTask(mCameraId).execute(data);
            camera.startPreview();
            captureCount--;
        }
	    
	}

	private class SaveImageTask extends AsyncTask<byte[], Void, Void> {
	    private int mCameraId;
	    
	    public SaveImageTask(int id) {
            // TODO Auto-generated constructor stub
	        mCameraId = id;
        }

		@Override
		protected Void doInBackground(byte[]... data) {
			FileOutputStream outStream = null;

			// Write to SD Card
			try {
				File sdCard = Environment.getExternalStorageDirectory();
				File dir = new File(
				        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM), this
                        .getClass().getPackage().getName());
				dir.mkdirs();				

				//String fileName = String.format("%d_%d.jpg", System.currentTimeMillis(), mCameraId);
				String fileName = new SimpleDateFormat(
	                    "'IMG"+ mCameraId + "_'" + "yyyyMMddHHmmss'.jpg'").format(new Date());
				File outFile = new File(dir, fileName);

				outStream = new FileOutputStream(outFile);
				outStream.write(data[0]);
				outStream.flush();
				outStream.close();

				Log.d(TAG, "camera " + mCameraId + " onPictureTaken - wrote bytes: " + data.length + " to " + outFile.getAbsolutePath());

				refreshGallery(outFile);
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			} finally {
			}
			return null;
		}

	}

	private final Object mStorageSpaceLock = new Object();
    private long mStorageSpaceBytes;
    private List<FileInfo> mFilelist = new ArrayList<FileInfo>();
    private static final long LOW_STORAGE_THRESHOLD_BYTES = 100000000; //100M
    protected long getStorageSpaceBytes() {
        synchronized (mStorageSpaceLock) {
            return mStorageSpaceBytes;
        }
    }

    public void getFileListByTime(String DIRECTORY) {
        mFilelist.clear();
        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return;
        }
        for (File file : dir.listFiles()) {
            mFilelist.add(new FileInfo(file));
        }
        Collections.sort(mFilelist);
    }

    public long getAvailableSpace(String DIRECTORY) {

        File dir = new File(DIRECTORY);
        dir.mkdirs();
        if (!dir.isDirectory() || !dir.canWrite()) {
            return -1;
        }

        try {
            StatFs stat = new StatFs(DIRECTORY);
            return stat.getAvailableBlocks() * (long) stat.getBlockSize();
        } catch (Exception e) {
            Log.i(TAG, "Fail to access external storage", e);
        }
        return -1;
    }
	@SuppressLint("NewApi")
    private void updateStorageSpaceAndHint(String DIRECTORY) {
        /*
         * We execute disk operations on a background thread in order to
         * free up the UI thread.  Synchronizing on the lock below ensures
         * that when getStorageSpaceBytes is called, the main thread waits
         * until this method has completed.
         *
         * However, .execute() does not ensure this execution block will be
         * run right away (.execute() schedules this AsyncTask for sometime
         * in the future. executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR)
         * tries to execute the task in parellel with other AsyncTasks, but
         * there's still no guarantee).
         * e.g. don't call this then immediately call getStorageSpaceBytes().
         * Instead, pass in an OnStorageUpdateDoneListener.
         */
        (new AsyncTask<String, Void, Long>() {
            @Override
            protected Long doInBackground(String...arg) {
                synchronized (mStorageSpaceLock) {
                    mStorageSpaceBytes = getAvailableSpace(arg[0]);
                    getFileListByTime(arg[0]);
                    return mStorageSpaceBytes;
                }
            }

            @Override
            protected void onPostExecute(Long bytes) {
                if (getStorageSpaceBytes() < LOW_STORAGE_THRESHOLD_BYTES) {
                    FileInfo file = mFilelist.get(0);
                    if (file != null)
                        file.mFile.delete();
                }FileInfo file = mFilelist.get(0);
                if (file != null)
                    Log.i(TAG, "mFilelist.get(0) = " + file.mFile.getName());
            }
        }).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, DIRECTORY);
    }

    private static class FileInfo implements Comparable<FileInfo> {
        final File mFile;

        FileInfo(File file) {
            mFile = file;
        }

        @Override
        public int compareTo(FileInfo that) {
            // for descending sort
            if (mFile.lastModified() >= that.mFile.lastModified())
                return 1;
            else
                return -1;
        }
    }
}


