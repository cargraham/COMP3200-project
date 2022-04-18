package Controller;

import Model.Graph;
import com.microsoft.graph.models.*;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.scene.web.WebView;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class ForwardEmailScreenController {

    @FXML
    public TextField fromTextField;

    @FXML
    public TextField toTextField;

    @FXML
    public TextField ccTextField;

    @FXML
    public TextField subjectTextField;

    @FXML
    public HBox attachmentsHBox;

    @FXML
    public TextArea bodyTextArea;

    @FXML
    public WebView forwardWebView;

    @FXML
    public Button draftButton;

    @FXML
    public ToggleButton importantToggle;

    @FXML
    public Button sendButton;

    private final String SEMI_COLON = "; ";
    private Message message;
    private String messageID;
    private String subject;
    private Stage thisStage;
    private final LinkedList<Attachment> attachments = new LinkedList<>();

    @FXML
    public void initialize(){
        fromTextField.setText(Graph.getUser().userPrincipalName);
    }

    public void initialiseMessage(Message message){

        this.message = message;
        this.messageID = message.id;
        this.subject = "FW: " + message.subject;
        subjectTextField.setText(subject);
        forwardWebView.getEngine().loadContent(message.body.content);
    }

    @FXML
    public void saveDraftMessage(Event event){

        String body = bodyTextArea.getText();
        ArrayList<String> toRecipients = new ArrayList<>();
        ArrayList<String> ccRecipients = new ArrayList<>();

        if(!toTextField.getText().isEmpty()){
            toRecipients = new ArrayList<>(List.of(toTextField.getText().split(SEMI_COLON)));
        }
        if(!ccTextField.getText().isEmpty()){
            ccRecipients = new ArrayList<>(List.of(ccTextField.getText().split(SEMI_COLON)));
        }

        Message newMessage = Graph.createForwardMessage(subject, body, toRecipients, ccRecipients, message);

        if(importantToggle.isSelected()){
            newMessage.importance = Importance.HIGH;
        }

        if(attachments.isEmpty()){
            Graph.saveDraft(newMessage);
        }
        else{
            Graph.saveDraftWithAttachment(newMessage, attachments);
        }

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void attachFile() throws IOException {

        FileChooser fileChooser = new FileChooser();
        File attachment = fileChooser.showOpenDialog(thisStage);

        FileAttachment fileAttachment = new FileAttachment();
        fileAttachment.name = attachment.getName();
        fileAttachment.contentBytes = Files.readAllBytes(attachment.toPath());
        fileAttachment.oDataType = "#microsoft.graph.fileAttachment";

        attachments.add(fileAttachment);

        Button attachmentButton = new Button(attachment.getName());
        attachmentsHBox.setPadding(new Insets(5));
        attachmentsHBox.setSpacing(5);
        attachmentsHBox.getChildren().add(attachmentButton);

        attachmentButton.setOnAction(event1 -> {

            Desktop desktop = Desktop.getDesktop();

            if(attachment.exists()){
                try {
                    desktop.open(attachment);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    @FXML
    public void sendMessage(Event event){

        String subject = subjectTextField.getText();
        String body = bodyTextArea.getText();
        ArrayList<String> toRecipients = new ArrayList<>();
        ArrayList<String> ccRecipients = new ArrayList<>();

        if(!toTextField.getText().isEmpty()){
            toRecipients = new ArrayList<>(List.of(toTextField.getText().split(SEMI_COLON)));
        }
        if(!ccTextField.getText().isEmpty()){
            ccRecipients = new ArrayList<>(List.of(ccTextField.getText().split(SEMI_COLON)));
        }

        Message newMessage = Graph.createForwardMessage(subject, body, toRecipients, ccRecipients, message);

        if(importantToggle.isSelected()){
            newMessage.importance = Importance.HIGH;
        }

        if(attachments.isEmpty()){
            Graph.forwardMessage(messageID, newMessage);
        }
        else{
            Graph.forwardMessageWithAttachment(messageID, newMessage, attachments);
        }


        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    public void setStage(Stage thisStage){
        this.thisStage = thisStage;
    }
}
