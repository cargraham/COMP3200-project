package Controller;

import Model.Graph;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;


public class NewFolderScreenController {

    @FXML
    public TextField folderNameTextField;

    @FXML
    public Button confirmButton;

    @FXML
    public Button cancelButton;

    private MainScreenController mainScreenController;

    public void setMainScreenController(MainScreenController mainScreenController){
        this.mainScreenController = mainScreenController;
    }

    @FXML
    public void confirmChoice(Event event){

        String folderName = folderNameTextField.getText();
        Graph.newFolder(folderName);

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
