package onelinepicture;

public class CanvasController
{
    private GameClient client;
    private boolean drawing;
    private int activeTouchId = -1;
    private double canvasWidth;
    private double canvasHeight;

    public CanvasController(GameClient client)
    {
        this.client = client;
    }

    public void setCanvasSize(double width, double height)
    {
        canvasWidth = Math.max(0, width);
        canvasHeight = Math.max(0, height);
    }

    public boolean isDrawing()
    {
        return drawing;
    }

    public void cancel()
    {
        pointerReleased();
    }

    public void pointerPressed(double x, double y)
    {
        if (!drawing && isInsideCanvas(x, y) && client.isConnected())
        {
            drawing = true;
            send("S", x, y);
        }
    }

    public void pointerMoved(double x, double y)
    {
        if (!drawing)
        {
            return;
        }
        if (isInsideCanvas(x, y))
        {
            send("P", x, y);
        }
        else
        {
            pointerReleased();
        }
    }

    public void pointerReleased()
    {
        if (drawing)
        {
            client.send("E|" + client.getPlayerId());
            drawing = false;
        }
    }

    public void pointerExited()
    {
        pointerReleased();
    }

    public void touchStarted(int touchId, double x, double y)
    {
        if (activeTouchId == -1 && isInsideCanvas(x, y))
        {
            activeTouchId = touchId;
            pointerPressed(x, y);
        }
    }

    public void touchMoved(int touchId, double x, double y)
    {
        if (touchId == activeTouchId)
        {
            pointerMoved(x, y);
        }
    }

    public void touchEnded(int touchId)
    {
        if (touchId == activeTouchId)
        {
            pointerReleased();
            activeTouchId = -1;
        }
    }

    private void send(String type, double x, double y)
    {
        if (canvasWidth <= 0 || canvasHeight <= 0)
        {
            return;
        }
        double normalizedX = Math.max(0, Math.min(1, x / canvasWidth));
        double normalizedY = Math.max(0, Math.min(1, y / canvasHeight));
        client.send(type + "|" + client.getPlayerId() + "|"
            + normalizedX + "|" + normalizedY);
    }

    public boolean isInsideCanvas(double x, double y)
    {
        return x >= 0 && y >= 0 && x <= canvasWidth && y <= canvasHeight;
    }
}
