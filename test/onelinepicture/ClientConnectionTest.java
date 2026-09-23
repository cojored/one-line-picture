package onelinepicture;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ClientConnectionTest
{
    @Test void closesAndIgnoresMessagesAfterClose()
    {
        ClientConnection connection = new ClientConnection(null);
        assertTrue(connection.isConnected());
        connection.send(null);
        connection.close();
        assertFalse(connection.isConnected());
        assertDoesNotThrow(() -> connection.send("ignored"));
    }
}
