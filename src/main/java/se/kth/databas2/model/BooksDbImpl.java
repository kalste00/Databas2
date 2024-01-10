/*
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
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.util.*;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Accumulators.first;
import static com.mongodb.client.model.Accumulators.push;
import static com.mongodb.client.model.Aggregates.*;
import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.*;
import static javax.management.Query.match;

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
            // Use parameterized query to search for books by title
            Bson regexFilter = Filters.regex("title", Pattern.quote(searchTitle), "i");

            FindIterable<Document> bookDocuments = database.getCollection("Book")
                    .find(regexFilter)
                    .projection(fields(excludeId()));

            for (Document bookDocument : bookDocuments) {
                int bookId = bookDocument.getInteger("bookId");
                String title = bookDocument.getString("title");
                String isbn = bookDocument.getString("ISBN");
                Date publishDate = bookDocument.getDate("publishDate");
                Genre genre = Genre.valueOf(bookDocument.getString("genre"));
                int rating = bookDocument.getInteger("rating");

                // Fetch authors associated with the book
                List<Author> bookAuthors = getAuthorsForBook(bookId);

                Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                book.setBookId(bookId);
                book.getAuthors().addAll(bookAuthors);

                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }

        return result;
    }

    @Override
    public List<Book> searchBooksByISBN(String searchISBN) throws BooksDbException {
        List<Book> result = new ArrayList<>();

        try {
            // Use parameterized query to search for books by ISBN
            Bson regexFilter = Filters.regex("ISBN", Pattern.quote(searchISBN), "i");

            FindIterable<Document> bookDocuments = database.getCollection("Book")
                    .find(regexFilter)
                    .projection(fields(excludeId()));

            for (Document bookDocument : bookDocuments) {
                int bookId = bookDocument.getInteger("bookId");
                String title = bookDocument.getString("title");
                String isbn = bookDocument.getString("ISBN");
                Date publishDate = bookDocument.getDate("publishDate");
                Genre genre = Genre.valueOf(bookDocument.getString("genre"));
                int rating = bookDocument.getInteger("rating");

                // Fetch authors associated with the book
                List<Author> bookAuthors = getAuthorsForBook(bookId);

                Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                book.setBookId(bookId);
                book.getAuthors().addAll(bookAuthors);

                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by ISBN", e);
        }

        return result;
    }



    @Override
    public List<Book> searchBooksByAuthor(String searchAuthor) throws BooksDbException {
        List<Book> result = new ArrayList<>();
        searchAuthor = searchAuthor.toLowerCase();

        try {
            // Use parameterized query to search for books by author name
            String regexPattern = ".*" + Pattern.quote(searchAuthor) + ".*";
            Bson regexFilter = Filters.regex("authors.authorName", regexPattern, "i");

            FindIterable<Document> bookDocuments = database.getCollection("Book")
                    .find(regexFilter)
                    .projection(fields(excludeId()));

            for (Document bookDocument : bookDocuments) {
                int bookId = bookDocument.getInteger("bookId");
                String title = bookDocument.getString("title");
                String isbn = bookDocument.getString("ISBN");
                Date publishDate = bookDocument.getDate("publishDate");
                Genre genre = Genre.valueOf(bookDocument.getString("genre"));
                int rating = bookDocument.getInteger("rating");

                // Fetch authors associated with the book
                List<Author> bookAuthors = getAuthorsForBook(bookId);

                Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                book.setBookId(bookId);
                book.getAuthors().addAll(bookAuthors);

                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by author", e);
        }

        return result;
    }


    @Override
    public List<Book> getAllBooks() throws BooksDbException {
        List<Book> result = new ArrayList<>();
        try {
            // Aggregation to join Book and Book_Author collections
            List<Bson> pipeline = Arrays.asList(
                    lookup("Book_Author", "bookId", "bookId", "authors"),
                    unwind("$authors"),
                    group("$bookId",
                            first("title", "$title"),
                            first("isbn", "$ISBN"),
                            first("publishDate", "$publishDate"),
                            first("genre", "$genre"),
                            first("rating", "$rating"),
                            push("authors", new Document("authorId", "$authors.authorId").append("authorName", "$authors.authorName"))
                    ),
                    project(fields(
                            excludeId(),
                            include("title", "isbn", "publishDate", "genre", "rating", "authors")
                    ))
            );

            AggregateIterable<Document> aggregationResult = database.getCollection("Book").aggregate(pipeline);

            for (Document doc : aggregationResult) {
                int bookId = doc.getInteger("_id");
                String title = doc.getString("title");
                String isbn = doc.getString("isbn");
                Date publishDate = doc.getDate("publishDate");
                String genreStr = doc.getString("genre");
                int rating = doc.getInteger("rating");

                @SuppressWarnings("unchecked")
                List<Document> authorsDocs = (List<Document>) doc.get("authors");
                List<Author> bookAuthors = new ArrayList<>();

                for (Document authorDoc : authorsDocs) {
                    int authorId = authorDoc.getInteger("authorId");
                    String authorName = authorDoc.getString("authorName");
                    Author author = new Author(authorId, authorName);
                    bookAuthors.add(author);
                }

                Genre genre = Genre.valueOf(genreStr);

                Book book = new Book(bookId, title, isbn, publishDate, genre, rating);
                book.setBookId(bookId);
                book.getAuthors().addAll(bookAuthors);

                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error getting all books", e);
        }
        return result;
    }

    // A method to retrieve authors for a given book
    @Override
    public List<Author> getAuthorsForBook(int bookId) throws BooksDbException {
        List<Author> authors = new ArrayList<>();
        try {
            // Aggregation to join Book_Author and Author collections
            List<Bson> pipeline = Arrays.asList(
                    match(eq("bookId", bookId)),
                    lookup("Book_Author", "authorId", "authorId", "authors"),
                    unwind("$authors"),
                    replaceRoot("$authors")
            );

            AggregateIterable<Document> aggregationResult = database.getCollection("Author").aggregate(pipeline);

            for (Document doc : aggregationResult) {
                int authorId = doc.getInteger("authorId");
                String authorName = doc.getString("authorName");
                Author author = new Author(authorId, authorName);
                authors.add(author);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching books by Title", e);
        }
        return authors;
    }


    @Override
    public void addBook(Book book) throws BooksDbException {
        try {
            // Create a document for the new book
            Document bookDoc = new Document()
                    .append("title", book.getTitle())
                    .append("isbn", book.getIsbn())
                    .append("publishDate", book.getPublishDate())
                    .append("genre", book.getGenre().toString())
                    .append("rating", book.getRating());

            // Insert the document into the "Book" collection
            database.getCollection("Book").insertOne(bookDoc);

            // Retrieve the generated bookId from the inserted document
            int bookId = bookDoc.getInteger("_id");

            System.out.println("Generated Book ID: " + bookId);
            System.out.println("Generated author ID: " + book.getAuthors());

            book.setBookId(bookId);

            // Clear existing author connections for the book
            clearBookAuthorConnections(bookId);

            // Add authors and connections
            addAuthorsAndConnections(book, book.getBookId(), book.getAuthors());

            System.out.println("Book added successfully.");

        } catch (MongoException e) {
            throw new BooksDbException("Error adding book: " + e.getMessage(), e);
        }
    }

    @Override
    public void clearBookAuthorConnections(int bookId) throws BooksDbException {
        try {
            // Delete author connections for the specified bookId
            database.getCollection("Book_Author")
                    .deleteMany(eq("bookId", bookId));

            System.out.println("Cleared existing author connections for book with ID " + bookId);
        } catch (MongoException e) {
            throw new BooksDbException("Error clearing author connections: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean authorExists(String authorName) throws BooksDbException {
        try {
            // Check if an author document with the given name exists
            return database.getCollection("Author")
                    .find(eq("authorName", authorName))
                    .limit(1)
                    .iterator()
                    .hasNext();
        } catch (MongoException e) {
            throw new BooksDbException("Error checking author existence: " + e.getMessage(), e);
        }
    }

    @Override
    public int getAuthorId(String authorName) throws BooksDbException {
        try {
            // Find the author document with the given name and retrieve the authorId
            Document authorDocument = database.getCollection("Author")
                    .find(eq("authorName", authorName))
                    .projection(fields(include("authorId"), excludeId()))
                    .limit(1)
                    .first();

            if (authorDocument != null) {
                return authorDocument.getInteger("authorId");
            } else {
                throw new BooksDbException("Author not found with name: " + authorName);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error getting author ID: " + e.getMessage(), e);
        }
    }

    @Override
    public int addAuthorAndGetId(Author author) throws BooksDbException {
        try {
            // Insert a new author document and retrieve the generated authorId
            Document authorDocument = new Document("authorName", author.getName());
            database.getCollection("Author").insertOne(authorDocument);

            if (authorDocument.containsKey("_id")) {
                return authorDocument.getInteger("_id");
            } else {
                throw new BooksDbException("Failed to get generated author ID");
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error adding author and getting ID: " + e.getMessage(), e);
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
            Bson filter = eq("bookId", book.getBookId());

            // Start a transaction
            ClientSession session = mongoClient.startSession();
            session.startTransaction();

            try {
                // Update the main book information
                Bson updateBook = Updates.combine(
                        Updates.set("title", book.getTitle()),
                        Updates.set("ISBN", book.getIsbn()),
                        Updates.set("publishDate", book.getPublishDate()),
                        Updates.set("genre", book.getGenre().toString()),
                        Updates.set("rating", book.getRating())
                );
                UpdateResult bookUpdateResult = database.getCollection("Book").updateOne(session, filter, updateBook);

                if (bookUpdateResult.getModifiedCount() == 0) {
                    throw new BooksDbException("Failed to update book with ID: " + book.getBookId());
                }

                // Update the authors
                updateBookAuthors(book, book.getAuthors(), session);

                // Commit the transaction
                session.commitTransaction();
            } catch (Exception e) {
                // Rollback the transaction if an error occurs
                session.abortTransaction();
                throw e;
            } finally {
                // Close the session
                session.close();
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error updating book: " + e.getMessage(), e);
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
                Bson filter = eq("bookId", book.getBookId());
                Bson update = Updates.set("title", book.getTitle());

                UpdateResult result = database.getCollection("Book").updateOne(filter, update);

                if (result.getModifiedCount() == 0) {
                    throw new BooksDbException("Failed to update title for book with ID: " + book.getBookId());
                }
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error updating title: " + e.getMessage(), e);
        }
    }

    private void updateIsbn(Book book) throws BooksDbException {
        try {
            if (book.getIsbn() != null && !book.getIsbn().isEmpty()) {
                Bson filter = eq("bookId", book.getBookId());
                Bson update = Updates.set("ISBN", book.getIsbn());

                UpdateResult result = database.getCollection("Book").updateOne(filter, update);

                if (result.getModifiedCount() == 0) {
                    throw new BooksDbException("Failed to update ISBN for book with ID: " + book.getBookId());
                }
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error updating ISBN: " + e.getMessage(), e);
        }
    }



    private void updatePublishDate(Book book) throws BooksDbException {
        try {
            if (book.getPublishDate() != null) {
                Bson filter = eq("bookId", book.getBookId());
                Bson update = Updates.set("publishDate", book.getPublishDate());

                UpdateResult result = database.getCollection("Book").updateOne(filter, update);

                if (result.getModifiedCount() == 0) {
                    throw new BooksDbException("Failed to update publish date for book with ID: " + book.getBookId());
                }
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error updating publish date: " + e.getMessage(), e);
        }
    }

    private void updateGenre(Book book) throws BooksDbException {
        try {
            if (book.getGenre() != null) {
                Bson filter = eq("bookId", book.getBookId());
                Bson update = Updates.set("genre", book.getGenre().toString());

                UpdateResult result = database.getCollection("Book").updateOne(filter, update);

                if (result.getModifiedCount() == 0) {
                    throw new BooksDbException("Failed to update genre for book with ID: " + book.getBookId());
                }
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error updating genre: " + e.getMessage(), e);
        }
    }


    private void updateRating(Book book) throws BooksDbException {
        try {
            // Find the book by bookId and update the rating
            database.getCollection("Book")
                    .updateOne(eq("bookId", book.getBookId()),
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
                    .projection(include("bookId"))
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


}