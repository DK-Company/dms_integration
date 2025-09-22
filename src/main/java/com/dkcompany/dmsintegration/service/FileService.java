package com.dkcompany.dmsintegration.service;

import com.dkcompany.dmsintegration.as4client.AS4Exception;
import com.dkcompany.dmsintegration.as4client.As4ClientResponseDto;
import com.dkcompany.dmsintegration.as4client.Tools;
import com.dkcompany.dmsintegration.enums.DeclarationAction;
import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.Document;
import com.dkcompany.dmsintegration.util.As4DkcClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.CompletableFuture;

//DKC/001/050724/TOP Added notificationRequests
//DKC/002/260825/TOP Added support for more fields in the notificationRequests
//DKC/003/020925/TOP Removed som loglines. Added alive ticker.

@Configuration
@EnableScheduling
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;
    private final NotificationService notificationService;

    // Initier FileService
    public FileService(
            As4DkcClient as4DkcClient,
            @Value("${configPath:null}") String configPath,
            NotificationService notificationService) {
        this.as4DkcClient = as4DkcClient;
        this.notificationService = notificationService;
        this.directories = new ArrayList<>();

        addDirectories(configPath);
    }

    // Gennemløber configPath og indlæser alle configfiler (1 fil pr CVR)
    private void addDirectories(String configPath) {
        List<Path> configFiles = getConfigFiles(configPath); // Hent alle configfiles til array

        // Loop configfiles og lave en Directory-instance pr. config
        configFiles.forEach(c -> {
            Properties properties = loadProperties(c.toString());
            directories.add(new Directory(properties));
        });

        // Loop alle Directory og tilføj til as4DkcClient.clients
        this.directories.forEach(d -> {
            //String certificatePrefix = d.getCertificatePrefix();
            this.as4DkcClient.addCertificate(d.getProperties()); // Tilføj til As4 clients arrayen på as4clienten
        });
    }

    // Gennemløb mappen og tilføj all configuration-files
    public static List<Path> getConfigFiles(String directoryPath) {
        List<Path> configFiles = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath), "*.{config}")) {
            for (Path path : directoryStream) {
                configFiles.add(path);
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return configFiles;
    }

    public static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            properties.load(fileInputStream);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return properties;
    }

    //DKC/001/START

    //  Load and decode all the "configfiles" into a list and return it
    public static List<Path> getFiles(String directoryPath, String fileType) {
        List<Path> configFiles = new ArrayList<>(); // List with configFiles to handle
        try (DirectoryStream<Path> directoryStream = Files.newDirectoryStream(Paths.get(directoryPath), "*.{" +fileType +"}")) {
            for (Path path : directoryStream) {
                configFiles.add(path); // Decode and add the configFile to the list
            }
        } catch (IOException ex) {
            ex.printStackTrace();
        }
        return configFiles;
    }

    // Scheduler to handle notificationRequests
    //
    // Scans the notification directory for notificationRequests and handles them
    //
    @Scheduled(fixedDelay = 10000)
    public void notificationRequests() {
        System.out.println("Requesting notifications");
        final int[] files = {0};
        // Handle outgoing requests
        directories.forEach(directory -> {
            //System.out.println("Checking for notificationRequests in %s \n", directory.getNotificationRequestDirectory().getPath());

            // Scan the directory for requests
            List<Path> requestFiles = getFiles(directory.getNotificationRequestDirectory().getPath() ,"request");
            // Handle each request
            requestFiles.forEach(requestFile -> {
                files[0]++;

                Properties requestProperties = loadProperties(requestFile.toString());
                //System.out.println("Handling request id %s from %s \n", requestProperties.getProperty("id"), requestFile.getFileName());
                //System.out.println("Handling request keep %s from %s \n", requestProperties.getProperty("keep"), requestFile.getFileName());

                // Send the notification request to the AS4 gateway
                // ToDo : Hent og valider opsætning fra config filen og send den til As4 serveren

                // Get setup for request
                String serviceEndpoint=requestProperties.getProperty("serviceEndpoint");
                String serviceType=requestProperties.getProperty("serviceType");

                // Get attributes from setup
                Map<String, String> serviceAttributes = new HashMap<>();;

                //DKC/002 If serviceType is Notification, we need to send the correct cvr
                if (serviceType.equals("Notification")){
                    // Notifications need a submitter
                    String submitterId=requestProperties.getProperty("submitterId");
                    if(submitterId==null){serviceAttributes.put("submitterId", directory.getProperties().getProperty("cvr"));}
                    // Check if we submit default from-to intervar
                    String dateFrom=requestProperties.getProperty("dateFrom");
                    if(dateFrom==null){
                        int minutes=7; // duration back to get notifications
                        // Check if we have a minutes property
                        try { minutes = Integer.parseInt(requestProperties.getProperty("minutes")); } catch (NumberFormatException ignore){}
                        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                        serviceAttributes.put("dateFrom", now.minusMinutes(minutes).toString().substring(0,19) );
                        serviceAttributes.put("dateTo", now.toString().substring(0,19) );
                    }
                }

                // Get mrn to look up
                String mrn=requestProperties.getProperty("mrn");
                if(mrn!=null){serviceAttributes.put("mrn", mrn);}

                // Get lsr to lookup
                String lrn=requestProperties.getProperty("lrn");
                if(lrn!=null){serviceAttributes.put("lrn", mrn);}

                // Get regime type EX, IM or TRA (For MRN info)
                String regime=requestProperties.getProperty("regime");
                if(regime!=null){serviceAttributes.put("regime", regime);}

                //DKC/002 get dateFrom
                String dateFrom=requestProperties.getProperty("dateFrom");
                if(dateFrom!=null && !dateFrom.isEmpty()){serviceAttributes.put("dateFrom", dateFrom);}

                //DKC/002 get toDate
                String dateTo=requestProperties.getProperty("dateTo");
                if(dateTo!=null && !dateTo.isEmpty()){serviceAttributes.put("dateTo", dateTo);}

                // Get/Set messageId to use
                String messageId;
                if (requestProperties.getProperty("messageId")==null){
                    messageId=UUID.randomUUID().toString();
                } else {
                    messageId=requestProperties.getProperty("messageId")+'_'+UUID.randomUUID();
                }

                //DKC/002 get "lang"
                String lang=requestProperties.getProperty("lang");
                if(lang!=null){serviceAttributes.put("lang", lang);}

                //DKC/002 get submitterId
                String submitterId=requestProperties.getProperty("submitterId");
                if(submitterId!=null){
                    if(submitterId.isEmpty()){
                        serviceAttributes.put("submitterId", directory.getProperties().getProperty("cvr"));
                    } else {
                        serviceAttributes.put("submitterId", submitterId);
                    }
                }

                // Send request if input is valid
                if(serviceType!=null && serviceEndpoint!=null) {
                    As4ClientResponseDto dto = notificationService.sendRequest(serviceEndpoint, serviceType, serviceAttributes, messageId , directory.getProperties());
                }

                // Move file to handled
                if(requestProperties.getProperty("keep")==null) {
                    directory.moveToHandled(requestFile.toString());
                }
            });
        });
    }
    //DKC/001/STOP

    // Be om nye notifikationer til notifikations-køen
    // -----------------------------------------------
    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void pushPullNotification() {
        // First step: push notification requests concurrently for each certificate
        var futures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(() -> {
                    return notificationService.pushNotificationRequests(d);
                }))
                .toList();
    }

    // Hent notifikationer fra notifikations-køen
    // ------------------------------------------
    @Scheduled(fixedDelay = 60000) // every 1 minutes Pull
    public void pullNotifications() {

        // Second step: pull notifications concurrently for each certificate
        var completableFutures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(() -> {
                    return notificationService.pullNotifications(d);
                }))
                .toList();

        // Third step: save notifications to files in directories
        completableFutures.parallelStream()
                .map(CompletableFuture::join)
                .forEach(notificationService::saveNotifications);
    }


    // Send dokumenter fra out-mappen
    // ------------------------------
    @Scheduled(fixedDelay = 10000)
    public void submitDeclarations() {
        directories.forEach(directory -> {
            List<Document> documents = directory.listFiles();
            if (documents == null || documents.isEmpty()) {
                return;
            }
            submitDocumentsForDirectory(directory, documents);
        });
    }

    private void submitDocumentsForDirectory(
            Directory directory,
            List<Document> documents
    ) {
        String certificatePrefix = directory.getCertificatePrefix();

        documents.forEach(document -> {

            File file = document.file();

            // Use document filename in messageId to identify response notifications for this document
            String messageId;
            messageId= file.getName()+'_'+UUID.randomUUID();

            // Get info on handeling from the document filename
            ProcedureType procedureType = document.procedureType();             // Get ex. "H2"
            DmsService dmsService = document.dmsService();                      // Get ex. "DMS.IMPORT"
            DeclarationAction declarationAction = document.declarationAction(); // get ex. "submit"

            //DKC/000
            // Show in colsole
            System.out.println("dmsService: " + dmsService);

            // Handle the document
            try {
                As4ClientResponseDto response = as4DkcClient.submitDeclaration(
                        file.getAbsolutePath(),
                        procedureType,
                        dmsService,
                        declarationAction,
                        certificatePrefix,
                        messageId // MessageId to use
                );

                var attachment = response.getFirstAttachment();
                String responseStatus = Tools.getStatus(attachment).getCode();
                if (responseStatus.equals("OK")) {
                    directory.moveToSuccess(file);
                } else {
                    directory.moveToError(file, attachment);
                }
            } catch (AS4Exception e) {
                System.out.printf("Error happened when submitting declaration: " + e);
            }
        });
    }
}