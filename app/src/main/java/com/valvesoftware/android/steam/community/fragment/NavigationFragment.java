package com.valvesoftware.android.steam.community.fragment;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.AnimationUtils;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.valvesoftware.android.steam.community.AndroidUtils;
import com.valvesoftware.android.steam.community.Config;
import com.valvesoftware.android.steam.community.FriendInfo;
import com.valvesoftware.android.steam.community.R;
import com.valvesoftware.android.steam.community.SteamCommunityApplication;
import com.valvesoftware.android.steam.community.SteamDBService;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_LOGININFO_DATA;
import com.valvesoftware.android.steam.community.SteamWebApi;
import com.valvesoftware.android.steam.community.activity.ActivityHelper;
import com.valvesoftware.android.steam.community.activity.CommunityActivity;
import com.valvesoftware.android.steam.community.activity.CommunityGroupsActivity;
import com.valvesoftware.android.steam.community.activity.FragmentActivityWithNavigationSupport;
import com.valvesoftware.android.steam.community.activity.LoginActivity.LoginAction;
import com.valvesoftware.android.steam.community.activity.SettingsActivity;
import com.valvesoftware.android.steam.community.activity.SteamMobileUriActivity;
import com.valvesoftware.android.steam.community.fragment.NavigationFragment.NavigationItem;
import com.valvesoftware.android.steam.community.fragment.NavigationFragment.NavigationItem_ClassActivity;
import com.valvesoftware.android.steam.community.fragment.NavigationFragment.NavigationItem_UriActivity_Base;
import java.util.ArrayList;
import java.util.Iterator;

public class NavigationFragment extends ListFragment {
    public static final Uri URI_AccountDetails;
    private static final Uri URI_Blotter;
    public static final Uri URI_Cart;
    public static final Uri URI_Catalog;
    public static final Uri URI_Feeds_SteamNews;
    public static final Uri URI_Feeds_SteamSpecials;
    public static final Uri URI_Feeds_Syndicated;
    public static final Uri URI_Store_Search;
    public static final Uri URI_Wishlist;
    private NavigationItemListAdapter m_adapter;
    private AnimationListener m_animListener;
    private boolean m_bNavActive;
    private boolean m_bNavAnimating;
    private boolean m_bRequestToHideNavWhileAnimating;
    public OnClickListener m_dummyClickListener;
    public NavigationItem_UriActivity_Profile m_itemProfile;
    private ArrayList<NavigationItem> m_items;
    private ArrayList<NavigationItem> m_itemsAvailable;
    private ListView m_listView;
    public OnClickListener m_navItemClickListener;
    private View m_splitViewContents;
    private View m_splitViewNavigation;

    private static class ExitingProgressDialog extends ProgressDialog {
        public ExitingProgressDialog(Context context) {
            super(context);
        }

        public void onBackPressed() {
        }
    }

    public static abstract class NavigationItem {
        public boolean m_bShowIfNotSignedIn;
        public int resid_category;
        public int resid_icon;

        public abstract String getDetails();

        public abstract int getName();

        public abstract void onClick();
    }

    private class NavigationItemListAdapter extends ArrayAdapter<NavigationItem> {
        public NavigationItemListAdapter() {
            super(NavigationFragment.this.getActivity(), 2130903054, NavigationFragment.this.m_items);
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            View v = convertView;
            Context appContext = SteamCommunityApplication.GetInstance();
            if (v == null) {
                v = ((LayoutInflater) appContext.getSystemService("layout_inflater")).inflate(R.layout.navigation_list_item, null);
                v.setClickable(true);
                v.setOnClickListener(NavigationFragment.this.m_navItemClickListener);
            }
            NavigationItem info = (NavigationItem) NavigationFragment.this.m_items.get(position);
            if (info != null) {
                NavigationFragment.this.setupView(v, info, position);
            }
            return v;
        }
    }

    public class NavigationItem_ClassActivity extends NavigationItem {
        public Class<?> activity;
        public String info;
        public int resid_name;

        public int getName() {
            return this.resid_name;
        }

        public String getDetails() {
            return "";
        }

        public void onClick() {
            NavigationFragment.this.getActivity().startActivity(getNewActivityIntent());
        }

        public Intent getNewActivityIntent() {
            return new Intent().addFlags(402653184).setClass(NavigationFragment.this.getActivity(), this.activity);
        }
    }

    public class NavigationItem_ClassActivity_SingleTop extends NavigationItem_ClassActivity {
        public NavigationItem_ClassActivity_SingleTop() {
            super();
        }

