package se.kth.databas.model;

/**
 * Represents an author of a book.
 * An author has a unique identifier (authorId) and a name.
 * The authorId is assigned when the author is persisted in a database.
 * If an author is not yet persisted, the authorId is set to -1.
 * The name is the full name of the author.
 *
 * @author (your name)
 */
public class Author {
    private int authorId;
    private String name;


    /**
     * Constructs an Author with the specified authorId and name.
     *
     * @param authorId The unique identifier for the author.
     * @param name     The name of the author.
     */
    public Author(int authorId, String name) {
        this.authorId = authorId;
        this.name = name;
    }
    /**
     * Constructs an Author with the specified name.
     * This constructor is typically used when creating new author that hasn't been used.
     * The authorId is set to -1 in this case.
     *
     * @param name The name of the author.
     */
    public Author(String name){
        this(-1, name);
    }
    /**
     * Gets the unique identifier of the author.
     *
     * @return The authorId.
     */
    public int getAuthorId(){
        return authorId;
    }
    /**
     * Gets the name of the author.
     *
     * @return The name of the author.
     */
    public String getName() {
        return name;
    }
    /**
     * Returns a string representation of the author.
     *
     * @return A string representation of the author.
     */
    @Override
    public String toString() {
        return name;
    }
}
