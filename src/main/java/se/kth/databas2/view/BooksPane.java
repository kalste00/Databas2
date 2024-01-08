package se.kth.databas.view;

import java.sql.Date;
import java.util.List;
import java.util.Optional;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import se.kth.databas.model.Book;
import se.kth.databas.model.BooksDbImpl;
import se.kth.databas.model.SearchMode;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;

import static javafx.scene.control.Alert.AlertType.WARNING;


/**
 * The main pane for the view, extending VBox and including the menus. An
 * internal BorderPane holds the TableView for books and a search utility.
 *
 * @author anderslm@kth.se
 */
public class BooksPane extends VBox{

    private TableView<Book> booksTable;
    private ObservableList<Book> booksInTable; // the data backing the table view

    private ComboBox<SearchMode> searchModeBox;
    private TextField searchField;
    private Button searchButton;

    private MenuBar menuBar;

    public BooksPane(BooksDbImpl booksDb) {
        final Controller controller = new Controller(booksDb, this);
        this.init(controller);
    }

    /**
     * Display a new set of books, e.g. from a database select, in the
     * booksTable table view.
     *
     * @param books the books to display
     */
    public void displayBooks(List<Book> books) {
        booksInTable.clear();
        booksInTable.addAll(books);
        booksTable.refresh(); // Add this line to refresh the TableView
    }


    /**
     * Notify user on input error or exceptions.
     *
     * @param msg the message
     * @param type types: INFORMATION, WARNING et c.
     */
    protected void showAlertAndWait(String msg, Alert.AlertType type) {
        // types: INFORMATION, WARNING et c.
        Alert alert = new Alert(type, msg);
        alert.showAndWait();
    }

    private void init(Controller controller) {

        booksInTable = FXCollections.observableArrayList();

        // init views and event handlers
        initBooksTable();
        initSearchView(controller);
        initMenus(controller);

        FlowPane bottomPane = new FlowPane();
        bottomPane.setHgap(10);
        bottomPane.setPadding(new Insets(10, 10, 10, 10));
        bottomPane.getChildren().addAll(searchModeBox, searchField, searchButton);

        BorderPane mainPane = new BorderPane();
        mainPane.setCenter(booksTable);
        mainPane.setBottom(bottomPane);
        mainPane.setPadding(new Insets(10, 10, 10, 10));

        this.getChildren().addAll(menuBar, mainPane);
        VBox.setVgrow(mainPane, Priority.ALWAYS);
    }

    private void initBooksTable() {
        booksTable = new TableView<>();
        booksTable.setEditable(false); // don't allow user updates (yet)
        booksTable.setPlaceholder(new Label("No rows to display"));

        // define columns
        TableColumn<Book, String> titleCol = new TableColumn<>("Title");
        TableColumn<Book, String> isbnCol = new TableColumn<>("ISBN");
        TableColumn<Book, Date> publishDateCol = new TableColumn<>("Published");
        TableColumn<Book, List<String>> authorCol = new TableColumn<>("Author/s");
        TableColumn<Book, String> genreCol = new TableColumn<>("Genre");
        TableColumn<Book, String> ratingCol = new TableColumn<>("Rating");
        booksTable.getColumns().addAll(titleCol, isbnCol, publishDateCol, authorCol, genreCol, ratingCol);
        // give title column some extra space
        titleCol.prefWidthProperty().bind(booksTable.widthProperty().multiply(0.4));
        authorCol.prefWidthProperty().bind(booksTable.widthProperty().multiply(0.2));

        // define how to fill data for each cell,
        // get values from Book properties
        titleCol.setCellValueFactory(new PropertyValueFactory<>("title"));
        isbnCol.setCellValueFactory(new PropertyValueFactory<>("isbn"));
        publishDateCol.setCellValueFactory(new PropertyValueFactory<>("publishDate"));
        authorCol.setCellValueFactory(new PropertyValueFactory<>("authors"));
        genreCol.setCellValueFactory(new PropertyValueFactory<>("genre"));
        ratingCol.setCellValueFactory(new PropertyValueFactory<>("rating"));
        // associate the table view with the data
        booksTable.setItems(booksInTable);
    }

    private void initSearchView(Controller controller) {
        searchField = new TextField();
        searchField.setPromptText("Search for...");
        searchModeBox = new ComboBox<>();
        searchModeBox.getItems().addAll(SearchMode.values());
        searchModeBox.setValue(SearchMode.Title);
        searchButton = new Button("Search");

        // event handling (dispatch to controller)
        searchButton.setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                String searchFor = searchField.getText();
                SearchMode mode = searchModeBox.getValue();
                controller.onSearchSelected(searchFor, mode);
            }
        });
    }

    private void initMenus(Controller controller) {

        Menu fileMenu = new Menu("File");

        MenuItem exitItem = new MenuItem("Exit");
        exitItem.setOnAction(event -> {
            controller.disconnectFromDatabase();
            System.exit(0);
        });
        exitItem.setOnAction(event -> System.exit(0));

        MenuItem connectItem = new MenuItem("Connect to Db");
        connectItem.setOnAction(event -> controller.connectToDatabase());

        MenuItem disconnectItem = new MenuItem("Disconnect");
        disconnectItem.setOnAction(event -> controller.disconnectFromDatabase());

        fileMenu.getItems().addAll(exitItem, connectItem, disconnectItem);
        Menu searchMenu = new Menu("Search");
        MenuItem titleItem = new MenuItem("Title");
        MenuItem isbnItem = new MenuItem("ISBN");
        MenuItem authorItem = new MenuItem("Author");
        MenuItem allBooks = new MenuItem("AllBooks");
        allBooks.setOnAction(event -> {
            controller.onSearchSelected("", SearchMode.AllBooks);
        });
        searchMenu.getItems().addAll(titleItem, isbnItem, authorItem, allBooks);
        Menu manageMenu = new Menu("Manage");
        MenuItem addItem = new MenuItem("Add");
        addItem.setOnAction(event -> {
            Optional<Book> newBook = Dialogs.showAddDialog();
            newBook.ifPresent(book -> controller.addItem(book));
        });
        MenuItem removeItem = new MenuItem("Remove");
        removeItem.setOnAction(event -> {
            Book selectedBook = booksTable.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                controller.deleteBooks(selectedBook);
            } else {
                showAlertAndWait("Select a book to remove.", WARNING);
            }
        });
        MenuItem updateItem = new MenuItem("Update");
        updateItem.setOnAction(event -> {
            Book selectedBook = booksTable.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                Optional<Book> updatedBook = Dialogs.showUpdateDialog(selectedBook);
                updatedBook.ifPresent(book -> controller.updateItem(book));
            } else {
                showAlertAndWait("Select a book to update.", WARNING);
            }
        });
        MenuItem rateItem = new MenuItem("Rate");
        rateItem.setOnAction(event -> {
            Book selectedBook = booksTable.getSelectionModel().getSelectedItem();
            if (selectedBook != null) {
                controller.rateBook(selectedBook);
            } else {
                showAlertAndWait("Select a book to rate.", WARNING);
            }
        });
        manageMenu.getItems().addAll(addItem, removeItem, updateItem, rateItem);

        menuBar = new MenuBar();
        menuBar.getMenus().addAll(fileMenu, searchMenu, manageMenu);
    }
}
