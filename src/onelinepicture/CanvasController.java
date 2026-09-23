package onelinepicture;

/** Handles mouse and touch input and sends normalized drawing actions. */
public class CanvasController
{
    private final GameClient client;
    private boolean drawing;
    private int activeTouchId = -1;
    private double canvasWidth;
    private double canvasHeight;

    public CanvasController(GameClient client)
    {
        this.client = requireClient(client);
    }

    public CanvasController(GameClient client, double canvasWidth,
        double canvasHeight)
    {
        this.client = requireClient(client);
        setCanvasSize(canvasWidth, canvasHeight);
    }

    private static GameClient requireClient(GameClient client)
    {
        if (client == null)
        {
            throw new IllegalArgumentException("Client cannot be null.");
        }
        return client;
    }

    public void setCanvasSize(double width, double height)
    {
        if (!Double.isFinite(width) || !Double.isFinite(height)
            || width <= 0 || height <= 0)
        {
            throw new IllegalArgumentException(
                "Canvas dimensions must be finite and positive.");
        }
        canvasWidth = width;
        canvasHeight = height;
    }

    public boolean isDrawing()
    {
        return drawing;
    }

    public void cancel()
    {
        cancelDrawing();
    }

    public void cancelDrawing()
    {
        drawing = false;
        activeTouchId = -1;
    }

    public void pointerPressed(double x, double y)
    {
        if (!client.isConnected())
        {
            cancelDrawing();
            return;
        }
        if (!drawing && isInsideCanvas(x, y))
        {
            drawing = true;
            activeTouchId = -1;
            sendPoint("S", x, y);
        }
    }

    public void pointerMoved(double x, double y)
    {
        if (!drawing || activeTouchId != -1)
        {
            return;
        }
        continueDrawing(x, y);
    }

    public void pointerReleased()
    {
        if (drawing && activeTouchId == -1)
        {
            finishDrawing();
        }
    }

    public void pointerExited()
    {
        pointerReleased();
    }

    public void touchStarted(int touchId, double x, double y)
    {
        if (!client.isConnected())
        {
            cancelDrawing();
            return;
        }
        if (!drawing && touchId >= 0 && isInsideCanvas(x, y))
        {
            drawing = true;
            activeTouchId = touchId;
            sendPoint("S", x, y);
        }
    }

    public void touchMoved(int touchId, double x, double y)
    {
        if (drawing && activeTouchId == touchId)
        {
            continueDrawing(x, y);
        }
    }

    public void touchEnded(int touchId)
    {
        if (drawing && activeTouchId == touchId)
        {
            finishDrawing();
        }
    }

    public boolean isInsideCanvas(double x, double y)
    {
        return x >= 0 && y >= 0 && x <= canvasWidth && y <= canvasHeight;
    }

    private void continueDrawing(double x, double y)
    {
        if (!client.isConnected())
        {
            cancelDrawing();
        }
        else if (!isInsideCanvas(x, y))
        {
            finishDrawing();
        }
        else
        {
            sendPoint("P", x, y);
        }
    }

    private void finishDrawing()
    {
        if (!drawing)
        {
            return;
        }
        cancelDrawing();
        if (client.isConnected())
        {
            client.send("E|" + client.getPlayerId());
        }
    }

    private void sendPoint(String type, double x, double y)
    {
        double normalizedX = Math.max(0, Math.min(1, x / canvasWidth));
        double normalizedY = Math.max(0, Math.min(1, y / canvasHeight));
        client.send(type + "|" + client.getPlayerId() + "|"
            + normalizedX + "|" + normalizedY);
    }
}
