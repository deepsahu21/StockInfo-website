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

            latestNewsLabel.setText("fetching news...");
            String symbol = query.getText();
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
        
        Thread thread = new Thread(() -> {

            String url = FMP_ENDPOINT + URLEncoder.encode(symbol, StandardCharsets.UTF_8);
            url += "?apikey=" + FMP_API_KEY;

            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();

                HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());
                System.out.println("Stock JSON Response: " + response.body());

                
                StockItem[] items = GSON.fromJson(response.body(), StockItem[].class);

                if (items != null && items.length > 0) { //Displays data

                    StockItem stock = items[0];

                    Platform.runLater(() -> {

                        nameLabel.setText("Name: " + stock.name);
                        priceLabel.setText("Price: " + stock.price);
                        volumeLabel.setText("Volume: " + stock.volume);
                        peRatioLabel.setText("PE Ratio: " + stock.pe);
                    });

                    getNews(symbol);
                } else {
                    //If invalid response
                    throw new IllegalArgumentException("No stock data found: " + symbol);
                }
            } catch (Exception e) {

                Platform.runLater(() -> {
                    nameLabel.setText("error fetching stock data");
                    nameLabel.setText("Name: ");
                    priceLabel.setText("Price: ");
                    volumeLabel.setText("Volume: ");
                    peRatioLabel.setText("PE Ratio: ");

                    latestNewsLabel.setText("Latest News:");
                    newsArticles.setText("");

                    Alert alert = new Alert(Alert.AlertType.ERROR);
                    alert.setTitle("Lookup Error");
                    alert.setHeaderText("Couldn't find '" + symbol + "'");
                    alert.setContentText(e.getMessage());
                    alert.show();
                });
            }
        });

        thread.setDaemon(true);
        thread.start();
    }

    /**
     * Fetches news articles with the provided stock symbol from the NewsData API.
     * Updates the UI with up to 5 unique news titles and their corresponding links.
     *
     * @param symbol the stock ticker symbol used as the query term for news
     */
    private void getNews(String symbol) {

        Thread thread = new Thread(() -> {

            String url = CURRENTS_ENDPOINT + CURRENTS_API_KEY + "&q="; //constructs url
            url += URLEncoder.encode(symbol, StandardCharsets.UTF_8) + "&language=en";

            try {
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).build();
                HttpResponse<String> response = HTTP_CLIENT.send(request, BodyHandlers.ofString());

                NewsResponse news = GSON.fromJson(response.body(), NewsResponse.class);

                if (news != null && news.results != null && news.results.length > 0) {
                    String output = "";
                    String[] addedTitles = new String[5];
                    int added = 0;

                    for (int i = 0; i < news.results.length && added < 5; i++) {

                        Article a = news.results[i];
                        boolean isDup = false;

                        for (int j = 0; j < added; j++) {

                            //Checks for duplicates
                            if (addedTitles[j] != null && addedTitles[j].equals(a.title)) {

                                isDup = true;
                                break;
                            }
                        }

                        if (!isDup) { // Adds if it is not a duplicate

                            addedTitles[added] = a.title;
                            output += a.title + "\n\t" + a.link + "\n\n";
                            added++;
                        }
                    }

                    final String finalOutput = output;
                    Platform.runLater(() -> newsArticles.setText(finalOutput));
                    Platform.runLater(() -> latestNewsLabel.setText("Latest News:"));
                } else {
                    throw new IllegalArgumentException("No news articles found");
                }
            } catch (Exception e) {

                Platform.runLater(() -> {
                    // Displays this if no articles found.
                    newsArticles.setText("No latest news on query");
                });
            }

        });

        thread.setDaemon(true);
        thread.start();
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