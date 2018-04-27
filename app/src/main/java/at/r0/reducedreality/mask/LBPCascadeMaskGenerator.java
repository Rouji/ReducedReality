package at.r0.reducedreality.mask;

import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import java.io.File;

import at.r0.reducedreality.data.VideoFrame;

public class LBPCascadeMaskGenerator implements IReplaceMaskGenerator
{
    private CascadeClassifier classifier;
    private MatOfRect faces;
    private Scalar whiteScalar;

    public LBPCascadeMaskGenerator(File lbpcascadeFile)
    {
        classifier = new CascadeClassifier(lbpcascadeFile.getAbsolutePath());
        if (classifier.empty())
            throw new IllegalArgumentException("cascade file could not be loaded, probably invalid file");
    }

    @Override
    public boolean generate(VideoFrame frame, Mat out)
    {
        if (whiteScalar == null) whiteScalar = new Scalar(255);
        if (faces == null) faces = new MatOfRect();


        classifier.detectMultiScale(frame.grey,
                                    faces,
                                    1.1,
                                    2,
                                    2,
                                    new Size((int)frame.grey.rows()*0.2,
                                             (int)frame.grey.rows()*0.2),
                                    new Size());
        if (faces.empty())
            return false;

        if (out.size().width != frame.grey.width()
                || out.size().height != frame.grey.height()
                || out.type() != CvType.CV_8U)
        {
            out.create(frame.grey.size(), CvType.CV_8UC1);
        }
        out.setTo(new Scalar(0));
        for (Rect rect : faces.toArray())
        {
            Imgproc.circle(out,
                           rectCenter(rect),
                           rectToRadius(rect),
                           new Scalar(255),
                           -1);
            //Imgproc.rectangle(out, rect.tl(), rect.br(), whiteScalar, -1);
        }
        return true;
    }

    private int rectToRadius(Rect rect)
    {
        return (rect.width + rect.height)/2;
    }

    private Point rectCenter(Rect rect)
    {
        return new Point(rect.x + rect.width/2, rect.y + rect.height/2);
    }
}
