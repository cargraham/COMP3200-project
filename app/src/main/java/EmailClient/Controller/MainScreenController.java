package EmailClient.Controller;

import EmailClient.App;
import EmailClient.Graph;
import com.microsoft.graph.models.MailFolder;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import com.microsoft.graph.models.User;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class MainScreenController {

    @FXML
    public ListView<HBox> messageListView;

    @FXML
    public VBox messageHeader;

    @FXML
    public Label senderText;

    @FXML
    public Label recipientText;

    @FXML
    public Label subjectText;

    @FXML
    public Label timeStampText;

    @FXML
    public Text bodyText;

    @FXML
    public TreeView<String> foldersList;

    @FXML
    public Pane webviewPane;

    public HashMap<HBox, Message> messageMap = new HashMap<>();
    public HashMap<String, String> folderMap = new HashMap<>();

    public void listMessages(String folderName){

        messageListView.getItems().clear();
        messageMap.clear();

        List<Message> messageList = Graph.getMailListFromFolder(folderName, 20);

        for (Message message: messageList) {

            Label sender = new Label(message.sender.emailAddress.name);
            Label subject = new Label(message.subject);
            Label bodyPreview = new Label(message.bodyPreview.split(System.lineSeparator())[0]);
            Label timestamp = new Label(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(message.receivedDateTime));
            //TODO add timestamp

            VBox vBox = new VBox(5, sender, subject, bodyPreview);
            vBox.setPadding(new Insets(10, 5, 10 , 5));
            vBox.setMaxWidth(300);
            vBox.setMinWidth(300);
            //TODO add listener to split pane?

            HBox hBox = new HBox(vBox, timestamp);

            messageMap.put(hBox, message);

            messageListView.getItems().add(hBox);
        }
    }

    private String recipientsString(List<Recipient> recipients){

        String recipientsString = "";

        for (Recipient recipient : recipients) {
            if (recipientsString.isEmpty()){
                recipientsString += recipient.emailAddress.name;
            }
            else{
                recipientsString += ", " + recipient.emailAddress.name;
            }
        }

        return recipientsString;
    }

    public void selectMessage(Message selectedMessage){
        senderText.setText("From: " + selectedMessage.sender.emailAddress.name);
        recipientText.setText("To: " + recipientsString(selectedMessage.toRecipients));
        subjectText.setText("Subject: " + selectedMessage.subject);

        webviewPane.getChildren().clear();

        OffsetDateTime receivedDateTime = selectedMessage.receivedDateTime;

        timeStampText.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(receivedDateTime));
        if(selectedMessage.body.contentType.name() == "TEXT"){
            bodyText.setText(selectedMessage.body.content);
        }
        else{
            WebView webview = new WebView();
            webview.getEngine().loadContent(selectedMessage.body.content);
            webviewPane.getChildren().add(webview);
        }
        //TODO figure out how to display this properly
    }

    @FXML
    public void handleListViewClick(MouseEvent event){

        selectMessage(messageMap.get(messageListView.getSelectionModel().getSelectedItem()));
    }

    @FXML
    public void handleListViewKeyPress(KeyEvent event){

        if(event.getCode().isArrowKey()){
            selectMessage(messageMap.get(messageListView.getSelectionModel().getSelectedItem()));
        }
    }

    @FXML
    public void handleFoldersListClick(){

        listMessages(folderMap.get(foldersList.getSelectionModel().getSelectedItem().getValue()));
    }

    public void loadFolders(){

        List<MailFolder> folders = Graph.getMailFolders();

        ArrayList<String> faveNames = new ArrayList<>(); //TODO this probably shouldn't hardcoded
        faveNames.add("Inbox");
        faveNames.add("Sent Items");
        faveNames.add("Drafts");
        faveNames.add("Outbox");
        faveNames.add("Deleted Items");
        faveNames.add("Junk Email");

        TreeItem<String> rootItem = new TreeItem<>();
        TreeItem<String> faveRootItem = new TreeItem<>("Favourites");
        TreeItem<String> otherRootItem = new TreeItem<>("Other Folders");

        for(String name : faveNames){
            faveRootItem.getChildren().add(new TreeItem<>(name));
        }

        for(MailFolder folder : folders){

            folderMap.put(folder.displayName, folder.id);

            if(!faveNames.contains(folder.displayName)){
                otherRootItem.getChildren().add(new TreeItem<>(folder.displayName));
            }
        }

        faveRootItem.setExpanded(true);
        otherRootItem.setExpanded(true);

        rootItem.getChildren().add(faveRootItem);
        rootItem.getChildren().add(otherRootItem);

        foldersList.setRoot(rootItem);
        foldersList.setShowRoot(false);
        foldersList.getSelectionModel().select(1);
    }

    public void logIn(){
        // Load OAuth settings
        final Properties oAuthProperties = new Properties();
        try {
            oAuthProperties.load(App.class.getResourceAsStream("oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
            return;
        }

        final String appId = oAuthProperties.getProperty("app.id");
        final List<String> appScopes = Arrays
                .asList(oAuthProperties.getProperty("app.scopes").split(","));

        // Initialize Graph with auth settings
        Graph.initializeGraphAuth(appId, appScopes);
        final String accessToken = Graph.getUserAccessToken();

        // Greet the user
        User user = Graph.getUser();
        System.out.println("Welcome " + user.displayName);
    }

    @FXML
    public void initialize(){
        logIn();
        loadFolders();
        listMessages(folderMap.get("Inbox"));

        //first message is selected on load - TODO should probably have its own method
        if(!messageListView.getItems().isEmpty()){

            messageListView.getSelectionModel().select(0);

            selectMessage(messageMap.get(messageListView.getSelectionModel().getSelectedItem()));
        }
    }
}
