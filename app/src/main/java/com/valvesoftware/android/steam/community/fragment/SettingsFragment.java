package com.valvesoftware.android.steam.community.fragment;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.AlertDialog.Builder;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings.System;
import android.support.v4.app.ListFragment;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.DatePicker;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.AndroidUtils;
import com.valvesoftware.android.steam.community.FriendInfo;
import com.valvesoftware.android.steam.community.GenericListDB.ListItemUpdatedListener;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SettingInfo;
import com.valvesoftware.android.steam.community.SettingInfo.AccessRight;
import com.valvesoftware.android.steam.community.SettingInfo.CustomDatePickerDialog;
import com.valvesoftware.android.steam.community.SettingInfo.DateConverter;
import com.valvesoftware.android.steam.community.SettingInfo.RadioSelectorItem;
import com.valvesoftware.android.steam.community.SettingInfo.SettingType;
import com.valvesoftware.android.steam.community.SettingInfoDB;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_MARKREADMESSAGES_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService.UmqConnectionState;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.activity.FragmentActivityWithNavigationSupport;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginAction;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener;
import java.util.ArrayList;
import java.util.Calendar;

public class SettingsFragment extends ListFragment {
    private SettingsListAdapter m_SettingsAdapter;
    private OnClickListener m_accountControlChangeUser;
    private boolean m_bLoggedOnPresentation;
    private ListView m_listView;
    private Activity m_owner;
    private OnClickListener m_profileClickListener;
    private final ListItemUpdatedListener m_profileUpdateListener;
    private ArrayList<SettingInfo> m_settingsInfoArray;
    private final UmqDBCallback m_umqdbIntentReceiver;
    private View m_viewProfile;

