package com.example.chatfirebaseapplication.listeners;

import com.example.chatfirebaseapplication.models.UserModel;
import com.google.firebase.firestore.auth.User;

public interface ConversionListener {
    void onClickItemConversion(UserModel user);
}
