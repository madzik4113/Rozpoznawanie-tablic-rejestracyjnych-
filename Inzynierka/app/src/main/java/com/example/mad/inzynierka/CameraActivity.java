package com.example.mad.inzynierka;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;


public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2{

    private static final String TAG = "CameraActivity.java";
    private static final Scalar    PLATE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private JavaCameraView cameraView;

    private Mat mRgba;
    private Mat mGray;

    private int mAbsolutePlateSize = 0;
    private float mRelativePlateSize = 0.2f;



    public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS: {
                    Log.i(TAG, "OpenCV zostal zaladowany!");
                    try {
                        //Inicjujemy && ladujemy klasifikator
                        InputStream is = getResources().openRawResource(R.raw.europe);
                        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
                        mCascadeFile = new File(cascadeDir, "europe.xml"); //wczytujemy klasyfikator z R.raw.cascade
                        FileOutputStream os = new FileOutputStream(mCascadeFile);

                        byte[] buffer = new byte[4096];
                        int bytesRead;
                        while ((bytesRead = is.read(buffer)) != -1) {
                            os.write(buffer, 0, bytesRead);
                        }
                        is.close();
                        os.close();

                        mJavaDetector = new CascadeClassifier(mCascadeFile.getAbsolutePath());
                        if (mJavaDetector.empty()) {
                            Log.e(TAG, "Blad z zaladowaniem klasyfikatora!");
                            Toast.makeText(getApplicationContext(), "Blad z zaladowaniem klasyfikatora!", Toast.LENGTH_SHORT).show();
                            mJavaDetector = null;
                        } else {
                            Log.e(TAG, "Klasyfikator zostal zaladowany prawidlowo!");
                            Toast.makeText(getApplicationContext(), "Klasyfikator zostal zaladowany prawidlowo!", Toast.LENGTH_SHORT).show();
                        }
                        cascadeDir.delete();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Nie udalo znalezc sie pliku z klasyfiaktorem!. Exception thrown: " + e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Blad z zaladowaniem klasyfikatora!. Exception thrown: " + e);
                    }
                    cameraView.enableView();
                    cameraView.enableFpsMeter();
                } break;
                default: {
                    super.onManagerConnected(status);
                }break;
            }


        }
    };

    protected void onCreate(Bundle savedInstanceState){

        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        cameraView = (JavaCameraView) findViewById(R.id.cameraView);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

    }




    @Override
    public void onCameraViewStarted(int width, int height) {
        mGray = new Mat();
        mRgba = new Mat();
    }

    @Override
    public void onCameraViewStopped() {
        mGray.release();
        mRgba.release();
    }

    public void onResume(){
        super.onResume();
        OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_0_0, this, mLoaderCallback);
    }

    public void onDestroy() {
        super.onDestroy();
        if(cameraView!=null){
            cameraView.disableView();
        }
    }

    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {

        mRgba = inputFrame.rgba();
        mGray = inputFrame.gray();

        if(mAbsolutePlateSize == 0){
            int height = mGray.rows();
            if(Math.round(height * mRelativePlateSize) > 0){
                mAbsolutePlateSize = Math.round(height * mRelativePlateSize);
            }
        }

        MatOfRect plates = new MatOfRect();

        if (mJavaDetector != null)
            mJavaDetector.detectMultiScale(
                    mGray,
                    plates,
                    1.1,
                    2,
                    2,
                    new Size(mAbsolutePlateSize, mAbsolutePlateSize),
                    new Size()
            );

        Rect[] platesArray = plates.toArray();
        for(int i=0;i<platesArray.length;i++){
            Imgproc.rectangle(mRgba, platesArray[i].tl(), platesArray[i].br(), PLATE_RECT_COLOR, 3);

        }

        return mRgba;
    }
}
