package at.r0.reducedreality.mask;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import at.r0.reducedreality.data.VideoFrame;

public class ColourMaskGenerator implements IReplaceMaskGenerator
{
    private double[] low = new double[3];
    private double[] high = new double[3];
    private Mat downscaled;
    private Mat bw;
    private int scaleFactor = 1;


    /**
     * @param lowHSV lower bound HSV values, 3 ints between 0-255
     * @param highHSV higher bound HSV values, 3 ints between 0-255
     * @param downscaleFactor factor by which to downscale input by (1 = don't scale, 2 = scale to half res, ...)
     */
    public ColourMaskGenerator(int lowHSV[], int highHSV[], int downscaleFactor)
    {
        if (downscaleFactor < 1)
            throw new IllegalArgumentException("downscaleFactor can't be smaller than 1");
        if (lowHSV == null || highHSV == null)
            throw new IllegalArgumentException("lowHSV and highHSV can't be null");
        if (lowHSV.length !=3 || highHSV.length != 3)
            throw new IllegalArgumentException("lowHSV and highHSV must be arrays of size 3");

        for (int i=0; i<3; ++i)
        {
            low[i]=lowHSV[i];
            high[i]=highHSV[i];
        }
        scaleFactor = downscaleFactor;
    }

    @Override
    public boolean generate(VideoFrame frame, Mat out)
    {
        if (downscaled == null) downscaled = new Mat();
        if (bw == null) bw = new Mat();

        Imgproc.resize(frame.rgba, downscaled, new Size(frame.rgba.width() / scaleFactor, frame.rgba.height() / scaleFactor));
        Imgproc.cvtColor(downscaled, downscaled, Imgproc.COLOR_RGB2HSV);
        Core.inRange(downscaled, new Scalar(low), new Scalar(high), downscaled);
        Imgproc.blur(downscaled, downscaled, new Size(10, 10));
        Core.inRange(downscaled, new Scalar(50), new Scalar(255), bw);
        if (Core.countNonZero(downscaled) / (downscaled.size().area()) < 0.05)
            return false;
        Imgproc.resize(bw, out, frame.rgba.size(), 0, 0, Imgproc.INTER_NEAREST);
        return true;
    }
}
