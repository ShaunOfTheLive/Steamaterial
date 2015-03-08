package com.valvesoftware.android.steam.community;

import android.content.Context;
import android.content.Intent;
import com.valvesoftware.android.steam.community.GroupInfo.GroupRelationship;
import com.valvesoftware.android.steam.community.GroupInfo.GroupType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import java.util.ArrayList;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class GroupInfoDB extends GenericListDB {
    public GroupInfo GetGroupInfo(Long steamID) {
        return (GroupInfo) this.m_itemsMap.get(steamID);
    }

    public void onReceive(Context context, Intent intent) {
        if (SteamUmqCommunicationService.INTENT_ACTION.equals(intent.getAction())) {
            String notificationType = intent.getStringExtra("type");
            if (notificationType.equalsIgnoreCase("personarelationship") && HasLiveItemData()) {
                this.m_bDataMightBeStale = true;
                if (this.m_bAutoRefreshIfDataMightBeStale) {
                    RefreshFromHttpOnly();
                    return;
                }
                return;
            } else if (!notificationType.equalsIgnoreCase("umqstate")) {
                return;
            } else {
                if ("active".equals(intent.getStringExtra("old_state"))) {
                    this.m_bDataMightBeStale = true;
                    return;
                } else if (this.m_bDataMightBeStale && this.m_bAutoRefreshIfDataMightBeStale && "active".equals(intent.getStringExtra("state"))) {
                    this.m_bDataMightBeStale = false;
                    RefreshFromHttpOnly();
                    return;
                } else {
                    return;
                }
            }
        }
        super.onReceive(context, intent);
    }

    protected RequestBase IssueSingleItemSummaryRequest(String[] param) {
        return SteamWebApi.GetRequestForGroupsBySteamIDs(param);
    }

    protected RequestBase IssueFullListRefreshRequest(boolean cached) {
        return SteamWebApi.GetRequestForGroupListByOwnerSteamID(SteamWebApi.GetLoginSteamID(), cached);
    }

    protected GroupInfo GetOrAllocateNewGroupInfo(String steamID) {
        Long steamIDLong = Long.valueOf(steamID);
        GroupInfo entry = (GroupInfo) this.m_itemsMap.get(steamIDLong);
        if (entry != null) {
            return entry;
        }
        entry = new GroupInfo();
        entry.m_steamID = steamIDLong;
        this.m_itemsMap.put(steamIDLong, entry);
        return entry;
    }

    protected void HandleItemSummaryDocument(JSONObject jsonOBJ, String steamID) {
        GroupInfo groupEntry = GetOrAllocateNewGroupInfo(steamID);
        groupEntry.m_groupName = jsonOBJ.optString("name");
        groupEntry.m_numMembersTotal = jsonOBJ.optInt("users");
        groupEntry.m_numMembersOnline = jsonOBJ.optInt("usersonline");
        groupEntry.m_groupType = GroupType.FromInteger(jsonOBJ.optInt("type"));
        groupEntry.m_avatarSmallURL = jsonOBJ.optString("avatar");
        groupEntry.m_avatarMediumURL = jsonOBJ.optString("avatarmedium");
        groupEntry.m_profileURL = jsonOBJ.optString("profileurl");
        if (!(groupEntry.m_profileURL == null || groupEntry.m_profileURL.equals(""))) {
            groupEntry.m_profileURL = (groupEntry.m_groupType == GroupType.OFFICIAL ? "/games/" : "/groups/") + groupEntry.m_profileURL;
        }
        groupEntry.m_appidFavorite = jsonOBJ.optInt("favoriteappid");
    }

    protected void HandleItemSummaryDocument(RequestBase req) {
        JSONObject doc = req.GetJSONDocument();
        if (doc != null) {
            try {
                JSONArray groupArray = doc.getJSONArray("groups");
                ArrayList<Long> updated = new ArrayList();
                for (int i = 0; i < groupArray.length(); i++) {
                    try {
                        JSONObject jsonOBJ = groupArray.getJSONObject(i);
                        String steamID = jsonOBJ.getString("steamid");
                        StoreItemSummaryDocumentInCache(steamID, jsonOBJ.toString());
                        HandleItemSummaryDocument(jsonOBJ, steamID);
                        updated.add(Long.valueOf(steamID));
                    } catch (Exception e) {
                    }
                }
                if (!updated.isEmpty()) {
                    this.m_eventBroadcaster.OnListItemInfoUpdated(updated, true);
                }
            } catch (Exception e2) {
            }
        }
    }

    protected void HandleListRefreshDocument(RequestBase req, boolean docWasCached) {
        JSONObject doc = req.GetJSONDocument();
        if (doc != null) {
            try {
                JSONArray arrItems = doc.getJSONArray("groups");
                int num = arrItems.length();
                ArrayList<String> steamIDList = new ArrayList();
                ArrayList<Long> oldSet = new ArrayList(this.m_itemsMap.keySet());
                int i = 0;
                while (i < num) {
                    try {
                        String steamID = arrItems.getJSONObject(i).getString("steamid");
                        GroupRelationship relationship = GroupRelationship.valueOf(arrItems.getJSONObject(i).getString("relationship"));
                        if (relationship == GroupRelationship.Member || relationship == GroupRelationship.Invited) {
                            steamIDList.add(steamID);
                            GroupInfo friendEntry = GetOrAllocateNewGroupInfo(steamID);
                            oldSet.remove(Long.valueOf(steamID));
                            friendEntry.m_relationship = relationship;
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
