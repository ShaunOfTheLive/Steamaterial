package com.valvesoftware.android.steam.community.fragment;

import android.app.AlertDialog.Builder;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.ClipboardManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.text.style.URLSpan;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnLongClickListener;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AbsListView;
import android.widget.AbsListView.OnScrollListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import android.widget.Toast;
import com.valvesoftware.android.steam.community.C2DMReceiverService;
import com.valvesoftware.android.steam.community.FriendInfo;
import com.valvesoftware.android.steam.community.FriendInfo.FriendRelationship;
import com.valvesoftware.android.steam.community.FriendInfo.PersonaState;
import com.valvesoftware.android.steam.community.GenericListDB.ListItemUpdatedListener;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.Message;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UmqInfo;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UserConversationInfo;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SettingInfoDB;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_LOGININFO_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_MARKREADMESSAGES_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_SENDMESSAGE_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService.UmqConnectionState;
import com.valvesoftware.android.steam.community.SteamUriHandler.CommandProperty;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.fragment.ChatFragment.Layout;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment.TitlebarButtonHander;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import org.json.JSONException;
import org.json.JSONObject;

public class ChatFragment extends Fragment implements OnClickListener {
    private static Message m_msgHeadline;
    private static Message m_msgLoadMore;
    private static Message m_msgTyping;
    private static final String[] s_safeURIs;
    private boolean m_bCanSendMessages;
    private boolean m_bCanSendTypingNotification;
    private boolean m_bChatOptionsVisible;
    private boolean m_bPaused;
    private boolean m_bRequiresMarkMessagesAsRead;
    private ChatViewAdapter m_chatViewAdapter;
    private ListView m_chatViewContents;
    private Button m_chatViewMessageButton;
    private EditText m_chatViewMessageText;
    private ArrayList<Message> m_chatViewMessages;
    private TextView m_chatViewStatus;
    private Layout m_eKnownLayout;
    private ListItemUpdatedListener m_friendsListener;
    InputMethodManager m_inputMethodManager;
    private BroadcastReceiver m_intentReceiver;
    private LayoutInflater m_layoutInflater;
    private FriendInfo m_myself;
    private int m_numSecondsTimestamps;
    private TitlebarButtonHander m_optionsHandler;
    private FriendInfo m_partner;
    private String m_partnerSteamId;
    private ArrayList<Message> m_sentMsgs;
    private UserConversationInfo m_userConversationInfo;

