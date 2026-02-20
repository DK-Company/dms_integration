package com.dkcompany.dmsintegration.as4client;

import lombok.Getter;
import lombok.Setter;
import org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

import javax.net.ssl.*;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.soap.*;
import javax.xml.transform.dom.DOMResult;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.cert.X509Certificate;
import java.util.Base64;
import java.util.List;

public class As4HttpClient {

    public static final String EBMS_3_0_NAMESPACE_URI =
            "http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/";

    private final JaxbThreadSafe marshaller;

    @Getter
    private final SecurityService securityService;

    @Getter
    @Setter
    private URI endpointURI;

    @Setter
    @Getter
    private Boolean disableSSL = false;

    public As4HttpClient(JaxbThreadSafe marshaller, SecurityService securityService, URI endpointURI) {
        this.marshaller = marshaller;
        this.securityService = securityService;
        this.endpointURI = endpointURI;
    }

    private static class TrustAllHosts implements HostnameVerifier {
        public boolean verify(String hostname, SSLSession session) {
            return true;
        }
    }

    public SOAPMessage sendRequest(Messaging messaging, As4Message as4Message) throws Exception {

        // The code below is to avoid the certificate check on SIT01
        if(!disableSSL) {
            TrustManager[] trustAllCerts = new TrustManager[]{new X509TrustManager() {
                public java.security.cert.X509Certificate[] getAcceptedIssuers() {
                    return null;
                }

                public void checkClientTrusted(X509Certificate[] certs, String authType) {
                }

                public void checkServerTrusted(X509Certificate[] certs, String authType) {
                }
            }
            };
            SSLContext sc = SSLContext.getInstance("SSL");
            sc.init(null, trustAllCerts, new java.security.SecureRandom());
            HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
            HttpsURLConnection.setDefaultHostnameVerifier(new As4HttpClient.TrustAllHosts());
            HttpsURLConnection httpsConnection;
            java.net.URL url = endpointURI.toURL();
            httpsConnection = (HttpsURLConnection) url.openConnection();
            httpsConnection.setHostnameVerifier(new As4HttpClient.TrustAllHosts());
            httpsConnection.connect();
        }

        SOAPConnectionFactory soapConnectionFactory = SOAPConnectionFactory.newInstance();
        SOAPConnection soapConnection = new As4SecurityDecorator(
                soapConnectionFactory.createConnection(), securityService);

        SOAPMessage soapMessage = createSOAPMessage(messaging, as4Message);

        return soapConnection.call(soapMessage, endpointURI.toURL());
    }

    private SOAPMessage createSOAPMessage(Messaging messaging, As4Message as4Message) throws Exception {
        MessageFactory messageFactory = MessageFactory.newInstance(SOAPConstants.SOAP_1_2_PROTOCOL);
        SOAPMessage soapMessage = messageFactory.createMessage();

        SOAPEnvelope envelope = soapMessage.getSOAPPart().getEnvelope();
        envelope.addNamespaceDeclaration("eb3", EBMS_3_0_NAMESPACE_URI);

        insertMessageToSoapHeader(messaging, soapMessage.getSOAPHeader());
        if (!as4Message.getAttachments().isEmpty()) {
            insertAttachments(soapMessage, as4Message.getAttachments());
        }

        if (as4Message.getBody() != null) {
            String body = new String(Base64.getDecoder().decode(as4Message.getBody().getContent()));
            org.w3c.dom.Node bodyNode = createBodyNode(body);
            org.w3c.dom.Node bodyNodeClone = soapMessage.getSOAPBody().getOwnerDocument().importNode(bodyNode, true);
            soapMessage.getSOAPBody().appendChild(bodyNodeClone);
        }

        return soapMessage;
    }

    private void insertAttachments(SOAPMessage soapMessage, List<As4Message.As4Part> attachments) throws SOAPException {
        for (As4Message.As4Part attachment : attachments) {
            AttachmentPart attachmentPart = soapMessage.createAttachmentPart();
            attachmentPart.setContentId(attachment.getId());

            // TODO:
            // insert functionality that checks for the "CompressionType=application/gzip" property and
            // gzip the contents before adding it to the attachement (spec: GZIP [RFC1952])

            //byte[] data = Base64.getDecoder().decode(attachment.getContent());
            byte[] data = attachment.getContent().getBytes(StandardCharsets.UTF_8);
            attachmentPart.setRawContent(new ByteArrayInputStream(data), "application/octet-stream");
            attachmentPart.addMimeHeader("Content-Transfer-Encoding", "binary");
            soapMessage.addAttachmentPart(attachmentPart);
        }
    }

    private Node createBodyNode(String body) throws Exception {
        DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
        dbf.setNamespaceAware(true);
        DocumentBuilder db = dbf.newDocumentBuilder();

        StringReader stringReader = new StringReader(body);
        return db.parse(new InputSource(stringReader)).getDocumentElement();
    }

    private void insertMessageToSoapHeader(Messaging messaging, SOAPHeader soapHeader) throws JAXBException {
        DOMResult domResult = new DOMResult(soapHeader);
        QName qName = new QName(EBMS_3_0_NAMESPACE_URI, "Messaging", "eb3");
        JAXBElement<Messaging> m = new JAXBElement<>(qName, Messaging.class, messaging);
        marshaller.marshal(m, domResult);
    }
}
