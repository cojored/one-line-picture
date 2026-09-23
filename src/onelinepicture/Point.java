package onelinepicture;

/**
 * Represents one coordinate (x, y) on the drawing canvas.
 */
public class Point
{
    private double x;
    private double y;

    /**
     * Creates a point at the given coordinates.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    public Point(double x, double y)
    {
        this.x = x;
        this.y = y;
    }

    /**
     * Returns the horizontal coordinate.
     *
     * @return the x-coordinate
     */
    public double getX()
    {
        return x;
    }

    /**
     * Returns the vertical coordinate.
     *
     * @return the y-coordinate
     */
    public double getY()
    {
        return y;
    }
}