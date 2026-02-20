package com.dkcompany.dmsintegration.as4client;

import org.apache.wss4j.common.ext.Attachment;
import org.apache.wss4j.common.ext.AttachmentRequestCallback;
import org.apache.wss4j.common.ext.AttachmentResultCallback;

import javax.security.auth.callback.Callback;
import javax.security.auth.callback.CallbackHandler;
import javax.security.auth.callback.UnsupportedCallbackException;
import java.util.ArrayList;
import java.util.List;

public class AttachmentCallbackHandler implements CallbackHandler {

    private final List<Attachment> attachments;

    public AttachmentCallbackHandler(List<Attachment> attachments) {
        this.attachments = attachments;
    }

    @Override
    public void handle(Callback[] callbacks)
            throws UnsupportedCallbackException {
        for (Callback callback : callbacks) {
            if (callback instanceof AttachmentRequestCallback attachmentRequestCallback) {
                attachmentRequestCallback.setAttachments(new ArrayList<>(attachments));

            } else if (callback instanceof AttachmentResultCallback attachmentResultCallback) {
                // Access to avoid "empty body" warning
                var ignore = attachmentResultCallback.getAttachment();
            } else {
                throw new UnsupportedCallbackException(callback, "Unrecognized Callback");
            }
        }
    }
}
