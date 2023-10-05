package com.dkcompany.dmsintegration.util;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class CryptoPropertiesFile {
    public static File generate(CryptoProperties cryptoProperties) {
        Map<String, String> lines = new HashMap<>();

        // Company Keystore
        lines.put("org.apache.wss4j.crypto.merlin.keystore.file", cryptoProperties.file());
        lines.put("org.apache.wss4j.crypto.merlin.keystore.password", cryptoProperties.password());
        lines.put("org.apache.wss4j.crypto.merlin.keystore.type", cryptoProperties.type());
        lines.put("org.apache.wss4j.crypto.merlin.keystore.alias", cryptoProperties.alias());
        lines.put("org.apache.wss4j.crypto.merlin.keystore.private.password", cryptoProperties.privatePassword());

        // Trust Store
        lines.put("org.apache.wss4j.crypto.merlin.truststore.file", "/security/trust-certificate.p12");
        lines.put("org.apache.wss4j.crypto.merlin.truststore.password", "");
        lines.put("org.apache.wss4j.crypto.merlin.truststore.alias", "secureftpgatewaytest.skat.dk (globalsign rsa ov ssl ca 2018)");
        lines.put("org.apache.wss4j.crypto.merlin.truststore.type", "PKCS12");

        String fileContent = generateStringFromMap(lines);
        return writeToFile(fileContent);
    }

    private static String generateStringFromMap(Map<String, String> lines) {
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> line : lines.entrySet()) {
            sb.append(line.toString());
            sb.append('\n');
        }

        return sb.toString();
    }

    private static File writeToFile(String fileContent) {
        try {
            File tempFile = File.createTempFile("crypto-", ".properties");
            String filePath = tempFile.getAbsolutePath();

            FileWriter writer = new FileWriter(filePath);
            writer.write(fileContent);
            writer.close();

            return tempFile;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
