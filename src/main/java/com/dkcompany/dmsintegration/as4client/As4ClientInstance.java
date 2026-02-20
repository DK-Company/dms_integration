package com.dkcompany.dmsintegration.as4client;

import org.apache.wss4j.common.util.XMLUtils;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;

import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;

//DKC/001/220925/TOP  Added comments + logging for debugging
//DKC/002/100226/TOP  Added function internalDocumentPush with support for a file-list to post a batch of files
//                    General cleanup and removal of unused functionality


public record As4ClientInstance(As4DtoCreator as4DtoCreator, As4HttpClient as4HttpClient) implements As4Client {

    @Override
    public As4ClientResponseDto executePush(String service, String action, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalPush(service, action, "", messageProperties, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, byte[] message, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalPush(service, action, new String(message, StandardCharsets.UTF_8), messageProperties, messageId);
    }

    //DKC/002/START
    @Override
    public As4ClientResponseDto executePush(String service, String action, List<File> files, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalBatchPush(service, action, files, messageProperties, messageId);
    }
    //DKC/002/STOP

    // Push message
    private As4ClientResponseDto internalPush(String service, String action, String message, Map<String, String> messageProperties, String messageId) throws AS4Exception {

        System.out.println("internalPush " + service + " " + action);

        if (messageId.isEmpty()) messageId = UUID.randomUUID().toString();
        As4Message as4Message = new As4Message();
        if (!message.isEmpty()) {
            // messageProperties;
            As4Message.As4Part part = CreatePart(message, "Declaration.xml");
            as4Message.getAttachments().add(part);
        }

        as4Message.setMessageProperties(messageProperties);
        Messaging messaging = as4DtoCreator.createMessaging(service, action, "placeholder", as4Message, messageId);
        As4ClientResponseDto as4ClientResponseDto = new As4ClientResponseDto();

        SOAPMessage soapMessage;
        try {
            soapMessage = as4HttpClient.sendRequest(messaging, as4Message);
            as4ClientResponseDto.setFirstAttachment(tryGetFirstAttachment(soapMessage)); // Hent svar string
            return as4ClientResponseDto;
        } catch (Exception e) {
            System.out.println("messagefailed: " + e);
            throw new AS4Exception("Failed to send (or receive) message", e);
        }
    }

    //DKC/002/START
    // Push Files
    private As4ClientResponseDto internalBatchPush(String service, String action, List<File> files, Map<String, String> messageProperties, String messageId) throws AS4Exception {

        System.out.println("internalBatchPush " + service + " " + action);
        System.out.println("number of files: " + files.size());

        if (messageId.isEmpty()) messageId = UUID.randomUUID().toString();
        As4Message as4Message = new As4Message();

        // Loop and add files to message
        for (int i = 0; i < files.size(); i++) {
            // Read the file to fileByttes
            File filename = files.get(i);
            Path path = Paths.get(filename.getAbsolutePath());
            byte[] fileBytes;
            try {
                fileBytes = Files.readAllBytes(path);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            // Convert bytes to uft8 string
            String message = new String(fileBytes, StandardCharsets.UTF_8);
            As4Message.As4Part part = CreatePart(message, "Declaration" + i + ".xml");
            as4Message.getAttachments().add(part);
        }

        //messageProperties = null;
        as4Message.setMessageProperties(messageProperties);
        Messaging messaging = as4DtoCreator.createMessaging(service, action, "placeholder", as4Message, messageId);
        As4ClientResponseDto as4ClientResponseDto = new As4ClientResponseDto();

        SOAPMessage soapMessage;
        try {
            soapMessage = as4HttpClient.sendRequest(messaging, as4Message);
            as4ClientResponseDto.setFirstAttachment(tryGetFirstAttachment(soapMessage));
            return as4ClientResponseDto;
        } catch (Exception e) {
            System.out.println("messagefailed: " + e);
            throw new AS4Exception("Failed to send (or receive) message", e);
        }
    }
    //DKC/002/STOP

    private As4Message.As4Part CreatePart(String message, String file) {
        As4Message.As4Part part = new As4Message.As4Part();
        part.setContent(message);
        part.setProperties(Collections.singletonMap("original-file-name", file));
        return part;
    }

    // executePull til notifications køen (mpc)
    // Sender Pull-request
    @Override
    public As4ClientResponseDto executePull(String mpc) throws AS4Exception {
        String messageId = UUID.randomUUID().toString();
        Messaging messaging = as4DtoCreator.createPullMessaging(mpc, messageId);
        SOAPMessage soapMessage = null;
        As4ClientResponseDto as4ClientResponseDto = new As4ClientResponseDto();
        try {
            // Send request
            soapMessage = as4HttpClient.sendRequest(messaging, new As4Message());
            // Returner data fra svaret på afsendelse af requesten
            as4ClientResponseDto.setRefToOriginalID(tryGetRefToOriginalID(soapMessage)); // Ev.t Original ID
            as4ClientResponseDto.setFirstAttachment(tryGetFirstAttachment(soapMessage)); // String attachment
            as4ClientResponseDto.setFirstAttachmentBytes(tryGetFirstAttachmentBytes(soapMessage)); // Byte attachment
            return as4ClientResponseDto;
        } catch (Exception e) {
            String debugMessage = null;
            try {
                if (soapMessage != null && soapMessage.getSOAPPart() != null && soapMessage.getSOAPPart().getOwnerDocument() != null) {
                    debugMessage = XMLUtils.prettyDocumentToString(soapMessage.getSOAPPart().getOwnerDocument());
                }
            } catch (IOException | TransformerException ex) {
                throw new AS4Exception("Failed to send (or receive) message", e);
            }
            throw new AS4Exception("Failed to send (or receive) message, recieved from server: " + debugMessage, e);
        }
    }

    // Hent første attachment som string (xml filer)
    private String tryGetFirstAttachment(SOAPMessage soapMessage) {
        try {
            return new String(soapMessage.getAttachments().next().getDataHandler().getInputStream().readAllBytes());
        } catch (Exception e) {
            return null;
        }
    }

    // Hent første attachment som bytes (bl.a. PDF filer)
    private byte[] tryGetFirstAttachmentBytes(SOAPMessage soapMessage) {
        try {
            // Get inputstream from soap message
            InputStream stream = soapMessage.getAttachments().next().getDataHandler().getInputStream();

            // get a tempFileName
            File tempFile = File.createTempFile("dmsintegration-", ".tmp");
            String tempFileName = tempFile.getName();

            // Save stream to tempFileName
            try (OutputStream output = new FileOutputStream(tempFileName)) {
                stream.transferTo(output);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }

            Path tempPath = Paths.get(tempFileName);

            // Read temp file
            byte[] allBytes = Files.readAllBytes(tempPath);

            // Delete temp file
            Files.deleteIfExists(tempPath);

            return allBytes;
        } catch (Exception e) {
            return null;
        }
    }

    // Forsøg at hente det original ID fra soapMessage
    private String tryGetRefToOriginalID(SOAPMessage soapMessage) {
        try {
            SOAPHeader header = soapMessage.getSOAPHeader();
            return header.getElementsByTagNameNS("*", "Property").item(0).getChildNodes().item(0).getNodeValue();
        } catch (Exception e) {
            return null;
        }

    }
}