        public Intent getNewActivityIntent() {
            Intent res = super.getNewActivityIntent().addFlags(536870912);
            res.setFlags(res.getFlags() & -134217729);
            return res;
        }
    }

    public class NavigationItem_ExitApplication extends NavigationItem {
        public int getName() {
            return R.string.Exit_Application;
        }

        public String getDetails() {
            return "";
        }

        public void onClick() {
            FragmentActivityWithNavigationSupport.finishAllExceptOne((FragmentActivityWithNavigationSupport) NavigationFragment.this.getActivity());
            if (SteamCommunityApplication.GetInstance().GetSteamDB() != null) {
                SteamCommunityApplication.GetInstance().m_bApplicationExiting = true;
                REQ_ACT_LOGININFO_DATA obj = new REQ_ACT_LOGININFO_DATA();
                obj.sOAuthToken = null;
                obj.sSteamID = null;
                SteamWebApi.SubmitSimpleActionRequest(SteamDBService.REQ_ACT_LOGININFO, obj);
                try {
                    ExitingProgressDialog dlg = new ExitingProgressDialog(NavigationFragment.this.getActivity());
                    dlg.setMessage(NavigationFragment.this.getActivity().getString(R.string.Logging_OutOf_Steam));
                    dlg.setIndeterminate(true);
                    dlg.setCancelable(false);
                    dlg.show();
                } catch (Exception e) {
                }
            }
        }
    }

    public class NavigationItem_UriActivity_Base extends NavigationItem_ClassActivity {
        public NavigationItem_UriActivity_Base() {
            super();
            this.activity = SteamMobileUriActivity.class;
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setAction("android.intent.action.VIEW").putExtra("title_resid", this.resid_name);
        }
    }

