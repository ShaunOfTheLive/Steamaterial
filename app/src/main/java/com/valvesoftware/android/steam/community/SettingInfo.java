package com.valvesoftware.android.steam.community;

import android.app.DatePickerDialog;
import android.app.DatePickerDialog.OnDateSetListener;
import android.content.Context;
import android.content.SharedPreferences.Editor;
import android.support.v4.view.MotionEventCompat;
import com.valvesoftware.android.steam.community.SettingInfo.Transaction;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import java.text.DateFormat;
import java.util.Calendar;

public class SettingInfo {
    private static final String m_sPrefs = "steam.settings.";
    public AccessRight m_access;
    public String m_defaultValue;
    public Object m_extraData;
    public String m_key;
    public UpdateListener m_pUpdateListener;
    public int m_resid;
    public int m_resid_detailed;
    public SettingType m_type;

    public enum AccessRight {
        NONE,
        VALID_ACCOUNT,
        USER,
        CODE
    }

    public static class CustomDatePickerDialog extends DatePickerDialog {
        private boolean m_bAllowSetTitle;

        public CustomDatePickerDialog(Context context, OnDateSetListener callBack, Calendar cal, int resid_title) {
            super(context, callBack, cal.get(1), cal.get(UriData.RESULT_HTTP_EXCEPTION), cal.get(MotionEventCompat.ACTION_POINTER_DOWN));
            this.m_bAllowSetTitle = true;
            setTitle(resid_title);
            this.m_bAllowSetTitle = false;
        }

        public void setTitle(CharSequence title) {
            if (this.m_bAllowSetTitle) {
                super.setTitle(title);
            }
        }
    }

    public static class DateConverter {
        public static Calendar makeCalendar(String value) {
            Calendar myDOB = Calendar.getInstance();
            if (!(value == null || value.equals(""))) {
                int intValue = Integer.valueOf(value).intValue();
                myDOB.set(intValue / 10000, (intValue - ((intValue / 10000) * 10000)) / 100, intValue % 100);
            }
            return myDOB;
        }

        public static String formatDate(String value) {
            return DateFormat.getDateInstance(0).format(makeCalendar(value).getTime());
        }

        public static String makeValue(int year, int monthOfYear, int dayOfMonth) {
            return "" + (((year * 10000) + (monthOfYear * 100)) + dayOfMonth);
        }

        public static String makeUnixTime(String value) {
            return "" + (makeCalendar(value).getTimeInMillis() / 1000);
        }
    }

    public static class RadioSelectorItem {
        public int resid_text;
        public int value;
    }

    public enum SettingType {
        SECTION,
        INFO,
        CHECK,
        DATE,
        URI,
        MARKET,
        UNREADMSG,
        RADIOSELECTOR,
        RINGTONESELECTOR
    }

    public static class Transaction {
        private boolean m_bCookiesMarkedForSync;
        private Editor m_editor;

        public Transaction(Context context) {
            this.m_editor = null;
            this.m_bCookiesMarkedForSync = false;
            String steamID = SteamWebApi.GetLoginSteamID();
            if (steamID != null && !steamID.equals("")) {
                this.m_editor = context.getSharedPreferences(m_sPrefs + steamID, 0).edit();
            }
        }

        public void setValue(SettingInfo setting, String value) {
            if (this.m_editor != null) {
                this.m_editor.putString(setting.m_key, value);
            }
            if (setting.m_pUpdateListener != null) {
                setting.m_pUpdateListener.OnSettingInfoValueUpdate(setting, value, this);
            }
        }

        public void commit() {
            if (this.m_bCookiesMarkedForSync) {
                SteamWebApi.SyncAllCookies();
                this.m_bCookiesMarkedForSync = false;
            }
            if (this.m_editor != null) {
                this.m_editor.commit();
            }
        }

        public void markCookiesForSync() {
            this.m_bCookiesMarkedForSync = true;
        }
    }

    public static interface UpdateListener {
        void OnSettingInfoValueUpdate(SettingInfo settingInfo, String str, Transaction transaction);
    }

    public SettingInfo() {
        this.m_resid = 0;
        this.m_resid_detailed = 0;
        this.m_key = null;
        this.m_type = SettingType.SECTION;
        this.m_access = AccessRight.NONE;
        this.m_defaultValue = null;
        this.m_extraData = null;
        this.m_pUpdateListener = null;
    }

    public String getValue(Context context) {
        String steamID = SteamWebApi.GetLoginSteamID();
        return (steamID == null || steamID.equals("")) ? this.m_defaultValue : context.getSharedPreferences(m_sPrefs + steamID, 0).getString(this.m_key, this.m_defaultValue);
    }

    public boolean getBooleanValue(Context context) {
        String val = getValue(context);
        return (val == null || val.equals("")) ? false : true;
    }

    public RadioSelectorItem getRadioSelectorItemValue(Context context) {
        RadioSelectorItem[] radios = null;
        try {
            radios = (RadioSelectorItem[]) this.m_extraData;
        } catch (Exception e) {
        }
        if (radios == null) {
            return null;
        }
        try {
            RadioSelectorItem res = findRadioSelectorItemByValue(Integer.valueOf(getValue(context)).intValue(), radios);
            if (res != null) {
                return res;
            }
        } catch (Exception e2) {
        }
        try {
            res = findRadioSelectorItemByValue(Integer.valueOf(this.m_defaultValue).intValue(), radios);
            if (res != null) {
                return res;
            }
        } catch (Exception e3) {
        }
        try {
            return radios[0];
        } catch (Exception e4) {
            return null;
        }
    }

    public void setValueAndCommit(Context context, String value) {
        Transaction tr = new Transaction(context);
        tr.setValue(this, value);
        tr.commit();
    }

    public static RadioSelectorItem findRadioSelectorItemByValue(int value, RadioSelectorItem[] radios) {
        for (int j = 0; j < radios.length; j++) {
            if (radios[j].value == value) {
                return radios[j];
            }
        }
        return null;
    }
}
