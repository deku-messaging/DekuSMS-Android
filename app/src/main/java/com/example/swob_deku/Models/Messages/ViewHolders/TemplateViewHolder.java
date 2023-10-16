package com.example.swob_deku.Models.Messages.ViewHolders;

import android.graphics.PorterDuff;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import com.example.swob_deku.Commons.Helpers;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.SMS.Conversations;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.R;

import io.getstream.avatarview.AvatarView;

public class TemplateViewHolder extends RecyclerView.ViewHolder {

    public String id;
    public long messageId;
    public TextView snippet;
    public TextView address;
    public TextView date;
    public TextView state;
    public TextView routingUrl;
    public TextView routingURLText;
    public AvatarView contactInitials;
    public TextView youLabel;

    public ImageView contactPhoto;
    public ImageView encryptedLock;

    public ConstraintLayout layout;


    public TemplateViewHolder(@NonNull View itemView) {
        super(itemView);

        snippet = itemView.findViewById(R.id.messages_thread_text);
        address = itemView.findViewById(R.id.messages_thread_address_text);
        date = itemView.findViewById(R.id.messages_thread_date);
        layout = itemView.findViewById(R.id.messages_threads_layout);
        state = itemView.findViewById(R.id.messages_route_state);
        routingUrl = itemView.findViewById(R.id.message_route_url);
        routingURLText = itemView.findViewById(R.id.message_route_status);
        youLabel = itemView.findViewById(R.id.message_you_label);
        contactInitials = itemView.findViewById(R.id.messages_threads_contact_initials);
        encryptedLock = itemView.findViewById(R.id.messages_thread_secured_lock);
    }

    public void init(Conversations conversation) {
        this.id = conversation.THREAD_ID;
//
        final SMS.SMSMetaEntity smsMetaEntity = conversation.getNewestMessage();
        String address = smsMetaEntity.getAddress();
        if(smsMetaEntity.isContact()) {
            address = smsMetaEntity.getContactName();
//            if(!address.isEmpty()) {
//                holder.contactInitials.setAvatarInitials(address.substring(0, 1));
//                holder.contactInitials.setAvatarInitialsBackgroundColor(Helpers.generateColor(address));
//            }
        }
        this.address.setText(address);
        this.date.setText(smsMetaEntity.getFormattedDate());
        this.snippet.setText(conversation.SNIPPET);
    }

    public static class ReadViewHolder extends TemplateViewHolder{
        public ReadViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setMaxLines(1);
        }
    }

    public static class UnreadViewHolder extends TemplateViewHolder{
        public UnreadViewHolder(@NonNull View itemView) {
            super(itemView);
            address.setTypeface(Typeface.DEFAULT_BOLD);
            address.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            snippet.setTypeface(Typeface.DEFAULT_BOLD);
            snippet.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));

            date.setTypeface(Typeface.DEFAULT_BOLD);
            date.setTextColor(itemView.getContext().getColor(R.color.primary_text_color));
        }
    }

    public static class UnreadEncryptedViewHolder extends TemplateViewHolder.UnreadViewHolder {
        public UnreadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.BOLD_ITALIC));
        }
    }

    public static class ReadEncryptedViewHolder extends TemplateViewHolder.ReadViewHolder {
        public ReadEncryptedViewHolder(@NonNull View itemView) {
            super(itemView);
            snippet.setText(R.string.messages_thread_encrypted_content);
            snippet.setTypeface(Typeface.defaultFromStyle(Typeface.ITALIC));
        }
    }

    public void highlight(){
        layout.setBackgroundResource(R.drawable.received_messages_drawable);
        this.setIsRecyclable(false);
    }

    public void unHighlight(){
        layout.setBackgroundResource(0);
        this.setIsRecyclable(true);
    }

}
