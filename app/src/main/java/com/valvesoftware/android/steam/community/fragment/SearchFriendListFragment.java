package com.valvesoftware.android.steam.community.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
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
import com.valvesoftware.android.steam.community.FriendInfoDB;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamUriHandler;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestDocumentType;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import java.util.ArrayList;
import java.util.Iterator;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.valvesoftware.android.steam.community.fragment.SearchFriendListFragment.SearchFriendInfoDB;

public class SearchFriendListFragment extends BaseSearchResultsListFragment<FriendInfo, SearchFriendInfoDB> {
    private static final String SearchFriendInfoDB_URI;
    private OnClickListener friendListClickListener;

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
                v.setOnClickListener(SearchFriendListFragment.this.friendListClickListener);
            }
            FriendInfo friendInfo = (FriendInfo) SearchFriendListFragment.this.m_presentationArray.get(position);
            if (friendInfo != null) {
                if (!friendInfo.IsAvatarSmallLoaded()) {
                    RequestVisibleAvatars();
                }
                ((TextView) v.findViewById(R.id.label)).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                TextView nameView = (TextView) v.findViewById(R.id.name);
                TextView statusView = (TextView) v.findViewById(R.id.status);
                ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
                ImageView avatarViewFrame = (ImageView) v.findViewById(R.id.avatar_frame);
                TextView steamidView = (TextView) v.findViewById(R.id.steamid);
                ((ImageView) v.findViewById(R.id.imageChevron)).setVisibility(0);
                ((Button) v.findViewById(R.id.chatButton)).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                steamidView.setText(Long.toString(friendInfo.m_steamID.longValue()));
                AndroidUtils.setTextViewText(nameView, friendInfo.m_personaName);
                avatarView.setImageBitmap(friendInfo.GetAvatarSmall());
                avatarViewFrame.setVisibility(0);
                if (friendInfo.m_personaState != PersonaState.OFFLINE || friendInfo.m_lastOnlineString == null) {
                    statusView.setText(friendInfo.m_personaState.GetDisplayString());
                } else {
                    statusView.setText(friendInfo.m_lastOnlineString);
                }
                if (friendInfo.m_currentGameString.length() > 0) {
                    avatarViewFrame.setImageResource(R.drawable.avatar_frame_ingame);
                    nameView.setTextColor(appContext.getResources().getColor(R.color.ingame));
                    statusView.setTextColor(appContext.getResources().getColor(R.color.ingame));
                    statusView.setText(appContext.getResources().getString(R.string.Playing) + " " + friendInfo.m_currentGameString);
                } else if (friendInfo.m_personaState == PersonaState.OFFLINE) {
                    avatarViewFrame.setImageResource(R.drawable.avatar_frame_offline);
                    nameView.setTextColor(appContext.getResources().getColor(R.color.offline));
                    statusView.setTextColor(appContext.getResources().getColor(R.color.offline));
                } else {
                    avatarViewFrame.setImageResource(R.drawable.avatar_frame_online);
                    nameView.setTextColor(appContext.getResources().getColor(R.color.online));
                    statusView.setTextColor(appContext.getResources().getColor(R.color.online));
                }
            }
            return v;
        }
    }

    public class SearchFriendInfoDB extends FriendInfoDB {
        public void SetAutoRefreshIfDataMightBeStale(boolean bAutoRefresh) {
        }

        protected RequestBase IssueFullListRefreshRequest(boolean cached) {
            RequestBase req = new RequestBase(SteamDBService.JOB_QUEUE_FRIENDS_DB);
            String sAccessToken = SteamWebApi.GetLoginAccessToken();
            String sQueryEncoded = Uri.encode(SearchFriendListFragment.this.m_queryText);
            StringBuilder sb = new StringBuilder(((((sAccessToken != null ? sAccessToken.length() : 0) + (SearchFriendInfoDB_URI.length() + 32)) + 32) + sQueryEncoded.length()) + 128);
            sb.append(SearchFriendInfoDB_URI);
            sb.append("?access_token=");
            sb.append(sAccessToken);
            sb.append("&keywords=");
            sb.append(sQueryEncoded);
            sb.append("&offset=");
            sb.append(SearchFriendListFragment.this.m_queryOffset);
            sb.append("&count=");
            sb.append(SearchFriendListFragment.this.m_queryPageSize);
            sb.append("&targets=users&fields=all");
            req.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            req.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
            return req;
        }

        protected void HandleListRefreshDocument(RequestBase req, boolean docWasCached) {
            JSONObject doc = req.GetJSONDocument();
            if (doc != null) {
                try {
                    JSONArray arrItems = doc.getJSONArray("results");
                    SearchFriendListFragment.this.m_queryTotalResults = doc.optInt("total");
                    SearchFriendListFragment.this.m_queryActualResults = doc.optInt("count");
                    int num = arrItems.length();
                    ArrayList<String> steamIDList = new ArrayList();
                    SearchFriendListFragment.this.m_searchResultsOrderMap.clear();
                    for (int i = 0; i < num; i++) {
                        try {
                            String steamID = arrItems.getJSONObject(i).getString("steamid");
                            steamIDList.add(steamID);
                            FriendInfo friendEntry = GetOrAllocateNewFriendInfo(steamID);
                            friendEntry.m_relationship = FriendRelationship.friend;
                            SearchFriendListFragment.this.m_searchResultsOrderMap.put(friendEntry.m_steamID, Integer.valueOf(i));
                        } catch (Exception e) {
                        }
                    }
                    String[] steamIDs = new String[steamIDList.size()];
                    steamIDList.toArray(steamIDs);
                    IssueItemSummaryRequest(steamIDs, docWasCached);
                    SearchFriendListFragment.this.refreshListView();
                } catch (JSONException e2) {
                }
            }
        }
    }

    public SearchFriendListFragment() {
        this.friendListClickListener = new OnClickListener() {
            public void onClick(View v) {
                TextView steamidView = (TextView) v.findViewById(R.id.steamid);
                if (steamidView != null) {
                    String sSteamID = steamidView.getText().toString();
                    if (!sSteamID.equals("0")) {
                        SearchFriendListFragment.this.getActivity().startActivity(new Intent().addFlags(402653184).setClass(SearchFriendListFragment.this.getActivity(), SteamMobileUriActivity.class).setData(Uri.parse(SteamUriHandler.MOBILE_PROTOCOL + Command.openurl + "?url=" + Config.URL_COMMUNITY_BASE + "/profiles/" + sSteamID)).setAction("android.intent.action.VIEW"));
                    }
                }
            }
        };
    }

    static {
        SearchFriendInfoDB_URI = Config.URL_WEBAPI_BASE + "/ISteamUserOAuth/Search/v0001";
    }

    protected SearchFriendInfoDB AllocateNewSearchDB() {
        return new SearchFriendInfoDB();
    }

    protected HelperDbListAdapter myCreateListAdapter() {
        return new FriendsListAdapter();
    }

    protected boolean myCbckShouldDisplayItemInList(FriendInfo item) {
        return item.HasPresentationData();
    }

    protected void myCbckProcessPresentationArray() {
        Iterator i$ = this.m_presentationArray.iterator();
        while (i$.hasNext()) {
            ((FriendInfo) i$.next()).UpdateLastOnlineTime(this.m_umqCurrentServerTime);
        }
        super.myCbckProcessPresentationArray();
    }

    protected int GetTitlebarFormatStringForQuery() {
        return R.string.Search_Players_Results;
    }
}
