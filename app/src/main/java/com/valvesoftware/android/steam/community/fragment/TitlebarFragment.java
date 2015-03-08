package com.valvesoftware.android.steam.community.fragment;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.AndroidUtils;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.fragment.TitlebarFragment.TitlebarButtonHander;

public class TitlebarFragment extends Fragment {
    private boolean m_bRefreshEnabled;
    private boolean m_bRefreshingNow;
    private TitlebarButtonHander m_searchHandler;

    public static interface TitlebarButtonHander {
        void onTitlebarButtonClicked(int i);
    }

    class AnonymousClass_2 implements OnClickListener {
        final /* synthetic */ int val$btnid;
        final /* synthetic */ TitlebarButtonHander val$hdlr;

        AnonymousClass_2(int i, TitlebarButtonHander titlebarButtonHander) {
            this.val$btnid = i;
            this.val$hdlr = titlebarButtonHander;
        }

        public void onClick(View v) {
            if ((this.val$btnid != 2131296312 || !TitlebarFragment.this.m_bRefreshingNow) && this.val$hdlr != null) {
                this.val$hdlr.onTitlebarButtonClicked(this.val$btnid);
            }
        }
    }

    public TitlebarFragment() {
        this.m_bRefreshingNow = false;
        this.m_bRefreshEnabled = true;
        this.m_searchHandler = null;
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.titlebar_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        View v = getActivity().findViewById(R.id.titleNavActivationButton);
        if (v != null) {
            v.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    NavigationFragment nav = ActivityHelper.GetNavigationFragmentForActivity(TitlebarFragment.this.getActivity());
                    if (nav != null) {
                        nav.onNavActivationButtonClicked();
                    }
                }
            });
        }
        int title_resid = getActivity().getIntent().getIntExtra("title_resid", 0);
        if (title_resid != 0) {
            setTitleLabel(title_resid);
        }
    }

    public boolean overrideActivityOnSearchPressed() {
        if (this.m_searchHandler == null) {
            return false;
        }
        this.m_searchHandler.onTitlebarButtonClicked(R.id.titleNavSearchButton);
        return true;
    }

    public void onResume() {
        super.onResume();
    }

    public void setTitleLabel(String s) {
        TextView v = (TextView) getActivity().findViewById(R.id.titleLabel);
        if (v != null) {
            AndroidUtils.setTextViewText(v, s);
        }
    }

    public void setTitleLabel(int resid) {
        TextView v = (TextView) getActivity().findViewById(R.id.titleLabel);
        if (v != null) {
            v.setText(resid);
        }
    }

    private void attachButtonHandler(int btnid, int residBtn, TitlebarButtonHander hdlr) {
        int i = R.color.almost_fully_transparent;
        View v = getActivity().findViewById(btnid);
        if (v != null) {
            v.setOnClickListener(new AnonymousClass_2(btnid, hdlr));
            if (btnid == 2131296312) {
                if (!this.m_bRefreshEnabled) {
                    residBtn = 2131099664;
                } else if (this.m_bRefreshingNow) {
                    residBtn = R.drawable.icon_coverup;
                }
                v.setBackgroundResource(residBtn);
                View vQuarter = getActivity().findViewById(R.id.titleNavRefreshQuarter);
                if (vQuarter != null && this.m_bRefreshingNow) {
                    vQuarter.setVisibility(this.m_bRefreshEnabled ? 0 : AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                    if (this.m_bRefreshEnabled) {
                        i = R.drawable.icon_quarter;
                    }
                    vQuarter.setBackgroundResource(i);
                    return;
                }
                return;
            }
            if (hdlr == null) {
                residBtn = 2131099664;
            }
            v.setBackgroundResource(residBtn);
        }
    }

    public void setRefreshInProgress(boolean isRefreshingNow) {
        int i = R.color.almost_fully_transparent;
        if (isRefreshingNow != this.m_bRefreshingNow) {
            this.m_bRefreshingNow = isRefreshingNow;
            View vRefresh = getActivity().findViewById(R.id.titleNavRefreshButton);
            View vQuarter = getActivity().findViewById(R.id.titleNavRefreshQuarter);
            if (vQuarter != null && vRefresh != null) {
                if (this.m_bRefreshingNow) {
                    int i2;
                    vRefresh.setBackgroundResource(this.m_bRefreshEnabled ? R.drawable.icon_coverup : 2131099664);
                    if (this.m_bRefreshEnabled) {
                        i2 = 0;
                    } else {
                        i2 = 8;
                    }
                    vQuarter.setVisibility(i2);
                    if (this.m_bRefreshEnabled) {
                        i = R.drawable.icon_quarter;
                    }
                    vQuarter.setBackgroundResource(i);
                    vQuarter.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.titlebar_refreshing));
                    return;
                }
                vRefresh.setBackgroundResource(this.m_bRefreshEnabled ? R.drawable.icon_refresh : 2131099664);
                vQuarter.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                vQuarter.setBackgroundResource(R.color.almost_fully_transparent);
                vQuarter.clearAnimation();
            }
        }
    }

    public void setRefreshHandler(TitlebarButtonHander hdlr) {
        this.m_bRefreshEnabled = hdlr != null;
        attachButtonHandler(R.id.titleNavRefreshButton, R.drawable.icon_refresh, hdlr);
    }

    public void setSearchHandler(TitlebarButtonHander hdlr) {
        setSearchHandler(hdlr, R.drawable.icon_search);
    }

    public void setSearchHandler(TitlebarButtonHander hdlr, int drawable) {
        this.m_searchHandler = hdlr;
        attachButtonHandler(R.id.titleNavSearchButton, drawable, hdlr);
    }
}
