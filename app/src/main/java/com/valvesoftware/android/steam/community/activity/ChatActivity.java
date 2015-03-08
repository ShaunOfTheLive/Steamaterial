package com.valvesoftware.android.steam.community.activity;

import android.os.Bundle;
import com.valvesoftware.android.steam.community.R;

public class ChatActivity extends FragmentActivityWithNavigationSupport {
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.chat_activity);
    }
}
