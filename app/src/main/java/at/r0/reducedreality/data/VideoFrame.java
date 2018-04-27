package at.r0.reducedreality.data;

import org.opencv.core.Mat;

public class VideoFrame
{
    public Mat rgba;
    public Mat grey;
    public Orientation orientation;
    public Mat mask;
    public long frameNumber;

    /**
     * @param rgba rgba frame (i.e. CvCameraViewFrame.rgba())
     * @param grey greyscale frame (i.e. CvCameraViewFrame.gray())
     * @param orientation Orienatation of the phone at the moment of capturing the frame
     * @param mask black/white mask of regions to be replaced in the frame, null if none
     * @param frameNumber incremental frame number
     */
    public VideoFrame(Mat rgba,
                      Mat grey,
                      Orientation orientation,
                      Mat mask,
                      long frameNumber)
    {
        this.rgba = rgba;
        this.grey = grey;
        this.mask = mask;
        this.orientation = orientation;
        this.frameNumber = frameNumber;
    }

    public VideoFrame(VideoFrame other)
    {
        this(other, false);
    }

    /**
     * @param other frame to copy
     * @param makeCopy pass true to make an actual copy instead of references
     */
    public VideoFrame(VideoFrame other, boolean makeCopy)
    {
        if (!makeCopy)
        {
            this.rgba = other.rgba;
            this.grey = other.grey;
            this.orientation = other.orientation;
            this.mask = other.mask;
        }
        else
        {
            this.rgba = new Mat();
            other.rgba.copyTo(this.rgba);
            this.grey = new Mat();
            other.grey.copyTo(this.grey);
            if (other.mask != null)
            {
                this.mask = new Mat();
                other.mask.copyTo(this.mask);
            }
            this.orientation = new Orientation(other.orientation);
        }
        this.frameNumber = other.frameNumber;
    }
}
