package onelinepicture;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Base64;

public class GameServer
{
    private final Game game;
    private final ArrayList<ClientConnection> clients = new ArrayList<ClientConnection>();
    private volatile boolean running;
    private ServerSocket serverSocket;

    public GameServer(Game game)
    {
        this.game = game;
    }

    public void start(int port)
    {
        if (running)
        {
            return;
        }
        try
        {
            serverSocket = new ServerSocket(port);
            running = true;
            Thread acceptor = new Thread(this::acceptLoop, "one-line-picture-server");
            acceptor.setDaemon(true);
            acceptor.start();
        }
        catch (IOException exception)
        {
            throw new IllegalStateException("Could not start WebSocket server", exception);
        }
    }

    private void acceptLoop()
    {
        while (running)
        {
            try
            {
                Socket socket = serverSocket.accept();
                String key = readHandshake(socket.getInputStream());
                writeHandshake(socket.getOutputStream(), key);
                ClientConnection client = new ClientConnection("unknown", socket, this);
                synchronized (clients)
                {
                    clients.add(client);
                }
                client.startReading();
            }
            catch (IOException exception)
            {
                if (running)
                {
                    continue;
                }
            }
        }
    }

    private String readHandshake(InputStream input) throws IOException
    {
        StringBuilder request = new StringBuilder();
        int previous = 0;
        int current;
        while ((current = input.read()) >= 0)
        {
            request.append((char) current);
            if (previous == '\r' && current == '\n'
                && request.toString().endsWith("\r\n\r\n"))
            {
                break;
            }
            previous = current;
        }
        for (String line : request.toString().split("\\r\\n"))
        {
            if (line.toLowerCase().startsWith("sec-websocket-key:"))
            {
                return line.substring(line.indexOf(':') + 1).trim();
            }
        }
        throw new IOException("Missing WebSocket key");
    }

    private void writeHandshake(OutputStream output, String key) throws IOException
    {
        try
        {
            String accept = Base64.getEncoder().encodeToString(
                MessageDigest.getInstance("SHA-1").digest(
                    (key + "258EAFA5-E914-47DA-95CA-C5AB0DC85B11")
                        .getBytes(StandardCharsets.US_ASCII)));
            String response = "HTTP/1.1 101 Switching Protocols\r\n"
                + "Upgrade: websocket\r\nConnection: Upgrade\r\n"
                + "Sec-WebSocket-Accept: " + accept + "\r\n\r\n";
            output.write(response.getBytes(StandardCharsets.US_ASCII));
            output.flush();
        }
        catch (Exception exception)
        {
            throw new IOException("Could not complete WebSocket handshake", exception);
        }
    }

    public void stop()
    {
        running = false;
        try
        {
            if (serverSocket != null)
            {
                serverSocket.close();
            }
        }
        catch (IOException ignored)
        {
        }
        synchronized (clients)
        {
            for (ClientConnection client : clients)
            {
                client.close();
            }
            clients.clear();
        }
    }

    public void handleMessage(ClientConnection client, String message)
    {
        if (client != null && client.isConnected() && message != null)
        {
            broadcast(message);
        }
    }

    public void broadcast(String message)
    {
        synchronized (clients)
        {
            for (ClientConnection client : clients)
            {
                if (client.isConnected())
                {
                    client.send(message);
                }
            }
        }
    }

    void removeClient(ClientConnection client)
    {
        synchronized (clients)
        {
            clients.remove(client);
        }
    }

    public Game getGame()
    {
        return game;
    }
}
