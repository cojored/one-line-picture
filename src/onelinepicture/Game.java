package onelinepicture;

import java.util.ArrayList;

public class Game
{
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

    public boolean addPlayer(Player player)
    {
        return false;
    }

    public void startGame()
    {
    }

    public boolean startStroke(String playerId, Point point)
    {
        return false;
    }

    public boolean addPoint(String playerId, Point point)
    {
        return false;
    }

    public boolean endStroke(String playerId)
    {
        return false;
    }

    public void nextTurn()
    {
    }

    public Player getCurrentPlayer()
    {
        return null;
    }

    public boolean isTurnExpired()
    {
        return false;
    }

    public boolean isFinished()
    {
        return finished;
    }

    public ArrayList<Stroke> getStrokes()
    {
        return new ArrayList<Stroke>(strokes);
    }
}
