package com.dkcompany.dmsintegration.as4client;

import java.util.Map;

public interface As4Client {
    As4ClientResponseDto executePush(String service, String action, String message, String file, Map<String, String> messageProperties, String messageId ) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, String message, Map<String, String> messageProperties, String messageId ) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, String message, String file, Map<String, String> messageProperties) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, String message, Map<String, String> messageProperties) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, Map<String, String> messageProperties, String messageId ) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, Map<String, String> messageProperties) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, byte[] message, String file, Map<String, String> messageProperties, String messageId) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, byte[] message, Map<String, String> messageProperties, String messageId) throws AS4Exception;
    As4ClientResponseDto executeDocumentPush(String service, String action, byte[] message, String file, Map<String, String> messageProperties) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, byte[] message, String file, Map<String, String> messageProperties) throws AS4Exception;
    As4ClientResponseDto executePush(String service, String action, byte[] message, Map<String, String> messageProperties) throws AS4Exception;
    As4ClientResponseDto executePull() throws AS4Exception;
    As4ClientResponseDto executePull(String mpc) throws AS4Exception;
}

