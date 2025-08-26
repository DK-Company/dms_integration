package com.dkcompany.dmsintegration.as4client;

import java.net.URI;

public interface As4SetEndpoint {
    /**
     *
     * @param url the url of the Axway portal
     * @return an As4SetCrypto instance, on which the crypto file path can be set.
     */
    As4SetCrypto setEndpoint(URI url);

    /**
     *
     * @param url the url of the Axway portal
     * @return an As4SetCrypto instance, on which the crypto file path can be set.
     * @throws AS4Exception
     */
    As4SetCrypto setEndpoint(String url) throws AS4Exception;
}

