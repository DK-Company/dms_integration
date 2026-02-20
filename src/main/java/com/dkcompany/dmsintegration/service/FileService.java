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

import org.springframework.boot.info.BuildProperties;
import org.springframework.stereotype.Service;

/*

    FileService starter alle tasks som kører på en scheduller og udfører opgaverne.

    directories : er en service pr. CVR nummer og "root folder" til CRV nummeret.

    scheduled services:
    notificationRequests : Tjekker om der er nogle notificationRequests i request-mappen og sender dem afsted.
    pushPullNotification : Requester nye notifications på hver directory.
    submitDeclarations: Sender declarations fra out-folderes
    submitBatchDeclarations : Sender declarations fra out-batch-folders.

 */

//DKC/001/050724/TOP Added notificationRequests
//DKC/002/260825/TOP Added support for more fields in the notificationRequests
//DKC/003/020925/TOP Removed som loglines. Added alive ticker.
//DKC/004/090226/TOP Added support for "batches" when submitting declerations
//                   General cleanup and removal of unused code

@Configuration
@EnableScheduling
@Service
@SuppressWarnings("unused")
public class FileService {
    private final List<Directory> directories;
    private final As4DkcClient as4DkcClient;
    private final NotificationService notificationService;

    //private final BuildProperties buildProperties;

    // Initier FileService
    public FileService(
            As4DkcClient as4DkcClient,
            @Value("${configPath:null}") String configPath,
            NotificationService notificationService,
            BuildProperties buildProperties) {

        // Show the tool build version
        //this.buildProperties = buildProperties;
        System.out.println("DMS Integration version: " + buildProperties.getVersion());

        this.as4DkcClient = as4DkcClient;
        this.notificationService = notificationService;
        this.directories = new ArrayList<>();
        // Initier alle root-directories. En pr. CRV nummer der er opsat. (config files)
        addDirectories(configPath);

        // System is ready
        System.out.println("\n\nSystem ready, running " + directories.size()+" directories.\n\n");
    }

