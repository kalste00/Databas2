package se.kth.databas2.model;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;

/**
 * En mock-implementering av gränssnittet BooksDbInterface för att visa hur man använder det
 * tillsammans med användargränssnittet.
 *
 * Denna implementation måste komma åt en riktig databas.
 *
 * @author anderslm@kth.se
 */
public class BooksDbImpl implements BooksDbInterface {

    private MongoClient mongoClient;
    private MongoDatabase database;

    @Override
    public boolean connect(String databaseName) throws BooksDbException {
        try {
            String connectionString = "mongodb+srv://kallestenbjelke:Gaming123@kcdb.gt7n66h.mongodb.net/";
            ConnectionString connString = new ConnectionString(connectionString);
            MongoClientSettings settings = MongoClientSettings.builder()
                    .applyConnectionString(connString)
                    .build();

            this.mongoClient = MongoClients.create(settings);
            this.database = this.mongoClient.getDatabase(databaseName);

            Document pingCommand = new Document("ping", 1);
            this.database.runCommand(pingCommand);

            System.out.println("Pinged your deployment. You successfully connected to MongoDB!");
            return true;
        } catch (MongoException e) {
            e.printStackTrace();
            throw new BooksDbException("Could not connect to MongoDB", e);
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
            throw new BooksDbException("Error disconnecting from MongoDB", e);
        }
    }

    private Book documentToBook(Document document) {
        int bookId = (document.getInteger("bookId") != null) ? document.getInteger("bookId").intValue() : 0;
        String title = document.getString("title");
        String isbn = document.getString("ISBN");
        LocalDate publishDate = LocalDate.parse(document.getString("publishDate"));
        Genre genre = Genre.valueOf(document.getString("genre"));
        int rating = document.getInteger("rating");

        List<Document> authorDocuments = (List<Document>) document.get("authors");
        List<Author> authors = new ArrayList<>();

        if (authorDocuments != null) {
            for (Document authorDocument : authorDocuments) {
                int authorId = (authorDocument.getInteger("authorId") != null) ? authorDocument.getInteger("authorId").intValue() : 0;
                String authorName = authorDocument.getString("authorName");
                Author author = new Author(authorId, authorName);
                authors.add(author);
            }
        }

        return new Book(bookId, title, isbn, publishDate, genre, rating, authors);
    }

    @Override
    public List<Book> searchBooksByTitle(String searchTitle) throws BooksDbException {
        if (this.database == null) {
            throw new BooksDbException("Database connection is null. Make sure to connect before performing queries.");
        }
        searchTitle = searchTitle.toLowerCase();
        List<Book> books = new ArrayList<>();
        FindIterable<Document> result = null;
        MongoCursor<Document> iterator = null;

        try {
            MongoCollection<Document> bookCollection = this.database.getCollection("Book");
            result = bookCollection.find(eq("title", searchTitle));
            iterator = result.iterator();
            while (iterator.hasNext()) {
                Document book = iterator.next();
                books.add(documentToBook(book));
            }
            return books;
        } catch (MongoException e) {
            throw new BooksDbException("Error searching by title", e);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    @Override
    public List<Book> searchBooksByISBN(String searchISBN) throws BooksDbException {
        List<Book> books = new ArrayList<>();
        FindIterable<Document> result = null;
        MongoCursor<Document> iterator = null;

        try {
            MongoCollection<Document> bookCollection = this.database.getCollection("Book");
            result = bookCollection.find(eq("ISBN", searchISBN));

            iterator = result.iterator();
            while (iterator.hasNext()) {
                Document book = iterator.next();
                books.add(documentToBook(book));
            }
            return books;
        } catch (MongoException e) {
            throw new BooksDbException("Error searching book by ISBN", e);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    @Override
    public List<Book> searchBooksByAuthor(String author) throws BooksDbException {
        List<Book> books = new ArrayList<>();
        FindIterable<Document> result = null;
        MongoCursor<Document> iterator = null;
        author.toLowerCase();

        try {
            MongoCollection<Document> bookCollection = this.database.getCollection("Book");
            result = bookCollection.find(eq("authors.authorName", author));

            iterator = result.iterator();
            while (iterator.hasNext()) {
                Document book = iterator.next();
                books.add(documentToBook(book));
            }
            return books;
        } catch (MongoException e) {
            throw new BooksDbException("Error searching book by author", e);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }

    @Override
    public List<Book> getAllBooks() throws BooksDbException {
        List<Book> result = new ArrayList<>();
        MongoCursor<Document> iterator = null;

        try {
            MongoCollection<Document> bookCollection = this.database.getCollection("Book");
            iterator = bookCollection.find().projection(fields(excludeId())).iterator();

            while (iterator.hasNext()) {
                Document book = iterator.next();
                result.add(documentToBook(book));
            }
            return result;
        } catch (MongoException e) {
            throw new BooksDbException("Error retrieving all books", e);
        } finally {
            if (iterator != null) {
                iterator.close();
            }
        }
    }


    @Override
    public void addBook(Book book) throws BooksDbException {
        try {
            this.database.getCollection("Book").insertOne(book);
            System.out.println("Added book with ID: " + book.getBookId());
        } catch (MongoException e) {
            System.out.println("Error adding book: " + e.getMessage());
            throw new BooksDbException("Error adding book", e);
        }
    }

    @Override
    public void updateBook(Book updatedItem) throws BooksDbException {
        try {
            Bson filter = eq("bookId", updatedItem.getBookId());
            Bson update = Updates.combine(
                    Updates.set("title", updatedItem.getTitle()),
                    Updates.set("ISBN", updatedItem.getIsbn()),
                    Updates.set("publishDate", updatedItem.getPublishDate().toString()),
                    Updates.set("genre", updatedItem.getGenre().toString()),
                    Updates.set("rating", updatedItem.getRating()),
                    Updates.set("authors", updatedItem.getAuthors())
            );

            UpdateResult updateResult = database.getCollection("Book", Book.class).updateOne(filter, update);

            if (updateResult.getModifiedCount() > 0) {
                System.out.println("Updated book with ID: " + updatedItem.getBookId());
            } else {
                System.out.println("No book found with ID: " + updatedItem.getBookId());
            }
        } catch (MongoException e) {
            System.out.println("Error updating book: " + e.getMessage());
            throw new BooksDbException("Error updating book", e);
        }
    }

    @Override
    public void deleteBook(Book itemToDelete) throws BooksDbException {
        try {
            Bson filter = eq("bookId", itemToDelete.getBookId());
            database.getCollection("Book", Book.class).deleteOne(filter);
            System.out.println("Deleted book with ID: " + itemToDelete.getBookId());
        } catch (MongoException e) {
            System.out.println("Error deleting book: " + e.getMessage());
            throw new BooksDbException("Error deleting book", e);
        }
    }
}
