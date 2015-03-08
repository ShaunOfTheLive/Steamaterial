package com.valvesoftware.android.steam.community.fragment;

import android.app.PendingIntent.CanceledException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.AndroidUtils;
import com.valvesoftware.android.steam.community.Config;
import com.valvesoftware.android.steam.community.FriendInfo;
import com.valvesoftware.android.steam.community.FriendInfo.FriendRelationship;
import com.valvesoftware.android.steam.community.FriendInfo.PersonaState;
import com.valvesoftware.android.steam.community.FriendInfo.PersonaStateCategoryInList;
import com.valvesoftware.android.steam.community.GenericListDB;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UserConversationInfo;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamUriHandler;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.activity.SearchFriendsActivity;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment.TitlebarButtonHander;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class FriendListFragment extends BasePresentationListFragmentWithSearch<FriendInfo> {
    private OnClickListener dummyClickListener;
    public OnClickListener friendChatClickListener;
    private OnClickListener friendListClickListener;
    private BaseFragmentWithLogin m_login;
    private TitlebarButtonHander m_refreshHandler;
    private Comparator<FriendInfo> m_sorter;

    static /* synthetic */ class AnonymousClass_6 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList = new int[PersonaStateCategoryInList.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.CHATS.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
        }
    }

    private class FriendsListAdapter extends HelperDbListAdapter {
        public FriendsListAdapter() {
            super(2130903049);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Context appContext = SteamCommunityApplication.GetInstance();
            if (v == null) {
                v = ((LayoutInflater) appContext.getSystemService("layout_inflater")).inflate(R.layout.friend_list_item, null);
                v.setClickable(true);
                v.setOnClickListener(FriendListFragment.this.friendListClickListener);
            }
            FriendInfo friendInfo = null;
            FriendInfo friendInfoPrev = null;
            try {
                friendInfo = (FriendInfo) FriendListFragment.this.m_presentationArray.get(position);
                friendInfoPrev = position > 0 ? (FriendInfo) FriendListFragment.this.m_presentationArray.get(position - 1) : null;
            } catch (Exception e) {
            }
            if (friendInfo != null) {
                if (!friendInfo.IsAvatarSmallLoaded()) {
                    RequestVisibleAvatars();
                }
                TextView labelView = (TextView) v.findViewById(2131296269);
                TextView nameView = (TextView) v.findViewById(2131296275);
                TextView statusView = (TextView) v.findViewById(2131296276);
                ImageView avatarView = (ImageView) v.findViewById(2131296270);
                ImageView avatarViewFrame = (ImageView) v.findViewById(2131296274);
                TextView steamidView = (TextView) v.findViewById(2131296280);
                ImageView chevronView = (ImageView) v.findViewById(2131296279);
                Button chatBtn = (Button) v.findViewById(2131296278);
                chatBtn.setOnClickListener(FriendListFragment.this.friendChatClickListener);
                if (position == 0 || friendInfoPrev == null || friendInfo.m_categoryInList != friendInfoPrev.m_categoryInList) {
                    labelView.setText(friendInfo.m_categoryInList.GetDisplayString());
                    labelView.setVisibility(0);
                } else {
                    labelView.setVisibility(8);
                }
                labelView.setOnClickListener(FriendListFragment.this.dummyClickListener);
                steamidView.setText(Long.toString(friendInfo.m_steamID.longValue()));
                AndroidUtils.setTextViewText(nameView, friendInfo.m_personaName);
                if (friendInfo != FriendListFragment.this.GetSearchItem()) {
                    avatarView.setImageBitmap(friendInfo.GetAvatarSmall());
                    avatarViewFrame.setVisibility(0);
                } else {
                    avatarView.setImageResource(2130837520);
                    avatarViewFrame.setVisibility(8);
                }
                if (friendInfo == FriendListFragment.this.GetSearchItem()) {
                    AndroidUtils.setTextViewText(statusView, FriendListFragment.this.getSearchModeText());
                } else {
                    if (friendInfo.m_personaState != PersonaState.OFFLINE || friendInfo.m_lastOnlineString == null) {
                        statusView.setText(friendInfo.m_personaState.GetDisplayString());
                    } else {
                        statusView.setText(friendInfo.m_lastOnlineString);
                    }
                }
                chatBtn.setText(friendInfo.m_chatInfo.numUnreadMsgs > 0 ? Integer.valueOf(friendInfo.m_chatInfo.numUnreadMsgs).toString() : "");
                if (friendInfo.m_chatInfo.numUnreadMsgs > 0) {
                    chatBtn.setBackgroundResource(2130837514);
                } else {
                    if (friendInfo.m_personaState == PersonaState.OFFLINE) {
                        chatBtn.setBackgroundResource(2130837513);
                    } else {
                        chatBtn.setBackgroundResource(2130837512);
                    }
                }
                if (friendInfo.m_currentGameString.length() > 0) {
                    avatarViewFrame.setImageResource(2130837504);
                    nameView.setTextColor(appContext.getResources().getColor(R.color.ingame));
                    statusView.setTextColor(appContext.getResources().getColor(R.color.ingame));
                    statusView.setText(appContext.getResources().getString(R.string.Playing) + " " + friendInfo.m_currentGameString);
                    chatBtn.setVisibility(0);
                    chevronView.setVisibility(8);
                } else {
                    if (friendInfo.m_personaState == PersonaState.OFFLINE) {
                        avatarViewFrame.setImageResource(2130837505);
                        nameView.setTextColor(appContext.getResources().getColor(R.color.offline));
                        statusView.setTextColor(appContext.getResources().getColor(R.color.offline));
                        chatBtn.setVisibility(friendInfo.m_chatInfo.numMsgsTotal > 0 ? null : AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                        chevronView.setVisibility(friendInfo.m_chatInfo.numMsgsTotal > 0 ? AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION : null);
                    } else {
                        avatarViewFrame.setImageResource(2130837506);
                        nameView.setTextColor(appContext.getResources().getColor(R.color.online));
                        statusView.setTextColor(appContext.getResources().getColor(R.color.online));
                        chevronView.setVisibility(8);
                        chatBtn.setVisibility(0);
                    }
                }
                if (FriendRelationship.friend != friendInfo.m_relationship) {
                    chatBtn.setVisibility(8);
                    chevronView.setVisibility(0);
                }
                View vAreaAroundChatBtn = v.findViewById(2131296277);
                if (vAreaAroundChatBtn != null) {
                    OnClickListener onClickListener;
                    if (chatBtn.getVisibility() == 0) {
                        onClickListener = FriendListFragment.this.friendChatClickListener;
                    } else {
                        onClickListener = FriendListFragment.this.friendListClickListener;
                    }
                    vAreaAroundChatBtn.setOnClickListener(onClickListener);
                }
            }
            return v;
        }
    }

    public FriendListFragment() {
        this.m_login = new BaseFragmentWithLogin(this);
        this.m_refreshHandler = new TitlebarButtonHander() {
            public void onTitlebarButtonClicked(int id) {
                if (SteamWebApi.IsLoggedIn()) {
                    FriendListFragment.this.myListDb().RefreshFromHttpOnly();
                }
            }
        };
        this.friendListClickListener = new OnClickListener() {
            public void onClick(View v) {
                TextView steamidView = (TextView) v.findViewById(R.id.steamid);
                if (steamidView != null) {
                    String sSteamID = steamidView.getText().toString();
                    if (sSteamID.equals("0")) {
                        FriendListFragment.this.getActivity().startActivity(new Intent().setClass(FriendListFragment.this.getActivity(), SearchFriendsActivity.class).addFlags(536870912).addFlags(268435456).setData(Uri.parse("searchfriend://" + System.currentTimeMillis() + FriendListFragment.this.getSearchModeText())).putExtra("query", FriendListFragment.this.getSearchModeText()));
                        return;
                    }
                    FriendListFragment.this.getActivity().startActivity(new Intent().addFlags(402653184).setClass(FriendListFragment.this.getActivity(), SteamMobileUriActivity.class).setData(Uri.parse(SteamUriHandler.MOBILE_PROTOCOL + Command.openurl + "?url=" + Config.URL_COMMUNITY_BASE + "/profiles/" + sSteamID)).setAction("android.intent.action.VIEW"));
                }
            }
        };
        this.friendChatClickListener = new OnClickListener() {
            public void onClick(View v) {
                TextView steamidView;
                if (v instanceof Button) {
                    steamidView = ((View) v.getParent()).findViewById(R.id.steamid);
                } else {
                    steamidView = v.findViewById(R.id.steamid);
                }
                if (steamidView != null) {
                    try {
                        SteamCommunityApplication.GetInstance().GetIntentToChatWithSteamID(steamidView.getText().toString()).send();
                    } catch (CanceledException e) {
                    }
                }
            }
        };
        this.dummyClickListener = new OnClickListener() {
            public void onClick(View v) {
            }
        };
        this.m_sorter = new Comparator<FriendInfo>() {
            static final /* synthetic */ boolean $assertionsDisabled;

            static {
                $assertionsDisabled = !FriendListFragment.class.desiredAssertionStatus();
            }

            public int compare(FriendInfo o1, FriendInfo o2) {
                if (o1.m_categoryInList != o2.m_categoryInList) {
                    return o1.m_categoryInList.ordinal() < o2.m_categoryInList.ordinal() ? -1 : 1;
                } else {
                    switch (AnonymousClass_6.$SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[o1.m_categoryInList.ordinal()]) {
                        case UriData.RESULT_INVALID_CONTENT:
                            if ($assertionsDisabled || o1.m_chatInfo.latestMsgId != o2.m_chatInfo.latestMsgId) {
                                return o1.m_chatInfo.latestMsgId <= o2.m_chatInfo.latestMsgId ? 1 : -1;
                            } else {
                                throw new AssertionError();
                            }
                        default:
                            return o1.m_personaName.compareToIgnoreCase(o2.m_personaName);
                    }
                }
            }
        };
    }

    protected FriendInfo myDbItemCreateSearchItem() {
        FriendInfo info = new FriendInfo();
        info.m_steamID = new Long(0);
        info.m_personaState = PersonaState.OFFLINE;
        info.m_categoryInList = PersonaStateCategoryInList.SEARCH_ALL;
        info.m_relationship = FriendRelationship.friend;
        String string = SteamCommunityApplication.GetInstance().getResources().getString(R.string.Friend_Search_All);
        info.m_realName = string;
        info.m_personaName = string;
        info.m_chatInfo = new UserConversationInfo();
        return info;
    }

    protected GenericListDB myListDb() {
        return SteamCommunityApplication.GetInstance().GetFriendInfoDB();
    }

    protected HelperDbListAdapter myCreateListAdapter() {
        return new FriendsListAdapter();
    }

    protected boolean myCbckShouldDisplayItemInList(FriendInfo item) {
        return item.m_relationship != FriendRelationship.myself && item.HasPresentationData() && ApplySearchFilterBeforeDisplay(item.m_personaName);
    }

    protected void myCbckProcessPresentationArray() {
        Iterator i$ = this.m_presentationArray.iterator();
        while (i$.hasNext()) {
            FriendInfo info = (FriendInfo) i$.next();
            if (info.m_relationship == FriendRelationship.requestrecipient) {
                info.m_categoryInList = PersonaStateCategoryInList.REQUEST_INCOMING;
            } else if (hasRecentChatsHistory(info)) {
                info.m_categoryInList = PersonaStateCategoryInList.CHATS;
            } else if (info.m_currentGameString.length() > 0) {
                info.m_categoryInList = PersonaStateCategoryInList.INGAME;
            } else if (info.m_personaState == PersonaState.OFFLINE) {
                info.m_categoryInList = PersonaStateCategoryInList.OFFLINE;
            } else {
                info.m_categoryInList = PersonaStateCategoryInList.ONLINE;
            }
            info.UpdateLastOnlineTime(this.m_umqCurrentServerTime);
        }
        super.myCbckProcessPresentationArray();
        Collections.sort(this.m_presentationArray, this.m_sorter);
    }

    protected void activateSearch(boolean active) {
        super.activateSearch(active);
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setRefreshHandler(active ? null : this.m_refreshHandler);
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.m_login.onActivityCreated(savedInstanceState);
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setTitleLabel((int) R.string.Friends);
            titlebar.setRefreshHandler(this.m_refreshHandler);
        }
    }

    public void onResume() {
        super.onResume();
        this.m_login.onResume();
    }

    protected void UpdateGlobalInformationPreSort() {
        super.UpdateGlobalInformationPreSort();
        this.m_login.UpdateGlobalInformationPreSort();
    }

    private boolean hasRecentChatsHistory(FriendInfo o) {
        return o.m_chatInfo.numUnreadMsgs > 0 || (o.m_chatInfo.numMsgsTotal > 0 && o.m_chatInfo.latestTimestamp != null && o.m_chatInfo.latestTimestamp.after(this.m_calRecentChatsThreshold));
    }
}
