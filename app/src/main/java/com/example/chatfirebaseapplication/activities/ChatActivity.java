package com.example.chatfirebaseapplication.activities;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.example.chatfirebaseapplication.adapters.ChatAdapter;
import com.example.chatfirebaseapplication.databinding.ActivityChatBinding;
import com.example.chatfirebaseapplication.models.ChatMessageModel;
import com.example.chatfirebaseapplication.models.UserModel;
import com.example.chatfirebaseapplication.network.ApiClients;
import com.example.chatfirebaseapplication.network.ApiServices;
import com.example.chatfirebaseapplication.network.PushNotification;
import com.example.chatfirebaseapplication.utils.Constants;
import com.example.chatfirebaseapplication.utils.PreferenceManager;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class ChatActivity extends BaseActivity {
    private ActivityChatBinding binding;
    private UserModel receivedUserModel;
    private List<ChatMessageModel> messageList;
    private ChatAdapter chatAdapter;
    private PreferenceManager preferenceManager;
    private FirebaseFirestore firebaseFirestore;
    private String conversionId;
    private boolean isAvailability = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        loadReceivedDataUser();
        setUpInit();
        listenMessage();
    }

    private void setUpInit() {
        preferenceManager = new PreferenceManager(getApplicationContext());
        messageList = new ArrayList<>();
        chatAdapter = new ChatAdapter(messageList, getBitmapFromEncodeString(receivedUserModel.getImage()), preferenceManager.getString(Constants.KEY_USER_ID));

        binding.rvChats.setAdapter(chatAdapter);
        binding.ivUser.setImageBitmap(getBitmapFromEncodeString(receivedUserModel.getImage()));
        firebaseFirestore = FirebaseFirestore.getInstance();
    }

    private void listenAvailabilityOfReceiver() {
        firebaseFirestore.collection(Constants.KEY_COLLECTION_USER).document(receivedUserModel.getId()).addSnapshotListener(this, ((value, error) -> {
            if (error != null) {
                return;
            }
            if (value != null) {
                if (value.getLong(Constants.KEY_AVAILABILITY) != null) {
                    int availability = Objects.requireNonNull(value.getLong(Constants.KEY_AVAILABILITY)).intValue();
                    isAvailability = (availability == 1);
                }
                receivedUserModel.setToken(value.getString(Constants.KEY_FCM_TOKEN));
                if (receivedUserModel.getImage() == null) {
                    receivedUserModel.setImage(value.getString(Constants.KEY_USER_IMAGE));
                    chatAdapter.setReceiverProfileImage(getBitmapFromEncodeString(receivedUserModel.getImage()));
                    chatAdapter.notifyItemRangeChanged(0, messageList.size());
                }
            }
            if (isAvailability) {
                binding.tvIsAvailability.setVisibility(View.VISIBLE);
            } else {
                binding.tvIsAvailability.setVisibility(View.GONE);
            }
        }));
    }

    private void sendMessage() {
        if (!binding.edInputMessage.getText().toString().trim().equals("")) {
            HashMap<String, Object> message = new HashMap<>();
            message.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
            message.put(Constants.KEY_RECEIVER_ID, receivedUserModel.getId());
            message.put(Constants.KEY_MESSAGE, binding.edInputMessage.getText().toString());
            message.put(Constants.KEY_TIMESTAMP, new Date());
            firebaseFirestore.collection(Constants.KEY_COLLECTION_CHAT).add(message);
            if (conversionId != null) {
                updateConversion(binding.edInputMessage.getText().toString());
            } else {
                addConversionHandler();
            }
            if (!isAvailability) {
                try {
                    JSONArray tokens = new JSONArray();
                    tokens.put(receivedUserModel.getToken());

                    JSONObject data = new JSONObject();
                    data.put(Constants.KEY_USER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
                    data.put(Constants.KEY_NAME, preferenceManager.getString(Constants.KEY_NAME));
                    data.put(Constants.KEY_FCM_TOKEN, preferenceManager.getString(Constants.KEY_FCM_TOKEN));
                    data.put(Constants.KEY_MESSAGE, binding.edInputMessage.getText().toString());

                    JSONObject body = new JSONObject();
                    body.put(Constants.KEY_REMOTE_MESSAGE_DATA, data);
                    body.put(Constants.KEY_REMOTE_REGISTRATION_IDS, tokens);

                    sendNotification(body.toString());
                } catch (Exception e) {
                    showToast(e.getMessage());
                }
            }
            binding.edInputMessage.setText("");
        }
    }

    private void addConversionHandler() {
        HashMap<String, Object> conversion = new HashMap<>();
        conversion.put(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID));
        conversion.put(Constants.KEY_SENDER_NAME, preferenceManager.getString(Constants.KEY_NAME));
        conversion.put(Constants.KEY_SENDER_IMAGE, preferenceManager.getString(Constants.KEY_USER_IMAGE));
        conversion.put(Constants.KEY_RECEIVER_ID, receivedUserModel.getId());
        conversion.put(Constants.KEY_RECEIVER_NAME, receivedUserModel.getName());
        conversion.put(Constants.KEY_RECEIVER_IMAGE, receivedUserModel.getImage());
        conversion.put(Constants.KEY_LAST_MESSAGE, binding.edInputMessage.getText().toString());
        conversion.put(Constants.KEY_TIMESTAMP, new Date());
        addConversion(conversion);
    }

    private void listenMessage() {
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CHAT).whereEqualTo(Constants.KEY_SENDER_ID, preferenceManager.getString(Constants.KEY_USER_ID)).whereEqualTo(Constants.KEY_RECEIVER_ID, receivedUserModel.getId()).addSnapshotListener(eventListener);
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CHAT).whereEqualTo(Constants.KEY_SENDER_ID, receivedUserModel.getId()).whereEqualTo(Constants.KEY_RECEIVER_ID, preferenceManager.getString(Constants.KEY_USER_ID)).addSnapshotListener(eventListener);
    }

    @SuppressLint("NotifyDataSetChanged")
    private final EventListener<QuerySnapshot> eventListener = (value, error) -> {
        if (error != null) {
            return;
        }
        if (value != null) {
            int count = messageList.size();
            for (DocumentChange documentChange : value.getDocumentChanges()) {
                if (documentChange.getType() == DocumentChange.Type.ADDED) {
                    ChatMessageModel messageModel = new ChatMessageModel();
                    messageModel.setSenderId(documentChange.getDocument().getString(Constants.KEY_SENDER_ID));
                    messageModel.setReceiverId(documentChange.getDocument().getString(Constants.KEY_RECEIVER_ID));
                    messageModel.setMessage(documentChange.getDocument().getString(Constants.KEY_MESSAGE));
                    messageModel.setDateTime(getReadableDateTime(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP)));
                    messageModel.setDateTimeObject(documentChange.getDocument().getDate(Constants.KEY_TIMESTAMP));
                    messageList.add(messageModel);
                }
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Collections.sort(messageList, Comparator.comparing(obj -> obj.dateTimeObject));
            }
            if (count == 0) {
                chatAdapter.notifyDataSetChanged();
            } else {
                chatAdapter.notifyItemRangeInserted(messageList.size(), messageList.size());
                binding.rvChats.smoothScrollToPosition(messageList.size() - 1);
            }
            binding.rvChats.setVisibility(View.VISIBLE);
        }
        binding.pbLoading.setVisibility(View.GONE);
        if (conversionId == null) {
            checkForConversion();
        }
    };

    private Bitmap getBitmapFromEncodeString(String encodeImage) {
        if (encodeImage != null) {
            byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
        }
        return null;
    }

    private void loadReceivedDataUser() {
        receivedUserModel = (UserModel) getIntent().getSerializableExtra(Constants.KEY_USER);
        binding.tvName.setText(receivedUserModel.getName());
    }

    private void setListeners() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
        binding.flLayoutSend.setOnClickListener(v -> sendMessage());
    }

    private String getReadableDateTime(Date date) {
        return new SimpleDateFormat("MMMM dd, yyyy - hh:mm a", Locale.getDefault()).format(date);
    }

    private void addConversion(HashMap<String, Object> conversions) {
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS).add(conversions).addOnSuccessListener(documentReference -> conversionId = documentReference.getId());
    }

    private void updateConversion(String message) {
        DocumentReference reference = firebaseFirestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS).document(conversionId);
        reference.update(Constants.KEY_LAST_MESSAGE, message, Constants.KEY_TIMESTAMP, new Date());
    }

    private void checkForConversion() {
        if (messageList.size() != 0) {
            checkForConversionRemotely(preferenceManager.getString(Constants.KEY_USER_ID), receivedUserModel.getId());
            checkForConversionRemotely(receivedUserModel.getId(), preferenceManager.getString(Constants.KEY_USER_ID));
        }
    }

    private void checkForConversionRemotely(String senderId, String receiverId) {
        firebaseFirestore.collection(Constants.KEY_COLLECTION_CONVERSATIONS).whereEqualTo(Constants.KEY_SENDER_ID, senderId).whereEqualTo(Constants.KEY_RECEIVER_ID, receiverId).get().addOnCompleteListener(conversionOnCompleteListener);
    }

    private final OnCompleteListener<QuerySnapshot> conversionOnCompleteListener = task -> {
        if (task.isSuccessful() && task.getResult() != null && task.getResult().getDocuments().size() > 0) {
            DocumentSnapshot snapshot = task.getResult().getDocuments().get(0);
            conversionId = snapshot.getId();
        }
    };

    private void showToast(String message) {
        Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
    }

    private void sendNotification(String messageBody) {
        ApiClients.getInstance().create(ApiServices.class).sendMessage(PushNotification.getRemoteMsgHeaders(), messageBody).enqueue(new Callback<String>() {
            @Override
            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                if (response.isSuccessful()) {
                    try {
                        if (response.body() != null) {
                            JSONObject object = new JSONObject(response.body());
                            JSONArray array = object.getJSONArray("results");
                            if (object.getInt("failure") == 1) {
                                JSONObject objectError = (JSONObject) array.get(0);
                                showToast(objectError.getString("error"));
                                return;
                            }
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    showToast("Notification send successfully");
                } else {
                    showToast("ERROR ===> " + response.code());
                }
            }

            @Override
            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                showToast(t.getMessage());
            }
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        listenAvailabilityOfReceiver();
    }
}