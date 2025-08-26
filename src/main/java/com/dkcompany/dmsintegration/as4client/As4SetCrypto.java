package com.dkcompany.dmsintegration.as4client;

public interface As4SetCrypto {
    /**
     * Sets the Crypto property.
     * @param filepath the path to the crypto.properties file.
     * @return a As4SetPasswordTokenDetails object, which is used to continue the builder pattern and set the password for the crypto.
     * @throws AS4Exception if the file is not currectly read, or a Crypto object cannot be built.
     */
    As4SetPasswordTokenDetails setCrypto(String filepath) throws AS4Exception;
}