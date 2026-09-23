package onelinepicture;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ReplayControllerTest
{
    @Test void replaysPointsInStrokeOrder()
    {
        Stroke first = new Stroke("one");
        first.addPoint(new Point(.1, .1));
        first.addPoint(new Point(.2, .2));
        Stroke second = new Stroke("two");
        second.addPoint(new Point(.3, .3));
        ArrayList<Stroke> strokes = new ArrayList<>();
        strokes.add(first); strokes.add(new Stroke("empty")); strokes.add(second);
        ReplayController replay = new ReplayController(strokes);
        replay.startReplay();
        assertEquals(.1, replay.nextPoint().getX());
        assertEquals(0, replay.getLastStrokeIndex());
        assertEquals(.2, replay.nextPoint().getX());
        assertEquals(.3, replay.nextPoint().getX());
        assertNull(replay.nextPoint());
        assertTrue(replay.isFinished());
    }

    @Test void ignoresInvalidSpeed()
    {
        ReplayController replay = new ReplayController(null);
        replay.setPlaybackSpeed(0);
        assertEquals(3.0, replay.getPlaybackSpeed());
    }
}
