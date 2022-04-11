package Controller;

import Model.Disturb;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import tornadofx.control.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class DisturbSettingsScreenController {

    @FXML
    public ToggleButton toggleButton;

    @FXML
    public RadioButton radioButton1Hour;

    @FXML
    public RadioButton radioButton8Hours;

    @FXML
    public RadioButton radioButton24Hours;

    @FXML
    public RadioButton radioButtonUntil;

    @FXML
    public DatePicker datePicker;

    @FXML
    public Spinner<Integer> hourPicker;

    @FXML
    public Text hoursText;

    @FXML
    public Spinner<Integer> minutePicker;

    @FXML
    public Text minutesText;

    @FXML
    public HBox untilHbox;

    @FXML
    public Button cancelButton;

    @FXML
    public Button confirmButton;

    private MainScreenController mainScreenController;
    private Disturb disturb;
    private Date disturbTime;
    private final ToggleGroup group = new ToggleGroup();

    @FXML
    public void initialize(){


        radioButton1Hour.setToggleGroup(group);
        radioButton8Hours.setToggleGroup(group);
        radioButton24Hours.setToggleGroup(group);
        radioButtonUntil.setToggleGroup(group);

        hourPicker.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 0, 1));
        minutePicker.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 1));
    }

    public void setMainScreenController(MainScreenController mainScreenController){

        this.mainScreenController = mainScreenController;
        disturb = mainScreenController.getDisturb();
        disturbTime = mainScreenController.getDisturbTime();

        if(disturbTime != null){
            LocalDate localDate = disturbTime.toInstant()
                    .atZone(ZoneId.systemDefault())
                    .toLocalDate();
            System.out.println(localDate);
            datePicker.setValue(localDate);
            Calendar calendar = GregorianCalendar.getInstance();
            calendar.setTime(disturbTime);
            hourPicker.getValueFactory().setValue(calendar.get(Calendar.HOUR_OF_DAY));
            minutePicker.getValueFactory().setValue(calendar.get(Calendar.MINUTE));
        }

        switch (disturb){
            case TIMED_1_HOUR -> group.selectToggle(radioButton1Hour);
            case TIMED_8_HOURS -> group.selectToggle(radioButton8Hours);
            case TIMED_24_HOURS -> group.selectToggle(radioButton24Hours);
            case TIMED_UNTIL -> group.selectToggle(radioButtonUntil);
            case ON -> {
                toggleButton.setSelected(true);
                toggleMode();
            }
            case OFF -> {
                toggleButton.setSelected(false);
                toggleMode();
            }
        }
    }

    @FXML
    public void toggleMode(){

        if(toggleButton.isSelected()){
            radioButton1Hour.setDisable(true);
            radioButton8Hours.setDisable(true);
            radioButton24Hours.setDisable(true);
            radioButtonUntil.setDisable(true);
            datePicker.setDisable(true);
            hourPicker.setDisable(true);
            hoursText.setDisable(true);
            minutePicker.setDisable(true);
            minutesText.setDisable(true);
            toggleButton.setText("Turn Do Not Disturb Mode Off");
        }
        else{
            radioButton1Hour.setDisable(false);
            radioButton8Hours.setDisable(false);
            radioButton24Hours.setDisable(false);
            radioButtonUntil.setDisable(false);
            datePicker.setDisable(false);
            hourPicker.setDisable(false);
            hoursText.setDisable(false);
            minutePicker.setDisable(false);
            minutesText.setDisable(false);
            toggleButton.setText("Turn Do Not Disturb Mode On");
        }
    }

    @FXML
    public void confirmChoice(Event event){

        RadioButton selectedToggle = (RadioButton) group.getSelectedToggle();

        if(toggleButton.isSelected()){
            disturb = Disturb.ON;
        }
        else if(!toggleButton.isSelected() && selectedToggle == null){
            disturb = Disturb.OFF;
        }
        else {

            Date date = new Date();
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(date);

            switch (selectedToggle.getText()){
                case "Turn Do Not Disturb on for 1 Hour" -> {
                    disturb = Disturb.TIMED_1_HOUR;
                    calendar.add(Calendar.HOUR_OF_DAY, 1);
                    disturbTime = calendar.getTime();
                }
                case "Turn Do Not Disturb on for 8 Hours" -> {
                    disturb = Disturb.TIMED_8_HOURS;
                    calendar.add(Calendar.HOUR_OF_DAY, 8);
                    disturbTime = calendar.getTime();
                }
                case "Turn Do Not Disturb on for 24 Hours" -> {
                    disturb = Disturb.TIMED_24_HOURS;
                    calendar.add(Calendar.HOUR_OF_DAY, 24);
                    disturbTime = calendar.getTime();
                }
                case "Turn Do Not Disturb on until:" -> {
                    disturb = Disturb.TIMED_UNTIL;
                    LocalDate date1 = datePicker.getValue();
                    LocalDateTime ldt = date1.atTime(hourPicker.getValue(), minutePicker.getValue());
                    Date out = Date.from(ldt.atZone(ZoneId.systemDefault()).toInstant());
                    disturbTime = out;
                }
            }

            mainScreenController.setDisturbTime(disturbTime);
            System.out.println(disturbTime);
        }

        mainScreenController.setDisturb(disturb);

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
