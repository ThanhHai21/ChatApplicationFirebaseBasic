package com.example.chatfirebaseapplication.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import com.example.chatfirebaseapplication.adapters.UserAdapter;
import com.example.chatfirebaseapplication.databinding.ActivityUserBinding;
import com.example.chatfirebaseapplication.listeners.UserListener;
import com.example.chatfirebaseapplication.models.UserModel;
import com.example.chatfirebaseapplication.utils.Constants;
import com.example.chatfirebaseapplication.utils.PreferenceManager;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserActivity extends BaseActivity implements UserListener {
    private ActivityUserBinding binding;
    private PreferenceManager preferenceManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityUserBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        preferenceManager = new PreferenceManager(getApplicationContext());
        getUsers();
        setListeners();
    }

    private void setListeners() {
        binding.ivBack.setOnClickListener(v -> onBackPressed());
    }

    private void getUsers() {
        loadingView(true);
        FirebaseFirestore firebaseFirestore = FirebaseFirestore.getInstance();
        firebaseFirestore.collection(Constants.KEY_COLLECTION_USER).get()
                .addOnCompleteListener(task -> {
                    loadingView(false);
                    String currentUserId = preferenceManager.getString(Constants.KEY_USER_ID);
                    if (task.isSuccessful() && task.getResult() != null) {
                        List<UserModel> users = new ArrayList<>();
                        for (QueryDocumentSnapshot snapshot : task.getResult()) {
                            if (currentUserId.equals(snapshot.getId())) {
                                continue;
                            }
                            UserModel user = new UserModel();
                            user.setId(snapshot.getId());
                            user.setName(snapshot.getString(Constants.KEY_NAME));
                            user.setEmail(snapshot.getString(Constants.KEY_EMAIL));
                            user.setImage(snapshot.getString(Constants.KEY_USER_IMAGE));
                            user.setToken(snapshot.getString(Constants.KEY_FCM_TOKEN));
                            users.add(user);
                        }
                        if (users.size() > 0) {
                            UserAdapter userAdapter = new UserAdapter(users, this);
                            binding.rvUsers.setAdapter(userAdapter);
                            binding.rvUsers.setVisibility(View.VISIBLE);
                        } else {
                            showErrorMessage();
                        }
                    } else {
                        showErrorMessage();
                    }
                });
    }

    private void showErrorMessage() {
        binding.tvErrorMessage.setText(String.format("%s", "No user available"));
        binding.tvErrorMessage.setVisibility(View.VISIBLE);
    }

    private void loadingView(boolean isLoading) {
        if (isLoading) {
            binding.pbLoading.setVisibility(View.VISIBLE);
        } else {
            binding.pbLoading.setVisibility(View.GONE);
        }
    }

    @Override
    public void onClickItemUser(UserModel userModel) {
        Intent intent = new Intent(this, ChatActivity.class);
        intent.putExtra(Constants.KEY_USER, userModel);
        startActivity(intent);
        finish();
    }
}