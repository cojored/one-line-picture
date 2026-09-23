package onelinepicture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class PlayerTest
{
    @Test void storesPlayerDetails()
    {
        Player player = new Player("p1", "Player One");
        assertEquals("p1", player.getId());
        assertEquals("Player One", player.getName());
    }
}
