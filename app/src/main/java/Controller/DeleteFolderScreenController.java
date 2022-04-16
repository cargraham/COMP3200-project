package Controller;

import Model.Graph;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DeleteFolderScreenController {

    @FXML
    public Label warningLabel;

    @FXML
    public Button cancelButton;

    @FXML
    public Button confirmButton;

    private MainScreenController mainScreenController;
    private String folderID;

    public void initialiseDelete(MainScreenController mainScreenController, String folderName, String folderID, boolean deletable){

        this.mainScreenController = mainScreenController;
        this.folderID = folderID;

        if(!deletable){
            confirmButton.setDisable(true);
            warningLabel.setText("You are not allowed to delete " + folderName);
        }
        else{
            warningLabel.setText("Are you sure you want to delete " + folderName + "?");
        }
    }

    @FXML
    public void confirmChoice(Event event){

        Graph.deleteFolder(folderID);
        mainScreenController.loadFolders();

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
