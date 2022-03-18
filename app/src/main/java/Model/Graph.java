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
import okhttp3.Request;

import java.net.URL;
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
                .challengeConsumer(challenge -> System.out.println(challenge.getMessage()))
                .build();

        authProvider = new TokenCredentialAuthProvider(scopes, credential); //not null

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

    public static List<Event> getCalendarView(
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

    public static void createEvent(
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

    /*public static Subscription getMailChangeNotifications() throws ParseException {
        Subscription subscription = new Subscription();
        subscription.changeType = "created";
        subscription.notificationUrl = "https://webhook.azurewebsites.net/api/send/myNotifyClient";
        subscription.resource = "me/mailFolders('Inbox')/messages";
        subscription.expirationDateTime = OffsetDateTimeSerializer.deserialize("2023-11-20T18:23:45.9356913Z");
        subscription.clientState = "secretClientValue";
        subscription.latestSupportedTlsVersion = "v1_2";

        Subscription sub = graphClient.subscriptions()
                .buildRequest()
                .post(subscription);

        return sub;
    }

    /*public static Conversation getMessageConversation(String messageID, String conversationID){
        if (graphClient == null) throw new NullPointerException(
                "Graph client has not been initialized. Call initializeGraphAuth before calling this method");

        Conversation conversation = graphClient.groups("4d81ce71-486c-41e9-afc5-e41bf2d0722a").conversations("AAQkAGRhZmRhMWM3LTYwZTktNDZmYy1hNWU1LThhZWU4NzI2YTEyZgAQABKPPJ682apIiV1UFlj7XxY=")
                .buildRequest()
                .get();
    }*/
}