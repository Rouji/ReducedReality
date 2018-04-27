package at.r0.reducedreality.util;

/**
 * keeps track of the last N values of a series
 * and calculates their average
 */
public class MovingWindowSmoother
{
    private float[] window;
    private int windowSize;
    private int currIdx;

    public MovingWindowSmoother(int windowSize)
    {
        setWindowSize(windowSize);
    }

    public void setWindowSize(int windowSize)
    {
        this.windowSize = windowSize;
        this.window = new float[windowSize];
        this.currIdx = 0;
    }

    public float smooth(float val)
    {
        float sum = 0;
        float top = (currIdx < windowSize ? currIdx : windowSize);
        window[currIdx%windowSize] = val;
        for (int i=0; i < top; ++i)
            sum += window[i];
        ++currIdx;
        return sum/top;
    }

}
