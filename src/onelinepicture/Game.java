package onelinepicture;

import java.util.ArrayList;

/**
 * Stores and manages the full state and rules of one game session:
 * players, strokes, turns, and completion.
 *
 * Coordinates are normalized: a point is on the canvas when both x and y
 * are between 0 and 1. That keeps every device in sync regardless of window
 * size.
 */
public class Game
{
    /** Fewest players needed before a game can start. */
    public static final int MIN_PLAYERS = 2;

    private ArrayList<Player> players = new ArrayList<Player>();
    private ArrayList<Stroke> strokes = new ArrayList<Stroke>();
    private Stroke currentStroke;
    private int currentPlayerIndex;
    private boolean started;
    private boolean finished;
    private int maxPlayers;
    private int totalTurns;
    private int completedTurns;
    private long turnTimeLimit;
    private long turnStartTime;

    /**
     * Creates a game with up to 8 players, one turn per player, and a
     * 15 second turn limit.
     */
    public Game()
    {
        this(8, 0, 15000);
    }

    /**
     * Creates a game with the given settings.
     *
     * @param maxPlayers most players allowed (0 or less means no limit)
     * @param totalTurns total turns in the game (0 or less means one turn
     *     per player, decided when the game starts)
     * @param turnTimeLimit turn length in milliseconds (0 or less means no
     *     limit)
     */
    public Game(int maxPlayers, int totalTurns, long turnTimeLimit)
    {
        this.maxPlayers = maxPlayers;
        this.totalTurns = totalTurns;
        this.turnTimeLimit = turnTimeLimit;
    }

    /**
     * Adds a player before the game starts.
     *
     * @param player the player to add
     * @return false if the game is full, already started, or the player is
     *     invalid or already joined
     */
    public boolean addPlayer(Player player)
    {
        if (player == null || player.getId() == null || started)
        {
            return false;
        }
        if (maxPlayers > 0 && players.size() >= maxPlayers)
        {
            return false;
        }
        if (findPlayer(player.getId()) != null)
        {
            return false;
        }
        players.add(player);
        return true;
    }

    /**
     * Locks the roster and begins the first turn. Does nothing if the game
     * already started or there are not enough players.
     */
    public void startGame()
    {
        if (started || players.size() < MIN_PLAYERS)
        {
            return;
        }
        if (totalTurns <= 0)
        {
            totalTurns = players.size();
        }
        started = true;
        finished = false;
        currentPlayerIndex = 0;
        completedTurns = 0;
        currentStroke = null;
        turnStartTime = System.currentTimeMillis();
    }

    /**
     * Begins the current player's line.
     *
     * @param playerId who is drawing
     * @param point where the line starts
     * @return false if it is not their turn, a line is already active, or
     *     the point is off the canvas
     */
    public boolean startStroke(String playerId, Point point)
    {
        if (!isPlayersTurn(playerId) || currentStroke != null
            || !isOnCanvas(point))
        {
            return false;
        }
        currentStroke = new Stroke(playerId);
        currentStroke.addPoint(point);
        return true;
    }

    /**
     * Extends the active line. If the point is off the canvas, the line is
     * finished and the turn ends, as if the player let go.
     *
     * @param playerId who is drawing
     * @param point the next point
     * @return false if it is not their turn, there is no active line, or
     *     the point is off the canvas
     */
    public boolean addPoint(String playerId, Point point)
    {
        if (!isPlayersTurn(playerId) || currentStroke == null
            || point == null)
        {
            return false;
        }
        if (!isOnCanvas(point))
        {
            endStroke(playerId);
            return false;
        }
        currentStroke.addPoint(point);
        return true;
    }

    /**
     * Finishes and stores the active line, then ends the turn.
     *
     * @param playerId who is drawing
     * @return false if it is not their turn or there is no active line
     */
    public boolean endStroke(String playerId)
    {
        if (!isPlayersTurn(playerId) || currentStroke == null)
        {
            return false;
        }
        nextTurn();
        return true;
    }