    // Gennemløber configPath og indlæser alle configfiler (1 fil pr CVR)
    private void addDirectories(String configPath) {
        List<Path> configFiles = getConfigFiles(configPath); // Hent alle configfiles til array

        // Loop configfiles og lave en Directory-instance pr. config
        configFiles.forEach(c -> {
            Properties properties = loadProperties(c.toString());
            directories.add(new Directory(properties));
            System.out.println("Adding service for directoy " + c + " " +
                    properties.getProperty("cvr")+" "+
                    properties.getProperty("certificatePrefix")+"\n\n");        });

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
            System.err.println("ERROR: Configuration file was not loaded. "+directoryPath);
        }
        return configFiles;
    }

    public static Properties loadProperties(String filePath) {
        Properties properties = new Properties();
        try (FileInputStream fileInputStream = new FileInputStream(filePath)) {
            properties.load(fileInputStream);
        } catch (IOException ex) {
            System.err.println("ERROR: Properties file was not loaded. "+filePath);
        }
        return properties;
    }

    //DKC/001/START

    public static List<Path> loadConfigFiles(String directoryPath, String fileType) {

        List<Path> configFiles = new ArrayList<>();

        try (DirectoryStream<Path> directoryStream =
                     Files.newDirectoryStream(Paths.get(directoryPath), "*." + fileType)) {

            for (Path path : directoryStream) {
                configFiles.add(path);
            }

        } catch (IOException ex) {
            System.err.printf(
                    "ERROR: Unable to load config files from directory '%s' with type '%s'%n",
                    directoryPath,
                    fileType
            );
            ex.printStackTrace(System.err);
            System.exit(1);   // ← terminate JVM with error code
        }

        return configFiles; // technically unreachable if exit happens
    }


    // Scheduler to handle notificationRequests
    //
    // Scans the notification directory for notificationRequests and handles them
    //
    @SuppressWarnings("unused")
    @Scheduled(fixedDelay = 10000)
    public void notificationRequests() {
        System.out.println("Requesting notifications");
        //final int[] files = {0};
        // Handle outgoing requests
        directories.forEach(directory -> {

            // Scan the directory for requests
            List<Path> requestFiles = loadConfigFiles(directory.getNotificationRequestDirectory().getPath(), "request");

            // Handle each request
            requestFiles.forEach(requestFile -> {
                //files[0]++;

                Properties requestProperties = loadProperties(requestFile.toString());

                // Get setup for request
                String serviceEndpoint = requestProperties.getProperty("serviceEndpoint");
                String serviceType = requestProperties.getProperty("serviceType");

                // Get attributes from setup
                Map<String, String> serviceAttributes = new HashMap<>();

                //DKC/002 If serviceType is Notification, we need to send the correct cvr
                if (serviceType.equals("Notification")) {
                    // Notifications need a submitter
                    String submitterId = requestProperties.getProperty("submitterId");
                    if (submitterId == null) {
                        serviceAttributes.put("submitterId", directory.getProperties().getProperty("cvr"));
                    }
                    // Check if we submit default from-to intervar
                    String dateFrom = requestProperties.getProperty("dateFrom");
                    if (dateFrom == null) {
                        int minutes = 7; // duration back to get notifications
                        // Check if we have a minutes property
                        try {
                            minutes = Integer.parseInt(requestProperties.getProperty("minutes"));
                        } catch (NumberFormatException ignore) {
                        }
                        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);
                        serviceAttributes.put("dateFrom", now.minusMinutes(minutes).toString().substring(0, 19));
                        serviceAttributes.put("dateTo", now.toString().substring(0, 19));
                    }
                }

                // Get mrn to look up
                String mrn = requestProperties.getProperty("mrn");
                if (mrn != null) {
                    serviceAttributes.put("mrn", mrn);
                }

                // Get lsr to lookup
                String lrn = requestProperties.getProperty("lrn");
                if (lrn != null) {
                    serviceAttributes.put("lrn", mrn);
                }

                // Get regime type EX, IM or TRA (For MRN info)
                String regime = requestProperties.getProperty("regime");
                if (regime != null) {
                    serviceAttributes.put("regime", regime);
                }

                //DKC/002 get dateFrom
                String dateFrom = requestProperties.getProperty("dateFrom");
                if (dateFrom != null && !dateFrom.isEmpty()) {
                    serviceAttributes.put("dateFrom", dateFrom);
                }

                //DKC/002 get toDate
                String dateTo = requestProperties.getProperty("dateTo");
                if (dateTo != null && !dateTo.isEmpty()) {
                    serviceAttributes.put("dateTo", dateTo);
                }

                //DKC/002 get "lang"
                String lang = requestProperties.getProperty("lang");
                if (lang != null) {
                    serviceAttributes.put("lang", lang);
                }

                //DKC/002 get submitterId
                String submitterId = requestProperties.getProperty("submitterId");
                if (submitterId != null) {
                    if (submitterId.isEmpty()) {
                        serviceAttributes.put("submitterId", directory.getProperties().getProperty("cvr"));
                    } else {
                        serviceAttributes.put("submitterId", submitterId);
                    }
                }

                // Get/Set messageId to use
                String messageId;
                if (requestProperties.getProperty("messageId")==null){
                    messageId=UUID.randomUUID().toString();
                } else {
                    messageId=requestProperties.getProperty("messageId")+'_'+UUID.randomUUID();
                }

                // Send request if input is valid
                if (!serviceType.isEmpty() && !serviceEndpoint.isEmpty()) {
                    notificationService.sendRequest(serviceEndpoint, serviceType, serviceAttributes, directory.getProperties(), messageId);
                }

                // Move file to handled
                if (requestProperties.getProperty("keep") == null) {
                    directory.moveToHandled(requestFile.toString());
                }
            });
        });
    }
    //DKC/001/STOP

    // Be om nye notifikationer til notifikations-køen
    // -----------------------------------------------
    @SuppressWarnings("unused")
    @Scheduled(fixedDelay = 300000) // every 5 minutes
    public void pushPullNotification() {

        var futures = directories.stream()
                .map(d -> CompletableFuture.runAsync(() ->
                        notificationService.pushNotificationRequests(d)
                ))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    // Hent notifikationer fra notifikations-køen
    // ------------------------------------------
    @SuppressWarnings("unused")
    @Scheduled(fixedDelay = 60000) // every 1 minutes Pull
    public void pullNotifications() {

        // Second step: pull notifications concurrently for each certificate
        var completableFutures = directories.stream()
                .map(d -> CompletableFuture.supplyAsync(
                        () -> notificationService.pullNotifications(d)
                ))
                .toList();

        // Third step: save notifications to files in directories
        completableFutures.parallelStream()
                .map(CompletableFuture::join)
                .forEach(notificationService::saveNotifications);
    }


    // Send dokumenter fra out-mappen
    // ------------------------------
    @SuppressWarnings("unused")
    @Scheduled(fixedDelay = 10000)
    public void submitDeclarations() {
        // Loop and send xml files in the out-folder
        directories.forEach(directory -> {
            List<Document> documents = directory.listDeclarationFiles();
            if (documents == null || documents.isEmpty()) {
                return;
            }
            submitDocumentsForDirectory(directory, documents);
        });
    }

    //DKC/004/START
    // Send dokumenter fra out-batch-mapper
    // ------------------------------
    @Scheduled(fixedDelay = 30000)
    public void submitBatchDeclarations() {
        System.out.print("Scanning for batches\n");
        // Loop and handle batch folders in the out-folder
        directories.forEach(directory ->
        {
            // Get a list of batch folders that are ready to be handled
            List<File> batches = directory.listBatches();
            if (batches == null || batches.isEmpty()) {
                return;
            }
            submitDocumentsInBatches(directory, batches);
        });
    }

    // Scan for batchfolders that are ready to be submitted
    private void submitDocumentsInBatches(Directory directory, List<File> batches) {
        for (File batchFolder : batches) {
            // handle batch folder
            long startTime = System.nanoTime();   // Start timing
            try {
                // Use Directory to discover+parse batch XML files (same style as listFiles())
                List<Document> documents = directory.listBatchFiles(batchFolder);
                if (documents == null || documents.isEmpty()) {
                    // Nothing to submit; do not mark error
                    continue;
                }

                // Validate: all documents must share same ProcedureType + DmsService
                ProcedureType expectedProcedureType = documents.get(0).procedureType();
                DmsService expectedDmsService = documents.get(0).dmsService();
                boolean mixed = documents.stream().anyMatch(d ->
                        d.procedureType() != expectedProcedureType ||
                                d.dmsService() != expectedDmsService
                );

                // Mark as error if there are mixed files in the batch
                if (mixed) {
                    String errorMessage="Mixed ProcedureType/DmsService in batch: "+ batchFolder.getName()+ " Expected: " +
                            expectedProcedureType + " / " + expectedDmsService;
                    writeBatchMessage(batchFolder,errorMessage,"error.txt");
                    continue;
                }

                // Folder OK -> delegate actual one-request submission
                submitBatchDocuments(directory, batchFolder, documents);
                // Write handled file
                long durationMs = (System.nanoTime() - startTime) / 1_000_000;
                writeBatchMessage(batchFolder,"Batch handled : "+ batchFolder.getName()+"\nDuration: " + durationMs + " ms","handled.txt");
                directory.moveToSuccess(batchFolder);

            } catch (Exception ex) {
                String errorMessage="Exception while validating/submitting batch: " +
                        ex.getClass().getSimpleName() + ": " + ex.getMessage();
                writeBatchMessage(batchFolder,errorMessage,"error.txt");
                directory.moveToError(batchFolder, errorMessage);
            }
        }
    }

    private void writeBatchMessage(File batchFolder, String message, String filename ) {
        try {
            File errorTxt = new File(batchFolder, filename);
            try (java.io.FileWriter fw = new java.io.FileWriter(errorTxt, false)) {
                fw.write(message);
                fw.write(System.lineSeparator());
            }
        } catch (Exception ex) {
            System.err.println("Failed to write error.txt in batch folder: " + batchFolder.getAbsolutePath());
        }
    }

    private void submitBatchDocuments(
            Directory directory,      // The current Directory service (CVR number)
            File batchFolder,         // The foler with files for this batch
            List<Document> documents  // The documents we need to send this batch
    ) throws AS4Exception {
        // Create one submit with all the files from the folder
        String certificatePrefix = directory.getCertificatePrefix();

        // Create list of files to use
        List<File> files =  new ArrayList<>();

        documents.forEach(document -> {
            File file = document.file();
            // Add the file to the list of files to send this batch
            files.add(file);
        });

        // Submit the documents
        As4ClientResponseDto response;
        try {
            response = as4DkcClient.submitBatch(
                    files,
                    documents.get(0).procedureType(),
                    documents.get(0).dmsService(),
                    documents.get(0).declarationAction(),
                    certificatePrefix
            );
        }
        catch (AS4Exception e) {
            System.out.printf("Error happened when submitting batch %s: %s%n",
                    batchFolder.getName(), e.getMessage());
            throw e;
        }
        System.out.printf("\n batch posted : " + batchFolder.getName() + "\n");
        System.out.printf("\n response : " + response + "\n");
    }
    //DKC/004/STOP


    private void submitDocumentsForDirectory(
            Directory directory,
            List<Document> documents
    ) {
        String certificatePrefix = directory.getCertificatePrefix();

        documents.forEach(document -> {

            File file = document.file();

            // Get info on handeling from the document filename
            ProcedureType procedureType = document.procedureType();             // Get ex. "H2"
            DmsService dmsService = document.dmsService();                      // Get ex. "DMS.IMPORT"
            DeclarationAction declarationAction = document.declarationAction(); // get ex. "submit"

            // Handle the document
            try {
                As4ClientResponseDto response = as4DkcClient.submitDeclaration(
                        file.getAbsolutePath(),
                        procedureType,
                        dmsService,
                        declarationAction,
                        certificatePrefix
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