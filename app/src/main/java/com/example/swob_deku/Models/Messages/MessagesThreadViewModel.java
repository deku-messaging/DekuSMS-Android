package com.example.swob_deku.Models.Messages;

import android.content.Context;
import android.database.Cursor;
import android.os.AsyncTask;
import android.util.Log;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.swob_deku.Fragments.Homepage.MessagesThreadFragment;
import com.example.swob_deku.Models.Archive.ArchiveHandler;
import com.example.swob_deku.Models.SMS.Conversations;
import com.example.swob_deku.Models.SMS.SMS;
import com.example.swob_deku.Models.SMS.SMSHandler;
import com.example.swob_deku.Models.Security.SecurityECDH;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Lists;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Ordering;
import com.google.common.collect.TreeMultimap;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class MessagesThreadViewModel extends ViewModel {
    private MutableLiveData<List<Conversations>> conversationsMutableLiveData;

    String messagesType;

    public LiveData<List<Conversations>> getMessages(Context context, String messagesType) throws GeneralSecurityException, IOException {
        if(conversationsMutableLiveData == null) {
            this.messagesType = messagesType;
            conversationsMutableLiveData = new MutableLiveData<>();
            loadSMSThreads(context);
        }
        return conversationsMutableLiveData;
    }

    public void informChanges(Context context) throws GeneralSecurityException, IOException {
        Log.d(getClass().getName(), "Running for informing changes");
        loadSMSThreads(context);
    }

    private void loadSMSThreads(Context context) throws GeneralSecurityException, IOException {
        ArchiveHandler archiveHandler = new ArchiveHandler(context);
        Cursor cursor = SMSHandler.fetchSMSForThreading(context);

        switch (messagesType) {
            case MessagesThreadFragment.ENCRYPTED_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ListMultimap<Long, Conversations> conversationsListMultimap = ArrayListMultimap.create();
                        try {
                            SecurityECDH securityECDH = new SecurityECDH(context);
                            Map<String, ?> encryptedContacts = securityECDH.securelyFetchAllSecretKey();
                            if (cursor.moveToFirst()) {
                                do {
                                    Conversations conversation = new Conversations(cursor);
                                    try {
                                        if(!encryptedContacts.containsKey(conversation.THREAD_ID) ||
                                                archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
                                            continue;
                                        }
                                        conversation.setNewestMessage(context);
                                        long date = conversation.getNewestMessage().getNewestDateTime();
                                        conversationsListMultimap.put(date, conversation);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } while (cursor.moveToNext());
                                cursor.close();
                            }
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                        List<Conversations> sortedList = new ArrayList<>();
                        List<Long> keys = new ArrayList<>(conversationsListMultimap.keySet());
                        keys.sort(Collections.reverseOrder());
                        for(Long date : keys) {
                            sortedList.addAll(conversationsListMultimap.get(date));
                        }
                        conversationsMutableLiveData.postValue(sortedList);
                        archiveHandler.close();
                    }
                }).start();
                break;
            }

            case MessagesThreadFragment.ALL_MESSAGES_THREAD_FRAGMENT: {
                Thread thread = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ListMultimap<Long, Conversations> conversationsListMultimap = ArrayListMultimap.create();
                        if (cursor.moveToFirst()) {
                            do {
                                Conversations conversation = new Conversations(cursor);
                                try {
                                    if(archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
                                        continue;
                                    }
                                    conversation.setNewestMessage(context);
                                    long date = conversation.getNewestMessage().getNewestDateTime();
                                    conversationsListMultimap.put(date, conversation);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            } while (cursor.moveToNext());
                            cursor.close();
                        }
                        List<Conversations> sortedList = new ArrayList<>();
                        List<Long> keys = new ArrayList<>(conversationsListMultimap.keySet());
                        keys.sort(Collections.reverseOrder());
                        for(Long date : keys) {
                            sortedList.addAll(conversationsListMultimap.get(date));
                        }

                        conversationsMutableLiveData.postValue(sortedList);
                        archiveHandler.close();
                    }

                });
                thread.start();
                break;
            }

            case MessagesThreadFragment.PLAIN_MESSAGES_THREAD_FRAGMENT: {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        ListMultimap<Long, Conversations> conversationsListMultimap = ArrayListMultimap.create();
                        try {
                            SecurityECDH securityECDH = new SecurityECDH(context);
                            Map<String, ?> encryptedContacts = securityECDH.securelyFetchAllSecretKey();
                            if (cursor.moveToFirst()) {
                                do {
                                    Conversations conversation = new Conversations(cursor);
                                    try {
                                        if(encryptedContacts.containsKey(conversation.THREAD_ID) ||
                                                archiveHandler.isArchived(Long.parseLong(conversation.THREAD_ID))) {
                                            continue;
                                        }
                                        conversation.setNewestMessage(context);
                                        long date = conversation.getNewestMessage().getNewestDateTime();
                                        conversationsListMultimap.put(date, conversation);
                                    } catch (InterruptedException e) {
                                        e.printStackTrace();
                                    }
                                } while (cursor.moveToNext());
                                cursor.close();
                            }
                        } catch (GeneralSecurityException | IOException e) {
                            e.printStackTrace();
                        }
                        List<Conversations> sortedList = new ArrayList<>();
                        List<Long> keys = new ArrayList<>(conversationsListMultimap.keySet());
                        keys.sort(Collections.reverseOrder());
                        for(Long date : keys) {
                            sortedList.addAll(conversationsListMultimap.get(date));
                        }
                        conversationsMutableLiveData.postValue(sortedList);
                        archiveHandler.close();
                    }
                }).start();
                break;
            }

        }
    }
}
