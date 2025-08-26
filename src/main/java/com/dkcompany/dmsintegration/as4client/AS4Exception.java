package com.dkcompany.dmsintegration.as4client;

public class AS4Exception extends Exception {
    public AS4Exception(String s) {
        super(s);
    }

    public AS4Exception(String s, Exception e) {
        super(s,e);
    }
}