    public class NavigationItem_UriActivity_AccountDetails extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_AccountDetails() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_AccountDetails);
        }
    }

    public class NavigationItem_UriActivity_Blotter extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Blotter() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Blotter);
        }
    }

    public class NavigationItem_UriActivity_Cart extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Cart() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Cart);
        }
    }

    public class NavigationItem_UriActivity_Catalog extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Catalog() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Catalog);
        }
    }

    public class NavigationItem_UriActivity_Feeds_SteamNews extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Feeds_SteamNews() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Feeds_SteamNews);
        }
    }

    public class NavigationItem_UriActivity_Feeds_SteamSpecials extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Feeds_SteamSpecials() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Feeds_SteamSpecials);
        }
    }

    public class NavigationItem_UriActivity_Feeds_Syndicated extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Feeds_Syndicated() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Feeds_Syndicated);
        }
    }

    public class NavigationItem_UriActivity_Profile extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Profile() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(Uri.parse("steammobile://openurl?url=" + Config.URL_COMMUNITY_BASE + "/profiles/" + SteamWebApi.GetLoginSteamID()));
        }

        public void onClick() {
            if (SteamWebApi.IsLoggedIn()) {
                super.onClick();
            } else {
                ActivityHelper.PresentLoginActivity(NavigationFragment.this.getActivity(), LoginAction.LOGIN_DEFAULT);
            }
        }
    }

    public class NavigationItem_UriActivity_StoreSearch extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_StoreSearch() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Store_Search);
        }
    }

    public class NavigationItem_UriActivity_Wishlist extends NavigationItem_UriActivity_Base {
        public NavigationItem_UriActivity_Wishlist() {
            super();
        }

        public Intent getNewActivityIntent() {
            return super.getNewActivityIntent().setData(URI_Wishlist);
        }
    }

    public NavigationFragment() {
        this.m_itemProfile = new NavigationItem_UriActivity_Profile();
        this.m_itemsAvailable = new ArrayList();
        this.m_items = new ArrayList();
        this.m_listView = null;
        this.m_adapter = null;
        this.m_splitViewNavigation = null;
        this.m_splitViewContents = null;
        this.m_navItemClickListener = new OnClickListener() {
            public void onClick(View v) {
                if (!NavigationFragment.this.m_bNavAnimating && NavigationFragment.this.m_bNavActive) {
                    TextView steamidView = (TextView) v.findViewById(R.id.steamid);
                    if (steamidView != null) {
                        int val = Integer.valueOf(steamidView.getText().toString()).intValue();
                        if (val >= -1 && val < NavigationFragment.this.m_items.size()) {
                            NavigationFragment.this.onNavActivationButtonClicked();
                            if (val >= 0) {
                                ((NavigationItem) NavigationFragment.this.m_items.get(val)).onClick();
                            } else {
                                NavigationFragment.this.m_itemProfile.onClick();
                            }
                        }
                    }
                }
            }
        };
        this.m_dummyClickListener = new OnClickListener() {
            public void onClick(View v) {
            }
        };
        this.m_bNavActive = false;
        this.m_bNavAnimating = false;
        this.m_bRequestToHideNavWhileAnimating = false;
        this.m_animListener = new AnimationListener() {
            public void onAnimationEnd(Animation animation) {
                NavigationFragment.this.m_bNavAnimating = false;
                if (!NavigationFragment.this.m_bNavActive) {
                    NavigationFragment.this.m_splitViewNavigation.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
                } else if (NavigationFragment.this.m_bRequestToHideNavWhileAnimating) {
                    NavigationFragment.this.m_bRequestToHideNavWhileAnimating = false;
                    NavigationFragment.this.onNavActivationButtonClicked();
                }
            }

            public void onAnimationRepeat(Animation animation) {
            }

            public void onAnimationStart(Animation animation) {
            }
        };
    }

    static {
        URI_Catalog = Uri.parse("steammobile://opencategoryurl?url=" + Config.URL_STORE_BASE_INSECURE + "/api/mobilestorefrontcategories/v0001");
        URI_Store_Search = Uri.parse("steammobile://opencategoryurl?url=" + Config.URL_STORE_BASE_INSECURE + "/api/mobilestorefrontindexcategories/v0001");
        URI_AccountDetails = Uri.parse("steammobile://opencategoryurl?url=" + Config.URL_STORE_BASE_INSECURE + "/api/mobilestorefrontaccountdetailscategories/v0001");
        URI_Feeds_SteamNews = Uri.parse("steammobile://openurl?url=" + Config.URL_STORE_BASE + "/mobilenews/steamnews");
        URI_Feeds_SteamSpecials = Uri.parse("steammobile://openurl?url=" + Config.URL_STORE_BASE + "/mobilenews/newsposts?feed=steam_specials");
        URI_Feeds_Syndicated = Uri.parse("steammobile://openurl?url=" + Config.URL_STORE_BASE + "/mobilenews/syndicatednews");
        URI_Wishlist = Uri.parse("steammobile://openurl?url=" + Config.URL_COMMUNITY_BASE + "/my/wishlist/");
        URI_Cart = Uri.parse("steammobile://openurl?url=" + Config.URL_STORE_BASE + "/mobilecart/");
        URI_Blotter = Uri.parse("steammobile://openurl?url=" + Config.URL_COMMUNITY_BASE + "/my/blotter/");
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.navigation_fragment, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.m_splitViewNavigation = getActivity().findViewById(R.id.splitView_Navigation);
        this.m_splitViewContents = getActivity().findViewById(R.id.splitView_Contents);
        View v = getActivity().findViewById(R.id.navFragment_ContentButton);
        if (v != null) {
            v.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    NavigationFragment.this.onNavActivationButtonClicked();
                }
            });
        }
        v = getActivity().findViewById(R.id.navFragment_TitleButton);
        if (v != null) {
            v.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    NavigationFragment.this.m_navItemClickListener.onClick(NavigationFragment.this.getActivity().findViewById(R.id.navFragment_TitleItem));
                }
            });
        }
        if (this.m_listView == null) {
            this.m_listView = getListView();
            this.m_listView.setTextFilterEnabled(false);
        }
        if (this.m_itemsAvailable.isEmpty()) {
            NavigationItem_UriActivity_Profile item = this.m_itemProfile;
            item.resid_category = 2131165197;
            item.resid_icon = 2130837528;
            item.resid_name = 2131165214;
            item.info = "";
            NavigationItem_ClassActivity item2 = new NavigationItem_ClassActivity_SingleTop();
            item2.activity = CommunityActivity.class;
            item2.resid_category = 2131165197;
            item2.resid_icon = 2130837528;
            item2.resid_name = 2131165198;
            item2.info = "";
            this.m_itemsAvailable.add(item2);
            item2 = new NavigationItem_ClassActivity_SingleTop();
            item2.activity = CommunityGroupsActivity.class;
            item2.resid_category = 2131165197;
            item2.resid_icon = 2130837529;
            item2.resid_name = 2131165225;
            item2.info = "";
            this.m_itemsAvailable.add(item2);
            item2 = new NavigationItem_UriActivity_Blotter();
            item2.resid_category = 2131165197;
            item2.resid_icon = 2130837524;
            item2.resid_name = 2131165242;
            item2.info = "";
            this.m_itemsAvailable.add(item2);
            NavigationItem_UriActivity_Catalog item3 = new NavigationItem_UriActivity_Catalog();
            item3.m_bShowIfNotSignedIn = true;
            item3.resid_category = 2131165186;
            item3.resid_icon = 2130837532;
            item3.resid_name = 2131165227;
            item3.info = "";
            this.m_itemsAvailable.add(item3);
            NavigationItem_UriActivity_Wishlist item4 = new NavigationItem_UriActivity_Wishlist();
            item4.resid_category = 2131165186;
            item4.resid_icon = 2130837533;
            item4.resid_name = 2131165189;
            item4.info = "";
            this.m_itemsAvailable.add(item4);
            NavigationItem_UriActivity_Cart item5 = new NavigationItem_UriActivity_Cart();
            item5.resid_category = 2131165186;
            item5.resid_icon = 2130837525;
            item5.resid_name = 2131165187;
            item5.info = "";
            this.m_itemsAvailable.add(item5);
            NavigationItem_UriActivity_StoreSearch item6 = new NavigationItem_UriActivity_StoreSearch();
            item6.m_bShowIfNotSignedIn = true;
            item6.resid_category = 2131165186;
            item6.resid_icon = 2130837530;
            item6.resid_name = 2131165188;
            item6.info = "";
            this.m_itemsAvailable.add(item6);
            if (SteamCommunityApplication.GetInstance().IsOverTheAirVersion()) {
                NavigationItem_UriActivity_Feeds_SteamSpecials item7 = new NavigationItem_UriActivity_Feeds_SteamSpecials();
                item7.m_bShowIfNotSignedIn = true;
                item7.resid_category = 2131165241;
                item7.resid_icon = 2130837527;
                item7.resid_name = 2131165348;
                item7.info = "";
                this.m_itemsAvailable.add(item7);
            }
            NavigationItem_UriActivity_Feeds_SteamNews item8 = new NavigationItem_UriActivity_Feeds_SteamNews();
            item8.m_bShowIfNotSignedIn = true;
            item8.resid_category = 2131165241;
            item8.resid_icon = 2130837527;
            item8.resid_name = 2131165199;
            item8.info = "";
            this.m_itemsAvailable.add(item8);
            NavigationItem_UriActivity_Feeds_Syndicated item9 = new NavigationItem_UriActivity_Feeds_Syndicated();
            item9.m_bShowIfNotSignedIn = true;
            item9.resid_category = 2131165241;
            item9.resid_icon = 2130837527;
            item9.resid_name = 2131165200;
            item9.info = "";
            this.m_itemsAvailable.add(item9);
            item2 = new NavigationItem_ClassActivity_SingleTop();
            item2.m_bShowIfNotSignedIn = true;
            item2.activity = SettingsActivity.class;
            item2.resid_category = 2131165190;
            item2.resid_icon = 2130837531;
            item2.resid_name = 2131165191;
            item2.info = "";
            this.m_itemsAvailable.add(item2);
            NavigationItem_UriActivity_AccountDetails item10 = new NavigationItem_UriActivity_AccountDetails();
            item10.m_bShowIfNotSignedIn = false;
            item10.resid_category = 2131165190;
            item10.resid_icon = 2130837531;
            item10.resid_name = 2131165194;
            item10.info = "";
            this.m_itemsAvailable.add(item10);
            NavigationItem_ExitApplication item11 = new NavigationItem_ExitApplication();
            item11.m_bShowIfNotSignedIn = true;
            item11.resid_category = 2131165205;
            item11.resid_icon = 2130837526;
            this.m_itemsAvailable.add(item11);
        }
        this.m_adapter = new NavigationItemListAdapter();
        setListAdapter(this.m_adapter);
    }

    public void onResume() {
        super.onResume();
        onNavSidebarPrepareForActivation();
    }

    private void onNavSidebarPrepareForActivation() {
        this.m_items.clear();
        boolean bSignedIn = SteamWebApi.IsLoggedIn();
        Iterator i$ = this.m_itemsAvailable.iterator();
        while (i$.hasNext()) {
            NavigationItem x = (NavigationItem) i$.next();
            if (bSignedIn || x.m_bShowIfNotSignedIn) {
                this.m_items.add(x);
            }
        }
        this.m_adapter.notifyDataSetChanged();
        setupView(getActivity().findViewById(R.id.navFragment_TitleItem), this.m_itemProfile, -1);
    }

    public void onPause() {
        super.onPause();
        overrideActivityOnConfigurationChanged();
    }

    private void setupView(View v, NavigationItem info, int position) {
        View titleLabelLayout = v.findViewById(R.id.navigation_section_title_layout);
        View nameViewUnderline = v.findViewById(R.id.navigation_section_name_underline);
        TextView labelView = (TextView) v.findViewById(R.id.label);
        TextView nameView = (TextView) v.findViewById(R.id.name);
        TextView statusView = (TextView) v.findViewById(R.id.status);
        TextView steamidView = (TextView) v.findViewById(R.id.steamid);
        ImageView avatarView = (ImageView) v.findViewById(R.id.avatar);
        ImageView avatarViewFrame = (ImageView) v.findViewById(R.id.avatar_frame);
        if (position < 0 || (position != 0 && info.resid_category == ((NavigationItem) this.m_items.get(position - 1)).resid_category)) {
            titleLabelLayout.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        } else {
            if (info.resid_category != 0) {
                labelView.setText(info.resid_category);
            } else {
                labelView.setText(" ");
            }
            titleLabelLayout.setVisibility(0);
            titleLabelLayout.setOnClickListener(this.m_dummyClickListener);
        }
        steamidView.setText(String.valueOf(position));
        avatarView.setImageResource(info.resid_icon);
        nameView.setText(info.getName());
        statusView.setText(info.getDetails());
        if (position < 0 || position + 1 >= this.m_items.size() || info.resid_category != ((NavigationItem) this.m_items.get(position + 1)).resid_category) {
            nameViewUnderline.setVisibility(AccessibilityNodeInfoCompat.ACTION_CLEAR_SELECTION);
        } else {
            nameViewUnderline.setVisibility(0);
        }
        boolean bOnline = false;
        if (position < 0) {
            avatarViewFrame.setVisibility(0);
            avatarViewFrame.setImageResource(R.drawable.avatar_frame_offline);
            avatarView.setImageResource(R.drawable.placeholder_contact);
            FriendInfo myself = null;
            if (SteamWebApi.IsLoggedIn()) {
                myself = SteamCommunityApplication.GetInstance().GetFriendInfoDB().GetFriendInfo(Long.valueOf(SteamWebApi.GetLoginSteamID()));
            } else {
                nameView.setText(R.string.Login);
            }
            if (myself != null && myself.HasPresentationData()) {
                AndroidUtils.setTextViewText(nameView, myself.m_personaName);
                myself.IsAvatarSmallLoaded();
                avatarView.setImageBitmap(myself.GetAvatarSmall());
                SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
                if (dbService != null && dbService.getSteamUmqConnectionState().isConnected()) {
                    avatarViewFrame.setImageResource(R.drawable.avatar_frame_online);
                    bOnline = true;
                }
            }
        }
        int clrOnline = getActivity().getResources().getColor(bOnline ? R.color.online : R.color.offline);
        nameView.setTextColor(clrOnline);
        statusView.setTextColor(clrOnline);
    }

    public void onNavActivationButtonClicked() {
        if (!this.m_bNavAnimating) {
            if (SteamWebApi.IsLoggedIn() || this.m_bNavActive) {
                this.m_bNavAnimating = true;
                Animation anim;
                if (this.m_bNavActive) {
                    this.m_bNavActive = false;
                    anim = AnimationUtils.loadAnimation(getActivity(), R.anim.splitview_navigation_hide);
                    anim.setAnimationListener(this.m_animListener);
                    this.m_splitViewNavigation.startAnimation(anim);
                    this.m_splitViewContents.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.splitview_content_show));
                } else {
                    this.m_bNavActive = true;
                    anim = AnimationUtils.loadAnimation(getActivity(), R.anim.splitview_navigation_show);
                    anim.setAnimationListener(this.m_animListener);
                    this.m_splitViewNavigation.setVisibility(0);
                    this.m_splitViewNavigation.startAnimation(anim);
                    this.m_splitViewContents.startAnimation(AnimationUtils.loadAnimation(getActivity(), R.anim.splitview_content_hide));
                    onNavSidebarPrepareForActivation();
                }
                View v = getActivity().findViewById(R.id.titleNavActivationButton);
                if (v != null) {
                    v.setBackgroundResource(this.m_bNavActive ? R.drawable.button_navigation_highlighted : R.drawable.button_navigation);
                }
            }
        }
    }

    public boolean overrideActivityOnBackPressed() {
        if (this.m_bNavAnimating) {
            return true;
        }
        if (!this.m_bNavActive) {
            return false;
        }
        onNavActivationButtonClicked();
        return true;
    }

    public void overrideActivityOnConfigurationChanged() {
        if (!this.m_bNavActive) {
            return;
        }
        if (this.m_bNavAnimating) {
            this.m_bRequestToHideNavWhileAnimating = true;
        } else {
            onNavActivationButtonClicked();
        }
    }
}
