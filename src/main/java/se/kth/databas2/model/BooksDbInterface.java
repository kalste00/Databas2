package se.kth.databas.model;

import java.sql.SQLException;
import java.util.List;

/**
 * This interface declares methods for querying a Books database.
 * Different implementations of this interface handles the connection and
 * queries to a specific DBMS and database, for example a MySQL or a MongoDB
 * database.
 *
 * NB! The methods in the implementation must catch the SQL/MongoDBExceptions thrown
 * by the underlying driver, wrap in a BooksDbException and then re-throw the latter
 * exception. This way the interface is the same for both implementations, because the
 * exception type in the method signatures is the same. More info in BooksDbException.java.
 * 
 * @author anderslm@kth.se
 */
public interface BooksDbInterface {
    
    /**
     * Connect to the database.
     * @param database
     * @return true on successful connection.
     */
    public boolean connect(String database) throws BooksDbException;
    /**
     * Disconnects from the database.
     *
     * @throws BooksDbException If an error occurs during disconnection.
     */
    public void disconnect() throws BooksDbException;
    /**
     * Searches for books by title in the database.
     *
     * @param title The title to search for.
     * @return A list of books matching the title.
     * @throws BooksDbException If an error occurs during the database query.
     */
    public List<Book> searchBooksByTitle(String title) throws BooksDbException;
    /**
     * Searches for books by ISBN in the database.
     *
     * @param isbn The ISBN to search for.
     * @return A list of books matching the ISBN.
     * @throws BooksDbException If an error occurs during the database query.
     */
    List<Book> searchBooksByISBN(String isbn) throws BooksDbException;
    /**
     * Searches for books by author in the database.
     *
     * @param author The author's name to search for.
     * @return A list of books written by the specified author.
     * @throws BooksDbException If an error occurs during the database query.
     */
    List<Book> searchBooksByAuthor(String author) throws BooksDbException;
    /**
     * Retrieves all books from the database.
     *
     * @return A list of all books in the database.
     * @throws BooksDbException If an error occurs during the database query.
     */
    List<Book> getAllBooks() throws BooksDbException;

    List<Author> getAuthorsForBook(int bookId) throws BooksDbException;
    /**
     *
     * adds a new book and trows if exeption if not.
     * @throws BooksDbException
     */
    void addBook(Book newItem) throws BooksDbException;

    void clearBookAuthorConnections(int bookId) throws BooksDbException;
    boolean authorExists(String authorName) throws BooksDbException;
    int getAuthorId(String authorName) throws BooksDbException;
    int addAuthorAndGetId(Author author) throws BooksDbException;
    /**
     *updates book with new info and if wrong throws an execption.
     * @throws BooksDbException
     */
    void updateBook(Book updatedItem) throws BooksDbException;
    void addAuthorsAndConnections(Book book, int bookId, List<Author> authors) throws BooksDbException;
    int getBookId(String bookTitle) throws BooksDbException;
    /**
     *
     * deletes book from list.
     * @throws BooksDbException
     */
    void deleteBook(Book itemToDelete) throws BooksDbException;
    void clearOrphanAuthors() throws BooksDbException;
    List<Integer> getAuthorIdsForBook(int bookId) throws BooksDbException;
    int getMaxAuthorIdInAuthorsTable() throws BooksDbException;
    void updateAuthorIdsAfterDelete(int deletedAuthorId) throws BooksDbException;
    void deleteAuthorsIfNeeded (List<Integer> authorIds) throws BooksDbException;
    void deleteBookFromDatabase (int bookId) throws BooksDbException;
    void updateBookIdsAfterDelete(int deletedBookId) throws BooksDbException;
    void deleteAuthorFromDatabase(int authorId) throws BooksDbException;
    int getMaxBookId() throws BooksDbException;
    boolean isAuthorConnectedToOtherBooks(int authorId) throws BooksDbException;


    // TODO: Add abstract methods for all inserts, deletes and queries 
    // mentioned in the instructions for the assignement.
}
