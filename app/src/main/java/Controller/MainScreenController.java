package Controller;

import Model.Disturb;
import Model.Graph;
import Model.Holiday;
import Model.Mode;
import com.microsoft.graph.models.*;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.*;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.ParseException;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class MainScreenController {

    @FXML
    public ListView<VBox> messageListView;

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
    public WebView webView;

    @FXML
    public HBox attachmentsHbox;

    @FXML
    public Button newEmailButton;

    @FXML
    public Button deleteButton;

    @FXML
    public Button replyButton;

    @FXML
    public Button replyAllButton;

    @FXML
    public Button forwardButton;

    @FXML
    public Button changeModeButton;

    public HashMap<VBox, Message> messageMap = new HashMap<>(); //TODO all could be private?
    public HashMap<String, String> folderMap = new HashMap<>();
    public Timer timer = new Timer();

    private Mode mode = Mode.NORMAL; //TODO initialise through text file settings
    private Holiday holiday = Holiday.NONE;
    private Disturb disturb = Disturb.OFF;
    private Date disturbTime;
    private long syncFrequency = 60000;
    private int notificationCount = 0;
    private int notificationThreshold = 5;
    private final int notificationLength = 50;
    private ArrayList<String> senders = new ArrayList<>();
    private ArrayList<String> keywords = new ArrayList<>();

    /*
    * GETTERS AND SETTERS
    * */
    public int getNotificationThreshold() {
        return notificationThreshold;
    }

    public void setNotificationThreshold(int notificationThreshold) {
        this.notificationThreshold = notificationThreshold;
    }

    public Mode getMode() {
        return mode;
    }

    public void setMode(Mode mode) {
        this.mode = mode;
    }

    public Holiday getHoliday() {
        return holiday;
    }

    public void setHoliday(Holiday holiday) {
        this.holiday = holiday;
    }

    public Disturb getDisturb() {
        return disturb;
    }

    public void setDisturb(Disturb disturb) {
        this.disturb = disturb;
    }

    public Date getDisturbTime() {
        return disturbTime;
    }

    public void setDisturbTime(Date disturbTime) {
        this.disturbTime = disturbTime;
    }

    public long getSyncFrequency() {
        return syncFrequency;
    }

    public void setSyncFrequency(long syncFrequency) {
        this.syncFrequency = syncFrequency;
    }

    public void setSenders(ArrayList<String> senders) {
        this.senders = senders;
    }

    public ArrayList<String> getSenders() {
        return senders;
    }

    public void setKeywords(ArrayList<String> keywords) {
        this.keywords = keywords;
    }

    public ArrayList<String> getKeywords() {
        return keywords;
    }

    public void listMessages(String folderName){

        messageListView.getItems().clear();
        messageMap.clear();

        List<Message> messageList = Graph.getMailListFromFolder(folderName, 20);

        for (Message message: messageList) {

            Label sender = new Label();

            if(message.sender == null){
                sender.setText("");
            }
            else{
                sender = new Label(message.sender.emailAddress.name);
            }

            Label subject = new Label(message.subject);
            Label bodyPreview = new Label(message.bodyPreview.split(System.lineSeparator())[0]);
            Label timestamp = new Label(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(message.receivedDateTime));

            HBox hBox = new HBox(timestamp);
            hBox.setMaxWidth(300);
            hBox.setAlignment(Pos.TOP_RIGHT);

            VBox vBox = new VBox(5, hBox, sender, subject, bodyPreview);
            vBox.setPadding(new Insets(10, 5, 10 , 5));
            vBox.setMaxWidth(300);
            vBox.setMinWidth(300);

            if(Boolean.FALSE.equals(message.isRead)){

                for(Node child : vBox.getChildren()){
                    child.setStyle("-fx-font-weight: bold;");
                }
            }

            //TODO add listener to split pane?

            messageMap.put(vBox, message);

            messageListView.getItems().add(vBox);
        }
    }

    private String recipientsString(List<Recipient> recipients){ //TODO replace with StringJoiner

        String recipientsString = "";

        for (Recipient recipient : recipients) {
            if (recipientsString.isEmpty()){
                recipientsString += recipient.emailAddress.name;
            }
            else{
                recipientsString += "; " + recipient.emailAddress.name;
            }
        }

        return recipientsString;
    }

    public void selectMessage(Message selectedMessage) {

        if(selectedMessage.sender != null){
            senderText.setText("From: " + selectedMessage.sender.emailAddress.name);
        }
        else{
            senderText.setText("From: ");
        }

        if(selectedMessage.toRecipients != null){
            recipientText.setText("To: " + recipientsString(selectedMessage.toRecipients));
        }
        else {
            recipientText.setText("To: ");
        }

        if(selectedMessage.subject != null){
            subjectText.setText("Subject: " + selectedMessage.subject);
        }
        else{
            subjectText.setText("Subject: ");
        }

        webView.getEngine().loadContent("");
        attachmentsHbox.getChildren().clear();

        if(selectedMessage.hasAttachments){

            for(Attachment attachment : Graph.getMessageAttachmentList(selectedMessage.id)){

                Button attachmentButton = new Button(attachment.name);
                attachmentsHbox.getChildren().add(attachmentButton);

                if (attachment.oDataType.equals("#microsoft.graph.fileAttachment")){
                    EventHandler<ActionEvent> event = event1 -> {

                        FileAttachment attachment1 = Graph.getMessageFileAttachment(selectedMessage.id, attachment.id);

                        String home = System.getProperty("user.home");

                        System.out.println("path separator: " + File.separator);

                        File file = new File(home + File.separator + "Downloads" + File.separator + attachment1.name);

                        try(FileOutputStream outputStream = new FileOutputStream(file)){
                            file.createNewFile();
                            outputStream.write(attachment1.contentBytes);
                            if(Desktop.isDesktopSupported()){
                                Desktop desktop = Desktop.getDesktop();
                                if(file.exists()){
                                    desktop.open(file);
                                }
                            }

                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    };

                    attachmentButton.setOnAction(event);
                }
            }
        }

        OffsetDateTime receivedDateTime = selectedMessage.receivedDateTime;

        timeStampText.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(receivedDateTime));

        webView.getEngine().loadContent(selectedMessage.body.content);

        //TODO figure out how to display this properly
    }

    @FXML
    public void handleListViewClick(MouseEvent event){

        VBox vbox = messageListView.getSelectionModel().getSelectedItem();

        if(vbox != null){
            Message selectedMessage = messageMap.get(vbox);

            for(Node child : vbox.getChildren()){
                child.setStyle("-fx-font-weight: normal;");
            }

            Graph.readMessage(selectedMessage); //TODO might need a try catch - socket timeout exception
            selectMessage(selectedMessage);
        }
    }

    @FXML
    public void handleListViewKeyPress(KeyEvent event){

        if(event.getCode().isArrowKey()){
            selectMessage(messageMap.get(messageListView.getSelectionModel().getSelectedItem()));
        }
    }

    @FXML
    public void handleFoldersListClick() {

        //doesn't let you select parent nodes
        if(foldersList.getSelectionModel().getSelectedItem().isLeaf()){
            listMessages(folderMap.get(foldersList.getSelectionModel().getSelectedItem().getValue()));
            //TODO create local variable to keep track of current folder - lowercase and remove spaces
        }
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
            oAuthProperties.load(getClass().getResourceAsStream("/oAuth.properties"));
        } catch (IOException e) {
            System.out.println("Unable to read OAuth configuration. Make sure you have a properly formatted oAuth.properties file. See README for details.");
            return;
        }

        final String appId = oAuthProperties.getProperty("app.id");
        final List<String> appScopes = Arrays
                .asList(oAuthProperties.getProperty("app.scopes").split(","));

        // Initialize Graph with auth settings
        Graph.initializeGraphAuth(appId, appScopes);
        //final String accessToken = Graph.getUserAccessToken();

        // Greet the user
        User user = Graph.getUser();
        System.out.println("Welcome " + user.displayName);
    }

    public void sendNotification(String sender, String subject, String bodyPreview){

        String ellipsis = "...";

        if(sender.length() > notificationLength){
            sender = sender.substring(0, notificationLength) + ellipsis;
        }

        if(subject.length() > notificationLength){
            subject = subject.substring(0, notificationLength) + ellipsis;
        }

        if(bodyPreview.length() > notificationLength){
            bodyPreview = bodyPreview.replaceAll("(\r\n|\r|\n)", "").substring(0, notificationLength) + ellipsis;
        }

        Notifications.create() //TODO take to inbox when clicked on?
                .title(sender)
                .text(subject + System.lineSeparator() + bodyPreview)
                .hideAfter(new Duration(5000))
                .show();
    }

    public void sendModeNotification(String mode, String sender, String subject, String bodyPreview){

        String ellipsis = "...";

        if(mode.length() > notificationLength){
            mode = mode.substring(0, notificationLength) + ellipsis;
        }

        if(sender.length() > notificationLength){
            sender = sender.substring(0, notificationLength) + ellipsis;
        }

        if(subject.length() > notificationLength){
            subject = subject.substring(0, notificationLength) + ellipsis;
        }

        if(bodyPreview.length() > notificationLength){
            bodyPreview = bodyPreview.replaceAll("(\r\n|\r|\n)", "").substring(0, notificationLength) + ellipsis;
        }

        Notifications.create() //TODO take to inbox when clicked on?
                .title(mode + System.lineSeparator() + sender)
                .text(subject + System.lineSeparator() + bodyPreview)
                .hideAfter(new Duration(5000))
                .show();
    }

    @FXML
    public void initialize() throws ParseException {
        logIn();
        loadFolders();
        listMessages(folderMap.get("Inbox"));
        syncTimer();

        //first message is selected on load - TODO should probably have its own method
        if(!messageListView.getItems().isEmpty()){

            messageListView.getSelectionModel().select(0);

            selectMessage(messageMap.get(messageListView.getSelectionModel().getSelectedItem()));
        }
    }

    @FXML
    public void launchNewEmailWindow() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/NewEmailScreen.fxml"));
        Stage stage = new Stage();
        Scene scene = new Scene(fxmlLoader.load(), 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("New Email");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void deleteMessage(){

        VBox messageVbox = messageListView.getSelectionModel().getSelectedItem();
        Message selectedMessage = messageMap.get(messageVbox);
        String currentFolderName = foldersList.getSelectionModel().getSelectedItem().getValue();

        Graph.deleteMessage(selectedMessage.id, currentFolderName);

        messageListView.getItems().remove(messageVbox);
        messageMap.remove(messageVbox);
    }

    @FXML
    public void replyToMessage() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ReplyEmailScreen.fxml"));
        Parent root = fxmlLoader.load();

        ReplyEmailScreenController replyEmailScreenController = fxmlLoader.getController();
        Message selectedMessage = messageMap.get(messageListView.getSelectionModel().getSelectedItem());
        Recipient recipient = selectedMessage.sender;
        String subject = selectedMessage.subject;

        replyEmailScreenController.initialiseReply(recipient, subject, selectedMessage.id);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Reply to Email");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void replyAllToMessage() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ReplyEmailScreen.fxml"));
        Parent root = fxmlLoader.load();

        ReplyEmailScreenController replyEmailScreenController = fxmlLoader.getController();
        Message selectedMessage = messageMap.get(messageListView.getSelectionModel().getSelectedItem());
        ArrayList<Recipient> recipients = (ArrayList<Recipient>) selectedMessage.toRecipients;
        recipients.add(selectedMessage.sender);
        String subject = selectedMessage.subject;

        replyEmailScreenController.initialiseReply(recipients, subject, selectedMessage.id);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Reply All to Email");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void forwardMessage() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ForwardEmailScreen.fxml"));
        Parent root = fxmlLoader.load();

        ForwardEmailScreenController forwardEmailScreenController = fxmlLoader.getController();
        Message selectedMessage = messageMap.get(messageListView.getSelectionModel().getSelectedItem());

        forwardEmailScreenController.initialiseMessage(selectedMessage);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Forward Email");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void changeMode() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ChangeModeScreen.fxml"));
        Parent root = fxmlLoader.load();

        ChangeModeScreenController changeModeScreenController = fxmlLoader.getController();
        changeModeScreenController.setMainScreenController(this);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 420);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setResizable(false);
        stage.setTitle("Change Mode");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.show();
    }

    public void cancelTimer(){
        timer.cancel();
        timer = new Timer();
    }

    public void syncTimer(){ //TODO could be done in a different class/file?

        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                Platform.runLater(() -> {

                    try{ //TODO if current folder is inbox - maybe a difference map for inbox? Could just be list of IDs?

                        System.out.println("MODE: " + mode);

                        if(mode != Mode.DISTURB || (mode == Mode.DISTURB && disturb == Disturb.OFF)){
                            List<Message> messages = Graph.getMailListFromFolder("inbox", messageMap.size()); //from graph
                            List<Message> difference = new ArrayList<>(messageMap.values()); //from app - doesn't work if inbox isn't selected

                            Map<String, Message> messageIDMap = messages.stream().collect(Collectors.toMap(message -> message.id, message -> message));
                            Map<String, Message> differenceIDMap = difference.stream().collect(Collectors.toMap(message -> message.id, message -> message));

                            for(String id : differenceIDMap.keySet()){
                                messageIDMap.remove(id);
                            }

                            System.out.println("MESSAGE ID SIZE: " + messageIDMap.size());

                            if (messageIDMap.size() > 0){
                                if(mode == Mode.NORMAL){
                                    for(Message message : messageIDMap.values()){

                                        String sender = message.sender.emailAddress.name;
                                        String subject = message.subject;
                                        String bodyPreview = message.bodyPreview;
                                        sendNotification(sender, subject, bodyPreview);
                                    }
                                }
                                else if(mode == Mode.CONCENTRATED){

                                    notificationCount += messageIDMap.size();

                                    System.out.println("IN CONCENTRATED MODE, NOTIFICATION THRESHOLD: " + notificationThreshold);
                                    System.out.println("NOTIFICATION COUNT: " + notificationCount);


                                    if(notificationCount >= notificationThreshold){
                                        sendNotification("Concentrated Mode", "You have " + notificationCount + " new emails in your Inbox", "");
                                        notificationCount = 0;
                                    }
                                }
                                else if(mode == Mode.HOLIDAY){
                                    System.out.println("HOLIDAY MODE");
                                    System.out.println(holiday);

                                    for(Message message : messageIDMap.values()){

                                        String sender = message.sender.emailAddress.address;
                                        String subject = message.subject;
                                        String bodyPreview = message.bodyPreview;

                                        if(holiday.equals(Holiday.SENDERS)){ //TODO could do with cleaning up - maybe reorder?
                                            if(fromSpecifiedSender(message)){
                                                senderHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                        else if(holiday.equals(Holiday.KEYWORDS)){
                                            if(containsKeywords(message)){
                                                keywordHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                        else if(holiday.equals(Holiday.IMPORTANT)){
                                            if(isImportant(message)){
                                                importantHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                        else if(holiday.equals(Holiday.SENDERS_AND_KEYWORDS)){
                                            if(fromSpecifiedSender(message)){
                                                senderHolidayNotification(sender, subject, bodyPreview);
                                            }
                                            else if(containsKeywords(message)){
                                                keywordHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                        else if(holiday.equals(Holiday.SENDERS_AND_IMPORTANT)){
                                            if(fromSpecifiedSender(message)){
                                                senderHolidayNotification(sender, subject, bodyPreview);
                                            }
                                            else if(isImportant(message)){
                                                importantHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                        else if(holiday.equals(Holiday.KEYWORDS_AND_IMPORTANT)){
                                            if(containsKeywords(message)){
                                                keywordHolidayNotification(sender, subject, bodyPreview);
                                            }
                                            else if(isImportant(message)){
                                                importantHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                        else if(holiday.equals(Holiday.SENDERS_AND_KEYWORDS_AND_IMPORTANT)){
                                            if(fromSpecifiedSender(message)){
                                                senderHolidayNotification(sender, subject, bodyPreview);
                                            }
                                            else if(containsKeywords(message)){

                                            }
                                            else if(isImportant(message)){

                                            }
                                        }
                                    }
                                }
                            }
                        }
                        else{

                            Date now = new Date();

                            if(disturb == Disturb.TIMED_1_HOUR || disturb == Disturb.TIMED_8_HOURS || disturb == Disturb.TIMED_24_HOURS || disturb == Disturb.TIMED_UNTIL){
                                if(now.compareTo(disturbTime) >= 0){
                                    mode = Mode.NORMAL;
                                    disturb = Disturb.OFF;
                                }
                            }
                        }

                        listMessages("inbox");

                    } catch (Exception e){
                        System.out.println(e);
                    }
                });
            }
        }, 0, syncFrequency);
    }

    public boolean fromSpecifiedSender(Message message){

        return senders.contains(message.sender.emailAddress.address);
    }

    public boolean containsKeywords(Message message){

        String lowercaseSubject = message.subject.toLowerCase();
        String lowercaseBody = message.body.content.toLowerCase(Locale.ROOT);

        boolean contains = false;

        for(String keyword : keywords){
            if(lowercaseBody.contains(keyword) || lowercaseSubject.contains(keyword)){
                contains = true;
                break;
            }
        }

        return contains;
    }

    public boolean isImportant(Message message){

        Importance importance = message.importance;

        return importance.equals(Importance.HIGH);
    }

    public void senderHolidayNotification(String sender, String subject, String bodyPreview){
        sendModeNotification("Holiday Mode - from a specified sender", sender, subject, bodyPreview);
    }

    public void keywordHolidayNotification(String sender, String subject, String bodyPreview){
        sendModeNotification("Holiday Mode - contains keywords", sender, subject, bodyPreview);
    }

    public void importantHolidayNotification(String sender, String subject, String bodyPreview){
        sendModeNotification("Holiday Mode - high importance", sender, subject, bodyPreview);
    }
}
