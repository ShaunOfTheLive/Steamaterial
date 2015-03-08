package com.valvesoftware.android.steam.community;

import android.content.Context;
import android.content.Intent;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.FriendInfo.FriendRelationship;
import com.valvesoftware.android.steam.community.FriendInfo.PersonaState;
import com.valvesoftware.android.steam.community.FriendInfoDB.ChatsUpdateType;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UserConversationInfo;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import java.util.ArrayList;
import java.util.Calendar;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class FriendInfoDB extends GenericListDB {
    private String m_sActiveChatPartnerSteamId;

    static /* synthetic */ class AnonymousClass_1 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType = new int[ChatsUpdateType.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType[ChatsUpdateType.mark_read.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType[ChatsUpdateType.clear.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType[ChatsUpdateType.msg_sent.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType[ChatsUpdateType.msg_incoming.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public enum ChatsUpdateType {
        clear,
        mark_read,
        msg_sent,
        msg_incoming
    }

    public FriendInfoDB() {
        this.m_sActiveChatPartnerSteamId = null;
    }

    public FriendInfo GetFriendInfo(Long steamID) {
        return (FriendInfo) this.m_itemsMap.get(steamID);
    }

    public void IncrementUnreadChatsWithUser(String steamID) {
        Long steamIDLong = Long.valueOf(steamID);
        FriendInfo friendEntry = GetFriendInfo(steamIDLong);
        if (friendEntry != null) {
            UserConversationInfo userConversationInfo = friendEntry.m_chatInfo;
            userConversationInfo.numUnreadMsgs++;
            userConversationInfo = friendEntry.m_chatInfo;
            userConversationInfo.numMsgsTotal++;
            friendEntry.m_chatInfo.latestTimestamp = Calendar.getInstance();
            ArrayList<Long> updatedIDs = new ArrayList();
            updatedIDs.add(steamIDLong);
            this.m_eventBroadcaster.OnListItemInfoUpdated(updatedIDs, true);
        }
    }

    public void ChatsWithUserUpdate(String steamID, int msgid, ChatsUpdateType eType, int count) {
        if (steamID == null || steamID.equals("") || steamID.equals("0")) {
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType[eType.ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    for (GenericListItem friendEntry : GetItemsMap().values()) {
                        ((FriendInfo) friendEntry).m_chatInfo.numUnreadMsgs = 0;
                    }
                    return;
                default:
                    return;
            }
        }
        Long steamIDLong = Long.valueOf(steamID);
        FriendInfo friendEntry2 = GetFriendInfo(steamIDLong);
        if (friendEntry2 != null) {
            UserConversationInfo userConversationInfo;
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$FriendInfoDB$ChatsUpdateType[eType.ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    if (friendEntry2.m_chatInfo.numUnreadMsgs != 0) {
                        friendEntry2.m_chatInfo.numUnreadMsgs = 0;
                    } else {
                        return;
                    }
                case UriData.RESULT_HTTP_EXCEPTION:
                    if (friendEntry2.m_chatInfo.numMsgsTotal != 0) {
                        friendEntry2.m_chatInfo.numMsgsTotal = 0;
                        friendEntry2.m_chatInfo.numUnreadMsgs = 0;
                        friendEntry2.m_chatInfo.latestTimestamp = null;
                        friendEntry2.m_chatInfo.latestMsgId = 0;
                    } else {
                        return;
                    }
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    userConversationInfo = friendEntry2.m_chatInfo;
                    userConversationInfo.numMsgsTotal += count;
                    friendEntry2.m_chatInfo.latestTimestamp = Calendar.getInstance();
                    if (msgid > 0) {
                        friendEntry2.m_chatInfo.latestMsgId = msgid;
                    }
                    break;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    userConversationInfo = friendEntry2.m_chatInfo;
                    userConversationInfo.numUnreadMsgs += count;
                    userConversationInfo = friendEntry2.m_chatInfo;
                    userConversationInfo.numMsgsTotal += count;
                    friendEntry2.m_chatInfo.latestTimestamp = Calendar.getInstance();
                    if (msgid > 0) {
                        friendEntry2.m_chatInfo.latestMsgId = msgid;
                    }
                    break;
            }
            ArrayList<Long> updatedIDs = new ArrayList();
            updatedIDs.add(steamIDLong);
            this.m_eventBroadcaster.OnListItemInfoUpdated(updatedIDs, true);
        }
    }

    public void setActiveChatPartnerSteamId(String steamid) {
        this.m_sActiveChatPartnerSteamId = steamid;
        if (this.m_sActiveChatPartnerSteamId != null && this.m_bDataMightBeStale) {
            IssueItemSummaryRequest(new String[]{this.m_sActiveChatPartnerSteamId}, false);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (SteamUmqCommunicationService.INTENT_ACTION.equals(intent.getAction())) {
            String notificationType = intent.getStringExtra("type");
            if (notificationType.equalsIgnoreCase("personastate") && HasLiveItemData()) {
                String[] steamids = null;
                boolean bRequestNow = this.m_bAutoRefreshIfDataMightBeStale;
                if (!(bRequestNow || this.m_sActiveChatPartnerSteamId == null)) {
                    if (null == null) {
                        steamids = intent.getStringArrayExtra("steamids");
                    }
                    for (String id : steamids) {
                        if (id.equals(this.m_sActiveChatPartnerSteamId)) {
                            bRequestNow = true;
                            break;
                        }
                    }
                }
                if (bRequestNow) {
                    if (steamids == null) {
                        steamids = intent.getStringArrayExtra("steamids");
                    }
                    IssueItemSummaryRequest(steamids, false);
                    return;
                }
                this.m_bDataMightBeStale = true;
                return;
            } else if (notificationType.equalsIgnoreCase("personarelationship") && HasLiveItemData()) {
                this.m_bDataMightBeStale = true;
                if (this.m_bAutoRefreshIfDataMightBeStale) {
                    RefreshFromHttpIfDataMightBeStale();
                    return;
                }
                return;
            } else if (!notificationType.equalsIgnoreCase("umqstate")) {
                return;
            } else {
                if ("active".equals(intent.getStringExtra("old_state"))) {
                    this.m_bDataMightBeStale = true;
                    return;
                } else if (!this.m_bDataMightBeStale || !"active".equals(intent.getStringExtra("state"))) {
                    return;
                } else {
                    if (this.m_bAutoRefreshIfDataMightBeStale) {
                        this.m_bDataMightBeStale = false;
                        RefreshFromHttpOnly();
                        return;
                    } else if (this.m_sActiveChatPartnerSteamId != null) {
                        setActiveChatPartnerSteamId(this.m_sActiveChatPartnerSteamId);
                        return;
                    } else {
                        return;
                    }
                }
            }
        }
        super.onReceive(context, intent);
    }

    protected RequestBase IssueSingleItemSummaryRequest(String[] param) {
        return SteamWebApi.GetRequestForUserSummariesBySteamIDs(param);
    }

    protected RequestBase IssueFullListRefreshRequest(boolean cached) {
        return SteamWebApi.GetRequestForFriendListByOwnerSteamID(SteamWebApi.GetLoginSteamID(), cached);
    }

    protected FriendInfo GetOrAllocateNewFriendInfo(String steamID) {
        Long steamIDLong = Long.valueOf(steamID);
        FriendInfo friendEntry = (FriendInfo) this.m_itemsMap.get(steamIDLong);
        if (friendEntry != null) {
            return friendEntry;
        }
        friendEntry = new FriendInfo();
        friendEntry.m_steamID = steamIDLong;
        friendEntry.m_chatInfo = SteamCommunityApplication.GetInstance().GetSteamDB().getSteamUmqCommunicationServiceDB().selectUserConversationInfo(SteamWebApi.GetLoginSteamID(), steamID);
        this.m_itemsMap.put(steamIDLong, friendEntry);
        return friendEntry;
    }

    protected void HandleItemSummaryDocument(JSONObject jsonOBJ, String steamID) {
        FriendInfo friendEntry = GetOrAllocateNewFriendInfo(steamID);
        friendEntry.m_personaName = jsonOBJ.optString("personaname");
        friendEntry.m_realName = jsonOBJ.optString("realname");
        friendEntry.m_avatarSmallURL = jsonOBJ.optString("avatar");
        friendEntry.m_personaState = PersonaState.FromInteger(jsonOBJ.optInt("personastate"));
        friendEntry.m_currentGameID = jsonOBJ.optInt("gameid");
        friendEntry.m_currentGameString = jsonOBJ.optString("gameextrainfo");
        friendEntry.m_lastOnlineTime = jsonOBJ.optInt("lastlogoff");
        if (friendEntry.m_relationship == FriendRelationship.myself && !friendEntry.IsAvatarSmallLoaded()) {
            RequestAvatarImageImmediately(friendEntry);
        }
    }

    protected void HandleItemSummaryDocument(RequestBase req) {
        JSONObject doc = req.GetJSONDocument();
        if (doc != null) {
            try {
                JSONArray playerArray = doc.getJSONArray("players");
                ArrayList<Long> updatedUsers = new ArrayList();
                for (int i = 0; i < playerArray.length(); i++) {
                    try {
                        JSONObject jsonOBJ = playerArray.getJSONObject(i);
                        String steamID = jsonOBJ.getString("steamid");
                        StoreItemSummaryDocumentInCache(steamID, jsonOBJ.toString());
                        HandleItemSummaryDocument(jsonOBJ, steamID);
                        updatedUsers.add(Long.valueOf(steamID));
                    } catch (Exception e) {
                    }
                }
                if (!updatedUsers.isEmpty()) {
                    this.m_eventBroadcaster.OnListItemInfoUpdated(updatedUsers, true);
                }
            } catch (Exception e2) {
            }
        }
    }

    protected void HandleListRefreshDocument(RequestBase req, boolean docWasCached) {
        JSONObject doc = req.GetJSONDocument();
        if (doc != null) {
            try {
                JSONArray arrItems = doc.getJSONArray("friends");
                int num = arrItems.length();
                ArrayList<String> steamIDList = new ArrayList();
                ArrayList<Long> oldSet = new ArrayList(this.m_itemsMap.keySet());
                String mysteamid = SteamWebApi.GetLoginSteamID();
                if (mysteamid != null) {
                    steamIDList.add(mysteamid);
                    oldSet.remove(Long.valueOf(mysteamid));
                    GetOrAllocateNewFriendInfo(mysteamid).m_relationship = FriendRelationship.myself;
                }
                int i = 0;
                while (i < num) {
                    try {
                        String steamID = arrItems.getJSONObject(i).getString("steamid");
                        FriendRelationship relationship = FriendRelationship.valueOf(arrItems.getJSONObject(i).getString("relationship"));
                        if (relationship == FriendRelationship.friend || relationship == FriendRelationship.requestrecipient) {
                            steamIDList.add(steamID);
                            oldSet.remove(Long.valueOf(steamID));
                            GetOrAllocateNewFriendInfo(steamID).m_relationship = relationship;
                            i++;
                        } else {
                            i++;
                        }
                    } catch (Exception e) {
                    }
                }
                RemoveOldItemList(oldSet);
                String[] steamIDs = new String[steamIDList.size()];
                steamIDList.toArray(steamIDs);
                IssueItemSummaryRequest(steamIDs, docWasCached);
            } catch (JSONException e2) {
            }
        }
    }
}
