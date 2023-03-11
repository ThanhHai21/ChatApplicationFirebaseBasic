package com.example.chatfirebaseapplication.network;

import com.example.chatfirebaseapplication.utils.Constants;

import java.util.HashMap;

public class PushNotification {
    public static HashMap<String, String> remoteMsgHeaders;

    public static HashMap<String, String> getRemoteMsgHeaders() {
        if (remoteMsgHeaders == null) {
            remoteMsgHeaders = new HashMap<>();
            remoteMsgHeaders.put(
                    Constants.KEY_REMOTE_MESSAGE_AUTH,
                    Constants.KEY_PUSH_NOTIFICATION
            );
            remoteMsgHeaders.put(
                    Constants.KEY_REMOTE_CONTENT_TYPE,
                    "application/json"
            );
        }
        return remoteMsgHeaders;
    }
}
