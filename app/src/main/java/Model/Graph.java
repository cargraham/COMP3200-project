package Model;

import Controller.MainScreenController;
import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import okhttp3.Request;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Graph {

    private static GraphServiceClient<Request> graphClient = null;
    private static TokenCredentialAuthProvider authProvider = null;
    private static String challengeString;

    //create an auth provider and graph client
    public static void initializeGraphAuth(String applicationId, List<String> scopes, MainScreenController mainScreenController) {
        // Create the auth provider
        final DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
                .clientId(applicationId)
                .tenantId("common")
                .challengeConsumer(challenge -> {
                    System.out.println(challenge.getMessage());
                    //TODO display this in fx dialog
                })
                .build();

        authProvider = new TokenCredentialAuthProvider(scopes, credential);

        // Create default logger to only log errors
        DefaultLogger logger = new DefaultLogger();
        logger.setLoggingLevel(LoggerLevel.ERROR);

        // Build a Graph client
        graphClient = GraphServiceClient.builder()
                .authenticationProvider(authProvider)
                .logger(logger)
                .buildClient();
    }

    //returns current user object
    public static User getUser() {
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        // GET /me to get authenticated user
        User me = graphClient
                .me()
                .buildRequest()
                .select("displayName,mailboxSettings")
                .get();

        return me;
    }

    //returns an list of a given number of message from a given folder
    public static List<Message> getMailListFromFolder(String folder, int noOfMessages){
        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        MessageCollectionPage messagePage = graphClient.me().mailFolders(folder).messages()
                .buildRequest()
                .top(noOfMessages)
                .get();

        return new ArrayList<>(messagePage.getCurrentPage());
    }

    //returns a list of all mail folders
    public static List<MailFolder> getMailFolders(){
        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        MailFolderCollectionPage mailFolders = graphClient.me().mailFolders()
                .buildRequest()
                .top(100)//TODO this may not be the best way to do it - check if the number of folders is 100 and then request more?
                .get();

        return new ArrayList<>(mailFolders.getCurrentPage());
    }

    //returns a list of attachments given a message ID
    public static List<Attachment> getMessageAttachmentList(String messageID){
        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        AttachmentCollectionPage attachmentPage = graphClient.me().messages(messageID).attachments()
                .buildRequest()
                .get();

        return new ArrayList<>(attachmentPage.getCurrentPage());
    }

    //returns a file attachment given a message ID and attachment ID
    public static FileAttachment getMessageFileAttachment(String messageID, String attachmentID){
        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        FileAttachment fileAttachment = (FileAttachment) graphClient.me().messages(messageID).attachments(attachmentID)
                .buildRequest()
                .get();

        return fileAttachment;
    }

    //returns a new message object given the subject, body, recipients, ccRecipients
    public static Message createMessage(String subject, String bodyText, List<String> recipients, List<String> ccRecipients){
        Message message = new Message();
        message.subject = subject;
        Recipient sender = new Recipient();
        EmailAddress userEmailAddress = new EmailAddress();
        userEmailAddress.address = getUser().userPrincipalName;
        sender.emailAddress = userEmailAddress;
        message.sender = sender;

        ItemBody body = new ItemBody();
        body.contentType = BodyType.HTML;
        body.content = newMessageHTMLConverter(bodyText);
        message.body = body;

        LinkedList<Recipient> toRecipientsList = new LinkedList<>();
        for(String recipient : recipients){
            Recipient toRecipient = new Recipient();
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.address = recipient;
            toRecipient.emailAddress = emailAddress;
            toRecipientsList.add(toRecipient);
        }
        message.toRecipients = toRecipientsList;

        LinkedList<Recipient> ccRecipientList = new LinkedList<>();
        for(String recipient : ccRecipients){
            Recipient ccRecipient = new Recipient();
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.address = recipient;
            ccRecipient.emailAddress = emailAddress;
            ccRecipientList.add(ccRecipient);
        }
        message.ccRecipients = ccRecipientList;

        return message;
    }

    //saves a given message as a draft
    public static void saveDraft(Message message){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        message.isDraft = true;

        graphClient.me().messages()
                .buildRequest()
                .post(message);
    }

    //saves a given message with attachment as a draft
    public static void saveDraftWithAttachment(Message message, LinkedList<Attachment> attachmentLinkedList){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        message.isDraft = true;

        AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();
        attachmentCollectionResponse.value = attachmentLinkedList;
        AttachmentCollectionPage attachmentCollectionPage = new AttachmentCollectionPage(attachmentCollectionResponse, null);
        message.attachments = attachmentCollectionPage;

        graphClient.me().messages()
                .buildRequest()
                .post(message);
    }

    //deletes a draft - used when a new version is saved or sent
    public static void deleteDraft(String messageID){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me().messages(messageID)
                .buildRequest()
                .delete();
    }

    //sends a given message object
    public static void sendMessage(Message message){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me()
                .sendMail(UserSendMailParameterSet
                        .newBuilder()
                        .withMessage(message)
                        .build())
                .buildRequest()
                .post();
    }

    //sends a given message object with attachments
    public static void sendMessageWithAttachment(Message message, LinkedList<Attachment> attachmentLinkedList) throws IOException {
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();
        attachmentCollectionResponse.value = attachmentLinkedList;
        AttachmentCollectionPage attachmentCollectionPage = new AttachmentCollectionPage(attachmentCollectionResponse, null);
        message.attachments = attachmentCollectionPage;

        graphClient.me()
                .sendMail(UserSendMailParameterSet
                        .newBuilder()
                        .withMessage(message)
                        .build())
                .buildRequest()
                .post();
    }

    //moves a message into 'deleted items' folder or deletes permanently if already in 'deleted items'
    public static void deleteMessage(String messageID, String folderName){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        if(folderName != "Deleted Items"){
            String destinationId = "deleteditems";

            graphClient.me().messages(messageID)
                    .move(MessageMoveParameterSet
                            .newBuilder()
                            .withDestinationId(destinationId)
                            .build())
                    .buildRequest()
                    .post();
        }
        else{
            graphClient.me().messages(messageID)
                    .buildRequest()
                    .delete();
        }
    }

    //sends a reply to message given a message ID and reply message object
    public static void replyToMessage(String messageID, Message reply){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me().messages(messageID)
                .reply(MessageReplyParameterSet
                        .newBuilder()
                        .withMessage(reply)
                        .build())
                .buildRequest()
                .post();
    }

    //sends a reply to message given a message ID and reply message object with attachments
    public static void replyToMessageWithAttachment(String messageID, Message reply, LinkedList<Attachment> attachmentLinkedList){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();
        attachmentCollectionResponse.value = attachmentLinkedList;
        AttachmentCollectionPage attachmentCollectionPage = new AttachmentCollectionPage(attachmentCollectionResponse, null);
        reply.attachments = attachmentCollectionPage;

        graphClient.me().messages(messageID)
                .reply(MessageReplyParameterSet
                        .newBuilder()
                        .withMessage(reply)
                        .build())
                .buildRequest()
                .post();
    }

    //creates a new message object where the body contains the body of the message to be forwarded
    public static Message createForwardMessage(String subject, String bodyText, List<String> recipients, List<String> ccRecipients, Message messageToForward){
        Message message = new Message();
        message.subject = subject;
        Recipient sender = new Recipient();
        EmailAddress userEmailAddress = new EmailAddress();
        userEmailAddress.address = getUser().userPrincipalName;
        sender.emailAddress = userEmailAddress;
        message.sender = sender;

        ItemBody body = new ItemBody();
        body.contentType = messageToForward.body.contentType;
        body.content = forwardingHTMLConverter(bodyText) + messageToForward.body.content;
        message.body = body;

        LinkedList<Recipient> toRecipientsList = new LinkedList<>();
        for(String recipient : recipients){
            Recipient toRecipient = new Recipient();
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.address = recipient;
            toRecipient.emailAddress = emailAddress;
            toRecipientsList.add(toRecipient);
        }

        message.toRecipients = toRecipientsList;

        LinkedList<Recipient> ccRecipientList = new LinkedList<>();
        for(String recipient : ccRecipients){
            Recipient ccRecipient = new Recipient();
            EmailAddress emailAddress = new EmailAddress();
            emailAddress.address = recipient;
            ccRecipient.emailAddress = emailAddress;
            ccRecipientList.add(ccRecipient);
        }

        message.ccRecipients = ccRecipientList;

        return message;
    }

    //forwards a message given message ID and the new message created
    public static void forwardMessage(String messageID, Message message) {
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me().messages(messageID)
                .forward(MessageForwardParameterSet
                        .newBuilder()
                        .withMessage(message)
                        .build())
                .buildRequest()
                .post();
    }

    //forwards a message given message ID and the new message created with attachments
    public static void forwardMessageWithAttachment(String messageID, Message message, LinkedList<Attachment> attachmentLinkedList){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        AttachmentCollectionResponse attachmentCollectionResponse = new AttachmentCollectionResponse();
        attachmentCollectionResponse.value = attachmentLinkedList;
        AttachmentCollectionPage attachmentCollectionPage = new AttachmentCollectionPage(attachmentCollectionResponse, null);
        message.attachments = attachmentCollectionPage;

        graphClient.me().messages(messageID)
                .forward(MessageForwardParameterSet
                        .newBuilder()
                        .withMessage(message)
                        .build())
                .buildRequest()
                .post();
    }

    //converts plain text to HTML format for a forwarding message
    public static String forwardingHTMLConverter(String body){
        String newBody = body.replaceAll("(\r\n|\r|\n)", "<br>");
        return "<html><head></head><body style=\"font-family:Helvetica Neue,Helvetica,Arial,sans-serif\">" + newBody + "<br><hr><br></body></html>";
    }

    //converts plain text to HTML format for a new message
    public static String newMessageHTMLConverter(String body){
        String newBody = body.replaceAll("(\r\n|\r|\n)", "<br>");
        return "<html><head></head><body style=\"font-family:Helvetica Neue,Helvetica,Arial,sans-serif\">" + newBody + "</body></html>";
    }

    //updates the given message to read
    public static void readMessage(Message message){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        Message newMessage = new Message();
        newMessage.subject = message.subject;
        ItemBody body = new ItemBody();
        body.contentType = message.body.contentType;
        body.content = message.body.content;
        newMessage.body = body;
        newMessage.isRead = true;

        graphClient.me()
                .messages(message.id)
                .buildRequest()
                .patch(newMessage);

    }

    //creates a new folder with the given name
    public static void newFolder(String name){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        MailFolder mailFolder = new MailFolder();
        mailFolder.displayName = name;

        graphClient.me().mailFolders()
                .buildRequest()
                .post(mailFolder);
    }

    //moves a given message to a given folder
    public static void moveMessage(String messageID, String folderID){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me().messages(messageID)
                .move(MessageMoveParameterSet
                        .newBuilder()
                        .withDestinationId(folderID)
                        .build())
                .buildRequest()
                .post();
    }

    //deletes a deletable folder
    public static void deleteFolder(String folderID){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me().mailFolders(folderID)
                .buildRequest()
                .delete();
    }

    //returns a message object given an ID
    public static Message getMessage(String messageID){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        return graphClient.me().messages(messageID)
                .buildRequest()
                .get();
    }
}