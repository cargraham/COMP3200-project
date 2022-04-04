package Controller;

import Model.Graph;
import com.microsoft.graph.models.Recipient;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

public class ReplyEmailScreenController {

    @FXML
    public TextField fromTextField;

    @FXML
    public TextField toTextField;

    @FXML
    public TextField ccTextField;

    @FXML
    public TextField subjectTextField;

    @FXML
    public TextArea bodyTextArea;

    @FXML
    public Button draftButton;

    @FXML
    public Button sendButton;

    final public String SEMI_COLON = "; ";
    public ArrayList<Recipient> recipient; //could be private?
    public String subject;
    public String messageID;

    @FXML
    public void initialize(){
        fromTextField.setText(Graph.getUser().userPrincipalName);
    }

    public void initialiseReply(Recipient recipient, String subject, String messageID){
        this.recipient = new ArrayList<>();
        this.recipient.add(recipient);
        this.subject = subject;
        this.messageID = messageID;
        toTextField.setText(recipient.emailAddress.address);
        subjectTextField.setText("RE: "  + subject);
    }

    public void initialiseReply(ArrayList<Recipient> recipients, String subject, String messageID){
        this.subject = subject;
        this.messageID = messageID;
        this.recipient = recipients;
        toTextField.setText(buildRecipientsString(recipients));
        subjectTextField.setText("RE: "  + subject);
    }

    public String buildRecipientsString(ArrayList<Recipient> recipients){

        StringJoiner recipientJoiner = new StringJoiner(SEMI_COLON);

        for(Recipient recipient : recipients){

            String recipientAddress = recipient.emailAddress.address;

            if(!recipientAddress.equals(Graph.getUser().userPrincipalName)){
                recipientJoiner.add(recipientAddress);
            }
        }

        return recipientJoiner.toString();
    }

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

        Graph.saveDraft(Graph.createMessage(subject, body, toRecipients, ccRecipients));

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
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

        Graph.replyToMessage(messageID, Graph.createMessage(subject, body, toRecipients, ccRecipients));

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
