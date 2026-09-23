package onelinepicture;

public class ClientConnection
{
    private String playerId;
    private boolean connected;

    public ClientConnection(String playerId)
    {
        this.playerId = playerId;
        this.connected = true;
    }

    public void send(String message)
    {
    }

    public void close()
    {
        connected = false;
    }

    public boolean isConnected()
    {
        return connected;
    }
}
