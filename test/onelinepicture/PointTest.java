package onelinepicture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PointTest
{
    @Test void storesCoordinates()
    {
        Point point = new Point(0.25, 0.75);
        assertEquals(0.25, point.getX());
        assertEquals(0.75, point.getY());
    }
}
