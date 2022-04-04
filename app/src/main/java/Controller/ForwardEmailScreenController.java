package Controller;

import Model.Graph;
import com.microsoft.graph.models.Message;
import com.microsoft.graph.models.Recipient;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

import java.util.ArrayList;
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
    public TextArea bodyTextArea;

    @FXML
    public WebView forwardWebView;

    @FXML
    public Button draftButton;

    @FXML
    public Button sendButton;

    final public String SEMI_COLON = "; ";
    final public String messageSeparator = "<html><br><hr><br></html>";
    public ArrayList<Recipient> recipient; //could be private?
    public Message message;
    public String messageID;
    public String subject;

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
    public void saveDraftMessage(Event event){ //edit this

        String subject = subjectTextField.getText();
        String body = bodyTextArea.getText() + messageSeparator + message.body.content;
        ArrayList<String> toRecipients = new ArrayList<>();
        ArrayList<String> ccRecipients = new ArrayList<>();

        if(!toTextField.getText().isEmpty()){
            toRecipients = new ArrayList<>(List.of(toTextField.getText().split(SEMI_COLON)));
        }
        if(!ccTextField.getText().isEmpty()){
            ccRecipients = new ArrayList<>(List.of(ccTextField.getText().split(SEMI_COLON)));
        }

        Graph.saveDraft(Graph.createForwardMessage(subject, body, toRecipients, ccRecipients, message));

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void sendMessage(Event event){ //edit this

        String subject = subjectTextField.getText();
        String body = bodyTextArea.getText() /*+ message.body.content*/;
        ArrayList<String> toRecipients = new ArrayList<>();
        ArrayList<String> ccRecipients = new ArrayList<>();

        if(!toTextField.getText().isEmpty()){
            toRecipients = new ArrayList<>(List.of(toTextField.getText().split(SEMI_COLON)));
        }
        if(!ccTextField.getText().isEmpty()){
            ccRecipients = new ArrayList<>(List.of(ccTextField.getText().split(SEMI_COLON)));
        }

        Graph.forwardMessage(messageID, toRecipients, Graph.createForwardMessage(subject, body, toRecipients, ccRecipients, message));

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
