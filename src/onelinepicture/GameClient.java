package onelinepicture;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.concurrent.CompletionStage;

public class GameClient
{
    private String playerId;
    private String hostAddress;
    private volatile boolean connected;
    private WebSocket socket;

    public void connect(String host, int port)
    {
        hostAddress = host;
        URI address = URI.create("ws://" + host + ":" + port);
        HttpClient.newHttpClient().newWebSocketBuilder().buildAsync(address,
            new WebSocket.Listener()
            {
                public void onOpen(WebSocket webSocket)
                {
                    socket = webSocket;
                    connected = true;
                    webSocket.request(1);
                }

                public CompletionStage<?> onText(
                    WebSocket webSocket, CharSequence data, boolean last)
                {
                    if (last)
                    {
                        receive(data.toString());
                    }
                    webSocket.request(1);
                    return null;
                }

                public void onError(WebSocket webSocket, Throwable error)
                {
                    connected = false;
                }
            }).exceptionally(error ->
            {
                connected = false;
                return null;
            });
    }

    public void disconnect()
    {
        connected = false;
        if (socket != null)
        {
            socket.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect");
            socket = null;
        }
    }

    public void send(String message)
    {
        if (connected && socket != null && message != null)
        {
            socket.sendText(message, true);
        }
    }

    public void receive(String message)
    {
    }

    public boolean isConnected()
    {
        return connected;
    }
}
