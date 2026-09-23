package onelinepicture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class StrokeTest
{
    @Test void storesOrderedNonNullPoints()
    {
        Stroke stroke = new Stroke("p1");
        stroke.addPoint(new Point(.25, .75));
        stroke.addPoint(null);
        assertEquals(1, stroke.getPoints().size());
    }

    @Test void rejectsBlankOwner()
    {
        assertThrows(IllegalArgumentException.class, () -> new Stroke(null));
        assertThrows(IllegalArgumentException.class, () -> new Stroke(" "));
    }
}
