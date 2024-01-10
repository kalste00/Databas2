/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 *//*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package se.kth.databas2.model;

import com.mongodb.*;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

/**
 * A mock implementation of the BooksDBInterface interface to demonstrate how to
 * use it together with the user interface.
 * <p>
 * Your implementation must access a real database.
 *
 * @author anderslm@kth.se
 */
public class BooksDbImpl implements BooksDbInterface {


    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public boolean connect(String databaseName) throws BooksDbException {
        try {
            // Anslut till MongoDB-servern
            mongoClient = MongoClients.create("mongodb+srv://kallestenbjelke:Gaming123@kcdb.gt7n66h.mongodb.net/?retryWrites=true&w=majority");

            // Välj den angivna databasen
            database = mongoClient.getDatabase(databaseName);

            System.out.println("Connected to MongoDB");
            return true;
        } catch (MongoException e) {
            throw new BooksDbException("Failed to connect to MongoDB", e);
        }
    }

    @Override
    public void disconnect() throws BooksDbException {
        try {
            if (mongoClient != null) {
                mongoClient.close();
                System.out.println("Disconnected from MongoDB");
            }
        } catch (Exception e) {
            throw new BooksDbException("Error while disconnecting from MongoDB", e);
        }
    }

    @Override
    public List<Book> searchBooksByTitle(String searchTitle) throws BooksDbException {
        List<Book> result = new ArrayList<>();
        searchTitle = searchTitle.toLowerCase();

        try {
            MongoCollection<Document> booksCollection = database.getCollection("books");

            // Create a regex pattern for case-insensitive partial matching
            Pattern regexPattern = Pattern.compile(Pattern.quote(searchTitle), Pattern.CASE_INSENSITIVE);

            // Construct the query
            BasicDBObject query = new BasicDBObject("title", regexPattern);

            // Execute the query and iterate over the results
            FindIterable<Document> findIterable = booksCollection.find(query);
            for (Document document : findIterable) {
                int bookId = document.getInteger("bookId");
                String title = document.getString("title");
                String isbn = document.getString("ISBN");
                Date publishDate = document.getDate("publishDate");
                Genre genre = Genre.valueOf(document.getString("genre"));
                int rating = document.getInteger("rating");

                // Fetch authors associated with the book
                List<Author> bookAuthors = getAuthorsForBook(bookId);

                Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                book.setBookId(bookId);
                book.getAuthors().addAll(bookAuthors);

                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by Title in MongoDB", e);
        }

        return result;
    }

    @Override
    public List<Book> searchBooksByISBN(String searchISBN) throws BooksDbException {
        List<Book> result = new ArrayList<>();
        try {
            String sql = "SELECT * FROM Book WHERE ISBN LIKE ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, "%" + searchISBN + "%");
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int bookId = rs.getInt("bookId");
                        String title = rs.getString("title");
                        String isbn = rs.getString("ISBN");
                        Date publishDate = rs.getDate("publishDate");
                        Genre genre = Genre.valueOf(rs.getString("genre"));
                        int rating = rs.getInt("rating");
                        System.out.println("book ID: " + bookId);

                        // Fetch authors associated with the book
                        List<Author> bookAuthors = getAuthorsForBook(bookId);

                        Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                        book.setBookId(bookId);
                        book.getAuthors().addAll(bookAuthors);

                        result.add(book);
                    }
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by ISBN", e);
        }

        return result;
    }


    @Override
    public List<Book> searchBooksByAuthor(String searchAuthor) throws BooksDbException {
        List<Book> result = new ArrayList<>();
        searchAuthor = searchAuthor.toLowerCase();

        try {
            String sql = "SELECT b.* FROM Book b JOIN Book_Author ba ON b.bookId = ba.bookId "
                    + "JOIN Author a ON a.authorId = ba.authorId WHERE a.authorName LIKE ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, "%" + searchAuthor + "%");

                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int bookId = rs.getInt("bookId");
                        String title = rs.getString("title");
                        String isbn = rs.getString("ISBN");
                        Date publishDate = rs.getDate("publishDate");
                        Genre genre = Genre.valueOf(rs.getString("genre"));
                        int rating = rs.getInt("rating");
                        System.out.println("book ID: " + bookId);

                        // Fetch authors associated with the book
                        List<Author> bookAuthors = getAuthorsForBook(bookId);

                        Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                        book.setBookId(bookId);
                        book.getAuthors().addAll(bookAuthors);

                        result.add(book);
                    }
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by author", e);
        }