    /**
     * Advances to the next player and resets the turn timer. Any line in
     * progress is saved first. Ignored before the start or after the end.
     */
    public void nextTurn()
    {
        if (!started || finished)
        {
            return;
        }
        if (currentStroke != null)
        {
            strokes.add(currentStroke);
            currentStroke = null;
        }
        completedTurns++;
        if (completedTurns >= totalTurns)
        {
            finished = true;
            return;
        }
        currentPlayerIndex = (currentPlayerIndex + 1) % players.size();
        turnStartTime = System.currentTimeMillis();
    }

    /**
     * Returns whose turn it is.
     *
     * @return the current player, or null before the start or after the end
     */
    public Player getCurrentPlayer()
    {
        if (!started || finished || players.isEmpty())
        {
            return null;
        }
        return players.get(currentPlayerIndex);
    }

    /**
     * Returns whether the current turn's time limit has passed.
     *
     * @return true if time is up or there is no active turn
     */
    public boolean isTurnExpired()
    {
        if (!started || finished)
        {
            return true;
        }
        return turnTimeLimit > 0 && getTurnTimeRemaining() <= 0;
    }

    /**
     * Returns whether all turns are done.
     *
     * @return true once the game is over
     */
    public boolean isFinished()
    {
        return finished;
    }

    /**
     * Returns all completed lines, in the order they were drawn.
     *
     * @return a copy of the completed strokes
     */
    public ArrayList<Stroke> getStrokes()
    {
        return new ArrayList<Stroke>(strokes);
    }

    // ----- Helpers used by the UI -----

    /**
     * Returns the line currently being drawn so it can be shown live.
     *
     * @return the active stroke, or null if none
     */
    public Stroke getCurrentStroke()
    {
        return currentStroke;
    }

    /**
     * Returns the players in turn order.
     *
     * @return a copy of the player list
     */
    public ArrayList<Player> getPlayers()
    {
        return new ArrayList<Player>(players);
    }

    /**
     * Returns whether the game has started.
     *
     * @return true after startGame succeeds
     */
    public boolean isStarted()
    {
        return started;
    }

    /**
     * Returns how many turns have been completed.
     *
     * @return completed turn count
     */
    public int getCompletedTurns()
    {
        return completedTurns;
    }

    /**
     * Returns the total number of turns.
     *
     * @return total turns (0 until decided at start if left automatic)
     */
    public int getTotalTurns()
    {
        return totalTurns;
    }

    /**
     * Sets the total number of turns. Ignored once the game has started.
     *
     * @param totalTurns total turns (0 or less means one per player)
     */
    public void setTotalTurns(int totalTurns)
    {
        if (!started)
        {
            this.totalTurns = totalTurns;
        }
    }

    /**
     * Sets the turn time limit. Ignored once the game has started.
     *
     * @param turnTimeLimit milliseconds per turn (0 or less means no limit)
     */
    public void setTurnTimeLimit(long turnTimeLimit)
    {
        if (!started)
        {
            this.turnTimeLimit = turnTimeLimit;
        }
    }

    /**
     * Returns milliseconds left in the current turn.
     *
     * @return time left, or Long.MAX_VALUE when there is no limit
     */
    public long getTurnTimeRemaining()
    {
        if (turnTimeLimit <= 0)
        {
            return Long.MAX_VALUE;
        }
        return Math.max(0,
            turnTimeLimit - (System.currentTimeMillis() - turnStartTime));
    }

    /**
     * Returns the player with the given ID.
     *
     * @param id the ID to look for
     * @return the player, or null if not in this game
     */
    public Player findPlayer(String id)
    {
        for (Player player : players)
        {
            if (player.getId().equals(id))
            {
                return player;
            }
        }
        return null;
    }

    /**
     * Returns a player's position in turn order.
     *
     * @param id the player's ID
     * @return their index, or -1 if not in this game
     */
    public int indexOfPlayer(String id)
    {
        for (int i = 0; i < players.size(); i++)
        {
            if (players.get(i).getId().equals(id))
            {
                return i;
            }
        }
        return -1;
    }

    private boolean isPlayersTurn(String playerId)
    {
        Player current = getCurrentPlayer();
        return current != null && current.getId().equals(playerId);
    }

    private boolean isOnCanvas(Point point)
    {
        return point != null && point.getX() >= 0 && point.getX() <= 1
            && point.getY() >= 0 && point.getY() <= 1;
    }
}
