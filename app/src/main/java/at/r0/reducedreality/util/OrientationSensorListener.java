package at.r0.reducedreality.util;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.util.Log;

import at.r0.reducedreality.data.Orientation;
import at.r0.reducedreality.util.MovingWindowSmoother;

public class OrientationSensorListener implements SensorEventListener
{
    private static final String TAG = "OrientationSensorListener";
    private float[] magnet = new float[3];
    private float[] gravity = new float[3];
    private float[] rotationMat = new float[9];
    private float[] inclinationMat = new float[9];
    private float[] orientation = new float[3];
    private MovingWindowSmoother yawSmoother = new MovingWindowSmoother(100);

    /**
     * register with a SensorManager
     *
     * @param sensorManager
     */
    public void register(SensorManager sensorManager)
    {
        Sensor gravitySensor = sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
        Sensor magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
        sensorManager.registerListener(this, gravitySensor, SensorManager.SENSOR_DELAY_FASTEST);
        sensorManager.registerListener(this, magnetSensor, SensorManager.SENSOR_DELAY_FASTEST);
    }

    /**
     * @return last known orientation
     */
    public Orientation getOrientation()
    {
        return new Orientation(orientation[0], orientation[2], orientation[1]);
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i)
    {
        //TODO: not really needed?
    }

    /**
     * called, every time we get a value from sensors
     */
    @Override
    public void onSensorChanged(SensorEvent sensorEvent)
    {
        if (sensorEvent.sensor.getType() == Sensor.TYPE_GRAVITY)
        {
            System.arraycopy(sensorEvent.values, 0, gravity, 0, 3);
        }
        else
        {
            System.arraycopy(sensorEvent.values, 0, magnet, 0, 3);
        }

        if (!SensorManager.getRotationMatrix(rotationMat, inclinationMat, gravity, magnet))
            return;

        orientation = SensorManager.getOrientation(rotationMat, orientation);
        for (int i = 0; i < 3; i++)
        {
            orientation[i] = (float) Math.toDegrees(orientation[i]);
            while (orientation[i] < 0)
                orientation[i] += 360.0f;
        }

        //smooth yaw
        float oldyaw = orientation[0];
        orientation[0] = yawSmoother.smooth(orientation[0]);

        //shift pitch so 0 is up, 180 is down
        orientation[2] -= 180.0f;

        Log.d(TAG, String.format("yaw: %.2f, smoothed_yaw: %.2f, pitch: %.2f",
                                 oldyaw,
                                 orientation[0],
                                 orientation[2]));
    }
}
