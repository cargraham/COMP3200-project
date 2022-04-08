package Controller;

import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.stage.Stage;

public class ConcentratedSettingsScreenController {

    @FXML
    public Spinner<Integer> spinner;

    @FXML
    public Button cancelButton;

    @FXML
    public Button confirmButton;

    public ChangeModeScreenController changeModeScreenController;
    public MainScreenController mainScreenController;
    public int notificationThreshold;

    @FXML
    public void confirmChoice(Event event){

        notificationThreshold = spinner.getValue();
        mainScreenController.setNotificationThreshold(notificationThreshold);

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

    //don't think I need this
    public void setChangeModeScreenController(ChangeModeScreenController changeModeScreenController){
        this.changeModeScreenController = changeModeScreenController;
    }

    public void setMainScreenController(MainScreenController mainScreenController){
        this.mainScreenController = mainScreenController;
        notificationThreshold = mainScreenController.getNotificationThreshold();
        spinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 100, notificationThreshold, 1));
    }
}