package Model;

import com.azure.identity.DeviceCodeCredential;
import com.azure.identity.DeviceCodeCredentialBuilder;
import com.microsoft.graph.authentication.TokenCredentialAuthProvider;
import com.microsoft.graph.logger.DefaultLogger;
import com.microsoft.graph.logger.LoggerLevel;
import com.microsoft.graph.models.*;
import com.microsoft.graph.options.HeaderOption;
import com.microsoft.graph.options.Option;
import com.microsoft.graph.options.QueryOption;
import com.microsoft.graph.requests.*;
import com.microsoft.graph.serializer.OffsetDateTimeSerializer;
import okhttp3.Request;

import java.net.URL;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

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

    public static List<Event> getCalendarView( //TODO not used
            ZonedDateTime viewStart, ZonedDateTime viewEnd, String timeZone) {
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        List<Option> options = new LinkedList<Option>();
        options.add(new QueryOption("startDateTime", viewStart.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        options.add(new QueryOption("endDateTime", viewEnd.format(DateTimeFormatter.ISO_OFFSET_DATE_TIME)));
        // Sort results by start time
        options.add(new QueryOption("$orderby", "start/dateTime"));

        // Start and end times adjusted to user's time zone
        options.add(new HeaderOption("Prefer", "outlook.timezone=\"" + timeZone + "\""));

        // GET /me/events
        EventCollectionPage eventPage = graphClient
                .me()
                .calendarView()
                .buildRequest(options)
                .select("subject,organizer,start,end")
                .top(25)
                .get();

        List<Event> allEvents = new LinkedList<Event>();

        // Create a separate list of options for the paging requests
        // paging request should not include the query parameters from the initial
        // request, but should include the headers.
        List<Option> pagingOptions = new LinkedList<Option>();
        pagingOptions.add(new HeaderOption("Prefer", "outlook.timezone=\"" + timeZone + "\""));

        while (eventPage != null) {
            allEvents.addAll(eventPage.getCurrentPage());

            EventCollectionRequestBuilder nextPage =
                    eventPage.getNextPage();

            if (nextPage == null) {
                break;
            } else {
                eventPage = nextPage
                        .buildRequest(pagingOptions)
                        .get();
            }
        }

        return allEvents;
    }

    public static void createEvent( //TODO not used
            String timeZone,
            String subject,
            LocalDateTime start,
            LocalDateTime end,
            Set<String> attendees,
            String body)
    {
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        Event newEvent = new Event();

        newEvent.subject = subject;

        newEvent.start = new DateTimeTimeZone();
        newEvent.start.dateTime = start.toString();
        newEvent.start.timeZone = timeZone;

        newEvent.end = new DateTimeTimeZone();
        newEvent.end.dateTime = end.toString();
        newEvent.end.timeZone = timeZone;

        if (attendees != null && !attendees.isEmpty()) {
            newEvent.attendees = new LinkedList<Attendee>();

            attendees.forEach((email) -> {
                Attendee attendee = new Attendee();
                // Set each attendee as required
                attendee.type = AttendeeType.REQUIRED;
                attendee.emailAddress = new EmailAddress();
                attendee.emailAddress.address = email;
                newEvent.attendees.add(attendee);
            });
        }

        if (body != null) {
            newEvent.body = new ItemBody();
            newEvent.body.content = body;
            // Treat body as plain text
            newEvent.body.contentType = BodyType.TEXT;
        }

        // POST /me/events
        graphClient
                .me()
                .events()
                .buildRequest()
                .post(newEvent);
    }

    public static List<Message> getMailList(int noOfMessages){

        if (graphClient == null) throw new NullPointerException("Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        MessageCollectionPage messagePage = graphClient.me().messages()
                .buildRequest()
                .top(noOfMessages)
                .get();

        return new ArrayList<Message>(messagePage.getCurrentPage());
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
        body.contentType = BodyType.TEXT;
        body.content = bodyText;
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

    public static void forwardMessage(String messageID, List<String> toRecipients, Message message) {

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

        //if(body.contentType == BodyType.HTML) {
            body.content = htmlParser(bodyText) + messageToForward.body.content;
        /*}
        else{
            body.content = bodyText + messageToForward.body.content;
        }*/

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

    public static String htmlParser(String body){

        String newBody = body.replaceAll("(\r\n|\r|\n)", "<br>");

        System.out.println(newBody);

        return "<html>" + newBody + "<br><hr><br></html>";
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

    public static void createSubscription() throws ParseException {

        GraphServiceClient graphClient = GraphServiceClient.builder().authenticationProvider( authProvider ).buildClient();

        Subscription subscription = new Subscription();
        subscription.changeType = "created";
        subscription.notificationUrl = "https://webhook.site/a9639b99-4933-4367-bab8-bca509f5b11d";
        subscription.resource = "me/mailFolders('Inbox')/messages";
        subscription.expirationDateTime = OffsetDateTimeSerializer.deserialize("2022-04-05T18:23:45.9356913Z");
        subscription.clientState = "secretClientValue";

        graphClient.subscriptions()
                .buildRequest()
                .post(subscription);
    }
}