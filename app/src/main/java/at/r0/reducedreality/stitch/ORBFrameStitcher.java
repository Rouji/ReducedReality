package at.r0.reducedreality.stitch;

import android.util.Log;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.DMatch;
import org.opencv.core.KeyPoint;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDMatch;
import org.opencv.core.MatOfKeyPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.features2d.DescriptorExtractor;
import org.opencv.features2d.DescriptorMatcher;
import org.opencv.features2d.FeatureDetector;
import org.opencv.imgproc.Imgproc;
import org.opencv.video.Video;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import at.r0.reducedreality.data.VideoFrame;
import at.r0.reducedreality.util.NanoTimer;

public class ORBFrameStitcher implements IFrameStitcher
{
    private static final String TAG="ORBFrameStitcher";
    private float fovX, fovY;
    private Scalar zeroScalar;
    private Mat inside;
    private Mat outside;
    private Mat invMask;
    private Mat translated;
    private Mat translatedGrey;
    private boolean useFeatures;

    /**
     * @param fovX horizontal FOV of camera/frames
     * @param fovY vertical FOV of camera/frames
     * @param useFeatures use feature matching
     */
    public ORBFrameStitcher(float fovX, float fovY, boolean useFeatures)
    {
        this.fovX = fovX;
        this.fovY = fovY;
        this.useFeatures = useFeatures;
    }

    @Override
    public boolean stitch(VideoFrame base, VideoFrame fill, Mat out)
    {
        if (base.rgba == null || base.mask == null || fill.rgba == null || out == null)
            throw new IllegalArgumentException("base.rgba, base.mask, fill.rgba and out cannot be null");

        NanoTimer timer = new NanoTimer();

        if (zeroScalar == null) zeroScalar = new Scalar(0);
        if (inside == null) inside = new Mat();
        if (outside == null) outside = new Mat();
        if (invMask == null) invMask = new Mat();
        if (translated == null) translated = new Mat();
        if (translatedGrey == null) translatedGrey = new Mat();

        timer.start();
        Core.bitwise_not(base.mask, invMask); //invert mask

        //translate fill to match base as closely as possible
        Mat mv = translationByOrientation(base, fill);
        Imgproc.warpAffine(fill.rgba, translated, mv, base.rgba.size());
        Imgproc.warpAffine(fill.grey, translatedGrey, mv, base.grey.size());
        Log.i(TAG, String.format("angle based translation took %.2fms", timer.stopMS()));

        //feature match to make overlap better
        if (useFeatures)
        {
            timer.start();
            mv = transformByFeatures(base.grey, translatedGrey);
            if (mv != null)
                Imgproc.warpAffine(translated, translated, mv, translated.size());
            Log.i(TAG, String.format("transform by features took %.2fms", timer.stopMS()));
        }

        //blend both images together
        timer.start();
        Core.bitwise_and(translated, translated, inside, base.mask); //cut out region to fill with
        Core.bitwise_and(base.rgba, base.rgba, outside, invMask); //cut hole into base
        //Core.add(inside, outside, out); // segfaults under ALL possible circumstances
        Core.addWeighted(inside, 1.0, outside, 1.0, 0, out);
        Log.i(TAG, String.format("blending took %.2fms", timer.stopMS()));


        //clear inside/outside to black
        //to not retain any artifacts from earlier frames
        inside.setTo(zeroScalar);
        outside.setTo(zeroScalar);
        return true;
    }

