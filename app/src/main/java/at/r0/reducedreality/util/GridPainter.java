package at.r0.reducedreality.util;

import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

public class GridPainter
{
    /**
     * draw a grid indicating true/false values as green/black
     * and a single highlighted field as red
     *
     * good for visualising FrameStore content
     *
     * @param fields 2d boolean array
     * @param highX x index into fields to highlight
     * @param highY x index into fields to highlight
     * @param out output image
     */
    public static void draw(boolean fields[][], int highX, int highY, Mat out)
    {
        Size cellSize = new Size(out.size().width/fields.length,
                                 out.size().height/fields[0].length);
        out.setTo(new Scalar(0));
        for (int x = 0; x < fields.length; ++x)
        {
            for (int y = 0; y < fields[0].length; ++y)
            {
                Scalar col;
                if (x == highX && y == highY)
                    col = new Scalar(255, 0, 0);
                else if (fields[x][y])
                    col = new Scalar(0, 255, 0);
                else
                    continue;

                Imgproc.rectangle(out,
                                  new Point(cellSize.width * x, cellSize.height * y),
                                  new Point(cellSize.width * (x + 1), cellSize.height * (y + 1)),
                                  col,
                                  -1);
            }
        }

        //draw grid lines
        for (int i=0; i<fields.length; ++i)
            Imgproc.line(out,
                         new Point(i*cellSize.width, 0),
                         new Point(i*cellSize.width, out.height()),
                         new Scalar(255,255,255),
                         2);
        for (int i=0; i<fields[0].length; ++i)
            Imgproc.line(out,
                         new Point(0,i*cellSize.height),
                         new Point(out.size().width, i*cellSize.height),
                         new Scalar(255,255,255),
                         2);
    }
}
