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
import javafx.stage.Modality;
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
    private final ToggleGroup toggleGroup = new ToggleGroup();

    //initialises the UI after FXML values have been injected
    @FXML
    public void initialize(){
        normalRadioButton.setToggleGroup(toggleGroup);
        disturbRadioButton.setToggleGroup(toggleGroup);
        concentratedRadioButton.setToggleGroup(toggleGroup);
        holidayRadioButton.setToggleGroup(toggleGroup);
    }

    //sets the main screen controller and imports the current settings
    public void setMainScreenController(MainScreenController mainScreenController){
        this.mainScreenController = mainScreenController;
        mode = mainScreenController.getMode();

        switch (mode) {
            case NORMAL -> toggleGroup.selectToggle(normalRadioButton);
            case DISTURB -> toggleGroup.selectToggle(disturbRadioButton);
            case CONCENTRATED -> toggleGroup.selectToggle(concentratedRadioButton);
            case HOLIDAY -> toggleGroup.selectToggle(holidayRadioButton);
        }
    }

    //launches the change sync frequency stage on button click
    @FXML
    public void showChangeSyncFrequency() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ChangeSyncFrequencyScreen.fxml"));
        Parent root = fxmlLoader.load();

        ChangeSyncFrequencyScreenController changeSyncFrequencyScreenController = fxmlLoader.getController();
        changeSyncFrequencyScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 300, 200);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setResizable(false);
        stage.setTitle("Change Sync Frequency");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.show();
    }

    //launches do not disturb settings on button click
    @FXML
    public void disturbSettings() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/DisturbSettingsScreen.fxml"));
        Parent root = fxmlLoader.load();

        DisturbSettingsScreenController disturbSettingsScreenController = fxmlLoader.getController();
        disturbSettingsScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setResizable(false);
        stage.setTitle("Do Not Disturb Settings");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.show();
    }

    //launches concentrated settings on button click
    @FXML
    public void concentratedSettings() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/ConcentratedSettingsScreen.fxml"));
        Parent root = fxmlLoader.load();

        ConcentratedSettingsScreenController concentratedSettingsScreenController = fxmlLoader.getController();
        concentratedSettingsScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 500, 250);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setResizable(false);
        stage.setTitle("Concentrated Mode Settings");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.show();
    }

    //launches holiday settings on button click
    @FXML
    public void holidaySettings() throws IOException {
        FXMLLoader fxmlLoader = new FXMLLoader(getClass().getResource("/FXML/HolidaySettingsScreen.fxml"));
        Parent root = fxmlLoader.load();

        HolidaySettingsScreenController holidaySettingsScreenController = fxmlLoader.getController();
        holidaySettingsScreenController.setMainScreenController(mainScreenController);

        Stage stage = new Stage();
        Scene scene = new Scene(root, 600, 400);
        scene.getStylesheets().add("/stylesheet.css");
        stage.setResizable(false);
        stage.setTitle("Holiday Mode Settings");
        stage.initModality(Modality.APPLICATION_MODAL);
        stage.setScene(scene);
        stage.show();
    }

    //sends selected choice to main screen controller on confirm button click
    @FXML
    public void confirmChoice(Event event){
        RadioButton selectedToggle = (RadioButton) toggleGroup.getSelectedToggle();

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

    //closes the stage and doesn't save setting choices on cancel button click
    @FXML
    public void cancel(Event event){
        Node source = (Node) event.getSource();
        Stage stage = (Stage) source.getScene().getWindow();
        stage.close();
    }
}
