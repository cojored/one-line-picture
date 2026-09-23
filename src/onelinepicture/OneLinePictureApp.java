package onelinepicture;

import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.UIManager;

/**
 * The One Line Picture window. Run this class to play.
 *
 * <p>How syncing works: the server relays every message to every client in
 * the same order, and each client applies those messages to its own copy of
 * {@link Game}. Because Game's rules are deterministic, every screen ends up
 * with the same canvas and turn state.</p>
 *
 * <p>Messages (fields separated by "|"):</p>
 * <ul>
 * <li>JOIN|id|name - a player asks to join (host answers with ROSTER)</li>
 * <li>ROSTER|id:name;id:name - host's player list, in turn order</li>
 * <li>START|totalTurns|turnMillis - host starts the game</li>
 * <li>S|id|x|y, P|id|x|y, E|id - start, extend, end a line (x, y in 0..1)</li>
 * <li>SKIP|id|turnNumber - turn timed out without a line</li>
 * </ul>
 */
public class OneLinePictureApp extends JFrame
{
    private static final int CANVAS_WIDTH = 800;
    private static final int CANVAS_HEIGHT = 560;
    private static final long SKIP_GRACE_MILLIS = 2000;
    private static final Color[] PALETTE = {
        new Color(0xE0, 0x3E, 0x3E), new Color(0x2F, 0x6F, 0xDB),
        new Color(0x1E, 0x9E, 0x5A), new Color(0xE0, 0x8A, 0x1E),
        new Color(0x8E, 0x44, 0xC4), new Color(0x14, 0x9E, 0xA8),
        new Color(0xD1, 0x4B, 0x9A), new Color(0x6B, 0x55, 0x3A)
    };
    private static final Color BG = new Color(0xF4, 0xF1, 0xEA);
    private static final Color INK = new Color(0x22, 0x22, 0x22);

    private final CardLayout cards = new CardLayout();
    private final JPanel root = new JPanel(cards);

    // Setup screen
    private final JTextField nameField = new JTextField("Player", 14);
    private final JTextField hostField = new JTextField("localhost", 14);
    private final JTextField portField = new JTextField("8080", 6);
    private final JSpinner turnSeconds =
        new JSpinner(new SpinnerNumberModel(15, 3, 120, 1));
    private final JSpinner rounds =
        new JSpinner(new SpinnerNumberModel(2, 1, 10, 1));

    // Lobby screen
    private final DefaultListModel<String> lobbyModel =
        new DefaultListModel<String>();
    private final JLabel lobbyInfo = new JLabel(" ");
    private final JButton startButton = new JButton("Start game");

    // Game screen
    private final JLabel statusLabel = new JLabel(" ");
    private final JLabel timerLabel = new JLabel(" ");
    private final DefaultListModel<Player> turnModel =
        new DefaultListModel<Player>();
    private final JButton replayButton = new JButton("Watch replay");
    private final JSlider speedSlider = new JSlider(1, 12, 3);
    private final DrawingCanvas canvas = new DrawingCanvas();
    private final JList<Player> turnList = new JList<Player>(turnModel);

    // Networking and state (only touched on the Swing thread)
    private final GameClient client = new GameClient();
    private final CanvasController controller = new CanvasController(client);
    private final String myId = UUID.randomUUID().toString().substring(0, 8);
    private final Map<String, String> hostRoster =
        new LinkedHashMap<String, String>();
    private GameServer server;
    private boolean isHost;
    private Game game = new Game();
    private int skipSentForTurn = -1;
    private int expiredTurn = -1;
    private long expiredSince;

    // Replay state
    private ReplayController replay;
    private final List<List<Point>> replayLines =
        new ArrayList<List<Point>>();
    private double replayBudget;

    /**
     * Builds the window.
     */
    public OneLinePictureApp()
    {
        super("One Line Picture");
        client.setPlayerId(myId);
        client.setMessageListener(message ->
            SwingUtilities.invokeLater(() -> handle(message)));
        controller.setCanvasSize(CANVAS_WIDTH, CANVAS_HEIGHT);

        root.add(buildSetup(), "setup");
        root.add(buildLobby(), "lobby");
        root.add(buildGame(), "game");
        setContentPane(root);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        pack();
        setLocationRelativeTo(null);

        new Timer(30, event -> tick()).start();
    }

