package com.dkcompany.dmsintegration.as4client;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class As4ClientResponseDto {
    private String FirstAttachment;
    private String RefToOriginalID;
    private byte[] FirstAttachmentBytes;
}
