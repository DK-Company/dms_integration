package com.dkcompany.dmsintegration.as4client;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Setter
@Getter
public class As4Message {
    private List<As4Message.As4Part> attachments = new ArrayList<>();
    private As4Message.As4Part body;
    private Map<String, String> messageProperties;

    @Setter
    @Getter
    public static class As4Part {
        private String content;
        private Map<String, String> properties;

        private String id;

    }
}

