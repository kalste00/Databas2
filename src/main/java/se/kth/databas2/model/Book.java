package se.kth.databas.model;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

/**
 * Representation of a book.
 *
 * @author anderslm@kth.se
 */
public class Book {

    private int bookId;
    private String isbn;
    private String title;
    private Date publishDate;
    private Genre genre;
    private int rating;
    private String storyLine = "";
    private List<Author> authors = new ArrayList<>();

    public Book(int bookId, String title, String isbn, Date publishDate, Genre genre, int rating) {
        this.bookId = bookId;
        this.title = title;
        this.isbn = isbn;
        this.publishDate = publishDate;
        this.genre = genre;
        this.rating = rating;
    }

    public Book(String title, String isbn, Date publishDate, Genre genre, int rating) {
        this(-1, title, isbn, publishDate, genre, rating);
    }

    public int getBookId() { return bookId; }

    public void setBookId(int bookId) {
        this.bookId = bookId;
    }

    public String getIsbn() { return isbn; }
    public String getTitle() { return title; }
    public Date getPublishDate() { return publishDate; }
    public String getStoryLine() { return storyLine; }

    public void setStoryLine(String storyLine) {
        this.storyLine = storyLine;
    }

    public List<Author> getAuthors() {
        return authors;
    }

    public void setAuthors(List<Author> authors) {
        this.authors = authors;
    }

    public void addAuthor(Author author) {
        authors.add(author);
    }

    public int getRating() {
        return rating;
    }

    public void setRating(int rating) {
        this.rating = rating;
    }

    public Genre getGenre() {
        return genre;
    }

    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setPublishDate(Date publishDate) {
        this.publishDate = publishDate;
    }

    public void setGenre(Genre genre) {
        this.genre = genre;
    }

    @Override
    public String toString() {
        return title + ", " + isbn + ", " + publishDate.toString() + ", " + authors + ", " + genre + ", " + rating;
    }
}
