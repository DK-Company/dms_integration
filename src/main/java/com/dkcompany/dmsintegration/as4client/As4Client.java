package com.dkcompany.dmsintegration.as4client;

import java.io.File;
import java.util.List;
import java.util.Map;


// DKC/001/100226/TOP Added new executePush with support for list of files (Batch push)
//                    General cleanup and removal of unused functions

public interface As4Client {
    As4ClientResponseDto executePull(String mpc) throws AS4Exception; // Send pull notifications

    As4ClientResponseDto executePush(String service, String action, Map<String, String> messageProperties, String messageId) throws AS4Exception; // pushNotificationRequest

    As4ClientResponseDto executePush(String service, String action, byte[] message, Map<String, String> messageProperties, String messageId) throws AS4Exception; // Push one declaration
    As4ClientResponseDto executePush(String service, String action, List<File> files, Map<String, String> messageProperties, String messageId) throws AS4Exception; //DKC/001 Push batch of declarations
}
