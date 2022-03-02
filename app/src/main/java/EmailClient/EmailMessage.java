package EmailClient;

import com.microsoft.graph.models.Message;

public class EmailMessage {

    private String senderName;
    private String senderAddress;
    private String subject;

    public EmailMessage(Message message){
        senderName = message.sender.emailAddress.name;
        senderAddress = message.sender.emailAddress.address;
        subject = message.subject;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getSenderAddress() {
        return senderAddress;
    }

    public String getSubject() {
        return subject;
    }
}