    private FeatureDetector featureDetector;
    private DescriptorExtractor descriptorExtractor;
    private DescriptorMatcher descriptorMatcher;
    /**
     * transformation matrix to match two greyscale images using ORB
     * @return null if no match found
     */
    private Mat transformByFeatures(Mat baseGrey, Mat fillGrey)
    {
        MatOfKeyPoint key1 = new MatOfKeyPoint();
        MatOfKeyPoint key2 = new MatOfKeyPoint();
        Mat des1 = new Mat();
        Mat des2 = new Mat();
        MatOfDMatch matches = new MatOfDMatch();

        NanoTimer timer = new NanoTimer();

        timer.start();
        if (featureDetector == null)
            featureDetector = FeatureDetector.create(FeatureDetector.ORB);
        featureDetector.detect(baseGrey, key1, invMask);
        featureDetector.detect(fillGrey, key2, invMask);
        Log.i(TAG, String.format("feature detector took %.2fms", timer.stopMS()));


        if (key1.empty() || key2.empty())
            return null;

        timer.start();
        if (descriptorExtractor == null)
            descriptorExtractor = DescriptorExtractor.create(DescriptorExtractor.ORB);
        descriptorExtractor.compute(baseGrey, key1, des1);
        descriptorExtractor.compute(fillGrey, key2, des2);
        if (des1.empty() || des2.empty())
            return null;
        Log.i(TAG, String.format("descriptor extractor took %.2fms", timer.stopMS()));


        timer.start();
        if (descriptorMatcher == null)
            descriptorMatcher = DescriptorMatcher.create(DescriptorMatcher.BRUTEFORCE_HAMMING);
        descriptorMatcher.match(des1, des2, matches);
        Log.i(TAG, String.format("descriptor matcher took %.2fms", timer.stopMS()));


        timer.start();
        //max distance we allow for matches, roughly 3% of the length of one side of the image
        double maxDist = ((baseGrey.size().width + baseGrey.size().height) / 2) / 30;

        //find smallest distance among matches
        DMatch smallest = null;
        List<DMatch> matchList = matches.toList();
        List<DMatch> goodMatches = new LinkedList<>();
        for (DMatch m : matchList)
        {
            if (smallest == null || (smallest.distance < m.distance && smallest.distance < maxDist))
                smallest = m;
        }
        if (smallest == null)
            return null;

        //only consider matches with distance within 2x smallest dist
        for (DMatch m : matchList)
        {
            if (m.distance <= smallest.distance * 2)
                goodMatches.add(m);
        }
        if (goodMatches.size() < 5)
            return null;

        //limit max. amount of matches
        if (goodMatches.size() > 40)
        {
            //sort so we get the ones with smallest distances
            goodMatches.sort((dMatch, t1) -> (int) Math.abs((dMatch.distance - t1.distance) * 100.0));
            goodMatches = goodMatches.subList(0,40);
        }
        Log.i(TAG, String.format("filtering good matches took %.2fms", timer.stopMS()));


        timer.start();
        Mat mv = transformByGoodMatches(goodMatches, key1, key2);
        if (mv.empty())
            return null;
        Log.i(TAG, String.format("transformByGoodMatches took %.2fms", timer.stopMS()));

        return mv;
    }

    /**
     * calculate transformation matrix by a list of good matches (i.e. from a DescriptorMatcher)
     * @param matches list of matches
     * @param keyPointBase keypoints of base frame
     * @param keyPointFill keypoints of fill frame
     * @return transformation matrix
     */
    private Mat transformByGoodMatches(List<DMatch> matches, MatOfKeyPoint keyPointBase, MatOfKeyPoint keyPointFill)
    {
        MatOfPoint2f base = new MatOfPoint2f();
        MatOfPoint2f fill = new MatOfPoint2f();

        List<KeyPoint> kpBase = keyPointBase.toList();
        List<KeyPoint> kpFill = keyPointFill.toList();
        List<Point> ptBase = new ArrayList<>(matches.size());
        List<Point> ptFill = new ArrayList<>(matches.size());
        for (DMatch m : matches)
        {
            ptBase.add(kpBase.get(m.queryIdx).pt);
            ptFill.add(kpFill.get(m.trainIdx).pt);
        }
        base.fromList(ptBase);
        fill.fromList(ptFill);

        return Video.estimateRigidTransform(fill, base, false);
    }

    /**
     * calculate translation matrix by FOV angles, frame orientations and frame sizes
     */
    private Mat translationByOrientation(VideoFrame base, VideoFrame fill)
    {
        Size frameSize = base.rgba.size();
        //simple translation matrix
        Mat mv = new Mat(2, 3, CvType.CV_32FC1, zeroScalar);
        mv.put(0,0,1);
        mv.put(1,1,1);
        mv.put(0,2, correctForFOV((float)frameSize.width, fovX, fill.orientation.yaw, base.orientation.yaw));
        mv.put(1,2, correctForFOV((float)frameSize.height, fovY, fill.orientation.pitch, base.orientation.pitch));
        return mv;
    }

    /**
     * calc. the amount of pixels to shift something in a frame based on two angles and FOV
     * works on simple percentages, no fancy distortion correction or anything
     * @param frameSize frame size in pixels (either width or height)
     * @param fov FOV corresponding to the frameSize
     * @param angle1 first angle
     * @param angle2 second angle
     * @return number of pixels to shift by
     */
    private static float correctForFOV(float frameSize, float fov, float angle1, float angle2)
    {
        return (frameSize/fov) * (angle1 - angle2) * (1 + (Math.abs(angle1-angle2) / fov));
    }
}
