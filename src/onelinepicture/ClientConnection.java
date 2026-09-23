package onelinepicture;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class ClientConnection
{
    private String playerId;
    private final Socket socket;
    private final InputStream input;
    private final OutputStream output;
    private volatile boolean connected;
    private GameServer server;

    public ClientConnection(String playerId)
    {
        this.playerId = playerId == null ? "unknown" : playerId;
        this.socket = null;
        this.input = null;
        this.output = null;
        this.connected = true;
    }

    ClientConnection(String playerId, Socket socket, GameServer server)
        throws IOException
    {
        this.playerId = playerId == null ? "unknown" : playerId;
        this.socket = socket;
        this.input = new BufferedInputStream(socket.getInputStream());
        this.output = new BufferedOutputStream(socket.getOutputStream());
        this.server = server;
        this.connected = true;
    }

    void startReading()
    {
        Thread reader = new Thread(this::readLoop, "one-line-picture-client");
        reader.setDaemon(true);
        reader.start();
    }

    private void readLoop()
    {
        try
        {
            while (connected)
            {
                String message = readTextFrame();
                if (message == null)
                {
                    break;
                }
                server.handleMessage(this, message);
            }
        }
        catch (IOException ignored)
        {
        }
        finally
        {
            close();
            if (server != null)
            {
                server.removeClient(this);
            }
        }
    }

    public synchronized void send(String message)
    {
        if (!connected || output == null || message == null)
        {
            return;
        }
        try
        {
            byte[] payload = message.getBytes(StandardCharsets.UTF_8);
            output.write(0x81);
            if (payload.length < 126)
            {
                output.write(payload.length);
            }
            else if (payload.length <= 65535)
            {
                output.write(126);
                output.write((payload.length >>> 8) & 0xff);
                output.write(payload.length & 0xff);
            }
            else
            {
                throw new IOException("WebSocket message is too large");
            }
            output.write(payload);
            output.flush();
        }
        catch (IOException exception)
        {
            close();
        }
    }

    private String readTextFrame() throws IOException
    {
        int first = input.read();
        int second = input.read();
        if (first < 0 || second < 0)
        {
            return null;
        }
        int opcode = first & 0x0f;
        boolean masked = (second & 0x80) != 0;
        int length = second & 0x7f;
        if (length == 126)
        {
            int high = input.read();
            int low = input.read();
            if (high < 0 || low < 0)
            {
                return null;
            }
            length = (high << 8) | low;
        }
        if (length == 127 || length > 65535)
        {
            throw new IOException("Unsupported WebSocket frame size");
        }
        byte[] mask = new byte[4];
        if (masked && readFully(mask) != mask.length)
        {
            return null;
        }
        byte[] payload = new byte[length];
        if (readFully(payload) != payload.length)
        {
            return null;
        }
        if (masked)
        {
            for (int i = 0; i < payload.length; i++)
            {
                payload[i] = (byte) (payload[i] ^ mask[i % 4]);
            }
        }
        if (opcode == 0x8)
        {
            return null;
        }
        if (opcode == 0x9)
        {
            sendPong(payload);
            return readTextFrame();
        }
        return opcode == 0x1 ? new String(payload, StandardCharsets.UTF_8) : "";
    }

    private void sendPong(byte[] payload) throws IOException
    {
        if (payload.length > 125)
        {
            throw new IOException("Invalid WebSocket control frame");
        }
        output.write(0x8A);
        output.write(payload.length);
        output.write(payload);
        output.flush();
    }

    private int readFully(byte[] bytes) throws IOException
    {
        int total = 0;
        while (total < bytes.length)
        {
            int count = input.read(bytes, total, bytes.length - total);
            if (count < 0)
            {
                break;
            }
            total += count;
        }
        return total;
    }

    public void close()
    {
        connected = false;
        if (socket != null)
        {
            try
            {
                socket.close();
            }
            catch (IOException ignored)
            {
            }
        }
    }

    public boolean isConnected()
    {
        return connected;
    }

    String getPlayerId()
    {
        return playerId;
    }

    void clearPlayerId()
    {
        playerId = "unknown";
    }

    void setPlayerId(String playerId)
    {
        this.playerId = playerId;
    }
}
