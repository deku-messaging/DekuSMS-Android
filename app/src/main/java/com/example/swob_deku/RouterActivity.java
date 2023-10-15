package com.example.swob_deku;

import androidx.annotation.Nullable;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.example.swob_deku.Models.CustomAppCompactActivity;
import com.example.swob_deku.Models.Messages.MessagesThreadRecyclerAdapter;
import com.example.swob_deku.Models.Messages.ViewHolders.TemplateViewHolder;
import com.example.swob_deku.Models.Router.RouterHandler;
import com.example.swob_deku.Models.Router.RouterViewModel;
import com.example.swob_deku.Models.SMS.SMS;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class RouterActivity extends CustomAppCompactActivity {

    RouterViewModel routerViewModel;
    public MessagesThreadRecyclerAdapter messagesThreadRecyclerAdapter;
    RecyclerView routedMessageRecyclerView;

    ActionBar ab;

    Toolbar myToolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_router);

        myToolbar = (Toolbar) findViewById(R.id.routed_messages_toolbar);
        myToolbar.setTitle(R.string.homepage_menu_routed);

        setSupportActionBar(myToolbar);

        // Get a support ActionBar corresponding to this toolbar
        ab = getSupportActionBar();

        // Enable the Up button
        ab.setDisplayHomeAsUpEnabled(true);

        routedMessageRecyclerView = findViewById(R.id.routed_messages_recycler_view);

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this,
                LinearLayoutManager.VERTICAL, false);
        routedMessageRecyclerView.setLayoutManager(linearLayoutManager);

        messagesThreadRecyclerAdapter = new MessagesThreadRecyclerAdapter( this,
                true, "", this);

        routedMessageRecyclerView.setAdapter(messagesThreadRecyclerAdapter);

        routerViewModel = new ViewModelProvider(this).get(
                RouterViewModel.class);

//        routerViewModel.getMessages(getApplicationContext()).observe(this,
//                new Observer<List<SMS>>() {
//                    @Override
//                    public void onChanged(List<SMS> smsList) {
//                        messagesThreadRecyclerAdapter.submitList(smsList);
//                        if(!smsList.isEmpty())
//                            findViewById(R.id.router_no_showable_messages_text).setVisibility(View.GONE);
//                        else {
//                            findViewById(R.id.router_no_showable_messages_text).setVisibility(View.VISIBLE);
//                            routedMessageRecyclerView.smoothScrollToPosition(0);
//                        }
//                    }
//                });

        listeners();
    }

    @Override
    protected void onPostCreate(@Nullable Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        configureBroadcastListeners(new Runnable() {
            @Override
            public void run() {
                Log.d(getLocalClassName(), "Updating the routing information");
                routerViewModel.informChanges(getApplicationContext());
            }
        });

    }

    private void listeners() {
        messagesThreadRecyclerAdapter.selectedItems.observe(this, new Observer<HashMap<String, TemplateViewHolder>>() {
            @Override
            public void onChanged(HashMap<String, TemplateViewHolder> stringTemplateViewHolderHashMap) {
                if(stringTemplateViewHolderHashMap != null) {
                    if(!stringTemplateViewHolderHashMap.isEmpty()) {
                        ab.setTitle(String.valueOf(stringTemplateViewHolderHashMap.size()));
                    }
                    else
                        ab.setTitle(R.string.homepage_menu_routed);
                    myToolbar.getMenu().findItem(R.id.messages_thread_routing_cancel)
                            .setVisible(!stringTemplateViewHolderHashMap.isEmpty());
                }
                else {
                    myToolbar.getMenu().findItem(R.id.messages_thread_routing_cancel)
                            .setVisible(false);
                }
            }
        });
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu){
        getMenuInflater().inflate(R.menu.routing_menu, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == R.id.messages_thread_routing_cancel) {
            if(messagesThreadRecyclerAdapter.selectedItems.getValue() != null) {
                for (Map.Entry<String, TemplateViewHolder> entry :
                        messagesThreadRecyclerAdapter.selectedItems.getValue().entrySet()) {
                    String messageId = String.valueOf(entry.getValue().messageId);
                    Log.d(getLocalClassName(), "Removing routing message: " + messageId);
                    RouterHandler.removeWorkForMessage(getApplicationContext(), messageId);
                }
                messagesThreadRecyclerAdapter.resetAllSelectedItems();
                routerViewModel.informChanges(getApplicationContext());
                return true;
            }
        }
        return false;
    }
}