package onelinepicture;

import java.util.ArrayList;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CanvasControllerTest
{
    private static class Client extends GameClient
    {
        ArrayList<String> messages = new ArrayList<>();
        Client() { setPlayerId("player"); }
        @Override public boolean isConnected() { return true; }
        @Override public void send(String message) { messages.add(message); }
    }

    @Test void handlesMouseAndCanvasBounds()
    {
        Client client = new Client();
        CanvasController controller = new CanvasController(client, 100, 100);
        assertTrue(controller.isInsideCanvas(0, 100));
        assertFalse(controller.isInsideCanvas(-1, 50));
        controller.pointerPressed(10, 20);
        controller.pointerMoved(30, 40);
        controller.pointerReleased();
        assertEquals("S|player|0.1|0.2", client.messages.get(0));
        assertEquals("E|player", client.messages.get(2));
    }

    @Test void endsWhenPointerLeavesCanvas()
    {
        Client client = new Client();
        CanvasController controller = new CanvasController(client, 100, 100);
        controller.pointerPressed(10, 10);
        controller.pointerMoved(120, 10);
        assertFalse(controller.isDrawing());
        assertEquals("E|player", client.messages.get(1));
    }

    @Test void acceptsOnlyFirstTouch()
    {
        Client client = new Client();
        CanvasController controller = new CanvasController(client, 100, 100);
        controller.touchStarted(1, 10, 10);
        controller.touchStarted(2, 20, 20);
        controller.touchMoved(2, 30, 30);
        controller.touchMoved(1, 30, 30);
        controller.touchEnded(1);
        assertEquals(3, client.messages.size());
    }
}