    static /* synthetic */ class AnonymousClass_8 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState = new int[UmqConnectionState.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.offline.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private class ChatViewAdapter extends ArrayAdapter<Message> {
        private boolean m_bFilledUpFullView;
        private boolean m_bTyping;
        private LayoutInflater m_layoutInflater;
        private ArrayList<Message> m_list;
        private OnLongClickListener m_longClickHandler;
        private OnLongClickListener m_longClickLoadMore;
        private Message m_msgLastLoaded;
        private int m_numMessagesFromConversationInfoFetched;
        private OnScrollListener m_scrollLoadMore;

        class AnonymousClass_1 implements OnLongClickListener {
            final /* synthetic */ ChatFragment val$this$0;

            AnonymousClass_1(ChatFragment chatFragment) {
                this.val$this$0 = chatFragment;
            }

            public boolean onLongClick(View v) {
                try {
                    ClipboardManager clipBoard = (ClipboardManager) ChatFragment.this.getActivity().getSystemService("clipboard");
                    if (clipBoard != null && (v instanceof TextView)) {
                        clipBoard.setText(((TextView) v).getText().toString());
                        int[] screenpos = new int[]{0, 0};
                        v.getLocationOnScreen(screenpos);
                        Toast toast = Toast.makeText(ChatFragment.this.getActivity(), R.string.notification_chat_copied, 0);
                        toast.setGravity(49, 0, screenpos[1]);
                        toast.show();
                        return true;
                    }
                } catch (Exception e) {
                }
                return false;
            }
        }

        class AnonymousClass_2 implements OnLongClickListener {
            final /* synthetic */ ChatFragment val$this$0;

            AnonymousClass_2(ChatFragment chatFragment) {
                this.val$this$0 = chatFragment;
            }

            public boolean onLongClick(View v) {
                ChatViewAdapter.this.LoadMoreMessagesInView();
                return true;
            }
        }

        class AnonymousClass_3 implements OnScrollListener {
            final /* synthetic */ ListView val$lv;

            AnonymousClass_3(ListView listView) {
                this.val$lv = listView;
            }

            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                if (firstVisibleItem == 0) {
                    this.val$lv.postDelayed(new Runnable() {
                        public void run() {
                            if (!ChatFragment.this.m_chatViewMessages.isEmpty() && ChatFragment.this.m_chatViewMessages.get(ChatFragment.this.m_chatViewMessages.size() - 1) == ChatFragment.GetLoadMoreMessage() && ChatFragment.this.m_chatViewContents.getFirstVisiblePosition() == 0) {
                                ChatFragment.this.m_chatViewAdapter.LoadMoreMessagesInView();
                            }
                        }
                    }, 100);
                } else if (firstVisibleItem > 0) {
                    ChatViewAdapter.this.m_bFilledUpFullView = true;
                }
            }
        }

        public void MarkTyping(boolean bTyping) {
            if (this.m_bTyping != bTyping) {
                if (this.m_bTyping) {
                    this.m_list.remove(ChatFragment.GetTypingMessage());
                } else {
                    this.m_list.add(0, ChatFragment.GetTypingMessage());
                }
                this.m_bTyping = bTyping;
            }
        }

        public boolean LoadMoreMessages() {
            SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
            if (dbService == null) {
                return false;
            }
            ChatFragment.this.m_chatViewMessages.remove(ChatFragment.GetLoadMoreMessage());
            if (this.m_numMessagesFromConversationInfoFetched < ChatFragment.this.m_userConversationInfo.numMsgsTotal) {
                this.m_numMessagesFromConversationInfoFetched += 10;
                ChatFragment.this.m_chatViewMessages.addAll(dbService.getSteamUmqCommunicationServiceDB().selectMessagesWithUser(SteamWebApi.GetLoginSteamID(), ChatFragment.this.m_partnerSteamId, 10, this.m_msgLastLoaded));
                if (this.m_numMessagesFromConversationInfoFetched < ChatFragment.this.m_userConversationInfo.numMsgsTotal) {
                    this.m_msgLastLoaded = (Message) ChatFragment.this.m_chatViewMessages.get(ChatFragment.this.m_chatViewMessages.size() - 1);
                    ChatFragment.this.m_chatViewMessages.add(ChatFragment.GetLoadMoreMessage());
                    return true;
                }
            }
            ChatFragment.this.m_chatViewMessages.add(ChatFragment.GetHeadlineMessage());
            return false;
        }

        public boolean LoadMoreMessagesInView() {
            int iPosOld = ChatFragment.this.m_chatViewContents.getFirstVisiblePosition();
            int numOld = ChatFragment.this.m_chatViewMessages.size();
            if (this.m_bFilledUpFullView) {
                ChatFragment.this.m_chatViewContents.setStackFromBottom(false);
            }
            boolean bHasMore = LoadMoreMessages();
            notifyDataSetChanged();
            if (this.m_bFilledUpFullView) {
                ChatFragment.this.m_chatViewContents.setSelection((ChatFragment.this.m_chatViewMessages.size() + iPosOld) - numOld);
                ChatFragment.this.m_chatViewContents.setStackFromBottom(true);
            }
            return bHasMore;
        }

        public ChatViewAdapter(Context context, ArrayList<Message> list, LayoutInflater layoutInflater) {
            super(context, -1, list);
            this.m_numMessagesFromConversationInfoFetched = 0;
            this.m_msgLastLoaded = null;
            this.m_bTyping = false;
            this.m_longClickHandler = null;
            this.m_longClickLoadMore = null;
            this.m_scrollLoadMore = null;
            this.m_bFilledUpFullView = false;
            this.m_list = list;
            this.m_layoutInflater = layoutInflater;
            this.m_longClickHandler = new AnonymousClass_1(ChatFragment.this);
            this.m_longClickLoadMore = new AnonymousClass_2(ChatFragment.this);
        }

        public void attach(ListView lv) {
            lv.setAdapter(this);
            this.m_scrollLoadMore = new AnonymousClass_3(lv);
            lv.setOnScrollListener(this.m_scrollLoadMore);
        }

        public View getView(int position, View v, ViewGroup parent) {
            Message message = (Message) this.m_list.get((this.m_list.size() - 1) - position);
            TextView textView = null;
            if (v != null) {
                if (message.bIncoming) {
                    textView = v.findViewById(2131296271);
                    if (textView == null) {
                        v = null;
                    }
                } else {
                    textView = v.findViewById(2131296272);
                    if (textView == null) {
                        v = null;
                    }
                }
            }
            if (v == null) {
                if (message.bIncoming) {
                    v = (LinearLayout) this.m_layoutInflater.inflate(ChatFragment.this.m_eKnownLayout == Layout.TextOnly ? R.layout.chat_view_entry_other_text : R.layout.chat_view_entry_other, null);
                    textView = v.findViewById(2131296271);
                } else {
                    LayoutInflater layoutInflater = this.m_layoutInflater;
                    Object obj;
                    if (ChatFragment.this.m_eKnownLayout == Layout.TextOnly) {
                        obj = R.layout.chat_view_entry_user_text;
                    } else {
                        obj = ChatFragment.this.m_eKnownLayout == Layout.Bubbles ? R.layout.chat_view_entry_user : R.layout.chat_view_entry_user_left;
                    }
                    v = (LinearLayout) layoutInflater.inflate(r17, null);
                    textView = v.findViewById(2131296272);
                }
            }
            TextView labelView = (TextView) v.findViewById(2131296269);
            if (labelView != null) {
                boolean bRenderTimestamp = false;
                boolean bMessageIsReal = (message == ChatFragment.GetLoadMoreMessage() || message == ChatFragment.GetHeadlineMessage()) ? false : true;
                if (position == 0) {
                    bRenderTimestamp = bMessageIsReal;
                } else if (bMessageIsReal) {
                    Message msgPrev = (Message) this.m_list.get((this.m_list.size() - 1) - (position - 1));
                    if (msgPrev == ChatFragment.GetLoadMoreMessage() || msgPrev == ChatFragment.GetHeadlineMessage()) {
                        bRenderTimestamp = message.msgtime != null;
                    } else if (!(message.msgtime == null || msgPrev.msgtime == null)) {
                        long deltaTime = message.msgtime.getTimeInMillis() - msgPrev.msgtime.getTimeInMillis();
                        Calendar cal1 = Calendar.getInstance();
                        cal1.setTimeInMillis(msgPrev.msgtime.getTimeInMillis());
                        cal1.set(11, 0);
                        cal1.set(12, 0);
                        cal1.set(13, 0);
                        cal1.set(14, 0);
                        Calendar cal2 = Calendar.getInstance();
                        cal2.setTimeInMillis(message.msgtime.getTimeInMillis());
                        cal2.set(11, 0);
                        cal2.set(12, 0);
                        cal2.set(13, 0);
                        cal2.set(14, 0);
                        boolean bDateIncluded = cal1.getTimeInMillis() != cal2.getTimeInMillis();
                        bRenderTimestamp = ChatFragment.this.m_numSecondsTimestamps <= 0 ? bDateIncluded : deltaTime > ((long) (ChatFragment.this.m_numSecondsTimestamps * 1000)) || (ChatFragment.this.m_numSecondsTimestamps == 86400 && bDateIncluded);
                    }
                }
                if (!bRenderTimestamp || message.msgtime == null) {
                    labelView.setVisibility(8);
                } else {
                    DateFormat fmtTimestamp;
                    switch (ChatFragment.this.m_numSecondsTimestamps) {
                        case SettingInfoDB.SETTING_NOTIFY_FIRST:
                        case 86400:
                            fmtTimestamp = SimpleDateFormat.getDateInstance(1);
                            break;
                        default:
                            fmtTimestamp = SimpleDateFormat.getDateTimeInstance(1, FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER);
                            break;
                    }
                    labelView.setText(fmtTimestamp.format(message.msgtime.getTime()));
                    labelView.setVisibility(0);
                }
            }
            ImageView avatarView = (ImageView) v.findViewById(2131296270);
            if (avatarView != null) {
                if (message == ChatFragment.GetHeadlineMessage()) {
                    avatarView.setImageResource(2130837540);
                } else {
                    avatarView.setImageBitmap((message.bIncoming ? ChatFragment.this.m_partner : ChatFragment.this.m_myself).GetAvatarSmall());
                }
            }
            ChatFragment.this.FormatMessageText(message, textView);
            if (message == ChatFragment.GetLoadMoreMessage()) {
                textView.setOnLongClickListener(this.m_longClickLoadMore);
            } else {
                textView.setOnLongClickListener(this.m_longClickHandler);
            }
            return v;
        }

        public View getDropDownView(int position, View convertView, ViewGroup parent) {
            return getView(position, convertView, parent);
        }
    }

