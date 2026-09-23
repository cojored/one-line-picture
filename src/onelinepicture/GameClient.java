package onelinepicture;

public class GameClient
{
    private String playerId;
    private String hostAddress;
    private boolean connected;

    public void connect(String host, int port)
    {
        hostAddress = host;
        connected = false;
    }

    public void disconnect()
    {
        connected = false;
    }

    public void send(String message)
    {
    }

    public void receive(String message)
    {
    }

    public boolean isConnected()
    {
        return connected;
    }
}
