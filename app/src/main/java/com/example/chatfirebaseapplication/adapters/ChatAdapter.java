package com.example.chatfirebaseapplication.adapters;

import android.graphics.Bitmap;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebaseapplication.databinding.ItemReceivedMessageBinding;
import com.example.chatfirebaseapplication.databinding.ItemSendMessageBinding;
import com.example.chatfirebaseapplication.models.ChatMessageModel;

import java.util.List;

public class ChatAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private final List<ChatMessageModel> messageList;
    private Bitmap receiverProfileImage;
    private final String senderId;

    public static final int VIEW_TYPE_SEND = 1;
    public static final int VIEW_TYPE_RECEIVED = 2;

    public ChatAdapter(List<ChatMessageModel> messageList, Bitmap receiverProfileImage, String senderId) {
        this.messageList = messageList;
        this.receiverProfileImage = receiverProfileImage;
        this.senderId = senderId;
    }

    public void setReceiverProfileImage(Bitmap bitmap) {
        receiverProfileImage = bitmap;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_SEND) {
            return new SendMessageViewHolder(ItemSendMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        } else {
            return new ReceivedMessageViewHolder(ItemReceivedMessageBinding.inflate(LayoutInflater.from(parent.getContext()), parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_SEND) {
            ((SendMessageViewHolder) holder).setData(messageList.get(position));
        } else {
            ((ReceivedMessageViewHolder) holder).setData(messageList.get(position), receiverProfileImage);
        }
    }

    @Override
    public int getItemCount() {
        if (messageList.size() > 0) {
            return messageList.size();
        }
        return 0;
    }

    @Override
    public int getItemViewType(int position) {
        if (messageList.get(position).senderId.equals(senderId)) {
            return VIEW_TYPE_SEND;
        }
        return VIEW_TYPE_RECEIVED;
    }

    static class SendMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemSendMessageBinding binding;

        public SendMessageViewHolder(ItemSendMessageBinding itemSendMessageBinding) {
            super(itemSendMessageBinding.getRoot());
            binding = itemSendMessageBinding;
        }

        void setData(ChatMessageModel messageModel) {
            binding.tvMessage.setText(messageModel.getMessage());
            binding.tvDateTime.setText(messageModel.getDateTime());
        }
    }

    static class ReceivedMessageViewHolder extends RecyclerView.ViewHolder {
        private final ItemReceivedMessageBinding binding;

        public ReceivedMessageViewHolder(ItemReceivedMessageBinding itemReceivedMessageBinding) {
            super(itemReceivedMessageBinding.getRoot());
            binding = itemReceivedMessageBinding;
        }

        void setData(ChatMessageModel messageModel, Bitmap bmReceivedUser) {
            if (bmReceivedUser != null) {
                binding.ivUser.setImageBitmap(bmReceivedUser);
            }
            binding.tvMessage.setText(messageModel.getMessage());
            binding.tvDateTime.setText(messageModel.getDateTime());
        }
    }
}
