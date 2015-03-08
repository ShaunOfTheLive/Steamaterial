package com.valvesoftware.android.steam.community.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.AndroidUtils;
import com.valvesoftware.android.steam.community.Config;
import com.valvesoftware.android.steam.community.GroupInfo;
import com.valvesoftware.android.steam.community.GroupInfo.GroupRelationship;
import com.valvesoftware.android.steam.community.GroupInfoDB;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import com.valvesoftware.android.steam.community.fragment.SearchGroupListFragment.SearchGroupInfoDB;

public class SearchGroupListFragment extends BaseSearchResultsListFragment<GroupInfo, SearchGroupInfoDB> {
    private static final String SearchFriendInfoDB_URI;
    private OnClickListener groupListClickListener;

    private class GroupsListAdapter extends HelperDbListAdapter {
        public GroupsListAdapter() {
            super(2130903051);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Context appContext = SteamCommunityApplication.GetInstance();
            if (v == null) {
                v = ((LayoutInflater) appContext.getSystemService("layout_inflater")).inflate(R.layout.group_list_item, null);
                v.setClickable(true);
                v.setOnClickListener(SearchGroupListFragment.this.groupListClickListener);
            }
            GroupInfo groupInfo = (GroupInfo) SearchGroupListFragment.this.m_presentationArray.get(position);
            if (groupInfo != null) {
                if (!groupInfo.IsAvatarSmallLoaded()) {
                    RequestVisibleAvatars();
                }
                ((TextView) v.findViewById(R.id.label)).setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                TextView nameView = (TextView) v.findViewById(R.id.name);
                TextView lblMembersTotal = (TextView) v.findViewById(R.id.groupMembersTotal);
                TextView lblMembersOnline = (TextView) v.findViewById(R.id.groupMembersOnline);
                ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
                ImageView avatarViewFrame = (ImageView) v.findViewById(R.id.avatar_frame);
                ((TextView) v.findViewById(R.id.steamid)).setText(Long.toString(groupInfo.m_steamID.longValue()));
                AndroidUtils.setTextViewText(nameView, groupInfo.m_groupName);
                avatarView.setImageBitmap(groupInfo.GetAvatarSmall());
                avatarViewFrame.setVisibility(0);
                lblMembersTotal.setText(Integer.toString(groupInfo.m_numMembersTotal) + " " + appContext.getResources().getString(R.string.Group_Num_Members_Total));
                lblMembersOnline.setText(Integer.toString(groupInfo.m_numMembersOnline) + " " + appContext.getResources().getString(R.string.Group_Num_Members_Online));
            }
            return v;
        }
    }

    public class SearchGroupInfoDB extends GroupInfoDB {
        public void SetAutoRefreshIfDataMightBeStale(boolean bAutoRefresh) {
        }

        protected RequestBase IssueFullListRefreshRequest(boolean cached) {
            RequestBase req = new RequestBase(SteamDBService.JOB_QUEUE_FRIENDS_DB);
            String sAccessToken = SteamWebApi.GetLoginAccessToken();
            String sQueryEncoded = Uri.encode(SearchGroupListFragment.this.m_queryText);
            StringBuilder sb = new StringBuilder(((((sAccessToken != null ? sAccessToken.length() : 0) + (SearchFriendInfoDB_URI.length() + 32)) + 32) + sQueryEncoded.length()) + 128);
            sb.append(SearchFriendInfoDB_URI);
            sb.append("?access_token=");
            sb.append(sAccessToken);
            sb.append("&keywords=");
            sb.append(sQueryEncoded);
            sb.append("&offset=");
            sb.append(SearchGroupListFragment.this.m_queryOffset);
            sb.append("&count=");
            sb.append(SearchGroupListFragment.this.m_queryPageSize);
            sb.append("&targets=groups&fields=all");
            req.SetUriAndDocumentType(sb.toString(), RequestDocumentType.JSON);
            req.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
            return req;
        }

        protected void HandleListRefreshDocument(RequestBase req, boolean docWasCached) {
            JSONObject doc = req.GetJSONDocument();
            if (doc != null) {
                try {
                    JSONArray arrItems = doc.getJSONArray("results");
                    SearchGroupListFragment.this.m_queryTotalResults = doc.optInt("total");
                    SearchGroupListFragment.this.m_queryActualResults = doc.optInt("count");
                    int num = arrItems.length();
                    ArrayList<String> steamIDList = new ArrayList();
                    SearchGroupListFragment.this.m_searchResultsOrderMap.clear();
                    for (int i = 0; i < num; i++) {
                        try {
                            String steamID = arrItems.getJSONObject(i).getString("steamid");
                            steamIDList.add(steamID);
                            GroupInfo entry = GetOrAllocateNewGroupInfo(steamID);
                            entry.m_relationship = GroupRelationship.Member;
                            SearchGroupListFragment.this.m_searchResultsOrderMap.put(entry.m_steamID, Integer.valueOf(i));
                        } catch (Exception e) {
                        }
                    }
                    String[] steamIDs = new String[steamIDList.size()];
                    steamIDList.toArray(steamIDs);
                    IssueItemSummaryRequest(steamIDs, docWasCached);
                    SearchGroupListFragment.this.refreshListView();
                } catch (JSONException e2) {
                }
            }
        }
    }

    public SearchGroupListFragment() {
        this.groupListClickListener = new OnClickListener() {
            public void onClick(View v) {
                TextView profileURLView = (TextView) v.findViewById(R.id.steamid);
                String sSteamID = "0";
                if (profileURLView != null) {
                    sSteamID = profileURLView.getText().toString();
                }
                if (!sSteamID.equals("0")) {
                    GroupInfo entry = ((GroupInfoDB) SearchGroupListFragment.this.myListDb()).GetGroupInfo(Long.valueOf(sSteamID));
                    if (entry != null) {
                        SearchGroupListFragment.this.getActivity().startActivity(new Intent().addFlags(402653184).setClass(SearchGroupListFragment.this.getActivity(), SteamMobileUriActivity.class).setData(Uri.parse(SteamUriHandler.MOBILE_PROTOCOL + Command.openurl + "?url=" + Config.URL_COMMUNITY_BASE + entry.m_profileURL)).setAction("android.intent.action.VIEW"));
                    }
                }
            }
        };
    }

    static {
        SearchFriendInfoDB_URI = Config.URL_WEBAPI_BASE + "/ISteamUserOAuth/Search/v0001";
    }

    protected SearchGroupInfoDB AllocateNewSearchDB() {
        return new SearchGroupInfoDB();
    }

    protected HelperDbListAdapter myCreateListAdapter() {
        return new GroupsListAdapter();
    }

    protected boolean myCbckShouldDisplayItemInList(GroupInfo item) {
        return item.HasPresentationData();
    }

    protected int GetTitlebarFormatStringForQuery() {
        return R.string.Search_Groups_Results;
    }
}
