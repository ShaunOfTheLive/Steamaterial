package com.valvesoftware.android.steam.community.fragment;

import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.GenericListDB;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public abstract class BaseSearchResultsListFragment<GenericDbItemInfo extends GenericListItem, SearchListDB extends GenericListDB> extends BasePresentationListFragment<GenericDbItemInfo> {
    private SearchListDB m_db;
    private TextView m_footerBtnNext;
    private TextView m_footerBtnPrev;
    private View m_footerButtons;
    private OnClickListener m_pagingOnClickListener;
    private TextView m_progressLabel;
    protected int m_queryActualResults;
    protected int m_queryOffset;
    protected int m_queryPageSize;
    protected String m_queryText;
    protected int m_queryTotalResults;
    protected final HashMap<Long, Integer> m_searchResultsOrderMap;
    private Comparator<GenericDbItemInfo> m_sorter;

    protected abstract SearchListDB AllocateNewSearchDB();

    protected abstract int GetTitlebarFormatStringForQuery();

    public BaseSearchResultsListFragment() {
        this.m_progressLabel = null;
        this.m_footerButtons = null;
        this.m_footerBtnPrev = null;
        this.m_footerBtnNext = null;
        this.m_db = null;
        this.m_queryText = "";
        this.m_queryOffset = 0;
        this.m_queryPageSize = 50;
        this.m_queryActualResults = -1;
        this.m_queryTotalResults = -1;
        this.m_searchResultsOrderMap = new HashMap();
        this.m_pagingOnClickListener = new OnClickListener() {
            public void onClick(View v) {
                BaseSearchResultsListFragment baseSearchResultsListFragment = BaseSearchResultsListFragment.this;
                baseSearchResultsListFragment.m_queryOffset = ((v == BaseSearchResultsListFragment.this.m_footerBtnPrev ? -1 : 1) * BaseSearchResultsListFragment.this.m_queryPageSize) + baseSearchResultsListFragment.m_queryOffset;
                if (BaseSearchResultsListFragment.this.m_queryOffset < 0) {
                    BaseSearchResultsListFragment.this.m_queryOffset = 0;
                }
                if (BaseSearchResultsListFragment.this.m_queryOffset > BaseSearchResultsListFragment.this.m_queryTotalResults - BaseSearchResultsListFragment.this.m_queryPageSize) {
                    BaseSearchResultsListFragment.this.m_queryOffset = BaseSearchResultsListFragment.this.m_queryTotalResults - BaseSearchResultsListFragment.this.m_queryPageSize;
                }
                BaseSearchResultsListFragment.this.m_queryActualResults = -1;
                BaseSearchResultsListFragment.this.m_queryTotalResults = -1;
                BaseSearchResultsListFragment.this.SwitchConfiguration(BaseSearchResultsListFragment.this.m_queryText);
            }
        };
        this.m_sorter = new Comparator<GenericDbItemInfo>() {
            public int compare(GenericDbItemInfo o1, GenericDbItemInfo o2) {
                Integer i1 = (Integer) BaseSearchResultsListFragment.this.m_searchResultsOrderMap.get(o1.m_steamID);
                Integer i2 = (Integer) BaseSearchResultsListFragment.this.m_searchResultsOrderMap.get(o2.m_steamID);
                if (i1 != null && i2 != null) {
                    return i1.compareTo(i2);
                }
                if (i1 == null && i2 == null) {
                    return 0;
                }
                return i1 != null ? -1 : 1;
            }
        };
    }

    public void SwitchConfiguration(String query) {
        boolean bFirstTime = true;
        if (myListDb() != null) {
            bFirstTime = false;
            SteamCommunityApplication.GetInstance().unregisterReceiver(myListDb());
            myListDb().DeregisterCallback(this.m_listDbListener);
            this.m_presentationArray.clear();
        }
        this.m_queryText = query;
        this.m_db = AllocateNewSearchDB();
        myListDb().RefreshFromHttpOnly();
        if (this.m_progressLabel != null) {
            this.m_progressLabel.setText(R.string.Search_Label_Searching);
        }
        if (this.m_footerButtons != null) {
            this.m_footerButtons.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        }
        if (!bFirstTime) {
            myListDb().RegisterCallback(this.m_listDbListener);
            refreshListView();
        }
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setTitleLabel(getActivity().getResources().getString(GetTitlebarFormatStringForQuery()).replace("#", query));
        }
    }

    protected void myCbckOnListRefreshError(RequestBase req) {
        if (this.m_progressLabel != null) {
            this.m_progressLabel.setText(R.string.Search_Label_Failed);
        }
        if (this.m_footerButtons != null) {
            this.m_footerButtons.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        }
    }

    protected GenericListDB myListDb() {
        return this.m_db;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        SwitchConfiguration(getActivity().getIntent().getStringExtra("query"));
        super.onActivityCreated(savedInstanceState);
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setRefreshHandler(null);
        }
        this.m_progressLabel = (TextView) getActivity().findViewById(R.id.search_progress_label);
        if (this.m_progressLabel != null) {
            this.m_progressLabel.setText(R.string.Search_Label_Searching);
        }
        this.m_footerButtons = getActivity().findViewById(R.id.search_footer_buttons);
        if (this.m_footerButtons != null) {
            this.m_footerButtons.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        }
        this.m_footerBtnPrev = (TextView) getActivity().findViewById(R.id.search_footer_button_prev);
        this.m_footerBtnNext = (TextView) getActivity().findViewById(R.id.search_footer_button_next);
    }

    public void onDestroy() {
        super.onDestroy();
        SteamCommunityApplication.GetInstance().unregisterReceiver(this.m_db);
        this.m_presentationArray.clear();
        this.m_db = null;
    }

    public void refreshListView() {
        super.refreshListView();
        if (this.m_progressLabel != null && this.m_db != null && this.m_queryTotalResults >= 0 && this.m_queryActualResults >= 0) {
            int resid = R.string.Search_Label_Results;
            if (this.m_queryTotalResults <= 0) {
                resid = R.string.Search_Label_ResultsNone;
            } else if (this.m_queryActualResults == this.m_queryTotalResults) {
                resid = R.string.Search_Label_ResultsAll;
            }
            this.m_progressLabel.setText(getActivity().getResources().getString(resid).replace("#", this.m_queryActualResults == this.m_queryTotalResults ? String.valueOf(this.m_queryTotalResults) : String.valueOf(this.m_queryOffset + 1) + "-" + String.valueOf(this.m_queryOffset + this.m_queryActualResults)).replace("$", String.valueOf(this.m_queryTotalResults)));
        }
    }

    protected void myCbckProcessPresentationArray() {
        OnClickListener onClickListener = null;
        if (this.m_db != null && this.m_queryTotalResults >= 0 && this.m_queryActualResults >= 0 && this.m_queryTotalResults > this.m_queryPageSize && !this.m_presentationArray.isEmpty()) {
            boolean on;
            if (this.m_footerButtons != null) {
                this.m_footerButtons.setVisibility(0);
            }
            if (this.m_footerBtnPrev != null) {
                OnClickListener onClickListener2;
                on = this.m_queryOffset > 0;
                this.m_footerBtnPrev.setText(on ? "<<" : " ");
                TextView textView = this.m_footerBtnPrev;
                if (on) {
                    onClickListener2 = this.m_pagingOnClickListener;
                } else {
                    onClickListener2 = null;
                }
                textView.setOnClickListener(onClickListener2);
            }
            if (this.m_footerBtnNext != null) {
                if (this.m_queryOffset + this.m_queryPageSize < this.m_queryTotalResults) {
                    on = true;
                } else {
                    on = false;
                }
                this.m_footerBtnNext.setText(on ? ">>" : " ");
                TextView textView2 = this.m_footerBtnNext;
                if (on) {
                    onClickListener = this.m_pagingOnClickListener;
                }
                textView2.setOnClickListener(onClickListener);
            }
        }
        Collections.sort(this.m_presentationArray, this.m_sorter);
    }
}
