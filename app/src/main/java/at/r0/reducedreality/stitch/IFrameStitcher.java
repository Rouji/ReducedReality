package at.r0.reducedreality.stitch;

import org.opencv.core.Mat;

import at.r0.reducedreality.data.VideoFrame;

public interface IFrameStitcher
{
    /**
     * @param base frame that has holes in to be filled with the "fill" frame
     * @param fill frame to use to do the filling in
     * @param out output image
     * @return true if stitching was successful and out holds a usable image
     */
    boolean stitch(VideoFrame base, VideoFrame fill, Mat out);
}
