package com.afkanerd.deku.DefaultSMS.Models.Conversations;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.provider.Telephony;
import android.telephony.SmsMessage;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.Room;

import com.afkanerd.deku.DefaultSMS.Models.Datastore;
import com.afkanerd.deku.DefaultSMS.Models.Migrations;
import com.afkanerd.deku.DefaultSMS.Models.SMS.Conversations;

import java.nio.charset.StandardCharsets;


@Entity
public class ThreadedConversations {
    @PrimaryKey
     long thread_id;
     int msg_count;
     int avatar_color;

     boolean is_archived;
     boolean is_blocked;

     boolean is_read;

     String snippet;

     String contact_name;

     String avatar_initials;

     String avatar_image;
     String formatted_datetime;

    public static ThreadedConversationsDao getDao(Context context) {
        Datastore databaseConnector = Room.databaseBuilder(context, Datastore.class,
                        Datastore.databaseName)
                .addMigrations(new Migrations.Migration8To9())
                .build();
        ThreadedConversationsDao threadedConversationsDao =  databaseConnector.threadedConversationsDao();
        databaseConnector.close();
        return threadedConversationsDao;
    }

    public static ThreadedConversations build(Cursor cursor) {
        int snippetIndex = cursor.getColumnIndexOrThrow(Telephony.Sms.Conversations.SNIPPET);
        int threadIdIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.THREAD_ID);
        int msgCountIndex = cursor.getColumnIndex(Telephony.Sms.Conversations.MESSAGE_COUNT);

        ThreadedConversations threadedConversations = new ThreadedConversations();
        threadedConversations.setSnippet(cursor.getString(snippetIndex));
        threadedConversations.setThread_id(Long.parseLong(cursor.getString(threadIdIndex)));
        threadedConversations.setMsg_count(cursor.getInt(msgCountIndex));

        return threadedConversations;
    }

    public long getThread_id() {
        return thread_id;
    }

    public void setThread_id(long thread_id) {
        this.thread_id = thread_id;
    }

    public int getMsg_count() {
        return msg_count;
    }

    public void setMsg_count(int msg_count) {
        this.msg_count = msg_count;
    }

    public int getAvatar_color() {
        return avatar_color;
    }

    public void setAvatar_color(int avatar_color) {
        this.avatar_color = avatar_color;
    }

    public boolean isIs_archived() {
        return is_archived;
    }

    public void setIs_archived(boolean is_archived) {
        this.is_archived = is_archived;
    }

    public boolean isIs_blocked() {
        return is_blocked;
    }

    public void setIs_blocked(boolean is_blocked) {
        this.is_blocked = is_blocked;
    }

    public boolean isIs_read() {
        return is_read;
    }

    public void setIs_read(boolean is_read) {
        this.is_read = is_read;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getContact_name() {
        return contact_name;
    }

    public void setContact_name(String contact_name) {
        this.contact_name = contact_name;
    }

    public String getAvatar_initials() {
        return avatar_initials;
    }

    public void setAvatar_initials(String avatar_initials) {
        this.avatar_initials = avatar_initials;
    }

    public String getAvatar_image() {
        return avatar_image;
    }

    public void setAvatar_image(String avatar_image) {
        this.avatar_image = avatar_image;
    }

    public String getFormatted_datetime() {
        return formatted_datetime;
    }

    public void setFormatted_datetime(String formatted_datetime) {
        this.formatted_datetime = formatted_datetime;
    }

    public static final DiffUtil.ItemCallback<ThreadedConversations> DIFF_CALLBACK = new DiffUtil.ItemCallback<ThreadedConversations>() {
        @Override
        public boolean areItemsTheSame(@NonNull ThreadedConversations oldItem, @NonNull ThreadedConversations newItem) {
            return oldItem.thread_id == newItem.thread_id;
        }

        @Override
        public boolean areContentsTheSame(@NonNull ThreadedConversations oldItem, @NonNull ThreadedConversations newItem) {
            return oldItem.equals(newItem);
        }
    };

    @Override
    public boolean equals(@Nullable Object obj) {
        if(obj instanceof ThreadedConversations) {
            ThreadedConversations threadedConversations = (ThreadedConversations) obj;
//            return threadedConversations.thread_id == this.thread_id &&
//                    threadedConversations.is_archived == this.is_archived &&
//                    threadedConversations.is_blocked == this.is_blocked &&
//                    threadedConversations.snippet.equals(this.snippet) &&
//                    threadedConversations.formatted_datetime.equals(this.formatted_datetime) &&
//                    threadedConversations.contact_name.equals(this.contact_name) &&
//                    threadedConversations.avatar_color == this.avatar_color &&
//                    threadedConversations.avatar_image.equals(this.avatar_image) &&
//                    threadedConversations.avatar_initials.equals(this.avatar_initials) &&
//                    threadedConversations.msg_count == this.msg_count;

            return threadedConversations.thread_id == this.thread_id &&
                    threadedConversations.is_archived == this.is_archived &&
                    threadedConversations.is_blocked == this.is_blocked &&
                    threadedConversations.snippet.equals(this.snippet) &&
                    threadedConversations.avatar_color == this.avatar_color &&
                    threadedConversations.msg_count == this.msg_count;
        }
        return super.equals(obj);
    }
}
