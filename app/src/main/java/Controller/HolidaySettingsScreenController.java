package Controller;

import Model.Holiday;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.StringJoiner;

public class HolidaySettingsScreenController {

    @FXML
    public RadioButton sendersRadioButton;

    @FXML
    public RadioButton keywordsRadioButton;

    @FXML
    public RadioButton importantRadioButton;

    @FXML
    public TextField sendersTextField;

    @FXML
    public TextField keywordsTextField;

    @FXML
    public Button confirmButton;

    @FXML
    public Button cancelButton;

    private Holiday holiday;
    private MainScreenController mainScreenController;
    private String sendersString;
    private String keywordsString;
    private ArrayList<String> sendersList;
    private ArrayList<String> keywordsList;
    private final String SEMI_COLON = "; ";

    public void setMainScreenController(MainScreenController mainScreenController){

        this.mainScreenController = mainScreenController;
        holiday = mainScreenController.getHoliday();

        switch (holiday) {
            case SENDERS_AND_KEYWORDS_AND_IMPORTANT -> {
                sendersRadioButton.setSelected(true);
                keywordsRadioButton.setSelected(true);
                importantRadioButton.setSelected(true);
            }
            case SENDERS_AND_IMPORTANT -> {
                sendersRadioButton.setSelected(true);
                importantRadioButton.setSelected(true);
            }
            case SENDERS_AND_KEYWORDS -> {
                sendersRadioButton.setSelected(true);
                keywordsRadioButton.setSelected(true);
            }
            case KEYWORDS_AND_IMPORTANT -> {
                keywordsRadioButton.setSelected(true);
                importantRadioButton.setSelected(true);
            }
            case SENDERS -> sendersRadioButton.setSelected(true);
            case KEYWORDS -> keywordsRadioButton.setSelected(true);
            case IMPORTANT -> importantRadioButton.setSelected(true);
        }

        StringJoiner senderStringJoiner = new StringJoiner(SEMI_COLON);
        sendersList = mainScreenController.getSenders();
        for(String sender : sendersList){
            senderStringJoiner.add(sender);
        }

        sendersString = senderStringJoiner.toString();
        sendersTextField.setText(sendersString);

        StringJoiner keywordsStringJoiner = new StringJoiner(SEMI_COLON);
        keywordsList = mainScreenController.getKeywords();
        for(String keywords : keywordsList){
            keywordsStringJoiner.add(keywords);
        }

        keywordsString = keywordsStringJoiner.toString();
        keywordsTextField.setText(keywordsString);
    }

    @FXML
    public void confirmChoice(Event event){

        if(!sendersString.equals(sendersTextField.getText())){

            sendersString = sendersTextField.getText().toLowerCase();

            for(String sender : sendersString.split(";")){
                sendersList.add(sender.trim());
            }

            mainScreenController.setSenders(sendersList);
        }

        if(!keywordsString.equals(keywordsTextField.getText())){

            keywordsString = keywordsTextField.getText().toLowerCase();

            for(String keyword : keywordsString.split(";")){
                keywordsList.add(keyword.trim());
            }

            mainScreenController.setKeywords(keywordsList);
        }


        if(sendersRadioButton.isSelected() && keywordsRadioButton.isSelected() && importantRadioButton.isSelected()){
            holiday = Holiday.SENDERS_AND_KEYWORDS_AND_IMPORTANT;
        }
        else if(sendersRadioButton.isSelected() && keywordsRadioButton.isSelected()){
            holiday = Holiday.SENDERS_AND_KEYWORDS;
        }
        else if(sendersRadioButton.isSelected() && importantRadioButton.isSelected()){
            holiday = Holiday.SENDERS_AND_IMPORTANT;
        }
        else if(keywordsRadioButton.isSelected() && importantRadioButton.isSelected()) {
            holiday = Holiday.KEYWORDS_AND_IMPORTANT;
        }
        else if(sendersRadioButton.isSelected()){
            holiday = Holiday.SENDERS;
        }
        else if (keywordsRadioButton.isSelected()){
            holiday = Holiday.KEYWORDS;
        }
        else if(importantRadioButton.isSelected()){
            holiday = Holiday.IMPORTANT;
        }
        else{
            holiday = Holiday.NONE;
        }

        mainScreenController.setHoliday(holiday);

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
