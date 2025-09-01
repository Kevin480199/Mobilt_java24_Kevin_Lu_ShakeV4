package com.example.mobiltjava24kevinlushake4;

import static android.content.ContentValues.TAG;

import static java.lang.Math.abs;

import android.content.Context;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity implements SensorEventListener{

    private ToggleButton toggleBarometer;
    private TextView textpressure;
    private Sensor pressureSensor;
    private SensorManager sensorManager;
    private Sensor currentSensor = null;
    private Sensor rotationSensor;
    ImageView compass;
    private int currentSensorType = -1; // Track active sensor type
    private boolean isSensorRegistered = false;

    private Button linearAccButton;
    private Button rotationVectorButton;
    private Button gyroscopeButton;

    private long lastUpdateTime = 0;
    private RadioGroup sensorRadioGroup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {


            sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);

            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationSensor != null) {
                sensorManager.registerListener(this, rotationSensor, SensorManager.SENSOR_DELAY_UI);
            }
            compass = findViewById(R.id.compass);
            toggleBarometer = findViewById(R.id.toggleBarometer);
            textpressure = findViewById(R.id.textPressure);

            linearAccButton = findViewById(R.id.button_linear);
            rotationVectorButton = findViewById(R.id.button_rotation);
            gyroscopeButton = findViewById(R.id.button_gyro);

            linearAccButton.setOnClickListener(view -> activateSensor(Sensor.TYPE_ACCELEROMETER));
            rotationVectorButton.setOnClickListener(view -> activateSensor(Sensor.TYPE_ROTATION_VECTOR));
            gyroscopeButton.setOnClickListener(view -> activateSensor(Sensor.TYPE_GYROSCOPE));

            sensorRadioGroup = findViewById(R.id.radioGroup);
            sensorRadioGroup.setOnCheckedChangeListener((group, checkedId) -> {
                if (checkedId == R.id.radioAccelerometer) {
                    activateSensor(Sensor.TYPE_ACCELEROMETER);
                } else if (checkedId == R.id.radioRotation) {
                    activateSensor(Sensor.TYPE_ROTATION_VECTOR);
                } else if (checkedId == R.id.radioGyroscope) {
                    activateSensor(Sensor.TYPE_GYROSCOPE);
                }
            });

            toggleBarometer.setOnCheckedChangeListener((buttonView, isChecked) -> {
                if (isChecked) {
                    // Try to activate pressure sensor
                    pressureSensor = sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE);
                    if (pressureSensor != null) {
                        sensorManager.registerListener(this, pressureSensor, SensorManager.SENSOR_DELAY_NORMAL);
                        textpressure.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, "Pressure Sensor not available", Toast.LENGTH_SHORT).show();
                        toggleBarometer.setChecked(false);
                    }
                } else {
                    // Deactivate
                    if (pressureSensor != null) {
                        sensorManager.unregisterListener(this, pressureSensor);
                        pressureSensor = null;
                    }
                    textpressure.setVisibility(View.GONE);
                }
            });


            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });


    }
    private void activateSensor(int sensorType) {
        if (sensorManager == null) return;

        // Unregister current sensor if one is active
        if (isSensorRegistered) {
            sensorManager.unregisterListener(this);
            isSensorRegistered = false;
            currentSensor = null;
            currentSensorType = -1;
        }

        // Get the new sensor
        Sensor newSensor = sensorManager.getDefaultSensor(sensorType);

        if (newSensor != null) {
            sensorManager.registerListener(this, newSensor, SensorManager.SENSOR_DELAY_NORMAL);
            currentSensor = newSensor;
            currentSensorType = sensorType;
            isSensorRegistered = true;
            Toast.makeText(this, "Sensor Activated: " + sensorTypeToString(sensorType), Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(this, "Sensor not available", Toast.LENGTH_SHORT).show();
        }
    }

    private String sensorTypeToString(int type) {
        switch (type) {
            case Sensor.TYPE_ACCELEROMETER: return "TYPE_ACCELEROMETER";
            case Sensor.TYPE_ROTATION_VECTOR: return "Rotation Vector";
            case Sensor.TYPE_GYROSCOPE: return "Gyroscope";
            default: return "Unknown";
        }
    }
    @Override
    public void onSensorChanged(SensorEvent event) {

        if (event.sensor.getType() == currentSensorType) {
            long currentTime = System.currentTimeMillis();
            float[] values = event.values;
            if (currentTime - lastUpdateTime > 200) { // log only every 200ms
                lastUpdateTime = currentTime;
                if(values[0] > abs(10) || values[1] > 10 || values[2] > 20){
                    Log.w("Overloaded", "X: " + values[0] + " Y: " + values[1] + " Z: " + values[2]);
                }else{
                    Log.d("SensorData", sensorTypeToString(currentSensorType) +
                            ": X=" + values[0] +
                            ", Y=" + values[1] +
                            ", Z=" + values[2]);
                    View mainLayout = findViewById(R.id.main);
                    int red = (int) Math.min(abs(values[0]) * 200, 255);
                    int green = (int) Math.min(abs(values[1]) * 200, 255);
                    int blue = (int) Math.min(abs(values[2]) * 200, 255);
                    mainLayout.setBackgroundColor(Color.rgb(red, green, blue));
                }

            }
        }

        if (event.sensor.getType() == Sensor.TYPE_ROTATION_VECTOR) {
            float[] rotationMatrix = new float[9];
            SensorManager.getRotationMatrixFromVector(rotationMatrix, event.values);

            float[] orientationAngles = new float[3];
            SensorManager.getOrientation(rotationMatrix, orientationAngles);

            float azimuth = (float) Math.toDegrees(orientationAngles[0]); // Rotation around Z axis

            compass.setRotation(-azimuth); // Rotate the imageView
        }

        if (event.sensor.getType() == Sensor.TYPE_PRESSURE) {
            float pressure = event.values[0];
            textpressure.setText("Pressure: " + pressure + "hPa");
        }

    }
    @Override
    protected void onPause() {
        super.onPause();
        if (sensorManager != null && isSensorRegistered) {
            sensorManager.unregisterListener(this);
            isSensorRegistered = false;
            currentSensorType = -1;
        }

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {}

}