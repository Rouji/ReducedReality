package at.r0.reducedreality.mask;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.objdetect.HOGDescriptor;

import java.io.File;

import at.r0.reducedreality.data.VideoFrame;

public class HogPeopleMaskGenerator implements IReplaceMaskGenerator
{
    private HOGDescriptor hog;
    private MatOfRect foundLocations;
    private MatOfDouble foundWeights;
    private Scalar whiteScalar;

    public HogPeopleMaskGenerator()
    {
        hog = new HOGDescriptor();
        hog.setSVMDetector(HOGDescriptor.getDefaultPeopleDetector());
    }

    @Override
    public boolean generate(VideoFrame frame, Mat out)
    {
        if (whiteScalar == null) whiteScalar = new Scalar(255);
        if (foundLocations == null) foundLocations = new MatOfRect();
        if (foundWeights == null) foundWeights = new MatOfDouble();


        hog.detectMultiScale(frame.grey,
                             foundLocations,
                             foundWeights,
                             0.0,
                             new Size(8, 8),
                             new Size(8,8),
                             1.1,
                             0.0,
                             false);
        if (foundLocations.empty())
            return false;

        if (out.size().width != frame.grey.width()
                || out.size().height != frame.grey.height()
                || out.type() != CvType.CV_8U)
        {
            out.create(frame.grey.size(), CvType.CV_8UC1);
        }
        out.setTo(new Scalar(0));

        for (Rect rect : foundLocations.toArray())
        {
            Imgproc.rectangle(out, rect.tl(), rect.br(), whiteScalar, -1);
        }
        return true;
    }
}
