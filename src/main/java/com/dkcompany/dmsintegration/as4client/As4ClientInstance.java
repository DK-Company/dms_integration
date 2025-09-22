package com.dkcompany.dmsintegration.as4client;

//DKC/001/220925/TOP  Added comments + logging for debugging

import lombok.Getter;
import org.apache.commons.io.FileUtils;
import org.apache.wss4j.common.util.XMLUtils;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;

import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;
import javax.xml.transform.TransformerException;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Map;
import java.util.UUID;

public class As4ClientInstance implements As4Client {

    @Getter
    private As4DtoCreator as4DtoCreator;
    @Getter
    private As4HttpClient as4HttpClient;
    private final String defaultMPC = "urn:fdc:dk.skat.mft.DMS/import2/response"; // Bliver ikke brugt da hver CRV har en notifikations-url opsætning

    public As4ClientInstance(As4DtoCreator as4DtoCreator, As4HttpClient as4HttpClient) {
        this.as4DtoCreator = as4DtoCreator;
        this.as4HttpClient = as4HttpClient;
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, byte[] message, Map<String, String> messageProperties) throws AS4Exception {
        return executePush(service, action, new String(message, StandardCharsets.UTF_8), "declaration.xml", messageProperties);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, Map<String, String> messageProperties) throws AS4Exception {
        String messageId = UUID.randomUUID().toString();
        return internalPush(service, action, "", "Declaration.xml", messageProperties, false, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, byte[] message, String file, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalPush(service, action, new String(message, StandardCharsets.UTF_8), file, messageProperties, false, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, byte[] message, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return executePush(service, action, new String(message, StandardCharsets.UTF_8), "document.pdf", messageProperties, messageId);
    }

    @Override
    public As4ClientResponseDto executeDocumentPush(String service, String action, byte[] message, String file, Map<String, String> messageProperties) throws AS4Exception {
        String messageId = UUID.randomUUID().toString();
        return internalDocumentPush(service, action, new String(message, StandardCharsets.UTF_8), file, messageProperties, true, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, byte[] message, String file, Map<String, String> messageProperties) throws AS4Exception {
        String messageId = UUID.randomUUID().toString();
        return internalPush(service, action, new String(message, StandardCharsets.UTF_8), file, messageProperties, true, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, String message, String file, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalPush(service, action, message, file, messageProperties, true,messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, String message, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalPush(service, action, message, "declaration.xml", messageProperties, true, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, String message, String file, Map<String, String> messageProperties) throws AS4Exception {
        String messageId = UUID.randomUUID().toString();
        return internalPush(service, action, message, file, messageProperties, true, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, String message, Map<String, String> messageProperties) throws AS4Exception {
        String messageId = UUID.randomUUID().toString();
        return internalPush(service, action, message, "declaration.xml", messageProperties, false, messageId);
    }

    @Override
    public As4ClientResponseDto executePush(String service, String action, Map<String, String> messageProperties, String messageId) throws AS4Exception {
        return internalPush(service, action, "", "Declaration.xml", messageProperties, false, messageId);
    }

    private As4ClientResponseDto internalPush(String service, String action, String message, String file, Map<String, String> messageProperties, Boolean includeAttachment, String messageId ) throws AS4Exception {

        //DKC/001
        // Test DMS.Import2
        //if(service.equals("DMS.Import")){service="DMS.Import2";}
        // Vis service kald variable til debug
        System.out.println("internalPush: ");
        System.out.println("service: " + service);
        System.out.println("action: " + action);
        //System.out.println("message: " + message);
        //System.out.println("file: " + file);


        As4Message as4Message = new As4Message();

        if(includeAttachment)
        {
            // messageProperties;
            As4Message.As4Part part = CreatePart(message, file);
            as4Message.getAttachments().add(part);
        }

        //messageProperties = null;
        as4Message.setMessageProperties(messageProperties);
        Messaging messaging = as4DtoCreator.createMessaging(service, action, "placeholder", as4Message, messageId);
        As4ClientResponseDto as4ClientResponseDto = new As4ClientResponseDto();

        SOAPMessage soapMessage;
        try {
            soapMessage = as4HttpClient.sendRequest(messaging, as4Message);
            as4ClientResponseDto.setFirstAttachment(tryGetFirstAttachment(soapMessage)); // Hent svar string
            return as4ClientResponseDto;
        } catch (Exception e) {
            //DKC/xxx
            System.out.println("messagefailed: " + e);
            throw new AS4Exception("Failed to send (or receive) message" , e);
        }
    }

    private As4ClientResponseDto internalDocumentPush(String service, String action, String message, String file, Map<String, String> messageProperties, Boolean includeAttachment, String messageId ) throws AS4Exception {
        As4Message as4Message = new As4Message();

        if(includeAttachment)
        {
            As4Message.As4Part part = CreatePart(message, file);
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
            throw new AS4Exception("Failed to send (or receive) message" , e);
        }

    }

    private As4Message.As4Part CreatePart(String message, String file) {
        As4Message.As4Part part = new As4Message.As4Part();
        part.setContent(message);
        part.setProperties(Collections.singletonMap("original-file-name", file));
        return part;
    }

    // Default executePull uden url. Vi bruger ikke denne pull da vi altid har en notification url fra configurationen
    // men vi skal have definitionen med, da den er en del af "as4Client" klassen.
    //@Override
    public As4ClientResponseDto executePull() throws AS4Exception {
        return executePull(defaultMPC);
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
                throw new AS4Exception("Failed to send (or receive) message" , e);
            }
            throw new AS4Exception("Failed to send (or receive) message, recieved from server: " + debugMessage , e);
        }
    }

    // Hent første attachment som string (xml filer)
    private String tryGetFirstAttachment(SOAPMessage soapMessage) {
        try {
            return new String(soapMessage.getAttachments().next().getDataHandler().getInputStream().readAllBytes());
        }
        catch (Exception e){
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

            // Read tempFileName to byte-data
            byte[] allBytes = Files.readAllBytes(Paths.get(tempFileName));

            // Delete tempfile
            File file = new File(tempFileName);
            if(file.exists()) file.delete();

            return allBytes;
        }
        catch (Exception e){
            return null;
        }
    }

    // Forsøg at hente det original ID fra soapMessage
    private String tryGetRefToOriginalID(SOAPMessage soapMessage) {
        try {
            SOAPHeader header = soapMessage.getSOAPHeader();
            return header.getElementsByTagNameNS("*","Property").item(0).getChildNodes().item(0).getNodeValue();
        }catch (Exception e){
            return null;
        }

    }
}

