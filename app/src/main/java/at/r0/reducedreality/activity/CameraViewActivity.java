package at.r0.reducedreality.activity;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.ToggleButton;

import java.io.File;
import java.io.IOException;
import java.util.LinkedList;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.CvType;
import org.opencv.core.Mat;

import at.r0.reducedreality.data.FrameNumberReplacementPolicy;
import at.r0.reducedreality.mask.HogPeopleMaskGenerator;
import at.r0.reducedreality.mask.LBPCascadeMaskGenerator;
import at.r0.reducedreality.stitch.IFrameStitcher;
import at.r0.reducedreality.stitch.ORBFrameStitcher;
import at.r0.reducedreality.util.GridPainter;
import at.r0.reducedreality.util.SplitView;
import at.r0.reducedreality.util.OrientationSensorListener;
import at.r0.reducedreality.R;
import at.r0.reducedreality.data.FrameStore;
import at.r0.reducedreality.data.Orientation;
import at.r0.reducedreality.data.VideoFrame;
import at.r0.reducedreality.mask.ColourMaskGenerator;
import at.r0.reducedreality.mask.IReplaceMaskGenerator;
import at.r0.reducedreality.util.FileUtil;
import at.r0.reducedreality.util.NanoTimer;

public class CameraViewActivity extends Activity implements CameraBridgeViewBase.CvCameraViewListener2
{
    private static final String TAG = "Main";
    private static final String TAG_PERF = "Perf";
    private CameraBridgeViewBase cameraView;
    private LinkedList<VideoFrame> frames = new LinkedList<>();
    private long frameNr = 0;
    private IReplaceMaskGenerator maskGen;
    private OrientationSensorListener orientation = new OrientationSensorListener();
    //TODO: dynamically get FOV values?
    private IFrameStitcher stitcher;
    private File cascadeFile;
    private FrameStore frameStore;
    private SharedPreferences pref;
    private boolean storeFrames = true;
    private Handler mainHandler;
    private boolean splitView = false;

    BaseLoaderCallback loaderCallback = new BaseLoaderCallback(this)
    {
        @Override
        public void onManagerConnected(int status)
        {
            if (status == LoaderCallbackInterface.SUCCESS)
            {
                cameraView.setMaxFrameSize(getInt("res_hor", "1280"),
                                           getInt("res_vert", "720"));
                cameraView.enableView();
            }
            else
            {
                super.onManagerConnected(status);
            }
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);

        //make app fullscreen and keep screen on
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                             WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        setContentView(R.layout.activity_main);

        mainHandler = new Handler(getMainLooper());

        pref = PreferenceManager.getDefaultSharedPreferences(this);

        try
        {
            cascadeFile = FileUtil.resToFile(this, R.raw.lbpcascade_people);
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }


        cameraView = (CameraBridgeViewBase) findViewById(R.id.surface_view);
        cameraView.setVisibility(SurfaceView.VISIBLE);
        cameraView.setCvCameraViewListener(this);

        orientation.register((SensorManager) getSystemService(SENSOR_SERVICE));
    }

