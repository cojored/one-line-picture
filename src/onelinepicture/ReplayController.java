package onelinepicture;

import java.util.ArrayList;

/**
 * Plays back a finished drawing point by point, in the order it was drawn.
 */
public class ReplayController
{
    private ArrayList<Stroke> strokes;
    private int strokeIndex;
    private int pointIndex;
    private double playbackSpeed = 3.0;
    private int lastStrokeIndex = -1;

    /**
     * Loads the finished drawing for playback.
     *
     * @param strokes the completed strokes, in drawing order
     */
    public ReplayController(ArrayList<Stroke> strokes)
    {
        this.strokes = strokes == null
            ? new ArrayList<Stroke>() : new ArrayList<Stroke>(strokes);
    }

    /**
     * Rewinds to the first point so playback can begin.
     */
    public void startReplay()
    {
        strokeIndex = 0;
        pointIndex = 0;
        lastStrokeIndex = -1;
    }

    /**
     * Returns the next point to draw, moving on to the next stroke when the
     * current one runs out.
     *
     * @return the next point, or null once playback is done
     */
    public Point nextPoint()
    {
        while (strokeIndex < strokes.size())
        {
            ArrayList<Point> points = strokes.get(strokeIndex).getPoints();
            if (pointIndex < points.size())
            {
                lastStrokeIndex = strokeIndex;
                return points.get(pointIndex++);
            }
            strokeIndex++;
            pointIndex = 0;
        }
        return null;
    }

    /**
     * Returns whether every point has been played.
     *
     * @return true once playback is done
     */
    public boolean isFinished()
    {
        for (int i = strokeIndex; i < strokes.size(); i++)
        {
            int size = strokes.get(i).getPoints().size();
            if (i == strokeIndex ? pointIndex < size : size > 0)
            {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns which stroke the last point from nextPoint belongs to, so the
     * display knows when a new line begins.
     *
     * @return the stroke index, or -1 before the first point
     */
    public int getLastStrokeIndex()
    {
        return lastStrokeIndex;
    }

    /**
     * Returns how many points are played per animation frame.
     *
     * @return the playback speed
     */
    public double getPlaybackSpeed()
    {
        return playbackSpeed;
    }

    /**
     * Sets how many points are played per animation frame.
     *
     * @param playbackSpeed the new speed (must be positive)
     */
    public void setPlaybackSpeed(double playbackSpeed)
    {
        if (playbackSpeed > 0)
        {
            this.playbackSpeed = playbackSpeed;
        }
    }
}
