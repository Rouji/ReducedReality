package at.r0.reducedreality.util;


public class MathsUtil
{
    static public float squareAngleDist(float a1, float a2)
    {

        return (((a1 + 180.0f) % 360.0f) - ((a2 + 180.0f) % 360.0f)) *
               (((a1 + 180.0f) % 360.0f) - ((a2 + 180.0f) % 360.0f));
    }

    static public float orientationSquareDist(float[] or1, float[] or2)
    {
        return squareAngleDist(or1[0], or2[0]) +
               squareAngleDist(or1[1], or2[1]) +
               squareAngleDist(or1[2], or2[2]);
    }

    static public boolean anyBigger(float[] values, float[] than)
    {
        for (int i=0; i<values.length; ++i)
            if (values[i] > than[i])
                return true;
        return false;
    }

    static public float sq(float n)
    {
        return n*n;
    }

    static public float clamp(float val, float min, float max)
    {
        if (min > max)
        {
            float tmp = min;
            min = max;
            max = tmp;
        }
        if (val < min)
            return min;
        if (val > max)
            return max;
        return val;
    }

    static public int wrap(int val, int max)
    {
        while (val < 0) val += max;
        while (val > max) val -= max;
        return val;
    }
}
