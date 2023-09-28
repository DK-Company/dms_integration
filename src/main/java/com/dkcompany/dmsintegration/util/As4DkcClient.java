package com.dkcompany.dmsintegration.util;

import dk.toldst.eutk.as4client.As4Client;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.builder.support.As4ClientBuilderInstance;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

@Component
public class As4DkcClient {
    private final As4Client as4Client;

    public As4DkcClient() throws AS4Exception {
        as4Client = SimpleAs4Client();
    }

    public As4ClientResponseDto SubmitDeclarationExample() throws AS4Exception {
        String declaration = "";
        try {
            declaration = new String(
                    As4DkcClient.class
                            .getResourceAsStream("/examples/B1C.xml")
                            .readAllBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        var declarationBytes = declaration.getBytes(StandardCharsets.UTF_8);

        return as4Client.executePush(
                DmsService.Export2.value,
                "Declaration.Submit",
                declarationBytes,
                Map.of("procedureType", ProcedureType.B1.value)
        );
    }

    public As4ClientResponseDto submitDeclaration(String filePath,
                                                  ProcedureType procedureType,
                                                  DmsService dmsService) throws AS4Exception {
        Path path = Paths.get(filePath);
        byte[] declarationBytes;
        try {
            declarationBytes = Files.readAllBytes(path);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return as4Client.executePush(
                dmsService.value,
                "Declaration.Submit",
                declarationBytes,
                Map.of("procedureType", procedureType.value)
        );
    }

    public As4ClientResponseDto pushNotificationRequest(LocalDateTime now) throws AS4Exception
    {
        LocalDateTime then = now.minusMinutes(5);

        return as4Client.executePush(
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

    public As4ClientResponseDto pullNotifications() throws AS4Exception {
         return as4Client.executePull();
    }

    private As4Client SimpleAs4Client() throws AS4Exception {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            InputStream stream = loader.getResourceAsStream("security/oces3-gateway.properties");
            prop.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new As4ClientBuilderInstance()
                .builder()
                .setEndpoint("https://secureftpgatewaytest.skat.dk:6384")
                .setCrypto("security/oces3-test-crypto.properties")
                .setPassword(prop.getProperty("oces3.gateway.password"))
                .build();
    }
}