    @Override
    public void onPause()
    {
        super.onPause();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onDestroy()
    {
        super.onDestroy();
        if (cameraView != null)
            cameraView.disableView();
    }

    @Override
    public void onResume()
    {
        super.onResume();
        if (!OpenCVLoader.initDebug())
        {
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION_3_3_0,
                                   this,
                                   loaderCallback);
        }
        else
        {
            loaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }

        frameStore = new FrameStore(getInt("cell_size", "5"),
                                    10,
                                    new FrameNumberReplacementPolicy(getInt("frame_dist", "30")));

        findViewById(R.id.label_status)
                .setVisibility(pref.getBoolean("statusbar", true) ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onCameraViewStarted(int width, int height)
    {
        if (maskThread != null)
        {
            maskThread.stopThread();
            maskThread = null;
        }

        cameraView.setMaxFrameSize(getInt("res_hor", "1280"),
                                   getInt("res_vert", "720"));

        String type = pref.getString("maskgen_type", "mask_lbp");
        if (type.equals("mask_colour"))
        {
            int low[] = new int[]{
                    getInt("low_h", "30"),
                    getInt("low_s", "60"),
                    getInt("low_v", "80")
            };
            int high[] = new int[]{
                    getInt("high_h", "80"),
                    getInt("high_s", "255"),
                    getInt("high_v", "255")
            };
            maskGen = new ColourMaskGenerator(low,
                                              high,
                                              getInt("downscale_factor", "4"));
        }
        else if (type.equals("mask_lbp"))
        {
            maskGen = new LBPCascadeMaskGenerator(cascadeFile);
        }
        else if (type.equals("mask_hog"))
        {
            maskGen = new HogPeopleMaskGenerator();
        }
        else
            throw new RuntimeException("unknown mask generator: " + type);

        stitcher = new ORBFrameStitcher(70, 50, getFloat("orb_scale", "1.0"),true);
    }

    @Override
    public void onCameraViewStopped()
    {
    }

    private Mat tmp;
    private Mat mask;
    private SplitView split;
    private Mat splitMat;
    private Mat grid;
    private NanoTimer frameTime = new NanoTimer();
    private MaskThread maskThread;
    @Override
    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame)
    {
        Mat out;
        float msGenerateMask = 0;
        float msStitch = 0;
        float msSearch = 0;

        if (split == null) split = new SplitView();
        if (tmp == null) tmp = new Mat();
        if (grid == null) grid = new Mat(inputFrame.rgba().size(), CvType.CV_8UC4);
        if (splitMat == null) splitMat = new Mat(inputFrame.rgba().size(), inputFrame.rgba().type());
        if (mask == null) mask = new Mat();
        if (maskThread == null) { maskThread = new MaskThread(mask, maskGen); maskThread.start(); }

        frameTime.start();

        VideoFrame f = new VideoFrame(inputFrame.rgba(),
                                      inputFrame.gray(),
                                      new Orientation(orientation.getOrientation()),
                                      null,
                                      frameNr);
        maskThread.input(f);

        NanoTimer t = new NanoTimer();
        t.start();
        mask = maskThread.getMask();
        if (mask != null)
        //if (maskGen.generate(f, mask))
            f.mask = mask;
        msGenerateMask = t.stopMS();
        Log.i(TAG_PERF, String.format("generate() took %.2fms", msGenerateMask));

        VideoFrame nearest = null;
        if (f.mask == null)
        {
            if (storeFrames)
                frameStore.replace(f);
            out = inputFrame.rgba();
        }
        else
        {
            Log.i(TAG_PERF, "store size: " + frameStore.getSize());

            t.start();
            nearest = frameStore.getNearest(f.orientation);
            msSearch = t.stopMS();
            if (nearest == null)
            {
                out = f.rgba;
            }
            else
            {
                Log.i(TAG, "filling frame " + f.frameNumber + " with " + nearest.frameNumber);
                t.start();
                stitcher.stitch(f, nearest, tmp);
                msStitch = t.stopMS();
                Log.i(TAG_PERF, String.format("stitch() took %.2fms", msStitch));
                //inputFrame.gray().copyTo(tmp);
                //Features2d.drawKeypoints(tmp, stitcher.key1, tmp);
                out = tmp;
            }
        }

        if (splitView)
        {
            int high[] = frameStore.indexByOrientation(orientation.getOrientation());
            GridPainter.draw(frameStore.getOccupiedFields(), high[0], high[1], grid);
            split.createSplitView(splitMat,
                                  2,
                                  2,
                                  mask,
                                  nearest == null ? null : nearest.rgba,
                                  out,
                                  grid);
            out = splitMat;
        }

        //update status bar
        float ft = frameTime.stopMS();
        if (frameNr % 10 == 0)
        {
            String status = String
                    .format("processing %.2fms (%.2ffps) | obj.det. %.2fms (%.1f%%) | stitch %.2fms (%.1f%%) | search: %.2fms (%.1f%%) | store: %d/%d frames",
                            ft,
                            (1000.0f / ft),
                            msGenerateMask,
                            (msGenerateMask / ft) * 100.0f,
                            msStitch,
                            (msStitch / ft) * 100.0f,
                            msSearch,
                            (msSearch / ft) * 100.0f,
                            frameStore.getSize(),
                            frameStore.getCapacity());
            mainHandler.post(() -> ((TextView) findViewById(R.id.label_status)).setText(status));
        }

        ++frameNr;
        return out;
    }

    private class MaskThread extends Thread
    {
        private Mat mask;
        private boolean found = false;
        private volatile VideoFrame input;
        private IReplaceMaskGenerator gen;
        private boolean stop = false;

        private MaskThread(Mat mask, IReplaceMaskGenerator gen)
        {
            this.mask = mask;
            this.gen = gen;
        }

        public void input(VideoFrame newFrame)
        {
            input = new VideoFrame(newFrame, true);
        }

        public Mat getMask()
        {
            if (found)
                return mask.clone();
            return null;
        }

        public void run()
        {
            if (mask == null) mask = new Mat();

            while (!stop)
            {
                if (input == null)
                {
                    try
                    {
                        Thread.sleep(10);
                        continue;
                    }
                    catch (InterruptedException e)
                    {
                        e.printStackTrace();
                        break;
                    }
                }

                found = gen.generate(input, mask);
                input = null;
            }
        }

        public void stopThread() {stop = true;}
    }

    public void prefClick(View view)
    {
        startActivity(new Intent(this, GlobalPreferencesActivity.class));
    }

    public void clearClick(View view)
    {
        frameStore.clear();
    }

    public void storeClick(View view)
    {
        storeFrames = ((ToggleButton) view).isChecked();
    }

    public void showhideClick(View view)
    {
        CharSequence sign = ((Button) view).getText();
        findViewById(R.id.extra_buttons)
                .setVisibility(sign.charAt(0) == '+' ? View.VISIBLE : View.GONE);
        ((Button) view).setText(sign.charAt(0) == '+' ? "-" : "+");
    }

    private int getInt(String name, String def)
    {
        return Integer.parseInt(pref.getString(name, def));
    }

    private float getFloat(String name, String def)
    {
        return Float.parseFloat(pref.getString(name, def));
    }

    public void splitClick(View view)
    {
        splitView = ((ToggleButton)view).isChecked();
    }
}
