package at.r0.reducedreality.data;

import android.util.Log;

import at.r0.reducedreality.util.MathsUtil;

public class FrameStore
{
    private final static String TAG = "FrameStore";

    /**
     * policy to decide whether or not to replace an existing old frame
     */
    public interface IReplacementPolicy
    {
        /**
         * @param oldFrame frame to be replaced by newFrame. may be null
         * @param newFrame new frame to be stored
         * @return true, if frame should be stored
         */
        boolean shouldReplace(VideoFrame oldFrame, VideoFrame newFrame);
    }

    private float cellSize;
    private float pitchMargin;
    private IReplacementPolicy policy;
    private VideoFrame frames[][];
    private int size = 0;

    /**
     * @param cellSize    cell size in degrees of pitch/yaw
     * @param pitchMargin margin at the top/bottom of pitch to ignore
     * @param policy      replacement policy for new frames
     */
    public FrameStore(float cellSize, float pitchMargin, IReplacementPolicy policy)
    {
        this.cellSize = cellSize;
        this.policy = policy;
        this.pitchMargin = pitchMargin;
        createFrameArray();
    }

    /**
     * initialise the 2d VideoFrame array
     */
    private void createFrameArray()
    {
        //360 degrees yaw
        //180 degrees pitch, minus margin at top AND bottom
        frames = new VideoFrame[(int) Math.ceil((360 / cellSize))]
                [(int) Math.ceil((180 - (2 * pitchMargin)) / cellSize)];
    }

    /**
     * get index into stored frames array by orientation
     */
    public int[] indexByOrientation(Orientation or)
    {
        return new int[]{(int) Math.round(or.yaw / cellSize) % frames.length,
                (int) Math.round(MathsUtil.clamp(or.pitch - pitchMargin, pitchMargin,
                                                 180 - pitchMargin) / cellSize)};
    }

    /**
     * insert or replace a new frame into the stor
     * IF appropriate. See IReplacementPolicy
     *
     * @param newFrame
     * @return true, if the frame was stored
     */
    public boolean replace(VideoFrame newFrame)
    {
        if (newFrame == null)
            throw new IllegalArgumentException("newFrame cannot be null");
        if (policy == null)
        {
            Log.w(TAG,"replace() called without having a policy set");
            return false;
        }
        int idx[] = indexByOrientation(newFrame.orientation);
        if (idx == null
                || idx[0] >= frames.length
                || idx[1] >= frames[0].length
                || idx[0] < 0
                || idx[1] < 0)
        {
            return false;
        }

        VideoFrame old = frames[idx[0]][idx[1]];
        if (!policy.shouldReplace(old, newFrame))
            return false;

        //update size
        if (old == null)
            ++size;

        frames[idx[0]][idx[1]] = new VideoFrame(newFrame, true);

        Log.d(TAG, String.format("stored frame nr %d in field %d,%d",
                                 newFrame.frameNumber,
                                 idx[0],
                                 idx[1]));
        return true;
    }

    /**
     * get frame from array while wrapping yaw values that are too large or negative
     *
     * @param yaw   yaw index into frames array
     * @param pitch pitch index into frames array
     * @return sored frame or null
     */
    private VideoFrame frameAt(int yaw, int pitch)
    {
        //return null if pitch is out of bounds
        if (pitch < 0 || pitch >= frames[0].length)
            return null;
        while (yaw < 0) yaw += frames.length;
        return frames[yaw % frames.length][pitch];
    }

    private VideoFrame frameAt(int[] or)
    {
        if (or == null)
            return null;
        return frameAt(or[0], or[1]);
    }

    /**
     * get the nearest stored frame to a given orientation
     */
    public VideoFrame getNearest(Orientation or)
    {
        //try the exact cell matching the orientation first
        int startIdx[] = indexByOrientation(or);
        VideoFrame f = frameAt(startIdx);

        if (f == null)
        {
            //search for frames outwards from startIdx in rings
            search:
            {
                for (int radius = 1; radius < (frames.length / 2); ++radius)
                {
                    for (int i = -radius; i <= radius; ++i)
                    {
                        f = frameAt(startIdx[0] + i, startIdx[1] - radius);
                        if (f != null)
                            break search;
                        f = frameAt(startIdx[0] + i, startIdx[1] + radius);
                        if (f != null)
                            break search;
                    }
                    for (int i = -(radius - 1); i <= (radius - 1); ++i)
                    {
                        f = frameAt(startIdx[0] - radius, startIdx[1] + i);
                        if (f != null)
                            break search;
                        f = frameAt(startIdx[0] + radius, startIdx[1] + i);
                        if (f != null)
                            break search;
                    }
                }
            }
        }
        if (f != null)
        {
            Log.d(TAG, String.format("searched from %.2f,%.2f found frame at %.2f,%.2f",
                                     or.yaw,
                                     or.pitch,
                                     f.orientation.yaw,
                                     f.orientation.pitch));
        }
        return f;
    }

    public boolean[][] getOccupiedFields()
    {
        boolean fields[][] = new boolean[frames.length][frames[0].length];
        for (int x = 0; x < fields.length; ++x)
            for (int y = 0; y < fields[0].length; ++y)
                fields[x][y] = frames[x][y] != null;
        return fields;
    }

    public void clear()
    {
        frames = new VideoFrame[frames.length][frames[0].length];
        size = 0;
    }

    public int getSize()
    {
        return size;
    }

    public int getCapacity()
    {
        return frames.length * frames[0].length;
    }
}
