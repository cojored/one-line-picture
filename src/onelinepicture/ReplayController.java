package onelinepicture;

import java.util.ArrayList;

public class ReplayController
{
    private ArrayList<Stroke> strokes;
    private int strokeIndex;
    private int pointIndex;
    private double playbackSpeed;

    public ReplayController(ArrayList<Stroke> strokes)
    {
        this.strokes = strokes == null
            ? new ArrayList<Stroke>() : new ArrayList<Stroke>(strokes);
    }

    public void startReplay()
    {
    }

    public Point nextPoint()
    {
        return null;
    }

    public boolean isFinished()
    {
        return false;
    }
}
