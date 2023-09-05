package com.dkcompany.dmsintegration;

import dk.skat.mft.dms_declaration_status._1.StatusResponseType;
import dk.toldst.eutk.as4client.As4Client;
import dk.toldst.eutk.as4client.builder.support.As4ClientBuilderInstance;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Properties;

@Component
public class As4Test {
    private final As4Client as4Client;

    public As4Test() throws AS4Exception {
        this.as4Client = SimpleAs4Client();
        StatusResponseType submitDeclarationResult = SubmitDeclarationExample();

        var submitDeclarationMessage = submitDeclarationResult.getMessage();
        var submitDeclarationCode = submitDeclarationResult.getCode();
        System.out.println("Submit declaration result (" + submitDeclarationCode + "): " + submitDeclarationMessage);

        StatusResponseType notificationResult = retrieveNotificationExample(this.as4Client);
        var notificationMessage = notificationResult.getMessage();
        var notificationCode = notificationResult.getCode();
        System.out.println("Notification result (" + notificationCode + "): " + notificationMessage);
    }

    public StatusResponseType SubmitDeclarationExample() throws AS4Exception {
        String declaration = "";
        try {
            declaration = new String(
                As4Test.class
                        .getResourceAsStream("/examples/import_h7.xml")
                        .readAllBytes()
            );
        } catch (IOException e) {
            e.printStackTrace();
        }

        var declarationBytes = declaration.getBytes(StandardCharsets.UTF_8);

        var declarationResult = as4Client.executePush(
            "DMS.Import",
            "Declaration.Submit",
            declarationBytes,
            Map.of("procedureType", "H7")
        );

        var declarationStatus = Tools.getStatus(declarationResult);

        return declarationStatus;
    }

    private StatusResponseType retrieveNotificationExample(As4Client client) throws AS4Exception
    {
        var pushResult = client.executePush(
                "DMS.Import",
                "Notification",
                Map.of("lang", "EN",
                        "submitterId", "45549113",
                        "dateFrom", "2023-09-05T12:30:00.000",
                        "dateTo", "2023-09-05T12:35:00.000")
        );

        StatusResponseType pushStatus = Tools.getStatus(pushResult);

        return pushStatus;
    }

    private As4Client SimpleAs4Client() throws AS4Exception {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            InputStream stream = loader.getResourceAsStream("security/oces2Gateway.properties");
            prop.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new As4ClientBuilderInstance()
                .builder()
                .setEndpoint("https://secureftpgatewaytest.skat.dk:6384")
                .setCrypto("security/as4-crypto.properties")
                .setPassword(prop.getProperty("oces2GatewayPassword"))
                .build();
    }
}