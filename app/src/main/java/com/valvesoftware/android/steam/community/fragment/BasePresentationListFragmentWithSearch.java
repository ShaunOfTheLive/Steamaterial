package com.valvesoftware.android.steam.community.fragment;

import android.os.Bundle;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.TextView.OnEditorActionListener;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment.TitlebarButtonHander;

public abstract class BasePresentationListFragmentWithSearch<GenericDbItemInfo extends GenericListItem> extends BasePresentationListFragment<GenericDbItemInfo> {
    private boolean m_bSearchModeActive;
    private GenericDbItemInfo m_mySearchItem;
    private OnClickListener m_searchDeactivateButtonHdlr;
    private EditText m_searchEditText;
    private TitlebarButtonHander m_searchHandler;
    private String m_searchModeFilter;
    private OnEditorActionListener m_searchTxtActionListener;
    private TextWatcher m_searchTxtWatcher;

    protected abstract GenericDbItemInfo myDbItemCreateSearchItem();

    public BasePresentationListFragmentWithSearch() {
        this.m_mySearchItem = null;
        this.m_searchHandler = new TitlebarButtonHander() {
            public void onTitlebarButtonClicked(int id) {
                if (SteamWebApi.IsLoggedIn()) {
                    BasePresentationListFragmentWithSearch.this.activateSearch(true);
                }
            }
        };
        this.m_searchDeactivateButtonHdlr = new OnClickListener() {
            public void onClick(View v) {
                BasePresentationListFragmentWithSearch.this.activateSearch(false);
            }
        };
        this.m_searchTxtActionListener = new OnEditorActionListener() {
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == 3) {
                    BasePresentationListFragmentWithSearch.this.searchFilterUpdateList();
                    BasePresentationListFragmentWithSearch.this.hideOnscreenKeyboard();
                }
                return true;
            }
        };
        this.m_searchTxtWatcher = new TextWatcher() {
            public void afterTextChanged(Editable s) {
                BasePresentationListFragmentWithSearch.this.searchFilterUpdateList();
            }

            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }
        };
        this.m_searchEditText = null;
        this.m_searchModeFilter = null;
        this.m_bSearchModeActive = false;
    }

    protected GenericDbItemInfo GetSearchItem() {
        if (this.m_mySearchItem == null) {
            this.m_mySearchItem = myDbItemCreateSearchItem();
        }
        return this.m_mySearchItem;
    }

    public boolean overrideActivityOnBackPressed() {
        if (!this.m_bSearchModeActive) {
            return false;
        }
        activateSearch(false);
        return true;
    }

    protected void activateSearch(boolean active) {
        if (active != this.m_bSearchModeActive) {
            this.m_bSearchModeActive = active;
            TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
            if (titlebar != null) {
                titlebar.setSearchHandler(active ? null : this.m_searchHandler, R.drawable.icon_search_add);
            }
            View v = getActivity().findViewById(R.id.list_search_bar);
            if (v != null) {
                v.setVisibility(active ? 0 : AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                if (active) {
                    if (this.m_searchEditText == null) {
                        View upbtn = v.findViewById(R.id.list_search_bar_button);
                        if (upbtn != null) {
                            upbtn.setOnClickListener(this.m_searchDeactivateButtonHdlr);
                        }
                        this.m_searchEditText = (EditText) v.findViewById(R.id.list_search_bar_text);
                        if (this.m_searchEditText != null) {
                            this.m_searchEditText.addTextChangedListener(this.m_searchTxtWatcher);
                            this.m_searchEditText.setOnEditorActionListener(this.m_searchTxtActionListener);
                        }
                    }
                    if (this.m_searchEditText != null) {
                        this.m_searchEditText.post(new Runnable() {
                            public void run() {
                                BasePresentationListFragmentWithSearch.this.m_searchEditText.requestFocusFromTouch();
                            }
                        });
                        return;
                    }
                    return;
                }
                if (this.m_searchEditText != null) {
                    this.m_searchEditText.setText("");
                }
                this.m_searchModeFilter = null;
                refreshListView();
                this.m_listView.requestFocusFromTouch();
                hideOnscreenKeyboard();
            }
        }
    }

    private void searchFilterUpdateList() {
        if (this.m_searchEditText != null) {
            this.m_searchModeFilter = this.m_searchEditText.getText().toString().toLowerCase();
            refreshListView();
        }
    }

    private void hideOnscreenKeyboard() {
        InputMethodManager imm = (InputMethodManager) getActivity().getSystemService("input_method");
        if (imm != null && this.m_searchEditText != null) {
            imm.hideSoftInputFromWindow(this.m_searchEditText.getWindowToken(), 0);
        }
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        TitlebarFragment titlebar = ActivityHelper.GetTitlebarFragmentForActivity(getActivity());
        if (titlebar != null) {
            titlebar.setSearchHandler(this.m_searchHandler, R.drawable.icon_search_add);
        }
    }

    public void onResume() {
        super.onResume();
        if (SteamWebApi.IsLoggedIn()) {
            refreshListView();
        }
    }

    protected void myCbckProcessPresentationArray() {
        if (this.m_searchModeFilter != null && this.m_searchModeFilter.length() >= 3) {
            GenericDbItemInfo item = GetSearchItem();
            if (item != null) {
                this.m_presentationArray.add(item);
            }
        }
    }

    protected boolean ApplySearchFilterBeforeDisplay(String sSearchable) {
        return this.m_searchModeFilter == null || this.m_searchModeFilter.equals("") || (sSearchable != null && sSearchable.toLowerCase().indexOf(this.m_searchModeFilter) >= 0);
    }

    protected String getSearchModeText() {
        return this.m_searchEditText.getText().toString();
    }
}
