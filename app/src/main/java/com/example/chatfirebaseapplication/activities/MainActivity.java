package com.example.chatfirebaseapplication.activities;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import com.example.chatfirebaseapplication.adapters.RecentConversationAdapter;
import com.example.chatfirebaseapplication.databinding.ActivityMainBinding;
import com.example.chatfirebaseapplication.listeners.ConversionListener;
import com.example.chatfirebaseapplication.models.ChatMessageModel;
import com.example.chatfirebaseapplication.models.UserModel;
import com.example.chatfirebaseapplication.utils.Constants;
import com.example.chatfirebaseapplication.utils.PreferenceManager;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.messaging.FirebaseMessaging;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public class MainActivity extends BaseActivity implements ConversionListener {
    private ActivityMainBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private List<ChatMessageModel> messageList;
    private RecentConversationAdapter conversationAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setUpInitConfig();
        initData();
        setListeners();

//        For Android 13
//        requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1000);
    }

    private void setUpInitConfig() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        messageList = new ArrayList<>();
        conversationAdapter = new RecentConversationAdapter(messageList, this);
        binding.rvConversions.setAdapter(conversationAdapter);
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    private void initData() {
        loadInfoUser();
        getUserToken();
        listenConversions();
    }

    private void setListeners() {
        binding.ivLogout.setOnClickListener(v -> signOut());
        binding.fabNewChat.setOnClickListener(v -> startActivity(new Intent(getApplicationContext(), UserActivity.class)));
    }

    private void loadInfoUser() {
        binding.tvName.setText(preferenceManager.getString(Constants.KEY_NAME));
        byte[] bytes = Base64.decode(preferenceManager.getString(Constants.KEY_USER_IMAGE), Base64.DEFAULT);
        Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        binding.ivProfile.setImageBitmap(bitmap);
    }

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void listenConversions() {
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS)
                .whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID))
                .addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        binding.pbLoading.setVisibility(View.VISIBLE);
        if (value != null) {
            processDataLocal(value);
            Collections.sort(messageList, (obj1, obj2) -> obj2.dateTimeObject.compareTo(obj1.dateTimeObject));
            conversationAdapter.notifyDataSetChanged();
            binding.rvConversions.smoothScrollToPosition(0);
            binding.rvConversions.setVisibility(View.VISIBLE);
            binding.pbLoading.setVisibility(View.GONE);
        }
        if (messageList.isEmpty()) {
            binding.tvErrorMessage.setText(String.format("%s", "No recent conversations"));
            binding.tvErrorMessage.setVisibility(View.VISIBLE);
        } else {
            binding.tvErrorMessage.setVisibility(View.GONE);
        }
    };

    private void processDataLocal(QuerySnapshot value) {
        for (DocumentChange documentChange : value.getDocumentChanges()) {
            if (documentChange.getType() == DocumentChange.Type.ADDED) {
                String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                ChatMessageModel messageModel = new ChatMessageModel();
                messageModel.setSenderId(senderId);
                messageModel.setReceiverId(receiverId);
                if (preferenceManager.getString(Constants.KEY_USER_ID).equals(senderId)) {
                    messageModel.setConversionImage(documentChange.getDocument().getString(Constants.KEY_RECEIVER_IMAGE));
                    messageModel.setConversionName(documentChange.getDocument().getString(Constants.KEY_RECEIVER_NAME));
                    messageModel.setConversionId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                } else {
                    messageModel.setConversionImage(documentChange.getDocument().getString(Constants.KEY_SENDER_IMAGE));
                    messageModel.setConversionName(documentChange.getDocument().getString(Constants.KEY_SENDER_NAME));
                    messageModel.setConversionId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                }
                messageModel.setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                messageModel.setDateTimeObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                messageList.add(messageModel);
            } else if (documentChange.getType() == DocumentChange.Type.MODIFIED) {
                for (int i = 0; i < messageList.size(); i++) {
                    String senderId = documentChange.getDocument().getString(Constants.KEY_SENDER_ID);
                    String receiverId = documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID);
                    if (messageList.get(i).senderId.equals(senderId) && messageList.get(i).receiverId.equals(receiverId)) {
                        messageList.get(i).setMessage(documentChange.getDocument().getString(Constants.KEY_LAST_MESSAGE));
                        messageList.get(i).setDateTimeObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                        break;
                    }
                }
            }
        }
    }

    private void getUserToken() {
        FirebaseMessaging.getInstance().getToken().addOnSuccessListener(this::updateUserToken);
    }

    private void updateUserToken(String userToken) {
        preferenceManager.putString(Constants.KEY_FCM_TOKEN, userToken);
        DocumentReference documentReference = firebaseFirestore
                .collection(Constants.KEY_COLLECTION_USER)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        documentReference.update(Constants.KEY_FCM_TOKEN, userToken)
                .addOnFailureListener(e -> showToast("Unable to update user token"));
    }

    private void signOut() {
        showToast("SignOut...");
        DocumentReference documentReference = firebaseFirestore
                .collection(Constants.KEY_COLLECTION_USER)
                .document(preferenceManager.getString(Constants.KEY_USER_ID));
        HashMap<String, Object> updateToken = new HashMap<>();
        updateToken.put(Constants.KEY_FCM_TOKEN, FieldValue.delete());
        documentReference.update(updateToken)
                .addOnSuccessListener(result -> {
                    preferenceManager.clear();
                    startActivity(new Intent(getApplicationContext(), SignInActivity.class));
                    finish();
                })
                .addOnFailureListener(e -> showToast("Unable to sign out"));
    }

    @Override
    public void onClickItemConversion(UserModel user) {
        Intent intent = new Intent(getApplicationContext(), ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, user);
        startActivity(intent);
    }
}