package se.kth.databas2.model;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.*;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.codecs.configuration.CodecRegistry;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.bson.conversions.Bson;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Projections.excludeId;
import static com.mongodb.client.model.Projections.fields;

/**
 * En mock-implementering av gränssnittet BooksDbInterface för att visa hur man använder det
 * tillsammans med användargränssnittet.
 * <p>
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
            mongoClient = MongoClients.create("mongodb+srv://kallestenbjelke:Gaming123@kcdb2.gt7n66h.mongodb.net/?retryWrites=true&w=majority");

            // Lägg till stöd för POJO-mappning
            CodecRegistry pojoCodecRegistry = org.bson.codecs.configuration.CodecRegistries.fromRegistries(
                    MongoClientSettings.getDefaultCodecRegistry(),
                    org.bson.codecs.configuration.CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
            );

            MongoClientSettings settings = MongoClientSettings.builder()
                    .codecRegistry(pojoCodecRegistry)
                    .build();

            database = mongoClient.getDatabase(databaseName).withCodecRegistry(pojoCodecRegistry);

            System.out.println("Connected to MongoDb");
            return true;
        } catch (MongoException e) {
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

    @Override
    public List<Book> searchBooksByTitle(String searchTitle) throws BooksDbException {
        List<Book> result = new ArrayList<>();
        searchTitle = searchTitle.toLowerCase();

        try {
            Bson regexFilter = Filters.regex("title", Pattern.quote(searchTitle), "i");

            FindIterable<Book> bookDocuments = database.getCollection("Book", Book.class)
                    .find(regexFilter)
                    .projection(fields(excludeId()));

            for (Book book : bookDocuments) {
                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching book by title", e);
        }

        return result;
    }

    @Override
    public List<Book> searchBooksByISBN(String searchISBN) throws BooksDbException {
        List<Book> result = new ArrayList<>();

        try {
            Bson regexFilter = Filters.regex("ISBN", Pattern.quote(searchISBN), "i");

            FindIterable<Book> bookDocuments = database.getCollection("Book", Book.class)
                    .find(regexFilter)
                    .projection(fields(excludeId()));

            for (Book book : bookDocuments) {
                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching book by ISBN", e);
        }

        return result;
    }

    @Override
    public List<Book> searchBooksByAuthor(String author) throws BooksDbException {
        List<Book> result = new ArrayList<>();

        try {
            Bson regexFilter = Filters.regex("authors.name", Pattern.quote(author), "i");

            FindIterable<Book> bookDocuments = database.getCollection("Book", Book.class)
                    .find(regexFilter)
                    .projection(fields(excludeId()));

            for (Book book : bookDocuments) {
                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching book by author", e);
        }

        return result;
    }

    @Override
    public List<Book> getAllBooks() throws BooksDbException {
        List<Book> result = new ArrayList<>();
        try {
            FindIterable<Book> bookDocuments = database.getCollection("Book", Book.class)
                    .find()
                    .projection(fields(excludeId()));

            for (Book book : bookDocuments) {
                result.add(book);
            }
        } catch (MongoException e) {
            throw new BooksDbException("Error searching for all books", e);
        }

        return result;
    }

    @Override
    public void addBook(Book book) throws BooksDbException {
        try {
            database.getCollection("Book", Book.class).insertOne(book);
            System.out.println("Added book with ID: " + book.getBookId());
        } catch (MongoException e) {
            System.out.println("Error adding book: " + e.getMessage());
            throw new BooksDbException("Error adding book", e);
        }
    }

    @Override
    public void updateBook(Book updatedItem) throws BooksDbException {
        try {
            // Uppdatera boken i "Book"-samlingen
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
