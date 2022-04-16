package Model;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.*;
import com.microsoft.graph.requests.*;
import com.microsoft.graph.tasks.LargeFileUploadTask;
import okhttp3.Request;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class Graph {

    private static GraphServiceClient<Request> graphClient = null;
    private static TokenCredentialAuthProvider authProvider = null;

    public static void initializeGraphAuth(String applicationId, List<String> scopes) {
        // Create the auth provider
        final DeviceCodeCredential credential = new DeviceCodeCredentialBuilder()
                .clientId(applicationId)
                .tenantId("common")
                .challengeConsumer(challenge -> System.out.println(challenge.getMessage())) //TODO display this in fx dialog
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

    public static String getUserAccessToken()
    {
        try {
            URL meUrl = new URL("https://graph.microsoft.com/v1.0/me");
            return authProvider.getAuthorizationTokenAsync(meUrl).get();
        } catch(Exception ex) {
            return null;
        }
    }

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

    public static List<Message> getMailListFromFolder(String folder, int noOfMessages){

        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        MessageCollectionPage messagePage = graphClient.me().mailFolders(folder).messages()
                .buildRequest()
                .top(noOfMessages)
                .get();

        return new ArrayList<>(messagePage.getCurrentPage());
    }

    public static List<MailFolder> getMailFolders(){

        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        MailFolderCollectionPage mailFolders = graphClient.me().mailFolders()
                .buildRequest()
                .top(100)//TODO this may not be the best way to do it - check if the number of folders is 100 and then request more?
                .get();

        return new ArrayList<>(mailFolders.getCurrentPage());
    }

    public static List<Attachment> getMessageAttachmentList(String messageID){

        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        AttachmentCollectionPage attachmentPage = graphClient.me().messages(messageID).attachments()
                .buildRequest()
                .get();

        return new ArrayList<>(attachmentPage.getCurrentPage());
    }

    public static Attachment getMessageAttachment(String messageID, String attachmentID){

        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        Attachment attachment = graphClient.me().messages(messageID).attachments(attachmentID)
                .buildRequest()
                .get();

        return attachment;
    }

    public static FileAttachment getMessageFileAttachment(String messageID, String attachmentID){

        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        FileAttachment fileAttachment = (FileAttachment) graphClient.me().messages(messageID).attachments(attachmentID)
                .buildRequest()
                .get();

        return fileAttachment;
    }

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
        body.content = newMessageHTMLParser(bodyText);
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

    public static void saveDraft(Message message){

        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        message.isDraft = true;

        graphClient.me().messages()
                .buildRequest()
                .post(message);
    }

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

    public static void replyAllToMessage(String messageID, Message reply){

        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        graphClient.me().messages(messageID)
                .replyAll(MessageReplyAllParameterSet
                        .newBuilder()
                        .withMessage(reply)
                        .build())
                .buildRequest()
                .post();
    }

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
        body.content = forwardingHTMLParser(bodyText) + messageToForward.body.content;
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

    public static String forwardingHTMLParser(String body){

        String newBody = body.replaceAll("(\r\n|\r|\n)", "<br>");

        return "<html><head></head><body style=\"font-family:Helvetica Neue,Helvetica,Arial,sans-serif\">" + newBody + "<br><hr><br></body></html>";
    }

    public static String newMessageHTMLParser(String body){

        String newBody = body.replaceAll("(\r\n|\r|\n)", "<br>");

        return "<html><head></head><body style=\"font-family:Helvetica Neue,Helvetica,Arial,sans-serif\">" + newBody + "</body></html>";
    }

    public static void readMessage(Message message){

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
}