package com.dkcompany.dmsintegration.as4client;

/**
 * builds a client using the Builder pattern.
 */
public interface As4ClientBuilder {
    /**
     * Builds an AS4 client with the supplied information
     * @return the client
     */
    As4Client build() throws AS4Exception;

    //As4Optionals optionals();
}
