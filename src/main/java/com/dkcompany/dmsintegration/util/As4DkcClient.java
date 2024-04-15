package com.dkcompany.dmsintegration.util;

import com.dkcompany.dmsintegration.Application;
import com.dkcompany.dmsintegration.enums.DeclarationAction;
import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.CryptoProperties;
import com.dkcompany.dmsintegration.service.FileService;
import dk.toldst.eutk.as4client.As4Client;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.builder.support.As4ClientBuilderInstance;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;

@Component
public class As4DkcClient {
    private final Map<String, As4Client> clients;
    private final String as4Endpoint;
    private static final Logger logger = LoggerFactory.getLogger(As4DkcClient.class);

    public As4DkcClient(@Value("${as4Endpoint:null}") String as4Endpoint) {
        this.clients = new HashMap<>();
        if (as4Endpoint.equals("null")) {
            this.as4Endpoint = "https://secureftpgatewaytest.skat.dk:6384";
        } else {
            this.as4Endpoint = as4Endpoint;
        }
    }

    public void addCertificate(Properties properties) {
        this.clients.put(properties.getProperty("certificatePrefix"), createAs4Client(properties));
    }

    private As4Client getClientFromCertificatePrefix(String certificatePrefix) {
        As4Client client = this.clients.get(certificatePrefix);
        Objects.requireNonNull(client, "No AS4Client registered with the given certificate.");

        return client;
    }

    public As4ClientResponseDto submitDeclaration(
            String filePath,
            ProcedureType procedureType,
            DmsService dmsService,
            DeclarationAction declarationAction,
            String certificatePrefix
    ) throws AS4Exception {
        Path path = Paths.get(filePath);
        byte[] declarationBytes;
        try {
            declarationBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        As4Client client = getClientFromCertificatePrefix(certificatePrefix);

        return client.executePush(
                dmsService.value,
                "Declaration." + declarationAction.value,
                declarationBytes,
                Map.of("procedureType", procedureType.value)
        );
    }

    public As4ClientResponseDto pushNotificationRequest(
            LocalDateTime then,
            LocalDateTime now,
            Properties properties //DKC/001 String certificatePrefix
    ) throws AS4Exception {
        String certificatePrefix = properties.getProperty("certificatePrefix"); //DKC/001
        As4Client client = getClientFromCertificatePrefix(certificatePrefix);

        System.out.println("Sending push-pull request for csv "+properties.getProperty("cvr")); //DKC/001

        return client.executePush(
                "DMS.Export",
                "Notification",
                Map.of(
                        "lang", "EN",
                        "submitterId", properties.getProperty("cvr"), //DKC/001 get cvr from properties "24431118",
                        "dateFrom", then.toString(),
                        "dateTo", now.toString()
                )
        );
    }

    public As4ClientResponseDto pullNotifications(Properties properties) {
        As4Client client = getClientFromCertificatePrefix(properties.getProperty("certificatePrefix"));

        String notificationQueueURL = properties.getProperty("notificationQueueURL");

        System.out.println("Pull notifications for csv "+properties.getProperty("cvr")); //DKC/001
        try {
            return client.executePull(notificationQueueURL); // needs specific
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        }
    }

    private As4Client createAs4Client(Properties properties) {
        var cryptoProperties = new CryptoProperties(
                properties.getProperty("file"),
                properties.getProperty("password"),
                properties.getProperty("type"),
                properties.getProperty("alias"),
                properties.getProperty("privatePassword")
        );

        // assert that path to certificate exists - ensures more readable exception if wrong path
        String certificatePath = properties.getProperty("file");
        if (!Files.exists(Paths.get(certificatePath))) {
            throw new RuntimeException("No certificate found at: " + certificatePath);
        }
        File cryptoPropertiesFile = CryptoPropertiesFile.generate(cryptoProperties);

        String cryptoPath = cryptoPropertiesFile.getAbsolutePath();
        String gatewayPassword = properties.getProperty("gatewayPassword");

        try {
            if (properties.getProperty("cvr") == null) //DKC/001
            {
                throw new Exception("cvr has not been set in .config file");
            }
            if (properties.getProperty("file") == null)
            {
                throw new Exception("file has not been set in .config file");
            }
            if (properties.getProperty("password") == null)
            {
                throw new Exception("password has not been set in .config file");
            }
            if (properties.getProperty("type") == null)
            {
                throw new Exception("type has not been set in .config file");
            }
            if (properties.getProperty("alias") == null)
            {
                throw new Exception("alias has not been set in .config file");
            }
            if (properties.getProperty("privatePassword") == null)
            {
                throw new Exception("privatePassword has not been set in .config file");
            }
            if (properties.getProperty("gatewayPassword") == null)
            {
                throw new Exception("gatewayPassword has not been set in .config file");
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
        }

        try {
            return new As4ClientBuilderInstance()
                    .builder()
                    .setEndpoint(this.as4Endpoint)
                    .setCrypto(cryptoPath)
                    .setPassword(gatewayPassword)
                    .build();
        } catch (AS4Exception e) {
            logger.error("Error happened when trying to create As4ClientBuilderInstance. inner exception: " + e.getMessage());
            throw new RuntimeException(e);
        } finally {
            cryptoPropertiesFile.delete();
        }
    }
}
