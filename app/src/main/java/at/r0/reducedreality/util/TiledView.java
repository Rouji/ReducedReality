package at.r0.reducedreality.util;

import android.util.Log;

import org.opencv.core.Mat;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class TiledView
{
    private final static String TAG = "TiledView";
    private Mat tmp;
    private Scalar zeroScalar;

    public TiledView()
    {
    }

    public void createSplitView(Mat out, int rows, int cols, Mat... in)
    {
        if (zeroScalar == null)
            zeroScalar = new Scalar(0);
        if (tmp == null)
            tmp = new Mat();

        out.setTo(zeroScalar);
        Size tileSize = new Size(
                out.size().width / cols,
                out.size().height / rows);
        int i = 0;
        for (Mat m : in)
        {
            if (i >= rows * cols)
            {
                Log.w(TAG, String.format("Got %d input images while expecting %d (%d*%d)at max",
                                         in.length,
                                         rows * cols,
                                         rows,
                                         cols));
                break;
            }

            Rect rect = new Rect((int) ((i % cols) * tileSize.width),
                                 (int) ((i / cols) * tileSize.height),
                                 (int) tileSize.width,
                                 (int) tileSize.height);
            if (m == null)
            {
                Imgproc.rectangle(out, rect.tl(), rect.br(), zeroScalar, -1);
            }
            else
            {
                Mat roi = new Mat(out, rect);

                if (m.type() != out.type())
                {
                    m.convertTo(tmp, out.type());
                    Imgproc.resize(tmp, roi, roi.size());
                }
                else
                {
                    Imgproc.resize(m, roi, roi.size());
                }
            }
            ++i;
        }
    }
}
