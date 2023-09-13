package com.dkcompany.dmsintegration.util;

import dk.skat.mft.dms_declaration_status._1.StatusResponseType;
import dk.toldst.eutk.as4client.As4Client;
import dk.toldst.eutk.as4client.As4ClientResponseDto;
import dk.toldst.eutk.as4client.builder.support.As4ClientBuilderInstance;
import dk.toldst.eutk.as4client.exceptions.AS4Exception;
import dk.toldst.eutk.as4client.utilities.Tools;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Properties;

@Component
public class As4DkcClient {
    private final As4Client as4Client;

    public As4DkcClient() throws AS4Exception {
        as4Client = SimpleAs4Client();
    }

    public StatusResponseType pushNotificationRequest(LocalDateTime now) throws AS4Exception
    {
        LocalDateTime then = now.minusMinutes(5);

        As4ClientResponseDto pushResult = as4Client.executePush(
                "DMS.Export",
                "Notification",
                Map.of(
                        "lang", "EN",
                        "submitterId", "24431118",
                        "dateFrom", then.toString(),
                        "dateTo", now.toString()
                )
        );

        return Tools.getStatus(pushResult.getFirstAttachment());
    }

    public StatusResponseType pullNotifications() throws AS4Exception {
        As4ClientResponseDto pullResult = as4Client.executePull();
        return Tools.getStatus(pullResult.getFirstAttachment());
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
