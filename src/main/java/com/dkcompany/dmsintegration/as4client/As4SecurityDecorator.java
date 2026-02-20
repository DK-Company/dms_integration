package com.dkcompany.dmsintegration.as4client;

import javax.xml.soap.SOAPConnection;
import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPMessage;

public class As4SecurityDecorator extends SOAPConnection {

    private final SOAPConnection soapConnection;
    private final SecurityService securityService;

    public As4SecurityDecorator(SOAPConnection soapConnection, SecurityService securityService) {
        this.soapConnection = soapConnection;
        this.securityService = securityService;
    }

    @Override
    public SOAPMessage call(SOAPMessage soapMessage, Object o) throws SOAPException {
        String userTokenId = securityService.usernameToken(soapMessage);
        securityService.signAndEncryptAs4(soapMessage, userTokenId);
        return soapConnection.call(soapMessage, o);
    }

    @Override
    public void close() throws SOAPException {
        soapConnection.close();
    }
}

