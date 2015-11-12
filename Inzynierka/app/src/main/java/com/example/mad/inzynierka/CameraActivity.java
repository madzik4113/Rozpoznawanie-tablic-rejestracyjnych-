package com.example.mad.inzynierka;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.content.res.AssetManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.GestureDetector;
import android.view.MotionEvent;
import android.view.SurfaceView;
import android.view.WindowManager;
import android.widget.Toast;

import com.googlecode.tesseract.android.TessBaseAPI;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.JavaCameraView;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class CameraActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2, GestureDetector.OnGestureListener{

    private static final String TAG = "CameraActivity.java";
    private static final Scalar    PLATE_RECT_COLOR     = new Scalar(0, 255, 0, 255);
    private File mCascadeFile;
    private CascadeClassifier mJavaDetector;
    private JavaCameraView cameraView;

    private Mat mRgba;
    private Mat mGray;
    private Mat newPlate;
    private Size sizePlate;

    private int mAbsolutePlateSize = 0;
    private float mRelativePlateSize = 0.2f;

    private GestureDetector mGestureDetector;
    TessBaseAPI baseApi;

    public static final String DATA_PATH = Environment.getExternalStorageDirectory().toString() + "/projektInz/";
    public static final String lang = "pol";
    final String galleryPath = DATA_PATH + "/plates/";

    public BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch(status){
                case BaseLoaderCallback.SUCCESS: {
                    Log.i(TAG, "OpenCV zostal zaladowany!");
                    try {
                        //Inicjujemy && ladujemy klasifikator
                        InputStream is = getResources().openRawResource(R.raw.europe);
                        File cascadeDir = getDir("europe", Context.MODE_PRIVATE);
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


                            cameraView.enableView();
                            cameraView.enableFpsMeter();


                        }
                        cascadeDir.delete();

                    } catch (FileNotFoundException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Nie udalo znalezc sie pliku z klasyfiaktorem!. Exception thrown: " + e);
                    } catch (IOException e) {
                        e.printStackTrace();
                        Log.e(TAG, "Blad z zaladowaniem klasyfikatora!. Exception thrown: " + e);
                    }




                } break;
                default: {
                    super.onManagerConnected(status);
                }break;
            }


        }
    };

    protected void onCreate(Bundle savedInstanceState) {

        String[] paths = new String[] { DATA_PATH, DATA_PATH + "tessdata/" };

        for (String path : paths) {
            File dir = new File(path);
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    Log.v(TAG, "ERROR: Creation of directory " + path + " on sdcard failed");
                    return;
                } else {
                    Log.v(TAG, "Created directory " + path + " on sdcard");
                }
            }

        }

        if (!(new File(DATA_PATH + "tessdata/" + lang + ".traineddata")).exists()) {
            try {

                AssetManager assetManager = getAssets();
                InputStream in = assetManager.open("tessdata/" + lang + ".traineddata");
                OutputStream out = new FileOutputStream(DATA_PATH
                        + "tessdata/" + lang + ".traineddata");

                byte[] buf = new byte[1024];
                int len;
                while ((len = in.read(buf)) > 0) {
                    out.write(buf, 0, len);
                }
                in.close();
                out.close();

                Log.v(TAG, "Copied " + lang + " traineddata");
            } catch (IOException e) {
                Log.e(TAG, "Was unable to copy " + lang + " traineddata " + e.toString());
            }
        }

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
//        Intent openCamera = new Intent(CameraActivity.this, App.class);
//        startActivity(openCamera);
//        finish();
    }

    public boolean onGenericMotionEvent(MotionEvent e) {
        mGestureDetector.onTouchEvent(e);
        return true;
    }

    @Override
    public boolean onDown(MotionEvent e) {
        return false;
    }

    @Override
    public void onShowPress(MotionEvent e) {

    }

    @Override
    public boolean onSingleTapUp(MotionEvent e) {
        return false;
    }

    @Override
    public boolean onScroll(MotionEvent e1, MotionEvent e2, float distanceX, float distanceY) {
        return false;
    }

    @Override
    public void onLongPress(MotionEvent e) {

    }

    public boolean onFling(MotionEvent e1, MotionEvent e2, float velocityX, float velocityY){
        return false;
    }


    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) throws InterruptedException {

        //szukamy tablicy

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
        {
            mJavaDetector.detectMultiScale(
                    mGray,
                    plates,
                    1.1,
                    2,
                    2,
                    new Size(mAbsolutePlateSize, mAbsolutePlateSize),
                    new Size()
            );
                sizePlate = plates.size();

        }


        Rect[] platesArray = plates.toArray();


        if(platesArray.length>0){
            //takePlate(mGray);
            for(int i=0;i<platesArray.length;i++){
                int x = platesArray[i].x;
                int y = platesArray[i].y;
                int w = platesArray[i].width;
                int h = platesArray[i].height;


                Imgproc.rectangle(mRgba, platesArray[i].tl(), platesArray[i].br(), PLATE_RECT_COLOR, 3);
                Log.e(TAG, "Tablica znaleziona!");
                Imgproc.putText(mRgba, "Znalazlem tablice!", new Point(x, y - 35), Core.FONT_HERSHEY_PLAIN, 3, new Scalar(255, 0, 0), 3);


/*


                newPlate =  mGray.submat(platesArray[i]);

                //Imgproc.GaussianBlur(newPlate, newPlate,new Size(3,3),0);
                //Imgproc.threshold(newPlate, newPlate, 0, 255, Imgproc.THRESH_OTSU);
                Bitmap bmp = Bitmap.createBitmap(newPlate.cols(),newPlate.rows(), Bitmap.Config.ARGB_8888);
                Utils.matToBitmap(newPlate, bmp);

                //takePlate(newPlate);

                TessBaseAPI baseApi = new TessBaseAPI();
                baseApi.setDebug(true);
                baseApi.init(DATA_PATH, lang);
                Log.e(TAG, "Tesseract dziala!");
                String whitelist = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
                String blacklist = "!@#$%^&*()_+=-[]}{;:~`,./<>?'";
                baseApi.setVariable(baseApi.VAR_CHAR_WHITELIST,whitelist);
                baseApi.setVariable(baseApi.VAR_CHAR_BLACKLIST,blacklist);
                baseApi.setImage(bmp);
                String recognizedText = baseApi.getUTF8Text();
                Log.e(TAG, "tablica rozpoznana = " + recognizedText);
*/
            }
        }


        Imgproc.putText(mRgba, "Utrzymaj telefon w miejscu aby zlapac ostrosc", new Point(225, 30), Core.FONT_HERSHEY_PLAIN, 2, new Scalar(0, 0, 0), 3);


        return mRgba;
    }


    private void takePlate(Mat newPlate) {

        Log.d(TAG, "INSIDE TAKE PHOTO");

        final long currentTimeMillis = System.currentTimeMillis();
        //final String appName = getString(R.string.app_name);
        final String appName="Inzynierka";
        Log.d(TAG, "INSIDE TAKE PHOTO1");
        Log.d(TAG, "INSIDE TAKE PHOTO2");
        final String albumPath = galleryPath;
        final String photoPath = albumPath + "/" +currentTimeMillis + ".png";
        final ContentValues values = new ContentValues();
        Log.d(TAG, "INSIDE TAKE PHOTO3");
        values.put(MediaStore.MediaColumns.DATA, photoPath);
        values.put(MediaStore.Images.Media.TITLE, appName);
        values.put(MediaStore.Images.Media.DESCRIPTION, appName);
        values.put(MediaStore.Images.Media.DATE_TAKEN, currentTimeMillis);
        // Ensure that the album directory exists.
        File album = new File(albumPath);
        Log.d(TAG, "INSIDE TAKE PHOTO4");
        if (!album.isDirectory() && !album.mkdirs()) {
            Log.e(TAG, "Failed to create album directory at " +albumPath);
            onTakePhotoFailed();
            return;
        }
        // Try to create the photo.
        Log.d(TAG, "INSIDE TAKE PHOTO4.1(debug)");
        //Imgproc.cvtColor(mRgba, mBgr, Imgproc.COLOR_RGBA2BGR, 3);
        Log.d(TAG, "INSIDE TAKE PHOTO5");
        if (!Imgcodecs.imwrite(photoPath, newPlate)) {
            Log.e(TAG, "Failed to save photo to " + photoPath);
            onTakePhotoFailed();
        }
        Log.d(TAG, "Photo saved successfully to " + photoPath);
        // Try to insert the photo into the MediaStore.
        Uri uri;
        try {
            uri = getContentResolver().insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        } catch (final Exception e) {
            Log.e(TAG, "Failed to insert photo into MediaStore");
            e.printStackTrace();
            // Since the insertion failed, delete the photo.
            File photo = new File(photoPath);
            if (!photo.delete()) {
                Log.e(TAG, "Failed to delete non-inserted photo");
            }
            onTakePhotoFailed();
            return;
        }
        // Open the photo in LabActivity.
        Log.d(TAG, "INSIDE TAKE PHOTO6");
    }
    private void onTakePhotoFailed() {

        }
    }