    // ------------------------------------------------------------ screens

    private JComponent buildSetup()
    {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBackground(BG);
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(6, 8, 6, 8);
        c.anchor = GridBagConstraints.WEST;

        JLabel title = new JLabel("One Line Picture");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 34f));
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(title, c);
        c.gridy++;
        panel.add(new JLabel("Draw one line each. Watch the picture come "
            + "together."), c);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;
        addRow(panel, c, "Your name", nameField);
        addRow(panel, c, "Host address", hostField);
        addRow(panel, c, "Port", portField);
        addRow(panel, c, "Seconds per turn (host)", turnSeconds);
        addRow(panel, c, "Rounds (host)", rounds);

        JButton host = new JButton("Host a game");
        JButton join = new JButton("Join a game");
        host.addActionListener(event -> hostGame());
        join.addActionListener(event -> joinGame(hostField.getText().trim()));
        JPanel buttons = new JPanel();
        buttons.setOpaque(false);
        buttons.add(host);
        buttons.add(join);
        c.gridx = 0;
        c.gridy++;
        c.gridwidth = 2;
        c.anchor = GridBagConstraints.CENTER;
        panel.add(buttons, c);
        panel.setPreferredSize(new Dimension(CANVAS_WIDTH + 220,
            CANVAS_HEIGHT + 90));
        return panel;
    }

    private void addRow(JPanel panel, GridBagConstraints c, String label,
        JComponent field)
    {
        c.gridy++;
        c.gridx = 0;
        panel.add(new JLabel(label), c);
        c.gridx = 1;
        panel.add(field, c);
    }

    private JComponent buildLobby()
    {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(30, 60, 30, 60));
        JLabel title = new JLabel("Lobby");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 26f));
        JPanel top = new JPanel();
        top.setOpaque(false);
        top.setLayout(new BoxLayout(top, BoxLayout.Y_AXIS));
        top.add(title);
        top.add(lobbyInfo);
        panel.add(top, BorderLayout.NORTH);
        JList<String> list = new JList<String>(lobbyModel);
        list.setFont(list.getFont().deriveFont(18f));
        panel.add(new JScrollPane(list), BorderLayout.CENTER);
        startButton.setEnabled(false);
        startButton.addActionListener(event -> sendStart());
        JPanel bottom = new JPanel();
        bottom.setOpaque(false);
        bottom.add(startButton);
        panel.add(bottom, BorderLayout.SOUTH);
        return panel;
    }

    private JComponent buildGame()
    {
        JPanel panel = new JPanel(new BorderLayout(10, 10));
        panel.setBackground(BG);
        panel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JPanel top = new JPanel(new BorderLayout());
        top.setOpaque(false);
        statusLabel.setFont(statusLabel.getFont().deriveFont(Font.BOLD, 18f));
        timerLabel.setFont(timerLabel.getFont().deriveFont(Font.BOLD, 18f));
        top.add(statusLabel, BorderLayout.WEST);
        top.add(timerLabel, BorderLayout.EAST);
        panel.add(top, BorderLayout.NORTH);

        panel.add(canvas, BorderLayout.CENTER);


        turnList.setCellRenderer(new TurnRenderer());
        turnList.setFocusable(false);
        JPanel side = new JPanel(new BorderLayout(4, 4));
        side.setOpaque(false);
        side.setPreferredSize(new Dimension(190, CANVAS_HEIGHT));
        side.add(new JLabel("Turn order"), BorderLayout.NORTH);
        side.add(new JScrollPane(turnList), BorderLayout.CENTER);

        JPanel replayBox = new JPanel();
        replayBox.setOpaque(false);
        replayBox.setLayout(new BoxLayout(replayBox, BoxLayout.Y_AXIS));
        replayButton.setEnabled(false);
        replayButton.addActionListener(event -> startReplay());
        speedSlider.setOpaque(false);
        speedSlider.addChangeListener(event ->
        {
            if (replay != null)
            {
                replay.setPlaybackSpeed(speedSlider.getValue());
            }
        });
        replayBox.add(replayButton);
        replayBox.add(Box.createVerticalStrut(4));
        replayBox.add(new JLabel("Replay speed"));
        replayBox.add(speedSlider);
        side.add(replayBox, BorderLayout.SOUTH);
        panel.add(side, BorderLayout.EAST);
        return panel;
    }

    // --------------------------------------------------------- connecting

    private void hostGame()
    {
        int port = readPort();
        if (port < 0)
        {
            return;
        }
        try
        {
            server = new GameServer(new Game());
            server.start(port);
        }
        catch (IllegalStateException exception)
        {
            error("Could not host on port " + port
                + ". Is something else using it?");
            server = null;
            return;
        }
        isHost = true;
        joinGame("localhost");
    }

    private void joinGame(String host)
    {
        int port = readPort();
        if (port < 0 || host.isEmpty())
        {
            return;
        }
        client.connect(host, port);
        long deadline = System.currentTimeMillis() + 4000;
        Timer wait = new Timer(100, null);
        wait.addActionListener(event ->
        {
            if (client.isConnected())
            {
                wait.stop();
                client.send("JOIN|" + myId + "|" + cleanName());
                showLobby();
            }
            else if (System.currentTimeMillis() > deadline)
            {
                wait.stop();
                error("Could not connect to " + host + ":" + port
                    + ". Check the address and that the host is running.");
                if (server != null)
                {
                    server.stop();
                    server = null;
                    isHost = false;
                }
            }
        });
        wait.start();
    }

    private void showLobby()
    {
        if (isHost)
        {
            lobbyInfo.setText("You are hosting. Others join with address "
                + localAddress() + " and port " + portField.getText().trim()
                + ".");
        }
        else
        {
            lobbyInfo.setText("Waiting for the host to start...");
        }
        startButton.setVisible(isHost);
        cards.show(root, "lobby");
    }

    private void sendStart()
    {
        int players = hostRoster.size();
        if (players < Game.MIN_PLAYERS)
        {
            return;
        }
        int totalTurns = players * (Integer) rounds.getValue();
        long millis = 1000L * (Integer) turnSeconds.getValue();
        client.send("START|" + totalTurns + "|" + millis);
    }

    // ------------------------------------------------- incoming messages

    private void handle(String message)
    {
        String[] part = message.split("\\|");
        try
        {
            switch (part[0])
            {
                case "JOIN":
                    onJoin(part[1], part.length > 2 ? part[2] : "Player");
                    break;
                case "ROSTER":
                    onRoster(part.length > 1 ? part[1] : "");
                    break;
                case "START":
                    onStart(Integer.parseInt(part[1]),
                        Long.parseLong(part[2]));
                    break;
                case "S":
                    game.startStroke(part[1], point(part));
                    break;
                case "P":
                    game.addPoint(part[1], point(part));
                    break;
                case "E":
                    game.endStroke(part[1]);
                    break;
                case "SKIP":
                    onSkip(part[1], Integer.parseInt(part[2]));
                    break;
                default:
                    break;
            }
        }
        catch (RuntimeException badMessage)
        {
            // Ignore malformed messages (bad input case: invalid data).
        }
        afterUpdate();
    }

    private void onJoin(String id, String name)
    {
        if (!isHost || game.isStarted())
        {
            return;
        }
        if (!hostRoster.containsKey(id) && hostRoster.size() < 8)
        {
            hostRoster.put(id, name);
        }
        StringBuilder roster = new StringBuilder("ROSTER|");
        for (Map.Entry<String, String> entry : hostRoster.entrySet())
        {
            roster.append(entry.getKey()).append(':')
                .append(entry.getValue()).append(';');
        }
        client.send(roster.toString());
    }

    private void onRoster(String roster)
    {
        if (game.isStarted())
        {
            return;
        }
        game = new Game();
        lobbyModel.clear();
        for (String entry : roster.split(";"))
        {
            int colon = entry.indexOf(':');
            if (colon > 0)
            {
                String id = entry.substring(0, colon);
                String name = entry.substring(colon + 1);
                game.addPlayer(new Player(id, name));
                lobbyModel.addElement(name
                    + (id.equals(myId) ? "  (you)" : ""));
            }
        }
        startButton.setEnabled(game.getPlayers().size() >= Game.MIN_PLAYERS);
        startButton.setText(game.getPlayers().size() >= Game.MIN_PLAYERS
            ? "Start game" : "Need at least " + Game.MIN_PLAYERS + " players");
    }

    private void onStart(int totalTurns, long turnMillis)
    {
        if (game.isStarted() || game.indexOfPlayer(myId) < 0)
        {
            return;
        }
        game.setTotalTurns(totalTurns);
        game.setTurnTimeLimit(turnMillis);
        game.startGame();
        turnModel.clear();
        for (Player player : game.getPlayers())
        {
            turnModel.addElement(player);
        }
        cards.show(root, "game");
    }

    private void onSkip(String id, int turnNumber)
    {
        Player current = game.getCurrentPlayer();
        if (current != null && current.getId().equals(id)
            && game.getCompletedTurns() == turnNumber
            && game.getCurrentStroke() == null)
        {
            game.nextTurn();
        }
    }

    private Point point(String[] part)
    {
        return new Point(Double.parseDouble(part[2]),
            Double.parseDouble(part[3]));
    }

    private void afterUpdate()
    {
        if (!isMyTurn() && controller.isDrawing())
        {
            controller.cancel();
        }
        if (game.isFinished() && replay == null)
        {
            replayButton.setEnabled(true);
            startReplay();
        }
        refreshStatus();
        canvas.repaint();
        turnList.repaint();
    }

    // ------------------------------------------------------ timer tick

    private void tick()
    {
        if (game.isStarted() && !game.isFinished())
        {
            checkTurnTimer();
        }
        if (replay != null && !replay.isFinished())
        {
            stepReplay();
        }
        refreshStatus();
        canvas.repaint();
        turnList.repaint();
    }

    private void checkTurnTimer()
    {
        Player current = game.getCurrentPlayer();
        int turn = game.getCompletedTurns();
        if (current == null || !game.isTurnExpired())
        {
            return;
        }
        if (isMyTurn())
        {
            if (controller.isDrawing())
            {
                controller.pointerReleased();
            }
            else if (skipSentForTurn != turn && game.getCurrentStroke() == null)
            {
                skipSentForTurn = turn;
                client.send("SKIP|" + myId + "|" + turn);
            }
            return;
        }
        // Someone else's turn ran out. If their device went quiet, skip
        // them after a short grace period so the game does not stall.
        if (expiredTurn != turn)
        {
            expiredTurn = turn;
            expiredSince = System.currentTimeMillis();
        }
        else if (skipSentForTurn != turn
            && System.currentTimeMillis() - expiredSince > SKIP_GRACE_MILLIS)
        {
            skipSentForTurn = turn;
            if (game.getCurrentStroke() != null)
            {
                client.send("E|" + current.getId());
            }
            else
            {
                client.send("SKIP|" + current.getId() + "|" + turn);
            }
        }
    }

    private void refreshStatus()
    {
        if (!game.isStarted())
        {
            return;
        }
        if (game.isFinished())
        {
            statusLabel.setText(replay != null && !replay.isFinished()
                ? "Replaying your masterpiece..."
                : "Finished! " + game.getStrokes().size() + " lines drawn.");
            timerLabel.setText(" ");
            canvas.setCursor(Cursor.getDefaultCursor());
            return;
        }
        Player current = game.getCurrentPlayer();
        String turnText = "Turn " + (game.getCompletedTurns() + 1) + " of "
            + game.getTotalTurns() + "  -  ";
        statusLabel.setText(turnText + (isMyTurn()
            ? "Your turn: draw ONE line!"
            : current.getName() + " is drawing..."));
        statusLabel.setForeground(isMyTurn() ? colorFor(myId) : INK);
        long left = game.getTurnTimeRemaining();
        timerLabel.setText(left == Long.MAX_VALUE ? " "
            : (left + 999) / 1000 + "s");
        timerLabel.setForeground(left < 4000 ? PALETTE[0] : INK);
        canvas.setCursor(isMyTurn()
            ? Cursor.getPredefinedCursor(Cursor.CROSSHAIR_CURSOR)
            : Cursor.getDefaultCursor());
    }

    // ------------------------------------------------------------ replay

    private void startReplay()
    {
        replay = new ReplayController(game.getStrokes());
        replay.setPlaybackSpeed(speedSlider.getValue());
        replay.startReplay();
        replayLines.clear();
        replayBudget = 0;
    }

    private void stepReplay()
    {
        replayBudget += replay.getPlaybackSpeed();
        while (replayBudget >= 1)
        {
            replayBudget--;
            Point next = replay.nextPoint();
            if (next == null)
            {
                break;
            }
            while (replayLines.size() <= replay.getLastStrokeIndex())
            {
                replayLines.add(new ArrayList<Point>());
            }
            replayLines.get(replay.getLastStrokeIndex()).add(next);
        }
    }

    // ----------------------------------------------------------- helpers

    private boolean isMyTurn()
    {
        Player current = game.getCurrentPlayer();
        return current != null && current.getId().equals(myId);
    }

    private Color colorFor(String playerId)
    {
        int index = game.indexOfPlayer(playerId);
        return index < 0 ? INK : PALETTE[index % PALETTE.length];
    }

    private String cleanName()
    {
        String name = nameField.getText().replaceAll("[|:;]", "").trim();
        if (name.isEmpty())
        {
            name = "Player";
        }
        return name.length() > 20 ? name.substring(0, 20) : name;
    }

    private int readPort()
    {
        try
        {
            int port = Integer.parseInt(portField.getText().trim());
            if (port > 0 && port < 65536)
            {
                return port;
            }
        }
        catch (NumberFormatException ignored)
        {
        }
        error("Port must be a number between 1 and 65535.");
        return -1;
    }

    private void error(String text)
    {
        JOptionPane.showMessageDialog(this, text, "One Line Picture",
            JOptionPane.WARNING_MESSAGE);
    }

    private static String localAddress()
    {
        try
        {
            for (NetworkInterface net : Collections.list(
                NetworkInterface.getNetworkInterfaces()))
            {
                if (!net.isUp() || net.isLoopback())
                {
                    continue;
                }
                for (InetAddress address : Collections.list(
                    net.getInetAddresses()))
                {
                    if (address instanceof Inet4Address
                        && address.isSiteLocalAddress())
                    {
                        return address.getHostAddress();
                    }
                }
            }
        }
        catch (Exception ignored)
        {
        }
        return "localhost";
    }

    // ------------------------------------------------------ inner classes

    /** The drawing surface. */
    private class DrawingCanvas extends JPanel
    {
        DrawingCanvas()
        {
            setPreferredSize(new Dimension(CANVAS_WIDTH, CANVAS_HEIGHT));
            setMinimumSize(getPreferredSize());
            setBackground(Color.WHITE);
            setBorder(BorderFactory.createLineBorder(new Color(0xCC, 0xC6,
                0xB8), 2));
            MouseAdapter mouse = new MouseAdapter()
            {
                public void mousePressed(MouseEvent event)
                {
                    if (isMyTurn() && game.getCurrentStroke() == null)
                    {
                        controller.pointerPressed(sx(event), sy(event));
                    }
                }

                public void mouseDragged(MouseEvent event)
                {
                    if (isMyTurn())
                    {
                        controller.pointerMoved(sx(event), sy(event));
                    }
                }

                public void mouseReleased(MouseEvent event)
                {
                    if (isMyTurn())
                    {
                        controller.pointerReleased();
                    }
                }

                public void mouseExited(MouseEvent event)
                {
                    if (isMyTurn())
                    {
                        controller.pointerExited();
                    }
                }
            };
            addMouseListener(mouse);
            addMouseMotionListener(mouse);
        }

        // Convert window pixels to canvas pixels (canvas is centered).
        private double sx(MouseEvent event)
        {
            return event.getX() - offsetX();
        }

        private double sy(MouseEvent event)
        {
            return event.getY() - offsetY();
        }

        private int offsetX()
        {
            return (getWidth() - CANVAS_WIDTH) / 2;
        }

        private int offsetY()
        {
            return (getHeight() - CANVAS_HEIGHT) / 2;
        }

        @Override
        protected void paintComponent(Graphics graphics)
        {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);
            g.translate(offsetX(), offsetY());
            g.setStroke(new BasicStroke(4f, BasicStroke.CAP_ROUND,
                BasicStroke.JOIN_ROUND));

            if (replay != null)
            {
                // "As if drawn by a single person": one ink color.
                g.setColor(INK);
                for (List<Point> line : replayLines)
                {
                    drawLine(g, line);
                }
            }
            else
            {
                for (Stroke stroke : game.getStrokes())
                {
                    g.setColor(colorFor(stroke.getPlayerId()));
                    drawLine(g, stroke.getPoints());
                }
                Stroke live = game.getCurrentStroke();
                if (live != null)
                {
                    g.setColor(colorFor(live.getPlayerId()));
                    drawLine(g, live.getPoints());
                }
            }
            if (game.isStarted() && !game.isFinished()
                && game.getStrokes().isEmpty()
                && game.getCurrentStroke() == null)
            {
                g.setColor(new Color(0xAA, 0xAA, 0xAA));
                g.setFont(getFont().deriveFont(Font.ITALIC, 16f));
                g.drawString(isMyTurn()
                    ? "Click and drag to draw your line. Let go to end your turn."
                    : "Waiting for the first line...", 20, 30);
            }
            g.dispose();
        }

        private void drawLine(Graphics2D g, List<Point> points)
        {
            if (points.isEmpty())
            {
                return;
            }
            Point first = points.get(0);
            if (points.size() == 1)
            {
                g.fillOval((int) (first.getX() * CANVAS_WIDTH) - 2,
                    (int) (first.getY() * CANVAS_HEIGHT) - 2, 5, 5);
                return;
            }
            Path2D.Double path = new Path2D.Double();
            path.moveTo(first.getX() * CANVAS_WIDTH,
                first.getY() * CANVAS_HEIGHT);
            for (int i = 1; i < points.size(); i++)
            {
                path.lineTo(points.get(i).getX() * CANVAS_WIDTH,
                    points.get(i).getY() * CANVAS_HEIGHT);
            }
            g.draw(path);
        }
    }

    /** Shows each player with their color and highlights whose turn it is. */
    private class TurnRenderer extends DefaultListCellRenderer
    {
        @Override
        public java.awt.Component getListCellRendererComponent(JList<?> list,
            Object value, int index, boolean selected, boolean focused)
        {
            Player player = (Player) value;
            String text = player.getName()
                + (player.getId().equals(myId) ? " (you)" : "");
            super.getListCellRendererComponent(list, text, index, false,
                false);
            setForeground(colorFor(player.getId()));
            Player current = game.getCurrentPlayer();
            boolean active = current != null
                && current.getId().equals(player.getId());
            setFont(getFont().deriveFont(active ? Font.BOLD : Font.PLAIN,
                15f));
            setText((active ? "▶ " : "   ") + text);
            return this;
        }
    }

    /**
     * Starts the app. Run this on each player's computer.
     *
     * @param args not used
     */
    public static void main(String[] args)
    {
        SwingUtilities.invokeLater(() ->
        {
            try
            {
                UIManager.setLookAndFeel(
                    UIManager.getSystemLookAndFeelClassName());
            }
            catch (Exception ignored)
            {
            }
            new OneLinePictureApp().setVisible(true);
        });
    }
}
