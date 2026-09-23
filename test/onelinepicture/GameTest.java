package onelinepicture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameTest
{
    @Test void validatesRosterAndTurnRules()
    {
        Game game = new Game(2, 2, 1000);
        assertFalse(game.addPlayer(null));
        assertFalse(game.addPlayer(new Player("", "Name")));
        assertFalse(game.addPlayer(new Player("p1", "")));
        assertTrue(game.addPlayer(new Player("p1", "One")));
        assertFalse(game.addPlayer(new Player("p1", "Duplicate")));
        assertTrue(game.addPlayer(new Player("p2", "Two")));
        assertFalse(game.addPlayer(new Player("p3", "Three")));
        assertFalse(game.startStroke("p1", new Point(.1, .1)));

        game.startGame();
        assertEquals("p1", game.getCurrentPlayer().getId());
        assertFalse(game.startStroke("p2", new Point(.1, .1)));
        assertFalse(game.startStroke("p1", new Point(-.1, .1)));
        assertTrue(game.startStroke("p1", new Point(.1, .1)));
        assertFalse(game.startStroke("p1", new Point(.2, .2)));
        assertTrue(game.addPoint("p1", new Point(.2, .2)));
        assertFalse(game.addPoint("p2", new Point(.3, .3)));
        assertFalse(game.addPoint("p1", new Point(1.1, .3)));
        assertEquals(1, game.getCompletedTurns());
        assertEquals("p2", game.getCurrentPlayer().getId());
        assertTrue(game.startStroke("p2", new Point(.5, .5)));
        assertTrue(game.endStroke("p2"));
        assertTrue(game.isFinished());
        assertEquals(2, game.getStrokes().size());
        assertFalse(game.addPlayer(new Player("late", "Late")));
        assertFalse(game.startStroke("p2", new Point(.2, .2)));
    }

    @Test void detectsExpiredTurns()
    {
        Game game = new Game(2, 1, 20);
        game.addPlayer(new Player("a", "A"));
        game.addPlayer(new Player("b", "B"));
        game.startGame();
        assertTimeoutPreemptively(java.time.Duration.ofMillis(200), () ->
        {
            while (!game.isTurnExpired()) Thread.sleep(5);
        });
        assertTrue(game.isTurnExpired());
    }
}
