package com.dkcompany.dmsintegration.util;

import com.dkcompany.dmsintegration.enums.DmsService;
import com.dkcompany.dmsintegration.enums.ProcedureType;
import com.dkcompany.dmsintegration.record.CryptoProperties;
import dk.toldst.eutk.as4client.As4Client;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.builder.support.As4ClientBuilderInstance;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class As4DkcClient {
    private final Map<String, As4Client> clients;

    public As4DkcClient() {
        this.clients = new HashMap<>();
        addCertificate("oces3");
    }

    public void addCertificate(String certificatePrefix) {
        this.clients.put(certificatePrefix, createAs4Client(certificatePrefix));
    }

    private As4Client getClientFromCertificatePrefix(String certificatePrefix) {
        As4Client client = this.clients.get(certificatePrefix);

        if (client == null) {
            throw new RuntimeException("No AS4Client registered with the given certificate.");
        }

        return client;
    }

    public As4ClientResponseDto submitDeclaration(
            String filePath,
            ProcedureType procedureType,
            DmsService dmsService,
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
                "Declaration.Submit",
                declarationBytes,
                Map.of("procedureType", procedureType.value)
        );
    }

    public As4ClientResponseDto pushNotificationRequest(
            LocalDateTime then,
            LocalDateTime now,
            String certificatePrefix
    ) throws AS4Exception {
        As4Client client = getClientFromCertificatePrefix(certificatePrefix);

        return client.executePush(
                "DMS.Export2",
                "Notification",
                Map.of(
                        "lang", "EN",
                        "submitterId", "24431118",
                        "dateFrom", then.toString(),
                        "dateTo", now.toString()
                )
        );
    }

    public As4ClientResponseDto pullNotifications(String certificatePrefix) throws AS4Exception {
        As4Client client = getClientFromCertificatePrefix(certificatePrefix);

        return client.executePull();
    }

    private As4Client createAs4Client(String certificatePrefix) {
        if (certificatePrefix == null) {
            certificatePrefix = "oces3";
        }

        var cryptoProperties = new CryptoProperties(
                System.getenv(certificatePrefix + ".file"),
                System.getenv(certificatePrefix + ".password"),
                System.getenv(certificatePrefix + ".type"),
                System.getenv(certificatePrefix + ".alias"),
                System.getenv(certificatePrefix + ".privatePassword")
        );

        // assert that path to certificate exists - ensures more readable exception if wrong path
        String certificatePath = System.getenv(certificatePrefix + ".file");
        if (!Files.exists(Paths.get(certificatePath))) {
            throw new RuntimeException("No certificate found at: " + certificatePath);
        }

        File cryptoPropertiesFile = CryptoPropertiesFile.generate(cryptoProperties);

        String cryptoPath = cryptoPropertiesFile.getAbsolutePath();
        String gatewayPassword = System.getenv(certificatePrefix + ".gatewayPassword");

        try {
            return new As4ClientBuilderInstance()
                    .builder()
                    .setEndpoint("https://secureftpgatewaytest.skat.dk:6384")
                    .setCrypto(cryptoPath)
                    .setPassword(gatewayPassword)
                    .build();
        } catch (AS4Exception e) {
            throw new RuntimeException(e);
        } finally {
            cryptoPropertiesFile.delete();
        }
    }
}
