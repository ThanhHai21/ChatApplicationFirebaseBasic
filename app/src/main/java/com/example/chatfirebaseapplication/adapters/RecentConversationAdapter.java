package com.example.chatfirebaseapplication.adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.chatfirebaseapplication.databinding.ItemRecentConversionBinding;
import com.example.chatfirebaseapplication.listeners.ConversionListener;
import com.example.chatfirebaseapplication.models.ChatMessageModel;
import com.example.chatfirebaseapplication.models.UserModel;

import java.util.List;

public class RecentConversationAdapter extends RecyclerView.Adapter<RecentConversationAdapter.ConversionViewHolder> {
    private final List<ChatMessageModel> messageList;
    private final ConversionListener listener;

    public RecentConversationAdapter(List<ChatMessageModel> messageList, ConversionListener listener) {
        this.messageList = messageList;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ConversionViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ConversionViewHolder(ItemRecentConversionBinding.inflate(
                LayoutInflater.from(parent.getContext()),
                parent,
                false
        ));
    }

    @Override
    public void onBindViewHolder(@NonNull ConversionViewHolder holder, int position) {
        holder.setData(messageList.get(position));
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    class ConversionViewHolder extends RecyclerView.ViewHolder {
        private final ItemRecentConversionBinding binding;

        public ConversionViewHolder(ItemRecentConversionBinding itemRecentConversionBinding) {
            super(itemRecentConversionBinding.getRoot());
            binding = itemRecentConversionBinding;
        }

        void setData(ChatMessageModel messageModel) {
            binding.ivUser.setImageBitmap(getConversionImage(messageModel.getConversionImage()));
            binding.tvName.setText(messageModel.getConversionName());
            binding.tvRecentMessage.setText(messageModel.getMessage());
            binding.getRoot().setOnClickListener(v -> {
                UserModel userModel = new UserModel();
                userModel.setId(messageModel.getConversionId());
                userModel.setName(messageModel.getConversionName());
                userModel.setImage(messageModel.getConversionImage());
                listener.onClickItemConversion(userModel);
            });
        }
    }

    private Bitmap getConversionImage(String encodeImage) {
        byte[] bytes = Base64.decode(encodeImage, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }
}
