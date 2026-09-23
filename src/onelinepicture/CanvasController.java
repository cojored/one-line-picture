package onelinepicture;

/**
 * Handles canvas input and sends drawing actions to the game client.
 */
public class CanvasController
{
    private GameClient client;
    private boolean drawing;
    private int activeTouchId = -1;
    private double canvasWidth;
    private double canvasHeight;

    /**
     * Creates a controller for a canvas with the given dimensions.
     *
     * @param client the client used to send drawing actions
     * @param canvasWidth the width of the drawing area
     * @param canvasHeight the height of the drawing area
     * @throws IllegalArgumentException if the client is null or
     *     either dimension is not finite and positive
     */
    public CanvasController(
        GameClient client,
        double canvasWidth,
        double canvasHeight)
    {
        if (client == null)
        {
            throw new IllegalArgumentException("Client cannot be null.");
        }

        if (!Double.isFinite(canvasWidth)
            || !Double.isFinite(canvasHeight)
            || canvasWidth <= 0
            || canvasHeight <= 0)
        {
            throw new IllegalArgumentException(
                "Canvas dimensions must be finite and positive.");
        }

        this.client = client;
        this.canvasWidth = canvasWidth;
        this.canvasHeight = canvasHeight;
        this.drawing = false;
    }

    /**
     * Starts a mouse stroke if connected, inside the canvas,
     * and not already drawing.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    public void pointerPressed(double x, double y)
    {
        if (!client.isConnected())
        {
            cancelDrawing();
            return;
        }

        if (drawing || !isInsideCanvas(x, y))
        {
            return;
        }

        drawing = true;
        activeTouchId = -1;
        sendStart(x, y);
    }

    /**
     * Extends a mouse stroke or ends it if the pointer leaves
     * the canvas.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    public void pointerMoved(double x, double y)
    {
        if (!drawing || activeTouchId != -1)
        {
            return;
        }

        continueDrawing(x, y);
    }

    /**
     * Ends the active mouse stroke.
     */
    public void pointerReleased()
    {
        if (drawing && activeTouchId == -1)
        {
            finishDrawing();
        }
    }

    /**
     * Ends the active mouse stroke when the pointer leaves
     * the canvas.
     */
    public void pointerExited()
    {
        pointerReleased();
    }

    /**
     * Starts a touch stroke. Additional touches are ignored
     * while a stroke is active.
     *
     * @param touchId the nonnegative ID of the touch
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    public void touchStarted(int touchId, double x, double y)
    {
        if (!client.isConnected())
        {
            cancelDrawing();
            return;
        }

        if (drawing || touchId < 0 || !isInsideCanvas(x, y))
        {
            return;
        }

        drawing = true;
        activeTouchId = touchId;
        sendStart(x, y);
    }

    /**
     * Extends the active touch stroke or ends it if the touch
     * leaves the canvas. Unrelated touches are ignored.
     *
     * @param touchId the ID of the touch that moved
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    public void touchMoved(int touchId, double x, double y)
    {
        if (!drawing || activeTouchId == -1
            || touchId != activeTouchId)
        {
            return;
        }

        continueDrawing(x, y);
    }

    /**
     * Ends the stroke when the active touch is released.
     *
     * @param touchId the ID of the touch that ended
     */
    public void touchEnded(int touchId)
    {
        if (drawing && activeTouchId != -1
            && touchId == activeTouchId)
        {
            finishDrawing();
        }
    }

    /**
     * Checks whether a coordinate is within the canvas.
     * Coordinates on the boundary are included.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     * @return true if the coordinate is inside or on the boundary
     */
    public boolean isInsideCanvas(double x, double y)
    {
        return x >= 0 && y >= 0
            && x <= canvasWidth && y <= canvasHeight;
    }

    /**
     * Resets local drawing state without sending a message.
     * Call when a stroke is rejected, a turn changes,
     * or the connection closes.
     *
     * @postcondition drawing is false and activeTouchId is -1
     */
    public void cancelDrawing()
    {
        drawing = false;
        activeTouchId = -1;
    }

    /**
     * Processes movement for the active mouse or touch stroke.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
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
            sendPoint(x, y);
        }
    }

    /**
     * Ends local drawing and sends one end message if connected.
     */
    private void finishDrawing()
    {
        if (!drawing)
        {
            return;
        }

        cancelDrawing();

        if (client.isConnected())
        {
            sendEnd();
        }
    }

    /**
     * Sends a provisional start-stroke message.
     *
     * @param x the starting horizontal coordinate
     * @param y the starting vertical coordinate
     */
    private void sendStart(double x, double y)
    {
        client.send("START " + x + " " + y);
    }

    /**
     * Sends a provisional add-point message.
     *
     * @param x the horizontal coordinate
     * @param y the vertical coordinate
     */
    private void sendPoint(double x, double y)
    {
        client.send("POINT " + x + " " + y);
    }

    /**
     * Sends a provisional end-stroke message.
     */
    private void sendEnd()
    {
        client.send("END");
    }
}