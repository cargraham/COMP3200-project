package Controller;

import Model.Graph;
import com.microsoft.graph.models.Attachment;
import com.microsoft.graph.models.FileAttachment;
import com.microsoft.graph.models.Importance;
import com.microsoft.graph.models.Message;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class NewEmailScreenController {

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
    public ToggleButton importantToggle;

    @FXML
    public Button attachFileButton;

    @FXML
    public Button draftButton;

    @FXML
    public Button sendButton;

    private final String SEMI_COLON = "; ";
    private Stage thisStage;
    private final LinkedList<Attachment> attachments = new LinkedList<>();

    //sets the 'From' field to the user's name after FXML values have been injected
    @FXML
    public void initialize(){
        fromTextField.setText(Graph.getUser().userPrincipalName);
    }

    //saves a draft message and closes window on button click
    @FXML
    public void saveDraftMessage(Event event){
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

        Message message = Graph.createMessage(subject, body, toRecipients, ccRecipients);

        if(importantToggle.isSelected()){
            message.importance = Importance.HIGH;
        }

        if(attachments.isEmpty()){
            Graph.saveDraft(message);
        }
        else{
            Graph.saveDraftWithAttachment(message, attachments);
        }

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    //launches file chooser to allow user to choose attachment
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

    //sends message on button click
    @FXML
    public void sendMessage(Event event) throws IOException {
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

        Message message = Graph.createMessage(subject, body, toRecipients, ccRecipients);

        if(importantToggle.isSelected()){
            message.importance = Importance.HIGH;
        }

        if(attachments.isEmpty()){
            Graph.sendMessage(message);
        }
        else{
            Graph.sendMessageWithAttachment(message, attachments);
        }

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    //sets the stage for use with file chooser
    public void setStage(Stage thisStage){
        this.thisStage = thisStage;
    }
}