    public enum Layout {
        Bubbles,
        BubblesLeft,
        TextOnly
    }

    private static class ParsedMessageData {
        String text;
        String type;

        private ParsedMessageData() {
        }
    }

    private class UnsafeClickableURL extends URLSpan {
        private boolean m_bShowUnsafeWarning;

        class AnonymousClass_1 implements DialogInterface.OnClickListener {
            final /* synthetic */ View val$finalView;

            AnonymousClass_1(View view) {
                this.val$finalView = view;
            }

            public void onClick(DialogInterface dialog, int which) {
                UnsafeClickableURL.this.HandleUserProcceedSelected(this.val$finalView);
            }
        }

        public UnsafeClickableURL(URLSpan other, boolean bShowUnsafeWarning) {
            super(other.getURL());
            this.m_bShowUnsafeWarning = false;
            this.m_bShowUnsafeWarning = bShowUnsafeWarning;
        }

        public void HandleUserProcceedSelected(View v) {
            try {
                super.onClick(v);
            } catch (Exception e) {
            }
        }

        public void onClick(View v) {
            if (this.m_bShowUnsafeWarning) {
                View finalView = v;
                Builder builder = new Builder(ChatFragment.this.getActivity());
                builder.setTitle(R.string.nonsteam_link_title);
                builder.setMessage(ChatFragment.this.getActivity().getString(R.string.nonsteam_link_text) + "\n\n" + getURL());
                builder.setPositiveButton(R.string.nonsteam_link_ok, new AnonymousClass_1(finalView));
                builder.setNegativeButton(R.string.Cancel, null);
                builder.create().show();
                return;
            }
            HandleUserProcceedSelected(v);
        }
    }

