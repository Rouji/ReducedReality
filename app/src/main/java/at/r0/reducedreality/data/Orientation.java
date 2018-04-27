package at.r0.reducedreality.data;

public class Orientation
{
    public float yaw;
    public float pitch;
    public float roll;

    public Orientation()
    {
        yaw = pitch = roll = 0;
    }

    public Orientation(float yaw, float pitch, float roll)
    {
        this.yaw = yaw;
        this.pitch = pitch;
        this.roll = roll;
    }

    public Orientation(Orientation other)
    {
        this.yaw = other.yaw;
        this.pitch = other.pitch;
        this.roll = other.roll;
    }
}
