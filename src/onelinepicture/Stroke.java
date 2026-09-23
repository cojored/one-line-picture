package onelinepicture;

import java.util.ArrayList;

/**
 * Represents one continuous line drawn by a player.
 */
public class Stroke
{
    private String playerId;
    private ArrayList<Point> points;

    /**
     * Creates an empty stroke belonging to the specified player.
     *
     * @param playerId the ID of the player drawing this stroke
     * @throws IllegalArgumentException if the ID is null or blank
     */
    public Stroke(String playerId)
    {
        if (playerId == null || playerId.strip().isEmpty())
        {
            throw new IllegalArgumentException(
                "Player ID cannot be null or blank.");
        }

        this.playerId = playerId;
        this.points = new ArrayList<Point>();
    }

    /**
     * Adds a point to the end of this stroke.
     * Null points are ignored.
     *
     * @param point the point to add
     */
    public void addPoint(Point point)
    {
        if (point != null)
        {
            points.add(point);
        }
    }

    /**
     * Returns the ID of the player who created this stroke.
     *
     * @return the player's ID
     */
    public String getPlayerId()
    {
        return playerId;
    }

    /**
     * Returns a copy of the points in drawing order.
     *
     * @return a list containing this stroke's points
     */
    public ArrayList<Point> getPoints()
    {
        return new ArrayList<Point>(points);
    }
}