package com.valvesoftware.android.steam.community;

import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem.RequestForAvatarImage;
import com.valvesoftware.android.steam.community.GroupInfo.GroupCategoryInList;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;

public class GroupInfo extends GenericListItem {
    public int m_appidFavorite;
    public String m_avatarMediumURL;
    public GroupCategoryInList m_categoryInList;
    public String m_groupName;
    public GroupType m_groupType;
    public int m_numMembersOnline;
    public int m_numMembersTotal;
    public String m_profileURL;
    public GroupRelationship m_relationship;

    static /* synthetic */ class AnonymousClass_1 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList = new int[GroupCategoryInList.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList[GroupCategoryInList.REQUEST_INVITE.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList[GroupCategoryInList.OFFICIAL.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList[GroupCategoryInList.PRIVATE.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList[GroupCategoryInList.PUBLIC.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList[GroupCategoryInList.SEARCH_ALL.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
        }
    }

    public enum GroupCategoryInList {
        REQUEST_INVITE,
        PRIVATE,
        PUBLIC,
        OFFICIAL,
        SEARCH_ALL;

        public int GetDisplayString() {
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$GroupInfo$GroupCategoryInList[ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                    return R.string.Group_Invites;
                case UriData.RESULT_HTTP_EXCEPTION:
                    return R.string.Official_Groups;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    return R.string.Private_Groups;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    return R.string.Public_Groups;
                case MotionEventCompat.ACTION_POINTER_DOWN:
                    return R.string.Search;
                default:
                    return R.string.Unknown;
            }
        }
    }

    public enum GroupRelationship {
        None,
        Blocked,
        Invited,
        Member,
        Kicked
    }

    public enum GroupType {
        PRIVATE,
        PUBLIC,
        LOCKED,
        DISABLED,
        OFFICIAL;

        public static com.valvesoftware.android.steam.community.GroupInfo.GroupType FromInteger(int status) {
            switch (status) {
                case SettingInfoDB.SETTING_NOTIFY_FIRST:
                    return PRIVATE;
                case UriData.RESULT_INVALID_CONTENT:
                    return PUBLIC;
                case UriData.RESULT_HTTP_EXCEPTION:
                    return LOCKED;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    return DISABLED;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    return OFFICIAL;
                default:
                    return PRIVATE;
            }
        }
    }

    public GroupInfo() {
        this.m_groupType = GroupType.PRIVATE;
        this.m_categoryInList = GroupCategoryInList.PUBLIC;
        this.m_relationship = GroupRelationship.None;
        this.m_appidFavorite = 0;
    }

    protected RequestForAvatarImage RequestAvatarImage(GenericListDB db) {
        return GetRequestForAvatarImage(SteamDBService.JOB_QUEUE_AVATARS_GROUPS, this.m_avatarSmallURL, this.m_steamID.toString(), db);
    }

    public boolean HasPresentationData() {
        return this.m_profileURL != null;
    }
}
