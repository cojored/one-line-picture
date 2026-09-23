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

    public void pointerPressed(double x, double y) { }
    public void pointerMoved(double x, double y) { }
    public void pointerReleased() { }
    public void pointerExited() { }
    public void touchStarted(int touchId, double x, double y) { }
    public void touchMoved(int touchId, double x, double y) { }
    public void touchEnded(int touchId) { }

    public boolean isInsideCanvas(double x, double y)
    {
        return x >= 0 && y >= 0 && x <= canvasWidth && y <= canvasHeight;
    }
}
