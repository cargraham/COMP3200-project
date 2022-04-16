package Controller;

import Model.Graph;
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

    public void initialiseMoveMessage(MainScreenController mainScreenController, String messageID){

        this.mainScreenController = mainScreenController;
        this.messageID = messageID;

        folderMap = mainScreenController.getFolderMap();

        for(String folderName : folderMap.keySet()){
            folderComboBox.getItems().add(folderName);
        }

        folderComboBox.getSelectionModel().selectFirst();
    }

    @FXML
    public void confirmChoice(Event event){

        String selectedFolder = folderComboBox.getSelectionModel().getSelectedItem();
        String folderID = folderMap.get(selectedFolder);

        Graph.moveMessage(messageID, folderID);

        if(mainScreenController.getCurrentFolder() == selectedFolder){
            mainScreenController.listMessages(selectedFolder);
        }

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }

    @FXML
    public void cancel(Event event){

        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
