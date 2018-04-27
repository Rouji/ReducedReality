package at.r0.reducedreality.util;

public class NanoTimer
{
    private long start = -1;

    public NanoTimer()
    {
        start();
    }

    /**
     * (re)start the timer
     */
    public void start()
    {
        start = System.nanoTime();
    }

    /**
     * @return time in nanoseconds since start() was called
     */
    public long stop()
    {
        return System.nanoTime() - start;
    }

    /**
     * @return time in milliseconds since start() was called
     */
    public float stopMS()
    {
        return stop()/1000000.0f;
    }
}
