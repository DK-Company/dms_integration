package com.dkcompany.dmsintegration.as4client;

public interface As4SetPasswordTokenDetails
{
    /**
     * Sets the password and returns the final AS4ClientBuilder which can then be built.
     * @param password for the Crypto.
     */
    As4ClientBuilder setPassword(String password);
}

