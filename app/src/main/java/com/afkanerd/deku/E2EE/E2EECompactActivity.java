package com.afkanerd.deku.E2EE;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.afkanerd.deku.DefaultSMS.CustomAppCompactActivity;
import com.afkanerd.deku.DefaultSMS.Models.Conversations.ThreadedConversations;
import com.afkanerd.deku.DefaultSMS.Models.SettingsHandler;
import com.afkanerd.deku.DefaultSMS.R;
import com.google.i18n.phonenumbers.NumberParseException;

import java.io.IOException;
import java.security.GeneralSecurityException;

public class E2EECompactActivity extends CustomAppCompactActivity {

    protected ThreadedConversations threadedConversations;
    View securePopUpRequest;

    protected String keystoreAlias;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }


    @Override
    public void sendTextMessage(final String text, int subscriptionId,
                                ThreadedConversations threadedConversations, String messageId) throws NumberParseException, InterruptedException {
        final String[] transmissionText = {text};
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if(threadedConversations.secured) {
                        byte[] cipherText = E2EEHandler.encryptText(getApplicationContext(),
                                keystoreAlias, text);
                        transmissionText[0] = E2EEHandler.buildTransmissionText(cipherText);
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
        thread.join();

        super.sendTextMessage(transmissionText[0], subscriptionId, threadedConversations, messageId);
    }

    @Override
    public void informSecured(boolean secured) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if(secured && securePopUpRequest != null) {
                    securePopUpRequest.setVisibility(View.GONE);
//                    Toast.makeText(getApplicationContext(),
//                            getString(R.string.conversation_inform_user_now_secured_toast),
//                            Toast.LENGTH_LONG).show();
                }

            }
        });
    }
    private void showSecureRequestPopUpMenu() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(getString(R.string.conversation_secure_popup_request_menu_title));

        View conversationSecurePopView = View.inflate(getApplicationContext(),
                R.layout.conversation_secure_popup_menu, null);
        builder.setView(conversationSecurePopView);

        Button yesButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_send);
        Button cancelButton = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_cancel);
        TextView descriptionText = conversationSecurePopView.findViewById(R.id.conversation_secure_popup_menu_text_description);
        String descriptionTextRevised = descriptionText.getText()
                .toString()
                .replaceAll("\\[contact name]", threadedConversations.getContact_name() == null ?
                        threadedConversations.getAddress() : threadedConversations.getContact_name());
        descriptionText.setText(descriptionTextRevised);

        AlertDialog dialog = builder.create();

        yesButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sendDataMessage(threadedConversations);
                dialog.dismiss();
            }
        });
        cancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialog.dismiss();
            }
        });

        dialog.show();
    }

    private void setSecurePopUpRequest() {
        securePopUpRequest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showSecureRequestPopUpMenu();
            }
        });
    }

    public void setEncryptionThreadedConversations(ThreadedConversations threadedConversations) {
        this.threadedConversations = threadedConversations;
    }

    @Override
    protected void onStart() {
        super.onStart();
        securePopUpRequest = findViewById(R.id.conversations_request_secure_pop_layout);
        setSecurePopUpRequest();
        if(!SettingsHandler.alertNotEncryptedCommunicationDisabled(getApplicationContext()))
            securePopUpRequest.setVisibility(View.VISIBLE);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (R.id.conversation_main_menu_encrypt_lock == item.getItemId()) {
            if(securePopUpRequest != null) {
                securePopUpRequest.setVisibility(securePopUpRequest.getVisibility() == View.VISIBLE ?
                        View.GONE : View.VISIBLE);
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Thread thread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    keystoreAlias = E2EEHandler.deriveKeystoreAlias(threadedConversations.getAddress(), 0);
                    threadedConversations.secured =
                            E2EEHandler.canCommunicateSecurely(getApplicationContext(), keystoreAlias);
                } catch (IOException | GeneralSecurityException | NumberParseException e) {
                    e.printStackTrace();
                }
            }
        });
        thread.start();
    }
}
