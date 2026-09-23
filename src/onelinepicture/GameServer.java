package onelinepicture;

import java.util.ArrayList;

public class GameServer
{
    private Game game;
    private ArrayList<ClientConnection> clients = new ArrayList<ClientConnection>();

    public GameServer(Game game)
    {
        this.game = game;
    }

    public void start(int port)
    {
    }

    public void stop()
    {
    }

    public void handleMessage(ClientConnection client, String message)
    {
    }

    public void broadcast(String message)
    {
    }

    public Game getGame()
    {
        return game;
    }
}
