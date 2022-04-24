package Controller;

import Model.Disturb;
import Model.Graph;
import Model.Holiday;
import Model.Mode;
import com.microsoft.graph.models.*;
import javafx.application.Platform;
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
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.stage.Modality;
import javafx.util.Duration;
import org.controlsfx.control.Notifications;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
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
    public Label importantText;

    @FXML
    public TreeView<String> foldersList;

    @FXML
    public WebView webView;

    @FXML
    public HBox attachmentsHBox;

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
    public Button editDraftButton;

    @FXML
    public Button newFolderButton;

    @FXML
    public Button deleteFolderButton;

    @FXML
    public Button moveMessageButton;

    @FXML
    public Button changeModeButton;

    private final HashMap<VBox, Message> messageMap = new HashMap<>();
    private final ArrayList<Message> inboxMessageList = new ArrayList<>();
    private final HashMap<String, String> folderMap = new HashMap<>();
    private Timer timer = new Timer();
    private Mode mode = Mode.NORMAL;
    private Holiday holiday = Holiday.NONE;
    private Disturb disturb = Disturb.OFF;
    private Date disturbTime;
    private long syncFrequency = 60000;
    private int notificationCount = 0;
    private int notificationThreshold = 5;
    private final int notificationLength = 50;
    private ArrayList<String> senders = new ArrayList<>();
    private ArrayList<String> keywords = new ArrayList<>();
    private final int noOfMessage = 30;
    private final String inboxString = "inbox";
    private String currentFolder = inboxString;
    private final ArrayList<String> faveNames = new ArrayList<>();

    /*
    * GETTERS AND SETTERS
    * */
    public HashMap<String, String> getFolderMap() {
        return folderMap;
    }

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

    public ArrayList<String> getSenders() {
        return senders;
    }

    public void setSenders(ArrayList<String> senders) {
        this.senders = senders;
    }

    public ArrayList<String> getKeywords() {
        return keywords;
    }

    public void setKeywords(ArrayList<String> keywords) {
        this.keywords = keywords;
    }

    public String getCurrentFolder() {
        return currentFolder;
    }

    //populates the listview and hashmap with message from the current folder
    public void listMessages(String folderName){
        messageListView.getItems().clear();
        messageMap.clear();

        List<Message> messageList = Graph.getMailListFromFolder(folderName, noOfMessage);

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

            Label importantLabel = new Label();
            importantLabel.setStyle("-fx-text-fill: #ff3639; -fx-font-size: 18px;");
            importantLabel.setPadding(new Insets(0, 5, 0, 0));

            if(message.importance == Importance.HIGH){
                importantLabel.setText("!");
            }

            bodyPreview.setMaxWidth(280);

            BorderPane borderPane = new BorderPane();
            borderPane.setLeft(bodyPreview);
            borderPane.setRight(importantLabel);

            HBox timestampHBox = new HBox(timestamp);
            timestampHBox.setMaxWidth(300);
            timestampHBox.setAlignment(Pos.TOP_RIGHT);

            VBox vBox = new VBox(5, timestampHBox, sender, subject, borderPane);
            vBox.setPadding(new Insets(10, 5, 10 , 5));
            vBox.setMaxWidth(300);
            vBox.setMinWidth(300);

            if(Boolean.FALSE.equals(message.isRead)){

                for(Node child : vBox.getChildren()){
                    child.setStyle("-fx-font-weight: bold;");
                }
            }

            messageMap.put(vBox, message);
            messageListView.getItems().add(vBox);
        }
    }

    //builds a string of recipients joined by a semicolon
    private String recipientsString(List<Recipient> recipients){
        StringJoiner recipientJoiner = new StringJoiner(";");

        for(Recipient recipient : recipients){
            recipientJoiner.add(recipient.emailAddress.name);
        }

        return recipientJoiner.toString();
    }

    //shows a message on the right pane
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

        if(selectedMessage.importance == Importance.HIGH){
            importantText.setText("!");
        }
        else{
            importantText.setText("");
        }

        webView.getEngine().loadContent("");
        attachmentsHBox.getChildren().clear();

        if(selectedMessage.hasAttachments){

            for(Attachment attachment : Graph.getMessageAttachmentList(selectedMessage.id)){

                Button attachmentButton = new Button(attachment.name);
                attachmentsHBox.getChildren().add(attachmentButton);

                if (attachment.oDataType.equals("#microsoft.graph.fileAttachment")){

                    attachmentButton.setOnAction(event1 -> {

                        FileAttachment attachment1 = Graph.getMessageFileAttachment(selectedMessage.id, attachment.id);
                        String home = System.getProperty("user.home");
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
                    });
                }
            }
        }

        OffsetDateTime receivedDateTime = selectedMessage.receivedDateTime;
        timeStampText.setText(DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT).format(receivedDateTime));
        webView.getEngine().loadContent(selectedMessage.body.content); //TODO figure out how to display this properly

        try{
            Graph.readMessage(selectedMessage);
        }catch(Exception ignored){}
    }

    //changes folder selection on tree view click
    @FXML
    public void handleFoldersListClick() {
        //doesn't let you select parent nodes
        if(foldersList.getSelectionModel().getSelectedItem().isLeaf()){
            String selectedFolder = foldersList.getSelectionModel().getSelectedItem().getValue();
            listMessages(folderMap.get(selectedFolder));
            currentFolder = selectedFolder.toLowerCase(Locale.ROOT).replaceAll("\\s+", "");
            selectFirstMessage();
        }
    }

    //populates arraylist with non-deletable folders
    public void populateFaveFolders(){
        faveNames.add("Inbox");
        faveNames.add("Sent Items");
        faveNames.add("Drafts");
        faveNames.add("Outbox");
        faveNames.add("Deleted Items");
        faveNames.add("Junk Email");
        faveNames.add("Conversation History");
        faveNames.add("Archive");
    }

    //load all the folders into the tree view
    public void loadFolders(){
        folderMap.clear();

        List<MailFolder> folders = Graph.getMailFolders();
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

    //logs the user in
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
        Graph.initializeGraphAuth(appId, appScopes, this);
        /*//final String accessToken = Graph.getUserAccessToken();

        // Greet the user
        User user = Graph.getUser();*/
    }

    //sends a javafx notification for an email
    public void sendEmailNotification(String sender, String subject, String bodyPreview){
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
                .darkStyle()
                .show();
    }

    //sends a javafx notification when a mode is chosen
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
                .darkStyle()
                .show();
    }

    //FXML initialize method to initialize screen after FXML values are injected
    @FXML
    public void initialize() {
        logIn();
        populateFaveFolders();
        loadFolders();
        listMessages(folderMap.get("Inbox"));
        syncTimer();
        selectFirstMessage();
        initialiseInboxMap();
        addListViewListener();
    }

    //add listener to list view for selection changes
    public void addListViewListener(){
        messageListView.getSelectionModel().selectedItemProperty()
                .addListener((observable, oldValue, newValue) -> {

                    try{

                        Message selectedMessage = messageMap.get(newValue);

                        if(selectedMessage != null && !selectedMessage.isDraft){
                            moveMessageButton.setDisable(false);
                        }
                        else{
                            moveMessageButton.setDisable(true);
                        }

                        if(selectedMessage.isDraft){
                            editDraftButton.setDisable(false);
                        }
                        else{
                            editDraftButton.setDisable(true);
                        }

                        if(!selectedMessage.isRead){
                            for(Node child : newValue.getChildren()){
                                child.setStyle("-fx-font-weight: normal;");
                            }
                        }

                        selectMessage(selectedMessage);

                    }catch(NullPointerException ignored){}
                });
    }

    //initialises inbox message map
    public void initialiseInboxMap(){
        inboxMessageList.addAll(Graph.getMailListFromFolder(inboxString, noOfMessage));
    }

    //first message is selected
    public void selectFirstMessage(){
        if(!messageListView.getItems().isEmpty()){

            messageListView.getSelectionModel().select(0);
            selectMessage(messageMap.get(messageListView.getSelectionModel().getSelectedItem()));
        }
    }

    //launches new email screen when 'New Email' button clicked
    @FXML
    public void newEmail() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/NewEmailScreen.fxml"));
        Parent root = fxmlLoader.load();
        Stage stage = new Stage();

        NewEmailScreenController newEmailScreenController = fxmlLoader.getController();
        newEmailScreenController.setStage(stage);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("New Email");
        stage.setScene(scene);
        stage.show();
    }

    //deletes selected message when 'Delete' button is clicked
    @FXML
    public void deleteMessage(){
        VBox messageVbox = messageListView.getSelectionModel().getSelectedItem();
        Message selectedMessage = messageMap.get(messageVbox);
        String currentFolderName = foldersList.getSelectionModel().getSelectedItem().getValue();

        Graph.deleteMessage(selectedMessage.id, currentFolderName);

        messageListView.getItems().remove(messageVbox);
        messageMap.remove(messageVbox);
        inboxMessageList.remove(selectedMessage);
    }

    //launches the reply screen when 'Reply' button is clicked
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
        replyEmailScreenController.setStage(stage);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Reply to Email");
        stage.setScene(scene);
        stage.show();
    }

    //launches reply all screen when 'Reply All' button is clicked
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
        replyEmailScreenController.setStage(stage);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Reply All to Email");
        stage.setScene(scene);
        stage.show();
    }

    //launches forward screen when 'Forward' button is clicked
    @FXML
    public void forwardMessage() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ForwardEmailScreen.fxml"));
        Parent root = fxmlLoader.load();

        ForwardEmailScreenController forwardEmailScreenController = fxmlLoader.getController();
        Message selectedMessage = messageMap.get(messageListView.getSelectionModel().getSelectedItem());
        forwardEmailScreenController.initialiseMessage(selectedMessage);

        Stage stage = new Stage();
        forwardEmailScreenController.setStage(stage);

        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Forward Email");
        stage.setScene(scene);
        stage.show();
    }

    //launches edit draft screen when 'Edit Draft' button is clicked
    @FXML
    public void editDraft() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/EditDraftScreen.fxml"));
        Parent root = fxmlLoader.load();

        EditDraftScreenController editDraftScreenController = fxmlLoader.getController();
        Message selectedMessage = messageMap.get(messageListView.getSelectionModel().getSelectedItem());
        editDraftScreenController.initialiseDraft(selectedMessage);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Edit Draft");
        stage.setScene(scene);
        stage.show();
    }

    //launches new folder screen when 'Add New Folder' button is clicked
    @FXML
    public void newFolder() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/NewFolderScreen.fxml"));
        Parent root = fxmlLoader.load();

        NewFolderScreenController newFolderScreenController = fxmlLoader.getController();
        newFolderScreenController.setMainScreenController(this);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 300, 200);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("New Folder");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    //launches delete folder screen when 'Delete Folder' button is clicked
    @FXML
    public void deleteFolder() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/DeleteFolderScreen.fxml"));
        Parent root = fxmlLoader.load();

        DeleteFolderScreenController deleteFolderScreenController = fxmlLoader.getController();
        String currentFolder = foldersList.getSelectionModel().getSelectedItem().getValue();
        String currentFolderID = folderMap.get(currentFolder);

        boolean deletable = !faveNames.contains(currentFolder);

        deleteFolderScreenController.initialiseDelete(this, currentFolder, currentFolderID, deletable);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 300, 200);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Delete Folder");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    //launches move message screen when 'Move Message' button is clicked
    @FXML
    public void moveMessage() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/MoveMessageScreen.fxml"));
        Parent root = fxmlLoader.load();

        MoveMessageScreenController moveMessageScreenController = fxmlLoader.getController();
        Message selectedMessage = messageMap.get(messageListView.getSelectionModel().getSelectedItem());
        moveMessageScreenController.initialiseMoveMessage(this, selectedMessage.id);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 300, 200);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setTitle("Move Message");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setResizable(false);
        stage.setScene(scene);
        stage.show();
    }

    //launches change mode screen when 'Change Mode' button is clicked
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

    //cancels timer so it can be reset with different syncFrequency
    public void cancelTimer(){
        timer.cancel();
        timer = new Timer();
    }

    //syncs the timer with the given syncFrequency
    public void syncTimer(){ //TODO could be done in a different class/file?
        timer.schedule(new TimerTask() {
            @Override
            public void run() {

                Platform.runLater(() -> {

                    try{

                        System.out.println("MODE: " + mode);

                        //if(mode != Mode.DISTURB || (mode == Mode.DISTURB && disturb == Disturb.OFF)){
                            List<Message> messages = Graph.getMailListFromFolder(inboxString, messageMap.size()); //from graph
                            //List<Message> difference = new ArrayList<>(messageMap.values()); //from app - doesn't work if inbox isn't selected

                            Map<String, Message> messageIDMap = messages.stream().collect(Collectors.toMap(message -> message.id, message -> message));
                            Map<String, Message> differenceIDMap = inboxMessageList.stream().collect(Collectors.toMap(message -> message.id, message -> message));

                            for(String id : differenceIDMap.keySet()){
                                messageIDMap.remove(id);
                            }

                            System.out.println("MESSAGE ID SIZE: " + messageIDMap.size());

                            if (messageIDMap.size() > 0){

                                inboxMessageList.addAll(messageIDMap.values());

                                if(mode == Mode.NORMAL){
                                    for(Message message : messageIDMap.values()){

                                        String sender = message.sender.emailAddress.name;
                                        String subject = message.subject;
                                        String bodyPreview = message.bodyPreview;
                                        sendEmailNotification(sender, subject, bodyPreview);
                                    }
                                }
                                else if(mode == Mode.CONCENTRATED){

                                    notificationCount += messageIDMap.size();

                                    System.out.println("IN CONCENTRATED MODE, NOTIFICATION THRESHOLD: " + notificationThreshold);
                                    System.out.println("NOTIFICATION COUNT: " + notificationCount);


                                    if(notificationCount >= notificationThreshold){
                                        sendEmailNotification("Concentrated Mode", "You have " + notificationCount + " new emails in your Inbox", "");
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
                                                keywordHolidayNotification(sender, subject, bodyPreview);
                                            }
                                            else if(isImportant(message)){
                                                importantHolidayNotification(sender, subject, bodyPreview);
                                            }
                                        }
                                    }
                                }
                            }
                        //}
                        //else{

                            Date now = new Date();

                            if(disturb == Disturb.TIMED_1_HOUR || disturb == Disturb.TIMED_8_HOURS || disturb == Disturb.TIMED_24_HOURS || disturb == Disturb.TIMED_UNTIL){
                                if(now.compareTo(disturbTime) >= 0){
                                    mode = Mode.NORMAL;
                                    disturb = Disturb.OFF;
                                }
                            }
                        //}

                        if(currentFolder.equals(inboxString)){
                            listMessages(inboxString);
                            selectFirstMessage();
                        }

                    } catch (Exception e){
                        e.printStackTrace();
                    }
                });
            }
        }, 0, syncFrequency);
    }

    //returns true if sender of message is a specified sender
    private boolean fromSpecifiedSender(Message message){
        return senders.contains(message.sender.emailAddress.address);
    }

    //returns true if message contains specified keywords
    private boolean containsKeywords(Message message){
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

    //returns true is message is labelled 'Important'
    private boolean isImportant(Message message){
        Importance importance = message.importance;
        return importance.equals(Importance.HIGH);
    }

    //sends a holiday notification from a specified sender
    private void senderHolidayNotification(String sender, String subject, String bodyPreview){
        sendModeNotification("Holiday Mode - from a specified sender", sender, subject, bodyPreview);
    }

    //sends a holiday notification that contains keywords
    private void keywordHolidayNotification(String sender, String subject, String bodyPreview){
        sendModeNotification("Holiday Mode - contains keywords", sender, subject, bodyPreview);
    }

    //sends a holiday notification for an important email
    private void importantHolidayNotification(String sender, String subject, String bodyPreview){
        sendModeNotification("Holiday Mode - high importance", sender, subject, bodyPreview);
    }

    //adds a given message to the list of messages in inbox
    public void addToInboxList(Message message){
        inboxMessageList.add(message);
    }

    //removes a given message to the list of message in inbox
    public void removeFromInboxList(Message message){
        inboxMessageList.remove(message);
    }
}