    static /* synthetic */ class AnonymousClass_4 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$AccessRight;
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType = new int[SettingType.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.INFO.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.CHECK.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.DATE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.URI.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.MARKET.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.RADIOSELECTOR.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.RINGTONESELECTOR.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.UNREADMSG.ordinal()] = 8;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[SettingType.SECTION.ordinal()] = 9;
            } catch (NoSuchFieldError e9) {
            }
            $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$AccessRight = new int[AccessRight.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$AccessRight[AccessRight.USER.ordinal()] = 1;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$AccessRight[AccessRight.VALID_ACCOUNT.ordinal()] = 2;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$AccessRight[AccessRight.CODE.ordinal()] = 3;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    public static class RadioSelectorItemOnClickListener implements OnClickListener {
        Activity activity;
        AlertDialog alert;
        DialogInterface.OnClickListener m_onRadioButtonSelected;
        SettingInfo settingInfo;
        TextView valueView;

        public RadioSelectorItemOnClickListener(Activity act, SettingInfo si, TextView value) {
            this.m_onRadioButtonSelected = new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int item) {
                    RadioSelectorItem[] radios = (RadioSelectorItem[]) com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.settingInfo.m_extraData;
                    if (item >= 0 && item < radios.length) {
                        if (com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.valueView != null) {
                            com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.valueView.setText(radios[item].resid_text);
                        }
                        com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.settingInfo.setValueAndCommit(com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.activity.getApplicationContext(), String.valueOf(radios[item].value));
                        com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.alert.dismiss();
                        com.valvesoftware.android.steam.community.fragment.SettingsFragment.RadioSelectorItemOnClickListener.this.onSettingChanged(radios[item]);
                    }
                }
            };
            this.activity = act;
            this.settingInfo = si;
            this.valueView = value;
        }

        public void onClick(View btn) {
            Builder builder = new Builder(this.activity);
            builder.setTitle(this.settingInfo.m_resid);
            RadioSelectorItem[] radios = (RadioSelectorItem[]) this.settingInfo.m_extraData;
            CharSequence[] builderItems = new CharSequence[radios.length];
            RadioSelectorItem rsiValue = this.settingInfo.getRadioSelectorItemValue(this.activity.getApplicationContext());
            int iCheckedItem = -1;
            for (int j = 0; j < radios.length; j++) {
                builderItems[j] = this.activity.getString(radios[j].resid_text);
                if (rsiValue == radios[j]) {
                    iCheckedItem = j;
                }
            }
            builder.setSingleChoiceItems(builderItems, iCheckedItem, this.m_onRadioButtonSelected);
            this.alert = builder.create();
            this.alert.show();
        }

        public void onSettingChanged(RadioSelectorItem sel) {
        }
    }

    private class SettingsListAdapter extends ArrayAdapter<SettingInfo> {
        private ArrayList<SettingInfo> items;

        class AnonymousClass_1 implements OnClickListener {
            final /* synthetic */ ImageView val$chevronView;
            final /* synthetic */ SettingInfo val$settingInfo;
            final /* synthetic */ TextView val$valueView;

            AnonymousClass_1(TextView textView, SettingInfo settingInfo, ImageView imageView) {
                this.val$valueView = textView;
                this.val$settingInfo = settingInfo;
                this.val$chevronView = imageView;
            }

            public void onClick(View btn) {
                try {
                    SettingsFragment.this.getActivity().startActivity(new Intent("android.intent.action.VIEW").setData(Uri.parse("market://details?id=com.valvesoftware.android.steam.community")));
                } catch (Exception e) {
                    this.val$valueView.setText(this.val$settingInfo.m_defaultValue + " / " + SettingsFragment.this.getActivity().getString(R.string.Market_Unavailable));
                    try {
                        SettingsFragment.this.getActivity().startActivity(new Intent("android.intent.action.VIEW").setData(Uri.parse("http://store.steampowered.com/mobile")));
                    } catch (Exception e2) {
                        this.val$chevronView.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                    }
                }
            }
        }

        class AnonymousClass_3 implements OnCheckedChangeListener {
            final /* synthetic */ SettingInfo val$settingInfo;

            AnonymousClass_3(SettingInfo settingInfo) {
                this.val$settingInfo = settingInfo;
            }

            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                this.val$settingInfo.setValueAndCommit(SettingsFragment.this.m_owner.getApplicationContext(), isChecked ? "1" : "");
            }
        }

        class AnonymousClass_4 implements OnClickListener {
            final /* synthetic */ CheckBox val$checkBox;

            AnonymousClass_4(CheckBox checkBox) {
                this.val$checkBox = checkBox;
            }

            public void onClick(View v) {
                this.val$checkBox.setChecked(!this.val$checkBox.isChecked());
            }
        }

        class AnonymousClass_5 implements OnClickListener {
            final /* synthetic */ Calendar val$myDOB;
            final /* synthetic */ SettingInfo val$settingInfo;
            final /* synthetic */ TextView val$valueView;

            AnonymousClass_5(Calendar calendar, TextView textView, SettingInfo settingInfo) {
                this.val$myDOB = calendar;
                this.val$valueView = textView;
                this.val$settingInfo = settingInfo;
            }

            public void onClick(View btn) {
                new CustomDatePickerDialog(SettingsFragment.this.m_owner, new OnDateSetListener() {
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        AnonymousClass_5.this.val$myDOB.set(year, monthOfYear, dayOfMonth);
                        String sValue = DateConverter.makeValue(year, monthOfYear, dayOfMonth);
                        if (!(sValue == null || sValue.equals(""))) {
                            AnonymousClass_5.this.val$valueView.setText(DateConverter.formatDate(sValue));
                        }
                        AnonymousClass_5.this.val$settingInfo.setValueAndCommit(SettingsFragment.this.m_owner.getApplicationContext(), sValue);
                    }
                }, this.val$myDOB, 2131165263).show();
            }
        }

        class AnonymousClass_6 implements OnClickListener {
            final /* synthetic */ SettingInfo val$settingInfo;

            AnonymousClass_6(SettingInfo settingInfo) {
                this.val$settingInfo = settingInfo;
            }

            public void onClick(View btn) {
                SettingsFragment.this.getActivity().startActivity(new Intent().addFlags(402653184).setClass(SettingsFragment.this.getActivity(), SteamMobileUriActivity.class).setAction("android.intent.action.VIEW").putExtra("title_resid", this.val$settingInfo.m_resid).setData(Uri.parse(this.val$settingInfo.m_defaultValue)));
            }
        }

        class AnonymousClass_7 implements OnClickListener {
            final /* synthetic */ int val$positionWhenStartingActivityForResult;
            final /* synthetic */ SettingInfo val$settingInfo;

            AnonymousClass_7(SettingInfo settingInfo, int i) {
                this.val$settingInfo = settingInfo;
                this.val$positionWhenStartingActivityForResult = i;
            }

            public void onClick(View v) {
                try {
                    Intent intent = new Intent("android.intent.action.RINGTONE_PICKER");
                    intent.putExtra("android.intent.extra.ringtone.TYPE", UriData.RESULT_HTTP_EXCEPTION);
                    intent.putExtra("android.intent.extra.ringtone.TITLE", SettingsFragment.this.getActivity().getString(this.val$settingInfo.m_resid));
                    try {
                        intent.putExtra("android.intent.extra.ringtone.EXISTING_URI", Uri.parse(this.val$settingInfo.getValue(SettingsFragment.this.m_owner.getApplicationContext())));
                    } catch (Exception e) {
                        intent.putExtra("android.intent.extra.ringtone.EXISTING_URI", (Uri) null);
                    }
                    intent.putExtra("android.intent.extra.ringtone.DEFAULT_URI", Uri.parse(this.val$settingInfo.m_defaultValue));
                    intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true);
                    intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", false);
                    SettingsFragment.this.getActivity().startActivityForResult(intent, this.val$positionWhenStartingActivityForResult);
                } catch (Exception e2) {
                }
            }
        }

        public SettingsListAdapter(Context context, int textViewResourceId, ArrayList<SettingInfo> items) {
            super(context, textViewResourceId, items);
            this.items = items;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            SettingInfo settingInfo = (SettingInfo) this.items.get(position);
            if (settingInfo == null) {
                return v;
            }
            if (v == null) {
                v = ((LayoutInflater) SettingsFragment.this.m_owner.getSystemService("layout_inflater")).inflate(R.layout.settings_list_item_info, null);
                v.setClickable(true);
            }
            v.setOnClickListener(null);
            TextView titleView = (TextView) v.findViewById(2131296269);
            titleView.setText(settingInfo.m_resid);
            TextView valueView = (TextView) v.findViewById(2131296305);
            valueView.setText("");
            if (settingInfo.m_resid_detailed != 0) {
                valueView.setText(settingInfo.m_resid_detailed);
            }
            ImageView chevronView = (ImageView) v.findViewById(2131296279);
            chevronView.setVisibility(8);
            CheckBox checkBox = (CheckBox) v.findViewById(2131296306);
            checkBox.setVisibility(8);
            switch (AnonymousClass_4.$SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[settingInfo.m_type.ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    valueView.setText(settingInfo.m_defaultValue);
                    break;
                case UriData.RESULT_HTTP_EXCEPTION:
                    boolean bValue = settingInfo.getBooleanValue(SettingsFragment.this.m_owner.getApplicationContext());
                    checkBox.setVisibility(0);
                    checkBox.setChecked(bValue);
                    checkBox.setOnCheckedChangeListener(new AnonymousClass_3(settingInfo));
                    v.setOnClickListener(new AnonymousClass_4(checkBox));
                    break;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    chevronView.setVisibility(0);
                    String sValue = settingInfo.getValue(SettingsFragment.this.m_owner.getApplicationContext());
                    Calendar myDOB = DateConverter.makeCalendar(sValue);
                    if (sValue == null || sValue.equals("")) {
                        valueView.setText(2131165271);
                    } else {
                        valueView.setText(DateConverter.formatDate(sValue));
                    }
                    v.setOnClickListener(new AnonymousClass_5(myDOB, valueView, settingInfo));
                    break;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    chevronView.setVisibility(0);
                    v.setOnClickListener(new AnonymousClass_6(settingInfo));
                    break;
                case MotionEventCompat.ACTION_POINTER_DOWN:
                    if (SteamCommunityApplication.GetInstance().IsOverTheAirVersion()) {
                        valueView.setText(settingInfo.m_defaultValue + " / INTERNAL BUILD");
                    } else {
                        chevronView.setVisibility(0);
                        valueView.setText(settingInfo.m_defaultValue);
                        v.setOnClickListener(new AnonymousClass_1(valueView, settingInfo, chevronView));
                    }
                    break;
                case MotionEventCompat.ACTION_POINTER_UP:
                    chevronView.setVisibility(0);
                    valueView.setText(settingInfo.getRadioSelectorItemValue(SettingsFragment.this.m_owner.getApplicationContext()).resid_text);
                    v.setOnClickListener(new RadioSelectorItemOnClickListener(SettingsFragment.this.getActivity(), settingInfo, valueView));
                    break;
                case MotionEventCompat.ACTION_HOVER_MOVE:
                    chevronView.setVisibility(0);
                    try {
                        String curValue = settingInfo.getValue(SettingsFragment.this.m_owner.getApplicationContext());
                        if (curValue == null || !settingInfo.m_defaultValue.equals(curValue)) {
                            valueView.setText(RingtoneManager.getRingtone(SettingsFragment.this.getActivity(), Uri.parse(curValue)).getTitle(SettingsFragment.this.getActivity()));
                            v.setOnClickListener(new AnonymousClass_7(settingInfo, position));
                        } else {
                            valueView.setText(2131165251);
                            v.setOnClickListener(new AnonymousClass_7(settingInfo, position));
                        }
                    } catch (Exception e) {
                        valueView.setText(2131165252);
                    }
                    break;
                case AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION:
                    chevronView.setVisibility(0);
                    SteamDBService steamDb = SteamCommunityApplication.GetInstance().GetSteamDB();
                    int numUnreadMsgs = 0;
                    if (steamDb != null && SteamWebApi.IsLoggedIn()) {
                        numUnreadMsgs = steamDb.getSteamUmqCommunicationServiceDB().selectCountOfUnreadMessages(SteamWebApi.GetLoginSteamID());
                    }
                    titleView.setText(SettingsFragment.this.getActivity().getString(settingInfo.m_resid).replace("#", String.valueOf(numUnreadMsgs)));
                    v.setOnClickListener(new OnClickListener() {
                        public void onClick(View v) {
                            REQ_ACT_MARKREADMESSAGES_DATA obj = new REQ_ACT_MARKREADMESSAGES_DATA();
                            obj.mysteamid = SteamWebApi.GetLoginSteamID();
                            obj.withsteamid = null;
                            obj.deleteAllMessages = false;
                            SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_MARKREADMESSAGES, obj);
                            Intent intent = new Intent(SteamUmqCommunicationService.INTENT_ACTION);
                            intent.putExtra("type", "chatmsg");
                            intent.putExtra("action", "read");
                            intent.putExtra("steamid", "0");
                            SteamCommunityApplication.GetInstance().getApplicationContext().sendBroadcast(intent);
                            SettingsFragment.this.refreshListView();
                        }
                    });
                    break;
            }
            return v;
        }
    }

    private class UmqDBCallback extends BroadcastReceiver {
        private UmqDBCallback() {
        }

        public void onReceive(Context context, Intent intent) {
            if (SettingsFragment.this.getActivity() == null || !intent.getStringExtra("type").equalsIgnoreCase("umqstate")) {
                return;
            }
            if (SteamWebApi.IsLoggedIn() != SettingsFragment.this.m_bLoggedOnPresentation) {
                SettingsFragment.this.refreshListView();
            } else {
                SettingsFragment.this.setupUserAccountView(SettingsFragment.this.m_viewProfile);
            }
        }
    }

    public SettingsFragment() {
        this.m_owner = null;
        this.m_settingsInfoArray = new ArrayList();
        this.m_listView = null;
        this.m_SettingsAdapter = null;
        this.m_viewProfile = null;
        this.m_profileClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (SettingsFragment.this.getActivity() != null) {
                    SettingsFragment.this.setupUserAccountView(SettingsFragment.this.m_viewProfile);
                    NavigationFragment nav = ActivityHelper.GetNavigationFragmentForActivity(SettingsFragment.this.getActivity());
                    if (nav != null) {
                        nav.m_itemProfile.onClick();
                    }
                }
            }
        };
        this.m_accountControlChangeUser = new OnClickListener() {
            public void onClick(View v) {
                FragmentActivityWithNavigationSupport.finishAll();
                ActivityHelper.PresentLoginActivity(null, LoginAction.LOGOUT);
            }
        };
        this.m_bLoggedOnPresentation = false;
        this.m_umqdbIntentReceiver = new UmqDBCallback();
        this.m_profileUpdateListener = new ListItemUpdatedListener() {
            public void OnListRefreshError(RequestBase req, boolean cached) {
            }

            public void OnListItemInfoUpdated(ArrayList<Long> arrayList, boolean requireRefresh) {
                if (SettingsFragment.this.getActivity() != null) {
                    SettingsFragment.this.setupUserAccountView(SettingsFragment.this.m_viewProfile);
                }
            }

            public void OnListItemInfoUpdateError(RequestBase req) {
            }

            public void OnListRequestsInProgress(boolean isRefreshing) {
            }
        };
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.settings_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.m_owner = getActivity();
        if (this.m_SettingsAdapter == null) {
            this.m_SettingsAdapter = new SettingsListAdapter(this.m_owner, 2130903059, this.m_settingsInfoArray);
        }
        if (this.m_listView == null) {
            this.m_listView = getListView();
        }
        if (this.m_viewProfile == null) {
            this.m_viewProfile = getActivity().findViewById(R.id.settingsFragment_TitleItem);
            this.m_viewProfile.setOnClickListener(this.m_profileClickListener);
        }
        getActivity().findViewById(R.id.settingsFragment_AccountControl_ChangeUser).setOnClickListener(this.m_accountControlChangeUser);
        setListAdapter(this.m_SettingsAdapter);
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setTitleLabel((int) R.string.Settings);
            titlebar.setRefreshHandler(null);
        }
    }

    public void onResume() {
        super.onResume();
        if (getActivity() != null) {
            refreshListView();
            getActivity().registerReceiver(this.m_umqdbIntentReceiver, new IntentFilter(SteamUmqCommunicationService.INTENT_ACTION));
            SteamCommunityApplication.GetInstance().GetFriendInfoDB().RegisterCallback(this.m_profileUpdateListener);
        }
    }

    public void onPause() {
        super.onPause();
        SteamCommunityApplication.GetInstance().GetFriendInfoDB().DeregisterCallback(this.m_profileUpdateListener);
        getActivity().unregisterReceiver(this.m_umqdbIntentReceiver);
    }

    private void setupUserAccountView(View v) {
        if (v != null) {
            ((TextView) v.findViewById(R.id.label)).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
            ((ImageView) v.findViewById(R.id.imageChevron)).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
            ((Button) v.findViewById(R.id.chatButton)).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
            TextView nameView = (TextView) v.findViewById(R.id.name);
            TextView statusView = (TextView) v.findViewById(R.id.status);
            ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
            ImageView avatarViewFrame = (ImageView) v.findViewById(R.id.avatar_frame);
            FriendInfo myself = null;
            if (SteamWebApi.IsLoggedIn()) {
                myself = SteamCommunityApplication.GetInstance().GetFriendInfoDB().GetFriendInfo(Long.valueOf(SteamWebApi.GetLoginSteamID()));
                nameView.setText(R.string.My_Profile);
            } else {
                nameView.setText(R.string.Login);
            }
            statusView.setText("");
            avatarView.setImageResource(R.drawable.placeholder_contact);
            boolean bOnline = false;
            if (myself != null && myself.HasPresentationData()) {
                AndroidUtils.setTextViewText(nameView, myself.m_personaName);
                myself.IsAvatarSmallLoaded();
                avatarView.setImageBitmap(myself.GetAvatarSmall());
                SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
                UmqConnectionState eState = UmqConnectionState.offline;
                if (dbService != null) {
                    eState = dbService.getSteamUmqConnectionState();
                }
                statusView.setText(eState.getStringResid());
                if (eState.isConnected()) {
                    bOnline = true;
                    avatarViewFrame.setImageResource(R.drawable.avatar_frame_online);
                }
            }
            avatarViewFrame.setImageResource(bOnline ? R.drawable.avatar_frame_online : R.drawable.avatar_frame_offline);
            int clrOnline = getActivity().getResources().getColor(bOnline ? R.color.online : R.color.offline);
            nameView.setTextColor(clrOnline);
            statusView.setTextColor(clrOnline);
        }
    }

    public void onFragmentActivityResult(int requestCode, int resultCode, Intent intent) {
        if (getActivity() != null) {
            if (resultCode == -1) {
                Uri uri = (Uri) intent.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI");
                if (uri != null) {
                    String ringtone = uri.toString();
                    if (requestCode >= 0 && requestCode < this.m_settingsInfoArray.size()) {
                        SettingInfo settingInfo = (SettingInfo) this.m_settingsInfoArray.get(requestCode);
                        if (settingInfo.m_type == SettingType.RINGTONESELECTOR) {
                            if (uri.equals(System.DEFAULT_NOTIFICATION_URI)) {
                                ringtone = settingInfo.m_defaultValue;
                            }
                            settingInfo.setValueAndCommit(this.m_owner.getApplicationContext(), ringtone);
                        }
                    }
                }
            }
            refreshListView();
        }
    }

    public void refreshListView() {
        this.m_bLoggedOnPresentation = SteamWebApi.IsLoggedIn();
        this.m_settingsInfoArray.clear();
        setupUserAccountView(this.m_viewProfile);
        ((Button) getActivity().findViewById(R.id.settingsFragment_AccountControl_ChangeUser)).setText(this.m_bLoggedOnPresentation ? R.string.Sign_Out : R.string.Login);
        SteamDBService steamDb = SteamCommunityApplication.GetInstance().GetSteamDB();
        SettingInfoDB settingInfoDb = SteamCommunityApplication.GetInstance().GetSettingInfoDB();
        this.m_settingsInfoArray.addAll(settingInfoDb.GetSettingsList());
        int j = this.m_settingsInfoArray.size();
        while (true) {
            int j2 = j - 1;
            if (j > 0) {
                boolean bValid = false;
                SettingInfo settingInfo = (SettingInfo) this.m_settingsInfoArray.get(j2);
                switch (AnonymousClass_4.$SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$AccessRight[settingInfo.m_access.ordinal()]) {
                    case UriData.RESULT_INVALID_CONTENT:
                        bValid = true;
                        break;
                    case UriData.RESULT_HTTP_EXCEPTION:
                        bValid = this.m_bLoggedOnPresentation;
                        break;
                    case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                        if (settingInfo == settingInfoDb.m_settingSslUntrustedPrompt) {
                            bValid = settingInfo.getRadioSelectorItemValue(this.m_owner.getApplicationContext()).value == -1;
                        }
                        break;
                }
                if (bValid) {
                    switch (AnonymousClass_4.$SwitchMap$com$valvesoftware$android$steam$community$SettingInfo$SettingType[settingInfo.m_type.ordinal()]) {
                        case UriData.RESULT_INVALID_CONTENT:
                        case UriData.RESULT_HTTP_EXCEPTION:
                        case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                        case AccessibilityNodeInfoCompat.ACTION_SELECT:
                        case MotionEventCompat.ACTION_POINTER_DOWN:
                        case MotionEventCompat.ACTION_POINTER_UP:
                        case MotionEventCompat.ACTION_HOVER_MOVE:
                            break;
                        case AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION:
                            if (steamDb == null || !SteamWebApi.IsLoggedIn() || steamDb.getSteamUmqCommunicationServiceDB().selectCountOfUnreadMessages(SteamWebApi.GetLoginSteamID()) <= 0) {
                                bValid = false;
                            } else {
                                bValid = true;
                            }
                            break;
                        default:
                            bValid = false;
                            break;
                    }
                }
                if (!bValid) {
                    this.m_settingsInfoArray.remove(j2);
                }
                j = j2;
            } else {
                this.m_SettingsAdapter.notifyDataSetChanged();
                return;
            }
        }
    }
}
