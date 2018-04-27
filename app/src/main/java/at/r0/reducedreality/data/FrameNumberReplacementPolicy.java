package at.r0.reducedreality.data;

/**
 * simple replacement policy to replace frames a certain number of captured frames apart
 */
public class FrameNumberReplacementPolicy implements FrameStore.IReplacementPolicy
{
    private int frames = 0;

    public FrameNumberReplacementPolicy(int frames)
    {
        this.frames = frames;
    }

    @Override
    public boolean shouldReplace(VideoFrame oldFrame, VideoFrame newFrame)
    {
        return newFrame.mask == null
                && (oldFrame == null
                || (newFrame.frameNumber - oldFrame.frameNumber >= frames));
    }
}