        return result;
    }

    @Override
    public List<Book> getAllBooks() throws BooksDbException {
        List<Book> result = new ArrayList<>();
        try {
            String sql = "SELECT * FROM Book";
            try (Statement stmt = connection.createStatement(); ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    int bookId = rs.getInt("bookId");
                    String title = rs.getString("title");
                    String isbn = rs.getString("ISBN");
                    Date publishDate = rs.getDate("publishDate");
                    String genreStr = rs.getString("genre");
                    int rating = rs.getInt("rating");
                    System.out.println("book ID: " + bookId);
                    List<Author> bookAuthors = getAuthorsForBook(bookId);

                    Genre genre = Genre.valueOf(genreStr);

                    Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                    book.setBookId(bookId);
                    book.getAuthors().addAll(bookAuthors);

                    result.add(book);
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error getting all books", e);
        }
        return result;
    }

    // A method to retrieve authors for a given book
    @Override
    public List<Author> getAuthorsForBook(int bookId) throws BooksDbException {
        String sql = "SELECT a.* FROM Author a JOIN Book_Author ba ON a.authorId = ba.authorId WHERE ba.bookId = ?";
        List<Author> authors = new ArrayList<>();
        try {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setInt(1, bookId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        int authorId = rs.getInt("authorId");
                        String authorName = rs.getString("authorName");
                        Author author = new Author(authorId, authorName);
                        authors.add(author);
                    }
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
        return authors;
    }

    @Override
    public void addBook(Book book) throws BooksDbException {
        try {
            try (PreparedStatement statement = connection.prepareStatement("INSERT INTO Book (title, isbn, publishDate, genre, rating) VALUES (?, ?, ?, ?, ?)", Statement.RETURN_GENERATED_KEYS)) {
                statement.setString(1, book.getTitle());
                statement.setString(2, book.getIsbn());
                statement.setDate(3, book.getPublishDate());
                statement.setString(4, book.getGenre().toString());
                statement.setInt(5, book.getRating());
                statement.executeUpdate();

                try (ResultSet generatedKeys = statement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        int bookId = generatedKeys.getInt(1);
                        System.out.println("Generated Book ID: " + bookId);
                        System.out.println("Generated author ID: " + book.getAuthors());

                        book.setBookId(bookId);

                        clearBookAuthorConnections(bookId);

                        addAuthorsAndConnections(book, book.getBookId(), book.getAuthors());

                        connection.commit();
                    } else {
                        throw new SQLException("Failed to get generated book ID");
                    }
                }
            }
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                throw new BooksDbException("Error rolling back transaction", rollbackException);
            }
            throw new BooksDbException("Error adding book: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new BooksDbException("Error setting auto-commit to true", e);
            }
        }
    }

    @Override
    public void clearBookAuthorConnections(int bookId) throws BooksDbException {
        try {
            try (PreparedStatement statement = connection.prepareStatement("DELETE FROM Book_Author WHERE bookId = ?")) {
                statement.setInt(1, bookId);
                statement.executeUpdate();
                System.out.println("Cleared existing author connections for book with ID " + bookId);
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
    }

    @Override
    public boolean authorExists(String authorName) throws BooksDbException {
        String sql = "SELECT * FROM Author WHERE authorName = ?";
        try {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, authorName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next();
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
    }

    @Override
    public int getAuthorId(String authorName) throws BooksDbException {
        String sql = "SELECT authorId FROM Author WHERE authorName = ?";
        try {
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, authorName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("authorId");
                    } else {
                        throw new SQLException("Author not found with name: " + authorName);
                    }
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
    }

    @Override
    public int addAuthorAndGetId(Author author) throws BooksDbException {
        try {
            try (PreparedStatement authorStatement = connection.prepareStatement("INSERT INTO Author (authorName) VALUES (?)", Statement.RETURN_GENERATED_KEYS)) {
                authorStatement.setString(1, author.getName());
                authorStatement.executeUpdate();
                try (ResultSet generatedKeys = authorStatement.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        return generatedKeys.getInt(1);
                    } else {
                        throw new SQLException("Failed to get generated author ID");
                    }
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
    }

    private void updateBookAuthors(Book book, List<Author> updatedAuthors) throws BooksDbException {
        List<Author> existingAuthors = getAuthorsForBook(book.getBookId());
        List<Author> newAuthors = new ArrayList<>();
        List<Author> removedAuthors = new ArrayList<>();

        for (Author updatedAuthor : updatedAuthors) {
            if (!existingAuthors.contains(updatedAuthor)) {
                newAuthors.add(updatedAuthor);
            }
        }

        for (Author existingAuthor : existingAuthors) {
            if (!updatedAuthors.contains(existingAuthor)) {
                removedAuthors.add(existingAuthor);
            }
        }

        if (!removedAuthors.isEmpty()) {
            clearBookAuthorConnections(book.getBookId());
        }

        if (!newAuthors.isEmpty()) {
            addAuthorsAndConnections(book, book.getBookId(), newAuthors);
        }
    }

    public void updateBook(Book book) throws BooksDbException {
        try {
            connection.setAutoCommit(false);
            updates(book);
            updateBookAuthors(book, book.getAuthors());
            connection.commit();
        } catch (SQLException e) {
            try {
                connection.rollback();
            } catch (SQLException rollbackException) {
                throw new BooksDbException("Error rolling back transaction", rollbackException);
            }
            throw new BooksDbException("Error updating book: " + e.getMessage(), e);
        } finally {
            try {
                connection.setAutoCommit(true);
            } catch (SQLException e) {
                throw new BooksDbException("Error setting auto-commit to true", e);
            }
        }
    }

    public void updates(Book book) throws BooksDbException {
        updateTitle(book);
        updateIsbn(book);
        updatePublishDate(book);
        updateGenre(book);
        updateRating(book);
    }

    private void updateTitle(Book book) throws BooksDbException {
        try {
            if (book.getTitle() != null && !book.getTitle().isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement("UPDATE Book SET title = ? WHERE bookId = ?")) {
                    stmt.setString(1, book.getTitle());
                    stmt.setInt(2, book.getBookId());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error updating title: " + e.getMessage(), e);
        }
    }

    private void updateIsbn(Book book) throws BooksDbException {
        try {
            System.out.println("bookId: " + book.getBookId());
            if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
                try (PreparedStatement stmt = connection.prepareStatement("UPDATE Book SET ISBN = ? WHERE bookId = ?")) {
                    stmt.setString(1, book.getIsbn());
                    stmt.setInt(2, book.getBookId());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error updating ISBN: " + e.getMessage(), e);
        }
    }

    private void updatePublishDate(Book book) throws BooksDbException {
        try {
            if (book.getPublishDate() != null) {
                try (PreparedStatement stmt = connection.prepareStatement("UPDATE Book SET publishDate = ? WHERE bookId = ?")) {
                    stmt.setDate(1, Date.valueOf(String.valueOf(book.getPublishDate())));
                    stmt.setInt(2, book.getBookId());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error updating publish date: " + e.getMessage(), e);
        }
    }

    private void updateGenre(Book book) throws BooksDbException {
        try {
            if (book.getGenre() != null) {
                try (PreparedStatement stmt = connection.prepareStatement("UPDATE Book SET genre = ? WHERE bookId = ?")) {
                    stmt.setString(1, book.getGenre().toString());
                    stmt.setInt(2, book.getBookId());
                    stmt.executeUpdate();
                }
            }
        } catch (SQLException e) {
            throw new BooksDbException("Error updating genre: " + e.getMessage(), e);
        }
    }

    private void updateRating(Book book) throws BooksDbException {
        try {
            // Find the book by bookId and update the rating
            database.getCollection("Book")
                    .updateOne(Filters.eq("bookId", book.getBookId()),
                            Updates.set("rating", book.getRating()));
        } catch (MongoException e) {
            throw new BooksDbException("Error updating rating: " + e.getMessage(), e);
        }
    }


    @Override
    public void addAuthorsAndConnections(Book book, int bookId, List<Author> authors) throws BooksDbException {
        try {
            for (Author author : authors) {
                int authorId;
                try {
                    if (authorExists(author.getName())) {
                        authorId = getAuthorId(author.getName());
                    } else {
                        authorId = addAuthorAndGetId(author);
                    }

                    if (!isAuthorConnectedToOtherBooks(authorId)) {
                        // Insert a new document into the Book_Author collection
                        Document document = new Document("bookId", bookId)
                                .append("authorId", authorId);

                        database.getCollection("Book_Author").insertOne(document);

                        System.out.println("Added author " + authorId + " for book " + bookId);
                    } else {
                        System.out.println("Author " + authorId + " is already connected to other books.");
                    }
                } catch (MongoException e) {
                    System.out.println("Error adding author for book " + bookId + ": " + e.getMessage());
                    throw e;
                }
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
    }


    @Override
    public int getBookId(String bookTitle) throws BooksDbException {
        try {
            // Find the bookId from the Book collection based on the title
            Document result = database.getCollection("Book")
                    .find(new Document("title", bookTitle))
                    .projection(Projections.include("bookId"))
                    .first();

            if (result != null) {
                Integer bookId = result.getInteger("bookId");
                if (bookId != null) {
                    return bookId;
                } else {
                    throw new BooksDbException("Book not found with title: " + bookTitle);
                }
            } else {
                throw new BooksDbException("Book not found with title: " + bookTitle);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by Title in Book collection", e);
        }
    }


    @Override
    public void deleteBook(Book book) throws BooksDbException {
        try {
            int deletedBookId = book.getBookId();

            clearBookAuthorConnections(deletedBookId);

            deleteBookFromDatabase(deletedBookId);

            clearOrphanAuthors();

            updateBookIdsAfterDelete(deletedBookId);

            List<Integer> authorIds = getAuthorIdsForBook(deletedBookId);

            deleteAuthorsIfNeeded(authorIds);

        } catch (BooksDbException e) {
            throw e; // Re-throw the exception to propagate it to the caller
        }
    }

    @Override
    public boolean isConnected() {
        try {
            return mongoClient != null && mongoClient.getCluster().isConnected();
        } catch (Exception e) {
            // Handle the exception or log it
            e.printStackTrace();
            return false;
        }
    }


    @Override
    public void clearOrphanAuthors() throws BooksDbException {
        try {
            // Delete authors that are not associated with any books
            database.getCollection("Author").deleteMany(
                    new Document("authorId", new Document("$nin", getDistinctAuthorIds()))
            );
        } catch (MongoException e) {
            throw new BooksDbException("Error clearing orphan authors", e);
        }
    }

    @Override
    public List<Integer> getAuthorIdsForBook(int bookId) throws BooksDbException {
        List<Integer> authorIds = new ArrayList<>();
        try {
            // Find authors associated with the given book
            MongoCursor<Document> cursor = database.getCollection("Book_Author")
                    .find(new Document("bookId", bookId))
                    .iterator();

            while (cursor.hasNext()) {
                Document document = cursor.next();
                int authorId = document.getInteger("authorId");
                authorIds.add(authorId);
            }

            cursor.close();
        } catch (MongoException e) {
            throw new BooksDbException("Error getting author IDs for book", e);
        }
        return authorIds;
    }

    // Helper method to get distinct author IDs from Book_Author collection
    private List<Integer> getDistinctAuthorIds() throws BooksDbException {
        List<Integer> distinctAuthorIds = new ArrayList<>();
        try {
            MongoCursor<Document> cursor = database.getCollection("Book_Author")
                    .distinct("authorId", Integer.class)
                    .iterator();

            while (cursor.hasNext()) {
                distinctAuthorIds.add(cursor.next());
            }

            cursor.close();
        } catch (MongoException e) {
            throw new BooksDbException("Error getting distinct author IDs", e);
        }
        return distinctAuthorIds;
    }


    /*
    private void resetAuthorIdSequence() throws SQLException {
        int maxAuthorId = getMaxAuthorIdInAuthorsTable();
        String sql = "ALTER TABLE Author AUTO_INCREMENT = ?";
        try (PreparedStatement resetSequence = connection.prepareStatement(sql)) {
            resetSequence.setInt(1, maxAuthorId);
            resetSequence.executeUpdate();
        }
    }*/
    @Override
    public int getMaxAuthorIdInAuthorsTable() throws BooksDbException {
        try {
            // Find the maximum authorId in the Author collection
            MongoCursor<Document> cursor = database.getCollection("Author")
                    .find()
                    .sort(new BasicDBObject("authorId", -1))
                    .limit(1)
                    .iterator();

            if (cursor.hasNext()) {
                return cursor.next().getInteger("authorId") + 1;
            } else {
                return 1;
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error getting max author ID in Author collection", e);
        }
    }

    @Override
    public void updateAuthorIdsAfterDelete(int deletedAuthorId) throws BooksDbException {
        try {
            // Update authorIds after deleting an author
            database.getCollection("Author").updateMany(
                    new Document("authorId", new Document("$gt", deletedAuthorId)),
                    new Document("$inc", new Document("authorId", -1))
            );
        } catch (MongoException e) {
            throw new BooksDbException("Error updating author IDs after delete", e);
        }
    }

    @Override
    public void deleteAuthorsIfNeeded(List<Integer> authorIds) throws BooksDbException {
        for (Integer authorId : authorIds) {
            if (!isAuthorConnectedToOtherBooks(authorId)) {
                deleteAuthorFromDatabase(authorId);
                updateAuthorIdsAfterDelete(authorId);
            } else {
                System.out.println("Author with ID " + authorId + " is still connected to other books.");
            }
        }
    }

    @Override
    public void deleteBookFromDatabase(int bookId) throws BooksDbException {
        try {
            // Delete the book with the specified bookId from the Book collection
            database.getCollection("Book").deleteOne(new Document("bookId", bookId));
        } catch (MongoException e) {
            throw new BooksDbException("Error deleting book from Book collection", e);
        }
    }

    @Override
    public void updateBookIdsAfterDelete(int deletedBookId) throws BooksDbException {
        try {
            // Update bookIds after deleting a book
            database.getCollection("Book").updateMany(
                    new Document("bookId", new Document("$gt", deletedBookId)),
                    new Document("$inc", new Document("bookId", -1))
            );
        } catch (MongoException e) {
            throw new BooksDbException("Error updating book IDs after delete", e);
        }
    }

    /*
        private void resetBookIdSequence() throws SQLException {
            try (PreparedStatement resetSequence = connection.prepareStatement("ALTER TABLE Book AUTO_INCREMENT = ?")) {
                resetSequence.setInt(1, getMaxBookId());
                resetSequence.executeUpdate();
            }
        }*/
    @Override
    public void deleteAuthorFromDatabase(int authorId) throws BooksDbException {
        try {
            // Delete the author with the specified authorId from the Author collection
            database.getCollection("Author").deleteOne(new Document("authorId", authorId));
        } catch (MongoException e) {
            throw new BooksDbException("Error deleting author from Author collection", e);
        }
    }

    @Override
    public int getMaxBookId() throws BooksDbException {
        try {
            // Find the maximum bookId from the Book collection
            Document result = database.getCollection("Book")
                    .aggregate(Collections.singletonList(
                            new Document("$group", new Document("_id", null)
                                    .append("maxBookId", new Document("$max", "$bookId"))
                            )
                    ))
                    .first();

            if (result != null) {
                Object maxBookId = result.get("maxBookId");
                if (maxBookId instanceof Number) {
                    return ((Number) maxBookId).intValue() + 1;
                }
            }

            return 1;
        } catch (MongoException e) {
            throw new BooksDbException("Error getting max bookId from Book collection", e);
        }
    }

    @Override
    public boolean isAuthorConnectedToOtherBooks(int authorId) throws BooksDbException {
        try {
            // Check if the author is connected to other books in the Book_Author collection
            return database.getCollection("Book_Author")
                    .countDocuments(new Document("authorId", authorId)) > 0;
        } catch (MongoException e) {
            throw new BooksDbException("Error checking author connections in Book_Author collection", e);
        }
    }
}
