package com.example.mad.inzynierka;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.Toast;

import org.opencv.android.OpenCVLoader;

public class App extends Activity {

    private ImageButton cameraBtn;
    private ImageButton setBtn;
    private ImageButton albumBtn;
    private ImageButton closeBtn;

    private boolean enable = false;



    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_app);

        //setting button action
        setBtn = (ImageButton) findViewById(R.id.setBtn);
        setBtn.setOnClickListener(new View.OnClickListener() {
            @Override

            public void onClick(View v) {
                if (!OpenCVLoader.initDebug()) {
                    Toast.makeText(getApplicationContext(), "OpenCV fail!", Toast.LENGTH_SHORT).show();
                    enable = false;
                } else if (!isDeviceSupportCamera()) {
                    Toast.makeText(getApplicationContext(), "Nie posiadasz kamery!", Toast.LENGTH_SHORT).show();
                    enable = false;
                } else if (OpenCVLoader.initDebug() && isDeviceSupportCamera()) {
                    Toast.makeText(getApplicationContext(), "Ustawienia sprawdzone. Mozesz korzystac z aplikacji swobodnie!", Toast.LENGTH_LONG).show();
                    enable = true;
                }
            }
        });


        //camera button action and opencv library tes
        cameraBtn = (ImageButton) findViewById(R.id.cameraBtn);
        cameraBtn.setOnClickListener((new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(getApplicationContext(), "Tutaj bedzie caly algorytm!", Toast.LENGTH_SHORT).show();
                //uruchamiamy klase kamery z opencv
                if(enable){
                    Intent openCamera = new Intent(App.this, CameraActivity.class);
                    startActivity(openCamera);
                    finish();
                }
                else{
                    Toast.makeText(getApplicationContext(), "Blokada! Zajrzyj do ustawien!", Toast.LENGTH_SHORT).show();
                }

            }
        }));

//album button action
        albumBtn = (ImageButton) findViewById(R.id.albumBtn);
        albumBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Album", Toast.LENGTH_SHORT).show();
            }
        });
//close button action
        closeBtn = (ImageButton) findViewById(R.id.closeBtn);
        closeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(),"Exit ...",Toast.LENGTH_SHORT).show();
                System.exit(0);
            }
        });
    }

//funkcja sprawdzajaca czy sprzet ma kamere

    private boolean isDeviceSupportCamera() {
        if (getApplicationContext().getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_CAMERA)) {
            // jest kamera w urzadzeniu
            return true;
        } else {
            // nie ma kamery
            return false;
        }
    }


}
