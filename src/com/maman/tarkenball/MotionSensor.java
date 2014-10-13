package com.maman.tarkenball;

import com.maman.tarkenball.utils.Config;

import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

public class MotionSensor {

	private SensorManager sensorManager;
	private Sensor accelerometer;
	private SensorEventListener accelerometerEventHandler = new AccelerometerEventHandler();
	private float[] theta = new float[3];
	private final float MOTION_SENSITIVITY = 2f;
	private float hitPower;

	public MotionSensor(Context context) {
		sensorManager = (SensorManager) context.getSystemService(Context.SENSOR_SERVICE);
		accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
		sensorManager.registerListener(accelerometerEventHandler , accelerometer, SensorManager.SENSOR_DELAY_GAME);		
	}
	public void onResume() {
		sensorManager.registerListener(accelerometerEventHandler, accelerometer, SensorManager.SENSOR_DELAY_GAME);
	}
	public void onPause() {
		// accelerometer can run down the phone battery if not managed correctly by the application
		sensorManager.unregisterListener(accelerometerEventHandler);
	}
	public float getYaw() {
		return this.theta[0] * MOTION_SENSITIVITY;
	}
	public float getPitch() {
		float pitch = Math.max(-10f, theta[1]-10f);
		pitch = Math.min(10f, pitch);
		return pitch;
	}
	public int getHitPower() {
		float power = 10f * Math.max(0, hitPower);
		if (Config.LOGCAT) {
			Log.v("RAW_HIT_POWER", Float.toString(power));
		}
		if (power > 0.5) {
			return 1;
		}
		else {
			return 0;
		}
	}

	private class AccelerometerEventHandler implements SensorEventListener {
		
	    private boolean initialized = false;
		public float[] oldAcceleration = new float[3];
		public float[] newAcceleration = new float[3];
		public float[] avgAcceleration = new float[3];

		@Override
		public void onAccuracyChanged(Sensor arg0, int arg1) {
			// TODO Auto-generated method stub
		}

		@Override
		public void onSensorChanged(SensorEvent event) {
			if (!initialized) {
				oldAcceleration[0] = oldAcceleration[1] = oldAcceleration[2] = 0f;
				avgAcceleration[0] = avgAcceleration[1] = avgAcceleration[2] = 0f;
				theta[0] = theta[1] = theta[2] = 0f;
				hitPower = 0;
				initialized = true;
				return;
			}

			for (int i = 0; i < 2; i++) {
				// keep values within the range of -10 to +10, so that we don't end up taking the arcsin of -1.1 or 1.1 and get NaN
				newAcceleration[i] = event.values[i];
				newAcceleration[i] = Math.max(Math.min(newAcceleration[i], 10f), -10f);
				avgAcceleration[i] = (0.75f * oldAcceleration[i]) + (0.25f * newAcceleration[i]);
				oldAcceleration[i] = avgAcceleration[i];
				theta[i] = (float)Math.asin(avgAcceleration[i]/10f);
				theta[i] *= 180f/Math.PI;
			}
			hitPower = newAcceleration[1] - avgAcceleration[1];
		}
	
	}// end class GyroscopeEventHandler
}
