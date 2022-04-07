package Controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.input.MouseEvent;
import javafx.stage.Stage;

public class ChangeSyncFrequencyScreenController {

    @FXML
    public ComboBox<String> comboBox;

    @FXML
    public Button confirmButton;

    @FXML
    public Button cancelButton;

    public MainScreenController mainScreenController;
    public long syncFrequency = 0;

    @FXML
    public void initialize(){
        comboBox.getItems().add("1 Minute");
        comboBox.getItems().add("5 Minutes");
        comboBox.getItems().add("10 Minutes");
        comboBox.getItems().add("30 Minutes");
        comboBox.getItems().add("60 Minutes");
    }

    public void setMainScreenController(MainScreenController mainScreenController){
        this.mainScreenController = mainScreenController;
        syncFrequency = mainScreenController.getSyncFrequency();
    }

    @FXML
    public void confirmFrequencyChoice(Event event){

        String minutes = comboBox.getValue();

        switch (minutes) {
            case "1 Minute" -> syncFrequency = 60000;
            case "5 Minutes" -> syncFrequency = 300000;
            case "10 Minutes" -> syncFrequency = 600000;
            case "30 Minutes" -> syncFrequency = 1800000;
            case "60 Minutes" -> syncFrequency = 3600000;
        }

        mainScreenController.setSyncFrequency(syncFrequency);
        mainScreenController.cancelTimer();
        mainScreenController.syncTimer();

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