    public ChatFragment() {
        this.m_eKnownLayout = Layout.Bubbles;
        this.m_numSecondsTimestamps = 900;
        this.m_layoutInflater = null;
        this.m_inputMethodManager = null;
        this.m_chatViewContents = null;
        this.m_chatViewAdapter = null;
        this.m_userConversationInfo = null;
        this.m_chatViewMessages = null;
        this.m_chatViewStatus = null;
        this.m_chatViewMessageText = null;
        this.m_chatViewMessageButton = null;
        this.m_bCanSendMessages = false;
        this.m_bCanSendTypingNotification = true;
        this.m_bPaused = true;
        this.m_bRequiresMarkMessagesAsRead = false;
        this.m_intentReceiver = new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (ChatFragment.this.getActivity() != null) {
                    ChatFragment.this.OnChatUpdated(intent);
                }
            }
        };
        this.m_sentMsgs = new ArrayList();
        this.m_optionsHandler = new TitlebarButtonHander() {
            public void onTitlebarButtonClicked(int id) {
                if (SteamWebApi.IsLoggedIn()) {
                    ChatFragment.this.toggleOptions();
                }
            }
        };
        this.m_bChatOptionsVisible = false;
        this.m_friendsListener = new ListItemUpdatedListener() {
            public void OnListRefreshError(RequestBase req, boolean cached) {
            }

            public void OnListItemInfoUpdated(ArrayList<Long> arrayList, boolean requireRefresh) {
                ChatFragment.this.UpdateStatusBar();
            }

            public void OnListItemInfoUpdateError(RequestBase req) {
            }

            public void OnListRequestsInProgress(boolean isRefreshing) {
            }
        };
    }

    private String GetDebugName() {
        return getClass().getSimpleName() + "/" + this.m_partnerSteamId + "(" + (this.m_partner != null ? this.m_partner.GetPersonaNameSafe() : "<null>") + ")";
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.chat_fragment, container, false);
    }

    private void SetPartnerDataFromCachedJSON(String chatPartnerSteamID) {
        byte[] data = SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite().Read(chatPartnerSteamID);
        JSONObject obj;
        String jsonName;
        String jsonAvatar;
        if (data != null) {
            if (data != null) {
                try {
                    obj = new JSONObject(new String(data));
                    jsonName = obj.optString("personaname");
                    if (jsonName != null && this.m_partner.m_personaName == null) {
                        this.m_partner.m_personaName = jsonName;
                    }
                    jsonAvatar = obj.optString("avatar");
                    if (jsonAvatar != null && this.m_partner.m_avatarSmallURL == null) {
                        this.m_partner.m_avatarSmallURL = jsonAvatar;
                    }
                } catch (JSONException e) {
                }
            }
        } else if (data != null) {
            obj = new JSONObject(new String(data));
            jsonName = obj.optString("personaname");
            this.m_partner.m_personaName = jsonName;
            jsonAvatar = obj.optString("avatar");
            if (jsonAvatar != null) {
            }
        }
    }

    private void PrepareParticipantsInformation() {
        Long longMySteamID;
        String chatPartnerSteamID = this.m_partnerSteamId;
        Long steamID = Long.valueOf(chatPartnerSteamID);
        FriendInfo info = SteamCommunityApplication.GetInstance().GetFriendInfoDB().GetFriendInfo(steamID);
        if (info != null && info.HasPresentationData()) {
            this.m_partner = info;
        }
        if (this.m_partner == null) {
            this.m_partner = new FriendInfo();
            this.m_partner.m_steamID = steamID;
            this.m_partner.m_relationship = FriendRelationship.friend;
            this.m_partner.m_personaState = PersonaState.ONLINE;
            SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
            if (dbService != null) {
                UmqInfo umqinfo = dbService.getSteamUmqCommunicationServiceDB().selectInfo(chatPartnerSteamID);
                if (!(umqinfo == null || umqinfo.name == null)) {
                    this.m_partner.m_personaName = umqinfo.name;
                }
            }
            SetPartnerDataFromCachedJSON(chatPartnerSteamID);
        }
        if (this.m_partner.IsAvatarSmallLoaded()) {
            longMySteamID = Long.valueOf(SteamWebApi.GetLoginSteamID());
            this.m_myself = SteamCommunityApplication.GetInstance().GetFriendInfoDB().GetFriendInfo(longMySteamID);
        } else {
            longMySteamID = Long.valueOf(SteamWebApi.GetLoginSteamID());
            this.m_myself = SteamCommunityApplication.GetInstance().GetFriendInfoDB().GetFriendInfo(longMySteamID);
        }
        if (this.m_myself == null) {
            this.m_myself = new FriendInfo();
            this.m_partner.m_steamID = longMySteamID;
            this.m_partner.m_relationship = FriendRelationship.myself;
        }
        if (!this.m_myself.IsAvatarSmallLoaded()) {
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FragmentActivity act = getActivity();
        if (act != null) {
            UpdateKnownLayoutFromSettings();
            this.m_partnerSteamId = act.getIntent().getStringExtra(CommandProperty.steamid.toString());
            this.m_layoutInflater = (LayoutInflater) act.getSystemService("layout_inflater");
            this.m_inputMethodManager = (InputMethodManager) act.getSystemService("input_method");
            this.m_chatViewContents = (ListView) act.findViewById(R.id.chat_view_contents);
            this.m_chatViewContents.setTranscriptMode(UriData.RESULT_HTTP_EXCEPTION);
            this.m_chatViewContents.setStackFromBottom(true);
            this.m_chatViewStatus = (TextView) act.findViewById(R.id.chat_view_status);
            this.m_chatViewMessageText = (EditText) act.findViewById(R.id.chat_view_say_text);
            this.m_chatViewMessageButton = (Button) act.findViewById(R.id.chat_view_say_button);
        }
        ControlsSetup();
    }

    private void UpdateKnownLayoutFromSettings() {
        Layout eLayout = Layout.Bubbles;
        int layoutSetting = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingChatsLayout.getRadioSelectorItemValue(SteamCommunityApplication.GetInstance().getApplicationContext()).value;
        if (layoutSetting == Layout.BubblesLeft.ordinal()) {
            eLayout = Layout.BubblesLeft;
        } else if (layoutSetting == Layout.TextOnly.ordinal()) {
            eLayout = Layout.TextOnly;
        }
        this.m_eKnownLayout = eLayout;
        this.m_numSecondsTimestamps = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingChatsTimestamp.getRadioSelectorItemValue(SteamCommunityApplication.GetInstance().getApplicationContext()).value;
    }

    static {
        s_safeURIs = new String[]{"steampowered.com", "steamgames.com", "steamcommunity.com", "valvesoftware.com", "youtube.com", "live.com", "msn.com", "myspace.com", "facebook.com", "hi5.com", "wikipedia.org", "orkut.com", "rapidshare.com", "blogger.com", "megaupload.com", "friendster.com", "fotolog.net", "google.fr", "baidu.com", "microsoft.com", "ebay.com", "shacknews.com", "bbc.co.uk", "cnn.com", "foxsports.com", "pcmag.com", "nytimes.com", "flickr.com", "amazon.com", "veoh.com", "pcgamer.com", "metacritic.com", "fileplanet.com", "gamespot.com", "gametap.com", "ign.com", "kotaku.com", "xfire.com", "pcgames.gwn.com", "gamezone.com", "gamesradar.com", "digg.com", "engadget.com", "gizmodo.com", "gamesforwindows.com", "xbox.com", "cnet.com", "l4d.com", "teamfortress.com", "tf2.com", "half-life2.com", "aperturescience.com", "dayofdefeat.com", "dota2.com", "steamtranslation.ru", "playdota.com"};
        m_msgTyping = null;
        m_msgLoadMore = null;
        m_msgHeadline = null;
    }

    public static boolean isUrlUnsafe(URLSpan other) {
        String surl = other.getURL();
        if (surl.startsWith("tel:") || surl.startsWith("mailto:") || surl.startsWith("geo:")) {
            return false;
        }
        int nPrefixLength;
        if (surl.startsWith("http://") || surl.startsWith("rtsp://")) {
            nPrefixLength = MotionEventCompat.ACTION_HOVER_MOVE;
        } else if (!surl.startsWith("https://")) {
            return true;
        } else {
            nPrefixLength = AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION;
        }
        char[] end = new char[]{':', '?', '/'};
        int nEndPos = surl.length();
        for (char c : end) {
            int nFoundPos = surl.indexOf(c, nPrefixLength);
            if (nFoundPos >= 0 && nFoundPos < nEndPos) {
                nEndPos = nFoundPos;
            }
        }
        String sDomain = surl.substring(nPrefixLength, nEndPos);
        int j = 0;
        while (j < s_safeURIs.length) {
            if (sDomain.endsWith(s_safeURIs[j])) {
                if (sDomain.length() <= s_safeURIs[j].length() || sDomain.charAt((sDomain.length() - s_safeURIs[j].length()) - 1) == '.') {
                    return false;
                }
            }
            j++;
        }
        return true;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void FormatMessageText(com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.Message r17_message, android.widget.TextView r18_text) {
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.android.steam.community.fragment.ChatFragment.FormatMessageText(com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase$Message, android.widget.TextView):void");
        /*
        this = this;
        r9 = 0;
        r0 = r16;
        r13 = r0.m_numSecondsTimestamps;	 Catch:{ Exception -> 0x0192 }
        if (r13 > 0) goto L_0x0041;
    L_0x0007:
        r13 = GetLoadMoreMessage();	 Catch:{ Exception -> 0x0192 }
        r0 = r17;
        if (r0 == r13) goto L_0x0041;
    L_0x000f:
        r13 = GetHeadlineMessage();	 Catch:{ Exception -> 0x0192 }
        r0 = r17;
        if (r0 == r13) goto L_0x0041;
    L_0x0017:
        r0 = r17;
        r13 = r0.msgtime;	 Catch:{ Exception -> 0x0192 }
        if (r13 == 0) goto L_0x0041;
    L_0x001d:
        r13 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x0192 }
        r13.<init>();	 Catch:{ Exception -> 0x0192 }
        r14 = 3;
        r14 = java.text.SimpleDateFormat.getTimeInstance(r14);	 Catch:{ Exception -> 0x0192 }
        r0 = r17;
        r15 = r0.msgtime;	 Catch:{ Exception -> 0x0192 }
        r15 = r15.getTime();	 Catch:{ Exception -> 0x0192 }
        r14 = r14.format(r15);	 Catch:{ Exception -> 0x0192 }
        r13 = r13.append(r14);	 Catch:{ Exception -> 0x0192 }
        r14 = " : ";
        r13 = r13.append(r14);	 Catch:{ Exception -> 0x0192 }
        r9 = r13.toString();	 Catch:{ Exception -> 0x0192 }
    L_0x0041:
        if (r9 == 0) goto L_0x00ca;
    L_0x0043:
        r13 = new java.lang.StringBuilder;	 Catch:{ Exception -> 0x00ea }
        r13.<init>();	 Catch:{ Exception -> 0x00ea }
        r13 = r13.append(r9);	 Catch:{ Exception -> 0x00ea }
        r0 = r17;
        r14 = r0.bindata;	 Catch:{ Exception -> 0x00ea }
        r13 = r13.append(r14);	 Catch:{ Exception -> 0x00ea }
        r13 = r13.toString();	 Catch:{ Exception -> 0x00ea }
        r10 = android.text.SpannableString.valueOf(r13);	 Catch:{ Exception -> 0x00ea }
    L_0x005c:
        r13 = 15;
        android.text.util.Linkify.addLinks(r10, r13);	 Catch:{ Exception -> 0x00ea }
        r13 = 0;
        r14 = r10.length();	 Catch:{ Exception -> 0x00ea }
        r15 = java.lang.Object.class;
        r11 = r10.getSpans(r13, r14, r15);	 Catch:{ Exception -> 0x00ea }
        if (r11 == 0) goto L_0x0136;
    L_0x006e:
        r13 = r11.length;	 Catch:{ Exception -> 0x00ea }
        if (r13 <= 0) goto L_0x0136;
    L_0x0071:
        r13 = com.valvesoftware.android.steam.community.SteamCommunityApplication.GetInstance();	 Catch:{ Exception -> 0x00ea }
        r13 = r13.GetSettingInfoDB();	 Catch:{ Exception -> 0x00ea }
        r13 = r13.m_settingChatsAlertLinks;	 Catch:{ Exception -> 0x00ea }
        r14 = com.valvesoftware.android.steam.community.SteamCommunityApplication.GetInstance();	 Catch:{ Exception -> 0x00ea }
        r14 = r14.getApplicationContext();	 Catch:{ Exception -> 0x00ea }
        r1 = r13.getBooleanValue(r14);	 Catch:{ Exception -> 0x00ea }
        r5 = 0;
    L_0x0088:
        r13 = r11.length;	 Catch:{ Exception -> 0x00ea }
        if (r5 >= r13) goto L_0x0136;
    L_0x008b:
        r12 = r11[r5];	 Catch:{ Exception -> 0x00ea }
        r8 = r10.getSpanStart(r12);	 Catch:{ Exception -> 0x00ea }
        r7 = r10.getSpanEnd(r12);	 Catch:{ Exception -> 0x00ea }
        r4 = r10.getSpanFlags(r12);	 Catch:{ Exception -> 0x00ea }
        r13 = r12 instanceof android.text.style.URLSpan;	 Catch:{ Exception -> 0x00ea }
        if (r13 == 0) goto L_0x00d5;
    L_0x009d:
        r10.removeSpan(r12);	 Catch:{ Exception -> 0x00ea }
        if (r9 == 0) goto L_0x00ac;
    L_0x00a2:
        r13 = r9.length();	 Catch:{ Exception -> 0x00ea }
        if (r8 >= r13) goto L_0x00ac;
    L_0x00a8:
        r8 = r9.length();	 Catch:{ Exception -> 0x00ea }
    L_0x00ac:
        if (r7 <= r8) goto L_0x00c7;
    L_0x00ae:
        r15 = new com.valvesoftware.android.steam.community.fragment.ChatFragment$UnsafeClickableURL;	 Catch:{ Exception -> 0x00ea }
        r0 = r12;
        r0 = (android.text.style.URLSpan) r0;	 Catch:{ Exception -> 0x00ea }
        r13 = r0;
        if (r1 == 0) goto L_0x00d3;
    L_0x00b6:
        r12 = (android.text.style.URLSpan) r12;	 Catch:{ Exception -> 0x00ea }
        r14 = isUrlUnsafe(r12);	 Catch:{ Exception -> 0x00ea }
        if (r14 == 0) goto L_0x00d3;
    L_0x00be:
        r14 = 1;
    L_0x00bf:
        r0 = r16;
        r15.<init>(r13, r14);	 Catch:{ Exception -> 0x00ea }
        r10.setSpan(r15, r8, r7, r4);	 Catch:{ Exception -> 0x00ea }
    L_0x00c7:
        r5 = r5 + 1;
        goto L_0x0088;
    L_0x00ca:
        r0 = r17;
        r13 = r0.bindata;	 Catch:{ Exception -> 0x00ea }
        r10 = android.text.SpannableString.valueOf(r13);	 Catch:{ Exception -> 0x00ea }
        goto L_0x005c;
    L_0x00d3:
        r14 = 0;
        goto L_0x00bf;
    L_0x00d5:
        if (r9 == 0) goto L_0x00c7;
    L_0x00d7:
        r13 = r9.length();	 Catch:{ Exception -> 0x00ea }
        if (r8 >= r13) goto L_0x00c7;
    L_0x00dd:
        r8 = r9.length();	 Catch:{ Exception -> 0x00ea }
        r10.removeSpan(r12);	 Catch:{ Exception -> 0x00ea }
        if (r7 <= r8) goto L_0x00c7;
    L_0x00e6:
        r10.setSpan(r12, r8, r7, r4);	 Catch:{ Exception -> 0x00ea }
        goto L_0x00c7;
    L_0x00ea:
        r3 = move-exception;
        if (r9 == 0) goto L_0x0158;
    L_0x00ed:
        r13 = new java.lang.StringBuilder;
        r13.<init>();
        r13 = r13.append(r9);
        r0 = r17;
        r14 = r0.bindata;
        r13 = r13.append(r14);
        r13 = r13.toString();
        r0 = r18;
        com.valvesoftware.android.steam.community.AndroidUtils.setTextViewText(r0, r13);
    L_0x0107:
        r0 = r17;
        r13 = r0.bIncoming;
        if (r13 != 0) goto L_0x0135;
    L_0x010d:
        r13 = GetHeadlineMessage();
        r0 = r17;
        if (r0 != r13) goto L_0x0115;
    L_0x0115:
        r13 = GetLoadMoreMessage();
        r0 = r17;
        if (r0 != r13) goto L_0x0162;
    L_0x011d:
        r2 = 2131099678; // 0x7f06001e float:1.7811716E38 double:1.052903139E-314;
    L_0x0120:
        r13 = com.valvesoftware.android.steam.community.SteamCommunityApplication.GetInstance();
        r13 = r13.getApplicationContext();
        r13 = r13.getResources();
        r13 = r13.getColor(r2);
        r0 = r18;
        r0.setTextColor(r13);
    L_0x0135:
        return;
    L_0x0136:
        r0 = r18;
        r0.setText(r10);	 Catch:{ Exception -> 0x014f }
        r6 = r18.getMovementMethod();	 Catch:{ Exception -> 0x014f }
        if (r6 == 0) goto L_0x0145;
    L_0x0141:
        r13 = r6 instanceof android.text.method.LinkMovementMethod;	 Catch:{ Exception -> 0x014f }
        if (r13 != 0) goto L_0x0107;
    L_0x0145:
        r13 = android.text.method.LinkMovementMethod.getInstance();	 Catch:{ Exception -> 0x014f }
        r0 = r18;
        r0.setMovementMethod(r13);	 Catch:{ Exception -> 0x014f }
        goto L_0x0107;
    L_0x014f:
        r3 = move-exception;
        r13 = "";
        r0 = r18;
        r0.setText(r13);	 Catch:{ Exception -> 0x00ea }
        goto L_0x0107;
    L_0x0158:
        r0 = r17;
        r13 = r0.bindata;
        r0 = r18;
        com.valvesoftware.android.steam.community.AndroidUtils.setTextViewText(r0, r13);
        goto L_0x0107;
    L_0x0162:
        r0 = r17;
        r13 = r0.msgtime;
        if (r13 != 0) goto L_0x0182;
    L_0x0168:
        r0 = r17;
        r13 = r0.bUnread;
        if (r13 != 0) goto L_0x017e;
    L_0x016e:
        r0 = r16;
        r13 = r0.m_eKnownLayout;
        r14 = com.valvesoftware.android.steam.community.fragment.ChatFragment.Layout.TextOnly;
        if (r13 != r14) goto L_0x017a;
    L_0x0176:
        r2 = 2131099676; // 0x7f06001c float:1.7811712E38 double:1.052903138E-314;
    L_0x0179:
        goto L_0x0120;
    L_0x017a:
        r2 = 2131099672; // 0x7f060018 float:1.7811704E38 double:1.052903136E-314;
        goto L_0x0179;
    L_0x017e:
        r2 = 2131099674; // 0x7f06001a float:1.7811708E38 double:1.052903137E-314;
        goto L_0x0120;
    L_0x0182:
        r0 = r16;
        r13 = r0.m_eKnownLayout;
        r14 = com.valvesoftware.android.steam.community.fragment.ChatFragment.Layout.TextOnly;
        if (r13 != r14) goto L_0x018e;
    L_0x018a:
        r2 = 2131099677; // 0x7f06001d float:1.7811714E38 double:1.0529031383E-314;
    L_0x018d:
        goto L_0x0120;
    L_0x018e:
        r2 = 2131099673; // 0x7f060019 float:1.7811706E38 double:1.0529031363E-314;
        goto L_0x018d;
    L_0x0192:
        r13 = move-exception;
        goto L_0x0041;
        */
    }

    private static Message GetTypingMessage() {
        if (m_msgTyping == null) {
            m_msgTyping = new Message();
            m_msgTyping.bIncoming = true;
            m_msgTyping.bindata = "...";
            m_msgTyping.bUnread = false;
            m_msgTyping.msgtype = "typing";
            m_msgTyping.id = 0;
            m_msgTyping.msgtime = null;
            m_msgTyping.sMySteamID = null;
            m_msgTyping.sWithSteamID = null;
        }
        return m_msgTyping;
    }

    private static Message GetLoadMoreMessage() {
        if (m_msgLoadMore == null) {
            m_msgLoadMore = new Message();
            m_msgLoadMore.bIncoming = false;
            m_msgLoadMore.bindata = SteamCommunityApplication.GetInstance().getString(R.string.Chat_Load_More_Msgs);
            m_msgLoadMore.bUnread = false;
            m_msgLoadMore.msgtype = "";
            m_msgLoadMore.id = 0;
            m_msgLoadMore.msgtime = null;
            m_msgLoadMore.sMySteamID = null;
            m_msgLoadMore.sWithSteamID = null;
        }
        return m_msgLoadMore;
    }

    private static Message GetHeadlineMessage() {
        if (m_msgHeadline == null) {
            m_msgHeadline = new Message();
            m_msgHeadline.bIncoming = true;
            m_msgHeadline.bindata = SteamCommunityApplication.GetInstance().getString(R.string.Chat_Headline_Message);
            m_msgHeadline.bUnread = false;
            m_msgHeadline.msgtype = "";
            m_msgHeadline.id = 0;
            m_msgHeadline.msgtime = null;
            m_msgHeadline.sMySteamID = null;
            m_msgHeadline.sWithSteamID = null;
        }
        return m_msgHeadline;
    }

    private void SendTypingNotification() {
        if (this.m_bCanSendTypingNotification) {
            this.m_bCanSendTypingNotification = false;
            REQ_ACT_SENDMESSAGE_DATA out = new REQ_ACT_SENDMESSAGE_DATA();
            out.mylogin = new REQ_ACT_LOGININFO_DATA();
            out.mylogin.sOAuthToken = SteamWebApi.GetLoginAccessToken();
            out.mylogin.sSteamID = SteamWebApi.GetLoginSteamID();
            out.msg = new Message();
            out.msg.sMySteamID = out.mylogin.sSteamID;
            out.msg.sWithSteamID = this.m_partnerSteamId;
            out.msg.bIncoming = false;
            out.msg.bUnread = false;
            out.msg.msgtime = null;
            out.msg.msgtype = "typing";
            out.msg.bindata = "";
            out.intentcontext = String.valueOf(System.identityHashCode(out.msg));
            SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_SENDMESSAGE, out);
        }
    }

    private boolean ParseMessageFromTextView(ParsedMessageData results) {
        String msgText = this.m_chatViewMessageText.getText().toString().trim();
        String msgType = "saytext";
        if (msgText.startsWith("/me ")) {
            msgText = msgText.substring(AccessibilityNodeInfoCompat.ACTION_SELECT).trim();
            msgType = "emote";
        }
        if (msgText.length() <= 0) {
            return false;
        }
        if (results != null) {
            results.text = msgText;
            results.type = msgType;
        }
        return true;
    }

    private void UpdateSendButton() {
        Button button = this.m_chatViewMessageButton;
        boolean z = this.m_bCanSendMessages && ParseMessageFromTextView(null);
        button.setEnabled(z);
    }

    private void ControlsSetup() {
        FragmentActivity act = getActivity();
        this.m_chatViewMessages = new ArrayList();
        if (act != null) {
            this.m_chatViewAdapter = new ChatViewAdapter(act, this.m_chatViewMessages, this.m_layoutInflater);
            this.m_chatViewAdapter.attach(this.m_chatViewContents);
        }
        View vClearChatHistory = act != null ? act.findViewById(R.id.chat_options_clear_history_button) : null;
        if (vClearChatHistory != null) {
            vClearChatHistory.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    ChatFragment.this.clearChatHistory();
                }
            });
        }
        this.m_chatViewMessageButton.setOnClickListener(this);
        this.m_chatViewMessageText.addTextChangedListener(new TextWatcher() {
            public void afterTextChanged(Editable s) {
                ChatFragment.this.UpdateSendButton();
                ChatFragment.this.SendTypingNotification();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        });
        this.m_chatViewMessageText.setOnEditorActionListener(new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == 4 || actionId == 6 || actionId == 0) {
                    ChatFragment.this.onClick(ChatFragment.this.m_chatViewMessageButton);
                }
                return true;
            }
        });
        this.m_chatViewMessageText.post(new Runnable() {
            public void run() {
                ChatFragment.this.m_chatViewMessageText.requestFocusFromTouch();
            }
        });
    }

    public void onDestroy() {
        super.onDestroy();
    }

    public void onResume() {
        super.onResume();
        if (SteamWebApi.IsLoggedIn()) {
            UpdateKnownLayoutFromSettings();
            PrepareParticipantsInformation();
            SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
            this.m_bPaused = false;
            if (this.m_bRequiresMarkMessagesAsRead) {
                this.m_bRequiresMarkMessagesAsRead = false;
                MarkMessagesAsReadWithUser();
            }
            if (getActivity() != null) {
                getActivity().registerReceiver(this.m_intentReceiver, new IntentFilter(SteamUmqCommunicationService.INTENT_ACTION));
            }
            SteamCommunityApplication.GetInstance().GetFriendInfoDB().RegisterCallback(this.m_friendsListener);
            UpdateView();
            SteamCommunityApplication.GetInstance().GetFriendInfoDB().setActiveChatPartnerSteamId(this.m_partnerSteamId);
        } else if (getActivity() != null) {
            getActivity().finish();
        }
    }

    public void onPause() {
        SteamCommunityApplication.GetInstance().GetFriendInfoDB().setActiveChatPartnerSteamId(null);
        super.onPause();
        if (!this.m_bPaused) {
            this.m_bPaused = true;
            SteamCommunityApplication.GetInstance().GetFriendInfoDB().DeregisterCallback(this.m_friendsListener);
            if (getActivity() != null) {
                getActivity().unregisterReceiver(this.m_intentReceiver);
            }
        }
    }

    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.chat_view_say_button:
                ParsedMessageData parsedMsgData = new ParsedMessageData();
                if (this.m_bCanSendMessages && ParseMessageFromTextView(parsedMsgData)) {
                    REQ_ACT_SENDMESSAGE_DATA out = new REQ_ACT_SENDMESSAGE_DATA();
                    out.mylogin = new REQ_ACT_LOGININFO_DATA();
                    out.mylogin.sOAuthToken = SteamWebApi.GetLoginAccessToken();
                    out.mylogin.sSteamID = SteamWebApi.GetLoginSteamID();
                    out.msg = new Message();
                    out.msg.sMySteamID = out.mylogin.sSteamID;
                    out.msg.sWithSteamID = this.m_partnerSteamId;
                    out.msg.bIncoming = false;
                    out.msg.bUnread = false;
                    out.msg.msgtime = null;
                    out.msg.msgtype = parsedMsgData.type;
                    out.msg.bindata = parsedMsgData.text;
                    out.intentcontext = String.valueOf(System.identityHashCode(out.msg));
                    this.m_sentMsgs.add(out.msg);
                    this.m_chatViewMessages.add(0, out.msg);
                    this.m_chatViewAdapter.notifyDataSetChanged();
                    ScrollToBottom(true);
                    SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_SENDMESSAGE, out);
                    ClearMessageBox();
                    this.m_bCanSendTypingNotification = true;
                }
            default:
                break;
        }
    }

    private void clearChatHistory() {
        REQ_ACT_MARKREADMESSAGES_DATA obj = new REQ_ACT_MARKREADMESSAGES_DATA();
        obj.mysteamid = SteamWebApi.GetLoginSteamID();
        obj.withsteamid = this.m_partnerSteamId;
        obj.deleteAllMessages = true;
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_MARKREADMESSAGES, obj);
        Intent intent = new Intent(SteamUmqCommunicationService.INTENT_ACTION);
        intent.putExtra("type", "chatmsg");
        intent.putExtra("action", "clear");
        intent.putExtra("steamid", this.m_partnerSteamId);
        SteamCommunityApplication.GetInstance().getApplicationContext().sendBroadcast(intent);
    }

    private void ClearMessageBox() {
        this.m_chatViewMessageText.setText("");
        this.m_chatViewMessageButton.setEnabled(false);
    }

    private void ScrollToBottom() {
        ScrollToBottom(false);
    }

    private void ScrollToBottom(boolean bForce) {
        this.m_chatViewContents.setSelection(this.m_chatViewMessages.size() - 1);
    }

    private void toggleOptions() {
        boolean z;
        int i = 0;
        if (this.m_bChatOptionsVisible) {
            z = false;
        } else {
            z = true;
        }
        this.m_bChatOptionsVisible = z;
        FragmentActivity act = getActivity();
        if (act != null) {
            View vRefresh = act.findViewById(R.id.titleNavRefreshButton);
            if (vRefresh != null) {
                vRefresh.setBackgroundResource(this.m_bChatOptionsVisible ? R.drawable.icon_options_hide : R.drawable.icon_options_show);
            }
            View vOptions = act.findViewById(R.id.chat_options_layout);
            if (vOptions != null) {
                if (!this.m_bChatOptionsVisible) {
                    i = AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION;
                }
                vOptions.setVisibility(i);
            }
        }
    }

    public boolean overrideActivityOnBackPressed() {
        if (!this.m_bChatOptionsVisible) {
            return false;
        }
        toggleOptions();
        return true;
    }

    private void UpdateView() {
        SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
        if (dbService != null) {
            FragmentActivity act = getActivity();
            if (act != null) {
                SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_UMQACTIVITY, new REQ_ACT_UMQACTIVITY_DATA());
                if (!this.m_chatViewMessages.isEmpty()) {
                    this.m_chatViewMessages.clear();
                    this.m_chatViewAdapter = new ChatViewAdapter(act, this.m_chatViewMessages, this.m_layoutInflater);
                    this.m_chatViewAdapter.attach(this.m_chatViewContents);
                }
                UpdateStatusBar();
                this.m_userConversationInfo = dbService.getSteamUmqCommunicationServiceDB().selectUserConversationInfo(SteamWebApi.GetLoginSteamID(), this.m_partnerSteamId);
                this.m_chatViewAdapter.LoadMoreMessages();
                this.m_chatViewAdapter.notifyDataSetChanged();
                ScrollToBottom(true);
                TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(act);
                if (titlebar != null) {
                    titlebar.setTitleLabel(this.m_partner.GetPersonaNameSafe());
                    titlebar.setRefreshHandler(this.m_optionsHandler);
                    View vRefresh = act.findViewById(R.id.titleNavRefreshButton);
                    if (vRefresh != null) {
                        vRefresh.setBackgroundResource(R.drawable.icon_options_show);
                    }
                }
                MarkMessagesAsReadWithUserIfNotPaused();
            }
        }
    }

    private void MarkMessagesAsReadWithUserIfNotPaused() {
        if (this.m_bPaused) {
            this.m_bRequiresMarkMessagesAsRead = true;
        } else {
            MarkMessagesAsReadWithUser();
        }
    }

    private void MarkMessagesAsReadWithUser() {
        REQ_ACT_MARKREADMESSAGES_DATA obj = new REQ_ACT_MARKREADMESSAGES_DATA();
        obj.mysteamid = SteamWebApi.GetLoginSteamID();
        obj.withsteamid = this.m_partnerSteamId;
        SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_MARKREADMESSAGES, obj);
        Intent intent = new Intent(SteamUmqCommunicationService.INTENT_ACTION);
        intent.putExtra("type", "chatmsg");
        intent.putExtra("action", "read");
        intent.putExtra("steamid", this.m_partnerSteamId);
        SteamCommunityApplication.GetInstance().getApplicationContext().sendBroadcast(intent);
    }

    private void UpdateStatusBar() {
        SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
        if (dbService != null) {
            UmqConnectionState eUmqState = dbService.getSteamUmqConnectionState();
            if (!eUmqState.isConnected()) {
                this.m_chatViewStatus.setVisibility(0);
                this.m_bCanSendMessages = false;
                switch (AnonymousClass_8.$SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[eUmqState.ordinal()]) {
                    case UriData.RESULT_INVALID_CONTENT:
                        this.m_chatViewStatus.setText(R.string.Offline);
                        UpdateSendButton();
                        if (getActivity() != null) {
                            getActivity().finish();
                            return;
                        }
                        return;
                    default:
                        this.m_chatViewStatus.setText(eUmqState.getStringResid());
                        break;
                }
            } else if (this.m_partner.m_personaState != PersonaState.OFFLINE) {
                this.m_chatViewStatus.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                this.m_bCanSendMessages = true;
            } else {
                this.m_chatViewStatus.setText(R.string.Friend_Is_Offline);
            }
            UpdateSendButton();
        }
    }

    public void OnChatUpdated(Intent intent) {
        if (SteamUmqCommunicationService.INTENT_ACTION.equals(intent.getAction())) {
            String notificationType = intent.getStringExtra("type");
            if (notificationType.equalsIgnoreCase("umqstate")) {
                UpdateStatusBar();
            } else if (notificationType.equalsIgnoreCase("chatmsg")) {
                if (this.m_partnerSteamId.equals(intent.getStringExtra("steamid"))) {
                    SteamDBService dbService;
                    String action = intent.getStringExtra("action");
                    if (!"read".equals(action)) {
                        if ("clear".equals(action)) {
                            if (getActivity() != null) {
                                getActivity().finish();
                            }
                        } else if ("send".equals(action)) {
                            String send = intent.getStringExtra("send");
                            if (SteamUmqCommunicationService.E_OK.equals(send) || C2DMReceiverService.EXTRA_ERROR.equals(send)) {
                                int msgid;
                                boolean bPendingSendUpdated = false;
                                int hash = Integer.valueOf(intent.getStringExtra("intentcontext")).intValue();
                                int jj = 0;
                                while (jj < this.m_sentMsgs.size()) {
                                    Message msgSent = (Message) this.m_sentMsgs.get(jj);
                                    if (System.identityHashCode(msgSent) == hash) {
                                        if (C2DMReceiverService.EXTRA_ERROR.equals(send)) {
                                            msgSent.bUnread = true;
                                        }
                                        this.m_sentMsgs.remove(jj);
                                        this.m_chatViewAdapter.notifyDataSetChanged();
                                        bPendingSendUpdated = true;
                                        if (!bPendingSendUpdated) {
                                            msgid = intent.getIntExtra("msgid", -1);
                                            if (msgid > 0) {
                                                dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
                                                if (dbService != null) {
                                                    this.m_chatViewMessages.addAll(0, dbService.getSteamUmqCommunicationServiceDB().selectMessagesByID(msgid));
                                                    this.m_chatViewAdapter.notifyDataSetChanged();
                                                }
                                            }
                                        }
                                    } else {
                                        jj++;
                                    }
                                }
                                if (bPendingSendUpdated) {
                                    msgid = intent.getIntExtra("msgid", -1);
                                    if (msgid > 0) {
                                        dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
                                        if (dbService != null) {
                                            this.m_chatViewMessages.addAll(0, dbService.getSteamUmqCommunicationServiceDB().selectMessagesByID(msgid));
                                            this.m_chatViewAdapter.notifyDataSetChanged();
                                        }
                                    }
                                }
                            }
                        }
                    }
                    if ("incoming".equals(action)) {
                        int msgidFirst = intent.getIntExtra("msgidFirst", -1);
                        if (msgidFirst > 0) {
                            dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
                            if (dbService != null) {
                                this.m_chatViewMessages.addAll(0, dbService.getSteamUmqCommunicationServiceDB().selectMessagesWithUserLatest(SteamWebApi.GetLoginSteamID(), this.m_partnerSteamId, msgidFirst));
                                if (intent.getIntExtra("incoming", 0) + intent.getIntExtra("my_incoming", 0) > 0) {
                                    MarkMessagesAsReadWithUserIfNotPaused();
                                }
                            }
                        }
                        if (intent.hasExtra("typing")) {
                            this.m_chatViewAdapter.MarkTyping(false);
                            if (intent.getIntExtra("typing", 0) > 0) {
                                this.m_chatViewAdapter.MarkTyping(true);
                            }
                        }
                        this.m_chatViewAdapter.notifyDataSetChanged();
                        ScrollToBottom();
                    }
                }
            }
        }
    }
}
