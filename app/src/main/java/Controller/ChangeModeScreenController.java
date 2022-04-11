package Controller;

import Model.Disturb;
import Model.Mode;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.ToggleGroup;
import javafx.stage.Stage;

import java.io.IOException;

public class ChangeModeScreenController {

    @FXML
    public RadioButton normalRadioButton;

    @FXML
    public RadioButton disturbRadioButton;

    @FXML
    public RadioButton concentratedRadioButton;

    @FXML
    public RadioButton holidayRadioButton;

    @FXML
    public Button syncFrequencyButton;

    @FXML
    public Button disturbSettingsButton;

    @FXML
    public Button concentratedSettingsButton;

    @FXML
    public Button holidaySettingsButton;

    @FXML
    public Button cancelButton;

    @FXML
    public Button confirmButton;

    private MainScreenController mainScreenController;
    private Mode mode;
    private final ToggleGroup group = new ToggleGroup();

    @FXML
    public void initialize(){

        normalRadioButton.setToggleGroup(group);
        disturbRadioButton.setToggleGroup(group);
        concentratedRadioButton.setToggleGroup(group);
        holidayRadioButton.setToggleGroup(group);
    }

    public void setMainScreenController(MainScreenController mainScreenController){

        this.mainScreenController = mainScreenController;
        mode = mainScreenController.getMode();

        switch (mode) {
            case NORMAL -> group.selectToggle(normalRadioButton);
            case DISTURB -> group.selectToggle(disturbRadioButton);
            case CONCENTRATED -> group.selectToggle(concentratedRadioButton);
            case HOLIDAY -> group.selectToggle(holidayRadioButton);
        }
    }

    @FXML
    public void disturbSettings() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/DisturbSettingsScreen.fxml"));
        Parent root = fxmlLoader.load();

        DisturbSettingsScreenController disturbSettingsScreenController = fxmlLoader.getController();
        disturbSettingsScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Do Not Disturb Settings");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void concentratedSettings() throws IOException {

        //TODO can't access windows behind this one?

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ConcentratedSettingsScreen.fxml"));
        Parent root = fxmlLoader.load();

        ConcentratedSettingsScreenController concentratedSettingsScreenController = fxmlLoader.getController();
        concentratedSettingsScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 500, 250);
        stage.setTitle("Concentrated Mode Settings");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void holidaySettings() throws IOException {

        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/HolidaySettingsScreen.fxml"));
        Parent root = fxmlLoader.load();

        HolidaySettingsScreenController holidaySettingsScreenController = fxmlLoader.getController();
        holidaySettingsScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        stage.setTitle("Holiday Mode Settings");
        stage.setScene(scene);
        stage.show();
    }

    @FXML
    public void confirmChoice(Event event){

        RadioButton selectedToggle = (RadioButton) group.getSelectedToggle();

        switch (selectedToggle.getText()) {
            case "Normal" -> mode = Mode.NORMAL;
            case "Do Not Disturb Mode" -> mode = Mode.DISTURB;
            case "Concentrated Mode" -> mode = Mode.CONCENTRATED;
            case "Holiday Mode" -> mode = Mode.HOLIDAY;
        }

        if(mainScreenController.getMode() == Mode.DISTURB && mainScreenController.getDisturb() == Disturb.OFF){
            mode = Mode.NORMAL;
        }

        mainScreenController.setMode(mode);

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
