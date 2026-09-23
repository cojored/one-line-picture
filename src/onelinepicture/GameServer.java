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
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class GameServer
{
    private final Game game;
    private final String gameId;
    private final ArrayList<ClientConnection> clients = new ArrayList<ClientConnection>();
    private volatile boolean running;
    private ServerSocket serverSocket;
    private ScheduledExecutorService timer;
    private String hostPlayerId;

    public GameServer(Game game)
    {
        this("default", game);
    }

    public GameServer(String gameId, Game game)
    {
        if (gameId == null || gameId.trim().isEmpty())
        {
            throw new IllegalArgumentException("Game ID cannot be blank");
        }
        this.gameId = gameId.trim();
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
            timer = Executors.newSingleThreadScheduledExecutor(runnable ->
            {
                Thread thread = new Thread(runnable, "one-line-picture-timer");
                thread.setDaemon(true);
                return thread;
            });
            timer.scheduleAtFixedRate(this::advanceExpiredTurn,
                100, 100, TimeUnit.MILLISECONDS);
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
        if (timer != null)
        {
            timer.shutdownNow();
            timer = null;
        }
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

    public synchronized void handleMessage(ClientConnection client, String message)
    {
        if (client == null || !client.isConnected() || message == null)
        {
            return;
        }

        String[] parts = message.split("\\|", -1);
        if (parts.length == 0)
        {
            return;
        }
        try
        {
            switch (parts[0])
            {
                case "JOIN":
                    handleJoin(client, parts);
                    break;
                case "ROSTER":
                    break;
                case "START":
                    handleStart(client, parts);
                    break;
                case "S":
                    handleStartStroke(client, parts);
                    break;
                case "P":
                    handlePoint(client, parts);
                    break;
                case "E":
                    handleEndStroke(client, parts);
                    break;
                case "SKIP":
                    handleSkip(client, parts);
                    break;
                default:
                    break;
            }
        }
        catch (RuntimeException ignored)
        {
            // Invalid messages are ignored by the server.
        }
    }

    private void handleJoin(ClientConnection client, String[] parts)
    {
        if (parts.length < 4)
        {
            reject(client, "Malformed join request");
            return;
        }
        if (!gameId.equals(parts[1]))
        {
            reject(client, "Unknown game ID");
            return;
        }
        if (game.isStarted())
        {
            reject(client, "Game has already started");
            return;
        }
        String id = parts[2];
        String name = parts[3];
        if (id.trim().isEmpty() || name.trim().isEmpty())
        {
            reject(client, "Player ID and name are required");
            return;
        }
        if (!client.getPlayerId().equals("unknown")
            && !id.equals(client.getPlayerId()))
        {
            reject(client, "Player identity cannot change");
            return;
        }
        if (game.findPlayer(id) != null)
        {
            reject(client, "Player ID is already in use");
            return;
        }
        if (!game.addPlayer(new Player(id, name)))
        {
            reject(client, "Game is full");
            return;
        }
        client.setPlayerId(id);
        if (hostPlayerId == null)
        {
            hostPlayerId = id;
        }
        client.send("WELCOME|" + gameId + "|" + id);
        broadcastRoster();
    }

    private void handleStart(ClientConnection client, String[] parts)
    {
        if (parts.length < 3 || !isKnownPlayer(client)
            || !client.getPlayerId().equals(hostPlayerId))
        {
            reject(client, "Only the host can start the game");
            return;
        }
        game.setTotalTurns(Integer.parseInt(parts[1]));
        game.setTurnTimeLimit(Long.parseLong(parts[2]));
        if (!game.isStarted())
        {
            game.startGame();
        }
        if (game.isStarted())
        {
            broadcast("START|" + game.getTotalTurns() + "|"
                + parts[2]);
        }
    }

    private void handleStartStroke(ClientConnection client, String[] parts)
    {
        if (parts.length < 4 || !isOwnedAction(client, parts[1]))
        {
            return;
        }
        if (game.startStroke(parts[1], new Point(
            Double.parseDouble(parts[2]), Double.parseDouble(parts[3]))))
        {
            broadcast(message(parts));
        }
    }

    private void handlePoint(ClientConnection client, String[] parts)
    {
        if (parts.length < 4 || !isOwnedAction(client, parts[1]))
        {
            return;
        }
        if (game.addPoint(parts[1], new Point(
            Double.parseDouble(parts[2]), Double.parseDouble(parts[3]))))
        {
            broadcast(message(parts));
        }
    }

    private void handleEndStroke(ClientConnection client, String[] parts)
    {
        if (parts.length < 2 || !isOwnedAction(client, parts[1]))
        {
            return;
        }
        if (game.endStroke(parts[1]))
        {
            broadcast(message(parts));
        }
    }

    private void handleSkip(ClientConnection client, String[] parts)
    {
        if (parts.length < 3 || !isOwnedAction(client, parts[1]))
        {
            return;
        }
        Player current = game.getCurrentPlayer();
        if (current != null && current.getId().equals(parts[1])
            && game.getCompletedTurns() == Integer.parseInt(parts[2])
            && game.getCurrentStroke() == null && game.isTurnExpired())
        {
            game.nextTurn();
            broadcast(message(parts));
        }
    }

    private boolean isOwnedAction(ClientConnection client, String playerId)
    {
        return playerId != null && playerId.equals(client.getPlayerId());
    }

    private boolean isKnownPlayer(ClientConnection client)
    {
        return !client.getPlayerId().equals("unknown")
            && game.findPlayer(client.getPlayerId()) != null;
    }

    private String message(String[] parts)
    {
        return String.join("|", parts);
    }

    private void advanceExpiredTurn()
    {
        synchronized (this)
        {
            if (!running || !game.isStarted() || game.isFinished()
                || !game.isTurnExpired())
            {
                return;
            }
            Player current = game.getCurrentPlayer();
            if (current == null)
            {
                return;
            }
            int turn = game.getCompletedTurns();
            game.nextTurn();
            broadcast("SKIP|" + current.getId() + "|" + turn);
        }
    }

    private void broadcastRoster()
    {
        StringBuilder roster = new StringBuilder("ROSTER|");
        for (Player player : game.getPlayers())
        {
            roster.append(player.getId()).append(':')
                .append(player.getName()).append(';');
        }
        broadcast(roster.toString());
    }

    private void reject(ClientConnection client, String reason)
    {
        client.send("REJECT|" + reason.replace('|', ' '));
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
            if (client.getPlayerId().equals(hostPlayerId))
            {
                hostPlayerId = null;
                for (Player player : game.getPlayers())
                {
                    for (ClientConnection remaining : clients)
                    {
                        if (player.getId().equals(remaining.getPlayerId()))
                        {
                            hostPlayerId = player.getId();
                            broadcast("HOST|" + hostPlayerId);
                            return;
                        }
                    }
                }
            }
        }
    }

    public String getGameId()
    {
        return gameId;
    }

    public Game getGame()
    {
        return game;
    }
}
