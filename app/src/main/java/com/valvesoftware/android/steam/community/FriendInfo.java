package com.valvesoftware.android.steam.community;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.FriendInfo.PersonaState;
import com.valvesoftware.android.steam.community.FriendInfo.PersonaStateCategoryInList;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem.RequestForAvatarImage;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UserConversationInfo;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;

public class FriendInfo extends GenericListItem {
    public PersonaStateCategoryInList m_categoryInList;
    public UserConversationInfo m_chatInfo;
    public int m_currentGameID;
    public String m_currentGameString;
    public String m_lastOnlineString;
    public int m_lastOnlineTime;
    public String m_personaName;
    public PersonaState m_personaState;
    public String m_realName;
    public FriendRelationship m_relationship;

    static /* synthetic */ class AnonymousClass_1 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState;
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList = new int[PersonaStateCategoryInList.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.REQUEST_INCOMING.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.CHATS.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.INGAME.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.ONLINE.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.REQUEST_SENT.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.OFFLINE.ordinal()] = 6;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[PersonaStateCategoryInList.SEARCH_ALL.ordinal()] = 7;
            } catch (NoSuchFieldError e7) {
            }
            $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState = new int[PersonaState.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState[PersonaState.OFFLINE.ordinal()] = 1;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState[PersonaState.ONLINE.ordinal()] = 2;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState[PersonaState.BUSY.ordinal()] = 3;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState[PersonaState.AWAY.ordinal()] = 4;
            } catch (NoSuchFieldError e11) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState[PersonaState.SNOOZE.ordinal()] = 5;
            } catch (NoSuchFieldError e12) {
            }
        }
    }

    public enum FriendRelationship {
        none,
        myself,
        friend,
        blocked,
        requestrecipient,
        requestinitiator,
        ignored,
        ignoredfriend,
        suggested
    }

    public enum PersonaState {
        OFFLINE,
        ONLINE,
        BUSY,
        AWAY,
        SNOOZE;

        public static com.valvesoftware.android.steam.community.FriendInfo.PersonaState FromInteger(int status) {
            switch (status) {
                case SettingInfoDB.SETTING_NOTIFY_FIRST:
                    return OFFLINE;
                case UriData.RESULT_INVALID_CONTENT:
                    return ONLINE;
                case UriData.RESULT_HTTP_EXCEPTION:
                    return BUSY;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    return AWAY;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    return SNOOZE;
                default:
                    return OFFLINE;
            }
        }

        public int GetDisplayString() {
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaState[ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    return R.string.Offline;
                case UriData.RESULT_HTTP_EXCEPTION:
                    return R.string.Online;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    return R.string.Busy;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    return R.string.Away;
                case MotionEventCompat.ACTION_POINTER_DOWN:
                    return R.string.Snooze;
                default:
                    return R.string.Unknown;
            }
        }
    }

    public enum PersonaStateCategoryInList {
        REQUEST_INCOMING,
        CHATS,
        INGAME,
        ONLINE,
        OFFLINE,
        REQUEST_SENT,
        SEARCH_ALL;

        public int GetDisplayString() {
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$FriendInfo$PersonaStateCategoryInList[ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    return R.string.Friend_Requests;
                case UriData.RESULT_HTTP_EXCEPTION:
                    return R.string.Chats;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    return R.string.In_Game;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    return R.string.Online;
                case MotionEventCompat.ACTION_POINTER_DOWN:
                case MotionEventCompat.ACTION_POINTER_UP:
                    return R.string.Offline;
                case MotionEventCompat.ACTION_HOVER_MOVE:
                    return R.string.Search;
                default:
                    return R.string.Unknown;
            }
        }
    }

    public FriendInfo() {
        this.m_personaState = PersonaState.OFFLINE;
        this.m_categoryInList = PersonaStateCategoryInList.OFFLINE;
        this.m_relationship = FriendRelationship.none;
        this.m_currentGameString = "";
        this.m_lastOnlineTime = 0;
        this.m_lastOnlineString = null;
    }

    protected RequestForAvatarImage RequestAvatarImage(GenericListDB db) {
        return GetRequestForAvatarImage(SteamDBService.JOB_QUEUE_AVATARS_FRIENDS, this.m_avatarSmallURL, this.m_steamID.toString(), db);
    }

    public boolean HasPresentationData() {
        return this.m_personaName != null;
    }

    public String GetPersonaNameSafe() {
        return HasPresentationData() ? this.m_personaName : "";
    }

    public void UpdateLastOnlineTime(int umqCurrentServerTime) {
        if (this.m_personaState != PersonaState.OFFLINE) {
            return;
        }
        if (umqCurrentServerTime == 0 || this.m_lastOnlineTime == 0) {
            this.m_lastOnlineString = null;
            return;
        }
        int numSeconds = umqCurrentServerTime - this.m_lastOnlineTime;
        if (numSeconds < 10) {
            numSeconds = 10;
        }
        if (numSeconds < 60) {
            this.m_lastOnlineString = SteamCommunityApplication.GetInstance().getResources().getString(R.string.LastOnline_SecondsAgo).replace("#", String.valueOf(numSeconds));
            return;
        }
        int numMinutes = (numSeconds / 60) + 1;
        if (numMinutes < 60) {
            this.m_lastOnlineString = SteamCommunityApplication.GetInstance().getResources().getString(R.string.LastOnline_MinutesAgo).replace("#", String.valueOf(numMinutes));
            return;
        }
        int numHours = (numMinutes / 60) + 1;
        if (numHours < 48) {
            this.m_lastOnlineString = SteamCommunityApplication.GetInstance().getResources().getString(R.string.LastOnline_HoursAgo).replace("#", String.valueOf(numHours));
            return;
        }
        int numDays = numHours / 24;
        if (numDays < 365) {
            this.m_lastOnlineString = SteamCommunityApplication.GetInstance().getResources().getString(R.string.LastOnline_DaysAgo).replace("#", String.valueOf(numDays));
        } else {
            this.m_lastOnlineString = SteamCommunityApplication.GetInstance().getResources().getString(R.string.LastOnline_YearOrMore);
        }
    }
}
