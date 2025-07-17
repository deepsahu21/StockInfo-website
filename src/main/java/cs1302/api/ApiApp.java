package cs1302.api;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.scene.Scene;
import javafx.stage.Stage;
import javafx.scene.layout.HBox;
import javafx.scene.control.TextField;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.geometry.Insets;


import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javafx.scene.control.Alert;



/**
 * A JavaFX application that allows the user the enter a stock symbol and then retrieve
 * basic stock market information and the 5 most recent distinct news artices related to the stock.
 * It uses the Financial Modeling Prep API to get stock quotes and the NewsData API
 * to fetch news articles.
 */
public class ApiApp extends Application {

    private static final boolean SIMULATION_MODE = true;


    Stage stage;
    Scene scene;
    VBox root;

    //3 sections of app
    private HBox topBox;
    private VBox middleBox;
    private VBox bottomBox;


    //topBox items
    private TextField query;
    private Button search;
    private Label prompt;

    //middleBox items
    private Label headerLabel;
    private Label nameLabel;
    private Label priceLabel;
    private Label volumeLabel;
    private Label peRatioLabel;

    //bottommBox items
    private Label latestNewsLabel;
    private TextArea newsArticles;


    //Stock data items

    /** HTTP client. */
    public static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
        .version(HttpClient.Version.HTTP_2)           // uses HTTP protocol version 2 where possible
        .followRedirects(HttpClient.Redirect.NORMAL)  // always redirects, except from HTTPS to HTTP
        .build();                                     // builds and returns a HttpClient object

    /** Google {@code Gson} object for parsing JSON-formatted strings. */
    public static Gson GSON = new GsonBuilder()
        .setPrettyPrinting()                          // enable nice output when printing
        .create();                                    // builds and returns a Gson object

    /**
     * Class representing stock items returned by the Financial Modeling Prep API.
     */
    private static class StockItem {
        public String name;
        public double price;
        public double volume;
        public double pe;
    }

    private static final String FMP_ENDPOINT = "https://financialmodelingprep.com/api/v3/quote/"; // base url for financial model prep API

    //API key for financial model prep API
    private static final String FMP_API_KEY = System.getenv("FMP_API_KEY");


    //News API items

    private static final String CURRENTS_ENDPOINT = "https://newsdata.io/api/1/latest?apikey="; //base url for the NewsData API

    //API key for the NewsData API
    private static final String CURRENTS_API_KEY = System.getenv("CURRENTS_API_KEY");

    /**
     * class for the response returned by the news API.
     */
    private static class NewsResponse {
        public Article[] results;

    }

    /**
     * Class representing news items returned by the news API.
     */
    private static class Article {
        public String title;
        public String link;

    }

    /**
     * Constructs an {@code ApiApp} object. This default (i.e., no argument)
     * constructor is executed in Step 2 of the JavaFX Application Life-Cycle.
     * initializes UI parts, sets their layout, and adds the action handler for the search button
     * to get stock and news data
     */
    public ApiApp() {
        root = new VBox();
        root.setSpacing(20);
        root.setPadding(new javafx.geometry.Insets(20));
        root.setStyle("-fx-background-color: #1e1e1e;"); // Dark background

        topBox = new HBox(10);
        topBox.setMaxWidth(Double.MAX_VALUE);
        
        middleBox = new VBox();
        middleBox.setStyle("-fx-background-color: #252526; -fx-background-radius: 8; -fx-padding: 10;");

        bottomBox = new VBox();

        root.getChildren().addAll(topBox, middleBox, bottomBox); //adds 3 sections to root

        setTopBox();
        topBox.setAlignment(javafx.geometry.Pos.CENTER);
        setMiddleBox();
        setBottomBox();

        search.setOnAction(e -> { //search button action
    String symbol = query.getText();

    if (SIMULATION_MODE && symbol.equalsIgnoreCase("INVALID")) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lookup Error");
            alert.setHeaderText("Couldn't find information for ticker");
            alert.setContentText("error: Invalid ticker.");
            alert.show();
        });
        return;
    }

