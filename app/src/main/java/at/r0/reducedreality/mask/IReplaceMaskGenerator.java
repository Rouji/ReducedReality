package at.r0.reducedreality.mask;

import org.opencv.core.Mat;

import at.r0.reducedreality.data.VideoFrame;

public interface IReplaceMaskGenerator
{
    /**
     * @param frame frame to generate a mask for
     * @param out black/white output mask
     * @return true, if a mask was generated
     */
    boolean generate(VideoFrame frame, Mat out);
}
