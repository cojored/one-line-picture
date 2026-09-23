package onelinepicture;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameClientTest
{
    @Test void rejectsInvalidConnectionArguments()
    {
        GameClient client = new GameClient();
        assertDoesNotThrow(() -> client.connect(null, 0));
        assertFalse(client.isConnected());
    }

    @Test void isolatesListenerFailures()
    {
        GameClient client = new GameClient();
        AtomicInteger count = new AtomicInteger();
        client.setMessageListener(message ->
        {
            count.incrementAndGet();
            throw new RuntimeException("listener failure");
        });
        assertDoesNotThrow(() -> client.receive("bad input"));
        assertEquals(1, count.get());
    }
}
