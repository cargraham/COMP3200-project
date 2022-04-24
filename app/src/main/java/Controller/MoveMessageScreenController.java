package Controller;

import Model.Graph;
import com.microsoft.graph.models.Message;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.stage.Stage;

import java.util.HashMap;

public class MoveMessageScreenController {

    @FXML
    public ComboBox<String> folderComboBox;

    @FXML
    public Button cancelButton;

    @FXML
    public Button confirmButton;

    private MainScreenController mainScreenController;
    private HashMap<String, String> folderMap = new HashMap<>();
    private String messageID;

    //sets main controller and ID of message being moved
    public void initialiseMoveMessage(MainScreenController mainScreenController, String messageID){
        this.mainScreenController = mainScreenController;
        this.messageID = messageID;

        folderMap = mainScreenController.getFolderMap();

        for(String folderName : folderMap.keySet()){
            folderComboBox.getItems().add(folderName);
        }

        folderComboBox.getSelectionModel().selectFirst();
    }

    //moves selected message to selected folder on button click
    @FXML
    public void confirmChoice(Event event){
        String selectedFolder = folderComboBox.getSelectionModel().getSelectedItem();
        String folderID = folderMap.get(selectedFolder);
        String currentFolder = mainScreenController.getCurrentFolder();
        Message selectedMessage = Graph.getMessage(messageID);

        Graph.moveMessage(messageID, folderID);

        mainScreenController.listMessages(currentFolder);

        if(currentFolder.equalsIgnoreCase("inbox")){
            mainScreenController.removeFromInboxList(selectedMessage);
        }

        if(selectedFolder.equalsIgnoreCase("inbox")){
            mainScreenController.addToInboxList(selectedMessage);
        }

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    //closes the stage and doesn't save choices on cancel button click
    @FXML
    public void cancel(Event event){

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
