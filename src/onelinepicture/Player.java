package onelinepicture;

/**
 * Represents a player and stores their ID and display name.
 */
public class Player
{
    private String id;
    private String name;

    /**
     * Creates a player with the given ID and display name.
     *
     * @param id the player's unique ID
     * @param name the player's display name
     */
    public Player(String id, String name)
    {
        this.id = id;
        this.name = name;
    }

    /**
     * Returns the player's unique ID.
     *
     * @return the player's ID, or null if none was supplied
     */
    public String getId()
    {
        return id;
    }

    /**
     * Returns the player's display name.
     *
     * @return the player's name, or null if none was supplied
     */
    public String getName()
    {
        return name;
    }
}