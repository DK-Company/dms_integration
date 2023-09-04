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
        var result = SubmitDeclarationExample();
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

    private As4Client SimpleAs4Client() throws AS4Exception {
        Properties prop = new Properties();
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        try {
            InputStream stream = loader.getResourceAsStream("security/certificate.properties");
            prop.load(stream);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        return new As4ClientBuilderInstance()
                .builder()
                .setEndpoint("https://www.testsys.dk/console/")
                .setCrypto("security/as4-crypto.properties")
                .setPassword(prop.getProperty("simpleClientPassword"))
                .build();
    }
}