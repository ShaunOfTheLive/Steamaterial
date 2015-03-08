package com.valvesoftware.android.steam.community.fragment;

import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import com.valvesoftware.android.steam.community.GenericListDB;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.GenericListDB.ListItemUpdatedListener;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import java.util.ArrayList;
import java.util.Calendar;

public abstract class BasePresentationListFragment<GenericDbItemInfo extends GenericListItem> extends ListFragment {
    protected Calendar m_calRecentChatsThreshold;
    protected ListDbListener m_listDbListener;
    protected ListView m_listView;
    protected ArrayList<GenericDbItemInfo> m_presentationArray;
    protected int m_umqCurrentServerTime;

    protected class HelperDbListAdapter extends ArrayAdapter<GenericDbItemInfo> {
        public HelperDbListAdapter(int textViewResourceId) {
            super(BasePresentationListFragment.this.getActivity(), textViewResourceId, BasePresentationListFragment.this.m_presentationArray);
        }

        public void RequestVisibleAvatars() {
            RequestVisibleAvatars(false);
        }

        public void RequestVisibleAvatars(boolean bDataSetChanged) {
            if (BasePresentationListFragment.this.getListAdapter() == this) {
                int iStart = BasePresentationListFragment.this.getListView().getFirstVisiblePosition();
                int iEnd = BasePresentationListFragment.this.getListView().getLastVisiblePosition();
                GenericListItem[] required = null;
                if (iEnd >= iStart && iStart >= 0) {
                    int numVisible = (iEnd - iStart) + 1;
                    required = new GenericListItem[numVisible];
                    int jj = 0;
                    while (jj < numVisible) {
                        if (BasePresentationListFragment.this.m_presentationArray.size() > iStart + jj) {
                            required[jj] = (GenericListItem) BasePresentationListFragment.this.m_presentationArray.get(iStart + jj);
                        }
                        if (required[jj] == null || required[jj].IsAvatarSmallLoaded()) {
                            required[jj] = null;
                        }
                        jj++;
                    }
                }
                BasePresentationListFragment.this.myListDb().RequestAvatarImage(required);
            }
        }

        public void notifyDataSetChanged() {
            super.notifyDataSetChanged();
            RequestVisibleAvatars(true);
        }
    }

    protected class ListDbListener implements ListItemUpdatedListener {
        protected HelperDbListAdapter m_adapter;

        protected ListDbListener() {
            this.m_adapter = null;
        }

        public void OnListItemInfoUpdated(ArrayList<Long> arrayList, boolean requireRefresh) {
            if (requireRefresh) {
                BasePresentationListFragment.this.UpdateGlobalInformationPreSort();
                BasePresentationListFragment.this.m_presentationArray.clear();
                for (GenericDbItemInfo tobj : BasePresentationListFragment.this.myListDb().GetItemsMap().values()) {
                    if (BasePresentationListFragment.this.myCbckShouldDisplayItemInList(tobj)) {
                        BasePresentationListFragment.this.m_presentationArray.add(tobj);
                    }
                }
                BasePresentationListFragment.this.myCbckProcessPresentationArray();
            }
            this.m_adapter.notifyDataSetChanged();
        }

        public void OnListItemInfoUpdateError(RequestBase req) {
        }

        public void OnListRefreshError(RequestBase req, boolean cached) {
            BasePresentationListFragment.this.myCbckOnListRefreshError(req);
        }

        public void OnListRequestsInProgress(boolean isRefreshing) {
            TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(BasePresentationListFragment.this.getActivity());
            if (titlebar != null) {
                titlebar.setRefreshInProgress(isRefreshing);
            }
        }
    }

    protected abstract void myCbckProcessPresentationArray();

    protected abstract boolean myCbckShouldDisplayItemInList(GenericDbItemInfo genericDbItemInfo);

    protected abstract HelperDbListAdapter myCreateListAdapter();

    protected abstract GenericListDB myListDb();

    public BasePresentationListFragment() {
        this.m_presentationArray = new ArrayList();
        this.m_listView = null;
        this.m_calRecentChatsThreshold = Calendar.getInstance();
        this.m_umqCurrentServerTime = 0;
        this.m_listDbListener = new ListDbListener();
    }

    protected void myCbckOnListRefreshError(RequestBase req) {
    }

    protected void refreshListView() {
        this.m_listDbListener.OnListItemInfoUpdated(null, true);
    }

    public void onResume() {
        super.onResume();
        myListDb().SetAutoRefreshIfDataMightBeStale(true);
    }

    public void onPause() {
        super.onPause();
        myListDb().SetAutoRefreshIfDataMightBeStale(false);
    }

    public void onDestroy() {
        super.onDestroy();
        myListDb().DeregisterCallback(this.m_listDbListener);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.friends_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.m_listDbListener.m_adapter = myCreateListAdapter();
        refreshListView();
        if (this.m_listView == null) {
            this.m_listView = getListView();
            this.m_listView.setTextFilterEnabled(false);
        }
        setListAdapter(this.m_listDbListener.m_adapter);
        this.m_listDbListener.m_adapter.RequestVisibleAvatars();
        myListDb().RegisterCallback(this.m_listDbListener);
    }

    protected void UpdateGlobalInformationPreSort() {
        this.m_calRecentChatsThreshold = Calendar.getInstance();
        this.m_calRecentChatsThreshold.setTimeInMillis(this.m_calRecentChatsThreshold.getTimeInMillis() - (((long) SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingChatsRecent.getRadioSelectorItemValue(SteamCommunityApplication.GetInstance().getApplicationContext()).value) * 1000));
        SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
        this.m_umqCurrentServerTime = 0;
        if (dbService != null) {
            this.m_umqCurrentServerTime = dbService.getSteamUmqCurrentServerTime();
        }
    }
}
