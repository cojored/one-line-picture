package onelinepicture;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class GameServerTest
{
    @Test void ignoresMalformedMessages()
    {
        GameServer server = new GameServer("room", new Game(2, 2, 100));
        ClientConnection client = new ClientConnection(null);
        String[] messages = { null, "", "JOIN", "JOIN|wrong", "START", "START|x|y",
            "S|a|not-a-number|0", "P|a|0|bad", "E", "SKIP|a|not-a-number", "UNKNOWN" };
        assertDoesNotThrow(() ->
        {
            for (String message : messages) server.handleMessage(client, message);
            server.stop();
        });
    }

    @Test void validatesJoinsAndRunsAuthoritativeTurns() throws Exception
    {
        GameServer server = new GameServer("room", new Game(2, 2, 150));
        GameClient host = new GameClient();
        GameClient guest = new GameClient();
        GameClient rejected = new GameClient();
        List<String> hostMessages = new CopyOnWriteArrayList<>();
        List<String> rejectedMessages = new CopyOnWriteArrayList<>();
        try
        {
            server.start(0);
            int port = server.getPort();
            host.setPlayerId("host"); guest.setPlayerId("guest"); rejected.setPlayerId("bad");
            host.setMessageListener(hostMessages::add);
            rejected.setMessageListener(rejectedMessages::add);
            host.connect("localhost", port); guest.connect("localhost", port);
            rejected.connect("localhost", port);
            waitFor(() -> host.isConnected() && guest.isConnected()
                && rejected.isConnected(), 2000);
            rejected.send("JOIN|wrong-room|bad|Bad");
            waitFor(() -> contains(rejectedMessages, "REJECT|"), 2000);
            host.send("JOIN|room|host|Host"); guest.send("JOIN|room|guest|Guest");
            waitFor(() -> contains(hostMessages, "ROSTER|host:Host;guest:Guest;"), 2000);
            rejected.send("JOIN|room|bad|Bad");
            waitFor(() -> contains(rejectedMessages, "REJECT|Game is full"), 2000);
            host.send("START|2|150");
            waitFor(() -> contains(hostMessages, "START|2|150"), 2000);
            rejected.send("JOIN|room|bad|Bad");
            waitFor(() -> contains(rejectedMessages, "REJECT|Game has already started"), 2000);
            guest.send("S|guest|0.4|0.4");
            Thread.sleep(100);
            assertNull(server.getGame().getCurrentStroke());
            host.send("S|host|0.1|0.1"); host.send("P|host|0.2|0.2"); host.send("E|host");
            waitFor(() -> contains(hostMessages, "E|host"), 2000);
            waitFor(() -> server.getGame().isFinished(), 3000);
        }
        finally
        {
            host.disconnect(); guest.disconnect(); rejected.disconnect(); server.stop();
        }
    }

    private static boolean contains(List<String> values, String value)
    {
        for (String item : values)
            if (item.equals(value) || item.startsWith(value)) return true;
        return false;
    }

    private static void waitFor(java.util.function.BooleanSupplier condition,
        long timeout)
    {
        long deadline = System.currentTimeMillis() + timeout;
        while (System.currentTimeMillis() < deadline)
        {
            if (condition.getAsBoolean()) return;
            try { Thread.sleep(10); }
            catch (InterruptedException error)
            {
                Thread.currentThread().interrupt();
                fail(error);
            }
        }
        fail("Timed out waiting for condition");
    }
}