    latestNewsLabel.setText("fetching news...");
    getStock(symbol);
});

    } // ApiApp

    /**
     * Initializes and sets the layout for the top section of the UI.
     * This section includes the prompt label, input text field, and search button.
     */
    private void setTopBox() {

        //intialize
        prompt = new Label("Enter Stock ticker symbol: ");
        prompt.setStyle("-fx-text-fill: #ffffff; -fx-font-size: 14px;");

        query = new TextField("TSLA");
        query.setStyle("-fx-background-radius: 6; -fx-padding: 5;");

        search = new Button("Search");
        search.setStyle("-fx-background-color: #0e639c; -fx-text-fill: #ffffff; -fx-background-radius: 6;");

        HBox.setHgrow(query, javafx.scene.layout.Priority.ALWAYS);
        query.setMaxWidth(Double.MAX_VALUE);

        
        

        topBox.getChildren().addAll(prompt, query, search);
        
    }

    /**
     * Initializes and sets the layout for the middle section of the UI.
     * This section includes stock information like the name,price, volume,and PE ratio
     */
    private void setMiddleBox() {
        headerLabel = new Label("Company Info:");
        nameLabel = new Label("Name: ");
        priceLabel = new Label("Price: ");
        volumeLabel = new Label("Volume: ");
        peRatioLabel = new Label("PE Ratio: ");

        //Styles
        headerLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        nameLabel.setStyle("-fx-text-fill: #cccccc;");
        priceLabel.setStyle("-fx-text-fill: #cccccc;");
        volumeLabel.setStyle("-fx-text-fill: #cccccc;");
        peRatioLabel.setStyle("-fx-text-fill: #cccccc;");

        
        
        // adds variables to middleBox
        middleBox.setPadding(new Insets(10));
        middleBox.getChildren().addAll(headerLabel, nameLabel); 
        middleBox.getChildren().addAll(priceLabel, volumeLabel, peRatioLabel);

        middleBox.setSpacing(10);

        

    }

    /**
     * Initializes and sets the layout for the bottom section of the UI.
     * This section includes the most recent 5 distinct news articles
     */
    private void setBottomBox() {

        latestNewsLabel = new Label("Latest news:");
        newsArticles = new TextArea();

        newsArticles.setEditable(false);


        bottomBox.getChildren().addAll(latestNewsLabel, newsArticles); //adds variables to bottomBox

        latestNewsLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #ffffff;");
        newsArticles.setStyle("-fx-background-radius: 6; -fx-control-inner-background: #2d2d2d; -fx-text-fill: #ffffff;");
    }


    /**
     * Gets the stock data from the Financial Modeling Prep API with the symbol as the query.
     * If data is successfully retrieved, it updates the UI with stock details
     * and calls getNext() method to get the related news
     *
     * @param symbol the stock ticker symbol
     */
    private void getStock(String symbol) {
    if (SIMULATION_MODE) {
        Platform.runLater(() -> {
            nameLabel.setText("Name: Tesla Inc.");
            priceLabel.setText("Price: $319.41");
            volumeLabel.setText("Volume: 73,310,775");
            peRatioLabel.setText("PE Ratio: 175.80");
        });
        getNews(symbol); // also simulate news
        return;
    }
    // ... your original code below ...
}

    /**
     * Fetches news articles with the provided stock symbol from the NewsData API.
     * Updates the UI with up to 5 unique news titles and their corresponding links.
     *
     * @param symbol the stock ticker symbol used as the query term for news
     */
    private void getNews(String symbol) {
    if (SIMULATION_MODE) {
        Platform.runLater(() -> {
            newsArticles.setText(
                "Tesla exec hints at useful and potentially killer Model Y L feature\n\thttps://www.teslarati.com/tesla-exec-hints-killer-model-y-l-feature/\n\n"+
                "Cathie Wood Buys $36 Million Of Tesla As The Stock Forms A New Base Ahead Of Earnings\n\thttps://www.investors.com/news/cathie-wood-stock-market-buys-more-tesla-stock-ahead-of-earnings/\n\n" +
                "Analysts predict growth for TSLA\n\thttps://teslanews.com/article3\n\n" +
                "Tesla expands into new markets\n\thttps://teslanews.com/article4\n\n" +
                "Elon Musk tweets spark interest\n\thttps://teslanews.com/article5"
            );
            latestNewsLabel.setText("Latest News:");
        });
        return;
    }
    // ... your original code below ...
}




    /** {@inheritDoc} */
    @Override
    public void start(Stage stage) {

        this.stage = stage;


        scene = new Scene(root, 500, 400);
        

        // setup stage
        stage.setTitle("Stock insights & news app");
        stage.setScene(scene);
        stage.setOnCloseRequest(event -> Platform.exit());
        stage.sizeToScene();
        stage.show();

    } // start

} // ApiApp