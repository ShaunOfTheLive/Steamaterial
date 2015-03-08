package com.valvesoftware.android.steam.community.fragment;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.AndroidUtils;
import com.valvesoftware.android.steam.community.Config;
import com.valvesoftware.android.steam.community.GenericListDB;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.GroupInfo;
import com.valvesoftware.android.steam.community.GroupInfo.GroupCategoryInList;
import com.valvesoftware.android.steam.community.GroupInfo.GroupRelationship;
import com.valvesoftware.android.steam.community.GroupInfo.GroupType;
import com.valvesoftware.android.steam.community.GroupInfoDB;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamUriHandler;
import com.valvesoftware.android.steam.community.SteamUriHandler.Command;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.activity.SearchGroupsActivity;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment.TitlebarButtonHander;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;

public class GroupListFragment extends BasePresentationListFragmentWithSearch<GroupInfo> {
    private OnClickListener dummyClickListener;
    private OnClickListener groupListClickListener;
    private BaseFragmentWithLogin m_login;
    private TitlebarButtonHander m_refreshHandler;
    private Comparator<GroupInfo> m_sorter;

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
                v.setOnClickListener(GroupListFragment.this.groupListClickListener);
            }
            GenericListItem groupInfo = (GroupInfo) GroupListFragment.this.m_presentationArray.get(position);
            if (groupInfo != null) {
                if (!groupInfo.IsAvatarSmallLoaded()) {
                    RequestVisibleAvatars();
                }
                TextView labelView = (TextView) v.findViewById(R.id.label);
                TextView nameView = (TextView) v.findViewById(R.id.name);
                TextView lblMembersTotal = (TextView) v.findViewById(R.id.groupMembersTotal);
                TextView lblMembersOnline = (TextView) v.findViewById(R.id.groupMembersOnline);
                ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
                ImageView avatarViewFrame = (ImageView) v.findViewById(R.id.avatar_frame);
                TextView steamidView = (TextView) v.findViewById(R.id.steamid);
                if (position == 0 || groupInfo.m_categoryInList != ((GroupInfo) GroupListFragment.this.m_presentationArray.get(position - 1)).m_categoryInList) {
                    labelView.setText(groupInfo.m_categoryInList.GetDisplayString());
                    labelView.setVisibility(0);
                } else {
                    labelView.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                }
                labelView.setOnClickListener(GroupListFragment.this.dummyClickListener);
                steamidView.setText(Long.toString(groupInfo.m_steamID.longValue()));
                AndroidUtils.setTextViewText(nameView, groupInfo.m_groupName);
                if (groupInfo != GroupListFragment.this.GetSearchItem()) {
                    avatarView.setImageBitmap(groupInfo.GetAvatarSmall());
                    avatarViewFrame.setVisibility(0);
                    lblMembersTotal.setText(Integer.toString(groupInfo.m_numMembersTotal) + " " + appContext.getResources().getString(R.string.Group_Num_Members_Total));
                    lblMembersOnline.setText(Integer.toString(groupInfo.m_numMembersOnline) + " " + appContext.getResources().getString(R.string.Group_Num_Members_Online));
                } else {
                    avatarView.setImageResource(R.drawable.icon_search);
                    avatarViewFrame.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                    AndroidUtils.setTextViewText(lblMembersTotal, GroupListFragment.this.getSearchModeText());
                    lblMembersOnline.setText("");
                }
            }
            return v;
        }
    }

    public GroupListFragment() {
        this.m_login = new BaseFragmentWithLogin(this);
        this.m_refreshHandler = new TitlebarButtonHander() {
            public void onTitlebarButtonClicked(int id) {
                if (SteamWebApi.IsLoggedIn()) {
                    GroupListFragment.this.myListDb().RefreshFromHttpOnly();
                }
            }
        };
        this.groupListClickListener = new OnClickListener() {
            public void onClick(View v) {
                TextView profileURLView = (TextView) v.findViewById(R.id.steamid);
                String sSteamID = "0";
                if (profileURLView != null) {
                    sSteamID = profileURLView.getText().toString();
                }
                if (sSteamID.equals("0")) {
                    GroupListFragment.this.getActivity().startActivity(new Intent().setClass(GroupListFragment.this.getActivity(), SearchGroupsActivity.class).addFlags(536870912).addFlags(268435456).setData(Uri.parse("searchgroup://" + System.currentTimeMillis() + GroupListFragment.this.getSearchModeText())).putExtra("query", GroupListFragment.this.getSearchModeText()));
                    return;
                }
                GroupInfo entry = ((GroupInfoDB) GroupListFragment.this.myListDb()).GetGroupInfo(Long.valueOf(sSteamID));
                if (entry != null) {
                    GroupListFragment.this.getActivity().startActivity(new Intent().addFlags(402653184).setClass(GroupListFragment.this.getActivity(), SteamMobileUriActivity.class).setData(Uri.parse(SteamUriHandler.MOBILE_PROTOCOL + Command.openurl + "?url=" + Config.URL_COMMUNITY_BASE + entry.m_profileURL)).setAction("android.intent.action.VIEW"));
                }
            }
        };
        this.dummyClickListener = new OnClickListener() {
            public void onClick(View v) {
            }
        };
        this.m_sorter = new Comparator<GroupInfo>() {
            public int compare(GroupInfo o1, GroupInfo o2) {
                if (o1.m_categoryInList != o2.m_categoryInList) {
                    return o1.m_categoryInList.ordinal() < o2.m_categoryInList.ordinal() ? -1 : 1;
                } else {
                    return o1.m_groupName.compareToIgnoreCase(o2.m_groupName);
                }
            }
        };
    }

    protected GroupInfo myDbItemCreateSearchItem() {
        GroupInfo info = new GroupInfo();
        info.m_steamID = new Long(0);
        info.m_groupType = GroupType.DISABLED;
        info.m_categoryInList = GroupCategoryInList.SEARCH_ALL;
        info.m_relationship = GroupRelationship.Member;
        info.m_groupName = SteamCommunityApplication.GetInstance().getResources().getString(R.string.Group_Search_All);
        return info;
    }

    protected GenericListDB myListDb() {
        return SteamCommunityApplication.GetInstance().GetGroupInfoDB();
    }

    protected HelperDbListAdapter myCreateListAdapter() {
        return new GroupsListAdapter();
    }

    protected boolean myCbckShouldDisplayItemInList(GroupInfo item) {
        return item.HasPresentationData() && ApplySearchFilterBeforeDisplay(item.m_groupName);
    }

    protected void myCbckProcessPresentationArray() {
        Iterator i$ = this.m_presentationArray.iterator();
        while (i$.hasNext()) {
            GroupInfo info = (GroupInfo) i$.next();
            if (info.m_relationship == GroupRelationship.Invited) {
                info.m_categoryInList = GroupCategoryInList.REQUEST_INVITE;
            } else if (info.m_groupType == GroupType.OFFICIAL) {
                info.m_categoryInList = GroupCategoryInList.OFFICIAL;
            } else if (info.m_groupType == GroupType.PRIVATE) {
                info.m_categoryInList = GroupCategoryInList.PRIVATE;
            } else {
                info.m_categoryInList = GroupCategoryInList.PUBLIC;
            }
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
            titlebar.setTitleLabel((int) R.string.Groups);
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
}
