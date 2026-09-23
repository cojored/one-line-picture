package onelinepicture;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.WebSocket;
import java.util.function.Consumer;
import java.util.concurrent.CompletionStage;

public class GameClient
{
    private String playerId;
    private String hostAddress;
    private volatile boolean connected;
    private WebSocket socket;
    private volatile Consumer<String> messageListener;
    private StringBuilder incomingText = new StringBuilder();

    public void setPlayerId(String playerId)
    {
        this.playerId = playerId;
    }

    public String getPlayerId()
    {
        return playerId;
    }

    public void setMessageListener(Consumer<String> messageListener)
    {
        this.messageListener = messageListener;
    }

    public void connect(String host, int port)
    {
        if (host == null || host.trim().isEmpty() || port < 1 || port > 65535)
        {
            connected = false;
            return;
        }
        hostAddress = host.trim();
        final URI address;
        try
        {
            address = URI.create("ws://" + hostAddress + ":" + port);
        }
        catch (IllegalArgumentException exception)
        {
            connected = false;
            return;
        }
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
                    incomingText.append(data);
                    if (last)
                    {
                        receive(incomingText.toString());
                        incomingText.setLength(0);
                    }
                    webSocket.request(1);
                    return null;
                }

                public void onError(WebSocket webSocket, Throwable error)
                {
                    connected = false;
                }

                public CompletionStage<?> onClose(
                    WebSocket webSocket, int statusCode, String reason)
                {
                    connected = false;
                    socket = null;
                    return null;
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
            try
            {
                socket.sendClose(WebSocket.NORMAL_CLOSURE, "disconnect");
            }
            catch (RuntimeException ignored)
            {
            }
            socket = null;
        }
    }

    public void send(String message)
    {
        if (connected && socket != null && message != null)
        {
            try
            {
                socket.sendText(message, true);
            }
            catch (RuntimeException exception)
            {
                connected = false;
            }
        }
    }

    public void receive(String message)
    {
        Consumer<String> listener = messageListener;
        if (listener != null && message != null)
        {
            try
            {
                listener.accept(message);
            }
            catch (RuntimeException ignored)
            {
            }
        }
    }

    public boolean isConnected()
    {
        return connected;
    }
}
