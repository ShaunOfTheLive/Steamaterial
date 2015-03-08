package com.valvesoftware.android.steam.community;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.net.Uri;
import android.os.Process;
import android.support.v4.view.MotionEventCompat;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.FriendInfo.FriendRelationship;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.Message;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.UmqInfo;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_LOGININFO_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_MARKREADMESSAGES_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_SENDMESSAGE_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_SETTINGCHANGE_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_UMQACTIVITY_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService.UmqCommState;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService.UmqCommand;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService.UmqConnectionState;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestDocumentType;
import com.valvesoftware.android.steam.community.activity.CommunityActivity;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class SteamUmqCommunicationService {
    public static final String E_NotLoggedOn = "Not Logged On";
    public static final String E_OK = "OK";
    public static final String E_Timeout = "Timeout";
    public static final String INTENT_ACTION = "com.valvesoftware.android.steam.community.SteamUmqCommunicationIntent";
    private static final String TAG = "SteamUmqCommunication";
    private static final int UMQ_TIMEOUT_DEFAULT_SO = 25000;
    private static final String URI_RequestUMQDeviceInfo;
    private static final String URI_RequestUMQLogoff;
    private static final String URI_RequestUMQLogon;
    private static final String URI_RequestUMQMessage;
    private static final String URI_RequestUMQPoll;
    private static final String URI_RequestUMQPollStatus;
    private static final String URI_RequestUMQServerInfo;
    private static final int m_nIdleTimeBeforePollingStopsSeconds = 60;
    private static final int m_nIdleTimeDisconnectedForSeconds = 604800;
    private static final int m_nIdleTimePausePollingForSeconds = 1800;
    private static final int m_nRetryLogonSleepSecondsMax = 3600;
    private static final int m_nRetryLogonSleepSecondsMed = 600;
    private static final int m_nRetryLogonSleepSecondsMin = 2;
    private static String m_sUMQID;
    private static long s_lastTimestampOnMessage;
    ThreadPoolQueueCounter m_ThreadPoolCounter;
    private ArrayList<UmqCommand> m_arrCommands;
    private boolean m_bRunningInForeground;
    private C2DM m_c2dmIntentReceiver;
    private CancelOngoingNotificationThread m_cancelOngoingNotificationThread;
    private UmqCommand m_cmdCurrent;
    private final Condition m_cmdCurrentCond;
    private final Lock m_cmdCurrentLock;
    private final Condition m_cmdRetryLogonCond;
    private SteamUmqCommunicationDatabase m_db;
    private UmqConnectionState m_eMyConnectionState;
    private int m_lastAndroidNotificationResID;
    private Object m_lockNotificationMessages;
    private int m_nRetryLogonSleepSeconds;
    private HashMap<String, MessageNotifyInfo> m_notificationsMessages;
    private ScreenStateReceiver m_screenStateReceiver;
    private boolean m_settingOngoingEnabled;
    private SteamServerInfo m_steamServerInfo;
    private SteamDBService m_svc;
    private Thread m_thread;

    static /* synthetic */ class AnonymousClass_2 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState;
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState = new int[UmqCommState.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState[UmqCommState.UMQ_LOGON_RETRY.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState[UmqCommState.UMQ_LOGON.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState[UmqCommState.UMQ_POLL_STATUS.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState[UmqCommState.UMQ_POLL.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState[UmqCommState.UMQ_LOGOFF.ordinal()] = 5;
            } catch (NoSuchFieldError e5) {
            }
            $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState = new int[UmqConnectionState.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.offline.ordinal()] = 1;
            } catch (NoSuchFieldError e6) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.invalidcredentials.ordinal()] = 2;
            } catch (NoSuchFieldError e7) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.connecting.ordinal()] = 3;
            } catch (NoSuchFieldError e8) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.reconnecting.ordinal()] = 4;
            } catch (NoSuchFieldError e9) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.idle.ordinal()] = 5;
            } catch (NoSuchFieldError e10) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[UmqConnectionState.active.ordinal()] = 6;
            } catch (NoSuchFieldError e11) {
            }
        }
    }

    private class BaseUMQRequest extends RequestBase {
        public BaseUMQRequest() {
            super(SteamUmqCommunicationService.this.m_ThreadPoolCounter.GetThreadPoolQueueId());
        }

        public BaseUMQRequest(String callerQueue) {
            super(callerQueue);
        }

        protected HttpParams GetDefaultHttpParams() {
            HttpParams params = super.GetDefaultHttpParams();
            HttpConnectionParams.setSoTimeout(params, UMQ_TIMEOUT_DEFAULT_SO);
            return params;
        }
    }

    private static class C2DM extends BroadcastReceiver {
        public boolean m_c2dmClientPushIsActive;
        public String m_c2dmRegistrationId;
        public String m_c2dmRegistrationLanguage;
        public boolean m_c2dmShouldBeSentToServer;

        private C2DM() {
            this.m_c2dmRegistrationId = null;
            this.m_c2dmRegistrationLanguage = null;
            this.m_c2dmShouldBeSentToServer = true;
            this.m_c2dmClientPushIsActive = false;
        }

        public void onReceive(Context context, Intent intent) {
            String newid = intent.getStringExtra(C2DMProcessor.INTENT_C2DM_REGISTRATION_READY_EXTRA_ID);
            if (newid != null && !newid.equals("")) {
                this.m_c2dmRegistrationId = newid;
                this.m_c2dmShouldBeSentToServer = true;
            }
        }

        public boolean isClientPushActive() {
            return (this.m_c2dmRegistrationId == null || this.m_c2dmRegistrationId.equals("") || SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingNotificationsIMs2.getRadioSelectorItemValue(SteamCommunityApplication.GetInstance().getApplicationContext()).value <= 0) ? false : true;
        }
    }

    private class CancelOngoingNotificationThread extends Thread {
        public long m_timeToClearOngoingNotification;

        private CancelOngoingNotificationThread() {
            this.m_timeToClearOngoingNotification = 0;
        }

        public void run() {
            while (true) {
                SteamUmqCommunicationService.this.m_cmdCurrentLock.lock();
                if (this.m_timeToClearOngoingNotification == 0) {
                    SteamUmqCommunicationService.this.m_cancelOngoingNotificationThread = null;
                    SteamUmqCommunicationService.this.m_cmdCurrentLock.unlock();
                    return;
                }
                long timeToSleep = this.m_timeToClearOngoingNotification - System.currentTimeMillis();
                if (timeToSleep <= 0) {
                    SteamUmqCommunicationService.this.cancelOngoingNotification();
                    SteamUmqCommunicationService.this.m_cancelOngoingNotificationThread = null;
                    SteamUmqCommunicationService.this.m_cmdCurrentLock.unlock();
                    return;
                }
                SteamUmqCommunicationService.this.m_cmdCurrentLock.unlock();
                try {
                    Thread.sleep(100 + timeToSleep);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    private static class MessageNotifyInfo {
        public String msg;
        public int num;

        private MessageNotifyInfo() {
        }
    }

    private class RequestUMQDeviceInfo extends BaseUMQRequest {
        private boolean m_bClientPushIsActive;

        public RequestUMQDeviceInfo(String sPostData) {
            super(SteamDBService.JOB_QUEUE_UMQ_DEVICEINFO);
            this.m_bClientPushIsActive = false;
            SetUriAndDocumentType(URI_RequestUMQDeviceInfo, RequestDocumentType.JSON);
            SetRequestAction(RequestActionType.DoHttpRequestNoCache);
            String deviceid = Uri.encode(SteamUmqCommunicationService.this.m_c2dmIntentReceiver.m_c2dmRegistrationId);
            StringBuilder sb = new StringBuilder(((((sPostData.length() + 16) + 8) + deviceid.length()) + 16) + 12);
            sb.append(sPostData);
            sb.append("&deviceid=");
            sb.append("GOOG%3A");
            sb.append(deviceid);
            sb.append("&lang=");
            sb.append(SteamUmqCommunicationService.this.m_c2dmIntentReceiver.m_c2dmRegistrationLanguage);
            sb.append("&im_enable=");
            this.m_bClientPushIsActive = SteamUmqCommunicationService.this.m_c2dmIntentReceiver.isClientPushActive();
            sb.append(this.m_bClientPushIsActive ? "1" : "0");
            sb.append("&im_id=");
            sb.append(C2DMProcessor.getIMID(SteamUmqCommunicationService.this.m_svc));
            SetPostData(sb.toString());
        }

        public void RequestSucceededOnResponseWorkerThread() {
            if (E_OK.equals(this.m_jsonDocument.optString(C2DMReceiverService.EXTRA_ERROR, "fail"))) {
                synchronized (SteamUmqCommunicationService.this.m_c2dmIntentReceiver) {
                    SteamUmqCommunicationService.this.m_c2dmIntentReceiver.m_c2dmClientPushIsActive = this.m_bClientPushIsActive;
                }
                return;
            }
            RequestFailedOnResponseWorkerThread();
        }

        public void RequestFailedOnResponseWorkerThread() {
            synchronized (SteamUmqCommunicationService.this.m_c2dmIntentReceiver) {
                SteamUmqCommunicationService.this.m_c2dmIntentReceiver.m_c2dmShouldBeSentToServer = true;
            }
        }
    }

    private class RequestUMQLogoff extends BaseUMQRequest {
        private UmqCommand m_cmd;
        private int m_numRetriesLeft;

        public RequestUMQLogoff(UmqCommand cmd) {
            super();
            this.m_numRetriesLeft = 2;
            this.m_cmd = cmd;
            this.m_cmd.m_request = this;
            SetUriAndDocumentType(URI_RequestUMQLogoff, RequestDocumentType.JSON);
        }

        public void RequestSucceededOnResponseWorkerThread() {
            onThisRequestFinished(E_OK);
        }

        public void RequestFailedOnResponseWorkerThread() {
            if (this.m_numRetriesLeft > 0) {
                this.m_numRetriesLeft--;
                SteamUmqCommunicationService.this.m_svc.SubmitRequest(this);
                return;
            }
            onThisRequestFinished(C2DMReceiverService.EXTRA_ERROR);
        }

        private void onThisRequestFinished(String logoffStatus) {
            if (SteamCommunityApplication.GetInstance().m_bApplicationExiting) {
                Process.killProcess(Process.myPid());
                return;
            }
            SteamUmqCommunicationService.this.CurrentJobDone(this.m_cmd);
            Intent intent = new Intent(INTENT_ACTION);
            intent.putExtra("type", "umqlogoff");
            intent.putExtra("umqlogoff", logoffStatus);
            SteamUmqCommunicationService.this.m_svc.getApplicationContext().sendBroadcast(intent);
        }
    }

    private class RequestUMQLogon extends BaseUMQRequest {
        private UmqCommand m_cmd;

        public RequestUMQLogon(UmqCommand cmd) {
            super();
            this.m_cmd = cmd;
            this.m_cmd.m_request = this;
            SetUriAndDocumentType(URI_RequestUMQLogon, RequestDocumentType.JSON);
        }

        public void RequestSucceededOnResponseWorkerThread() {
            if (this.m_jsonDocument != null) {
                String responseError = "";
                try {
                    responseError = this.m_jsonDocument.getString(C2DMReceiverService.EXTRA_ERROR);
                } catch (JSONException e) {
                }
                if (this.m_jsonDocument.has("umqid")) {
                    try {
                        m_sUMQID = this.m_jsonDocument.getString("umqid");
                        Editor edt = SteamCommunityApplication.GetInstance().getApplicationContext().getSharedPreferences("steamumqcommunication", 0).edit();
                        edt.putString("umqid", m_sUMQID);
                        edt.commit();
                    } catch (JSONException e2) {
                        responseError = "";
                    }
                }
                if (this.m_jsonDocument.has("push") && SteamUmqCommunicationService.this.m_c2dmIntentReceiver.isClientPushActive() != this.m_jsonDocument.optBoolean("push", false)) {
                    SteamUmqCommunicationService.this.m_c2dmIntentReceiver.m_c2dmShouldBeSentToServer = true;
                }
                int nMessageBase = 0;
                if (this.m_jsonDocument.has("message")) {
                    try {
                        nMessageBase = this.m_jsonDocument.getInt("message");
                    } catch (JSONException e3) {
                    }
                }
                if (E_OK.equals(responseError)) {
                    SteamUmqCommunicationService.this.m_screenStateReceiver.m_timeLastApplicationActivity = System.currentTimeMillis();
                    UmqCommand cmd = new UmqCommand();
                    cmd.m_eCommand = UmqCommState.UMQ_POLL_STATUS;
                    cmd.m_sOAuthToken = this.m_cmd.m_sOAuthToken;
                    cmd.m_sMySteamID = this.m_cmd.m_sMySteamID;
                    cmd.m_nMessage = nMessageBase;
                    SteamUmqCommunicationService.this.AddCommandToQueue(cmd);
                    onThisRequestFinished(E_OK);
                    return;
                }
            }
            onThisRequestFailed(C2DMReceiverService.EXTRA_ERROR);
        }

        public void RequestFailedOnResponseWorkerThread() {
            if (GetHttpResponseUriData().m_httpResult != 401) {
                onThisRequestFailed(C2DMReceiverService.EXTRA_ERROR);
                return;
            }
            onThisRequestFinished("invalidcredentials");
            SteamUmqCommunicationService.this.SetUmqConnectionState(UmqConnectionState.invalidcredentials);
            REQ_ACT_LOGININFO_DATA svcreq = new REQ_ACT_LOGININFO_DATA();
            svcreq.sOAuthToken = null;
            svcreq.sSteamID = null;
            SteamUmqCommunicationService.this.SvcReq_SetUmqCommunicationSettings(svcreq);
        }

        private void onThisRequestFailed(String logonStatus) {
            UmqCommand cmd = new UmqCommand();
            cmd.m_eCommand = UmqCommState.UMQ_LOGON_RETRY;
            cmd.m_sOAuthToken = this.m_cmd.m_sOAuthToken;
            cmd.m_sMySteamID = this.m_cmd.m_sMySteamID;
            cmd.m_nMessage = this.m_cmd.m_nMessage;
            SteamUmqCommunicationService.this.AddCommandToQueue(cmd);
            onThisRequestFinished(logonStatus);
        }

        private void onThisRequestFinished(String logonStatus) {
            SteamUmqCommunicationService.this.CurrentJobDone(this.m_cmd);
            Intent intent = new Intent(INTENT_ACTION);
            intent.putExtra("type", "umqlogon");
            intent.putExtra("umqlogon", logonStatus);
            SteamUmqCommunicationService.this.m_svc.getApplicationContext().sendBroadcast(intent);
        }
    }

    private class RequestUMQMessage extends BaseUMQRequest {
        public RequestUMQMessage() {
            super(SteamDBService.JOB_QUEUE_UMQ_SENDMESSAGE);
            SetUriAndDocumentType(URI_RequestUMQMessage, RequestDocumentType.JSON);
            SetRequestAction(RequestActionType.DoHttpRequestNoCache);
        }

        public void RequestSucceededOnResponseWorkerThread() {
            if (this.m_jsonDocument != null) {
                String responseError = "";
                try {
                    responseError = this.m_jsonDocument.getString(C2DMReceiverService.EXTRA_ERROR);
                } catch (JSONException e) {
                }
                if (E_OK.equals(responseError)) {
                    REQ_ACT_SENDMESSAGE_DATA msgData = (REQ_ACT_SENDMESSAGE_DATA) GetObjData();
                    msgData.msg.sMySteamID = msgData.mylogin.sSteamID;
                    msgData.msg.bIncoming = false;
                    msgData.msg.bUnread = false;
                    msgData.msg.msgtime = Calendar.getInstance();
                    if (!msgData.msg.msgtype.equals("typing")) {
                        SteamUmqCommunicationService.this.m_db.insertMessage(msgData.msg);
                    }
                    onThisRequestFinished(E_OK);
                    return;
                }
            }
            onThisRequestFinished(C2DMReceiverService.EXTRA_ERROR);
        }

        public void RequestFailedOnResponseWorkerThread() {
            onThisRequestFinished(C2DMReceiverService.EXTRA_ERROR);
        }

        private void onThisRequestFinished(String sendStatus) {
            REQ_ACT_SENDMESSAGE_DATA data = (REQ_ACT_SENDMESSAGE_DATA) GetObjData();
            Intent intent = new Intent(INTENT_ACTION);
            intent.putExtra("type", "chatmsg");
            intent.putExtra("action", "send");
            intent.putExtra("send", sendStatus);
            intent.putExtra("steamid", data.msg.sWithSteamID);
            intent.putExtra("msgid", data.msg.id);
            if (data.intentcontext != null) {
                intent.putExtra("intentcontext", data.intentcontext);
            }
            SteamUmqCommunicationService.this.m_svc.getApplicationContext().sendBroadcast(intent);
        }
    }

    private class RequestUMQPoll extends BaseUMQRequest {
        private ArrayList<String> m_arrSteamIdsPersonaState;
        private ArrayList<String> m_arrSteamIdsRelationshipChanges;
        private UmqCommand m_cmd;
        private HashMap<Long, Intent> m_mapMessagesFromSteamIds;

        public RequestUMQPoll(UmqCommand cmd) {
            super();
            this.m_arrSteamIdsPersonaState = null;
            this.m_arrSteamIdsRelationshipChanges = null;
            this.m_mapMessagesFromSteamIds = null;
            this.m_cmd = cmd;
            this.m_cmd.m_request = this;
            SetUriAndDocumentType(isSecure() ? URI_RequestUMQPoll : URI_RequestUMQPollStatus, RequestDocumentType.JSON);
        }

        private boolean isSecure() {
            return this.m_cmd.m_eCommand == UmqCommState.UMQ_POLL;
        }

        protected HttpParams GetDefaultHttpParams() {
            HttpParams params = super.GetDefaultHttpParams();
            if (this.m_cmd.m_nTimeout > 0) {
                HttpConnectionParams.setSoTimeout(params, Math.max((this.m_cmd.m_nTimeout + 7) * 1000, UMQ_TIMEOUT_DEFAULT_SO));
            }
            return params;
        }

        private void DispatchPersonaStateNotifications() {
            if (this.m_arrSteamIdsPersonaState != null && !this.m_arrSteamIdsPersonaState.isEmpty()) {
                Intent intent = new Intent(INTENT_ACTION);
                intent.putExtra("type", "personastate");
                intent.putExtra("steamids", (String[]) this.m_arrSteamIdsPersonaState.toArray(new String[this.m_arrSteamIdsPersonaState.size()]));
                SteamUmqCommunicationService.this.m_svc.getApplicationContext().sendBroadcast(intent);
                this.m_arrSteamIdsPersonaState.clear();
            }
        }

        private void DispatchRelationshipChangesNotifications() {
            if (this.m_arrSteamIdsRelationshipChanges != null && !this.m_arrSteamIdsRelationshipChanges.isEmpty()) {
                Intent intent = new Intent(INTENT_ACTION);
                intent.putExtra("type", "personarelationship");
                intent.putExtra("steamids", (String[]) this.m_arrSteamIdsRelationshipChanges.toArray(new String[this.m_arrSteamIdsRelationshipChanges.size()]));
                SteamUmqCommunicationService.this.m_svc.getApplicationContext().sendBroadcast(intent);
                this.m_arrSteamIdsRelationshipChanges.clear();
            }
        }

        private void DispatchIncomingMessagesNotifications() {
            if (this.m_mapMessagesFromSteamIds != null && !this.m_mapMessagesFromSteamIds.isEmpty()) {
                for (Intent intent : this.m_mapMessagesFromSteamIds.values()) {
                    if (intent.hasExtra("notifymsgtext")) {
                        SteamUmqCommunicationService.this.UpdateAndroidNotificationMessage(intent.getStringExtra("steamid"), intent.getStringExtra("notifymsgtext"), intent.getIntExtra("incoming", 1));
                        intent.removeExtra("notifymsgtext");
                    }
                    SteamUmqCommunicationService.this.m_svc.getApplicationContext().sendBroadcast(intent);
                }
                SteamUmqCommunicationService.this.m_screenStateReceiver.m_timeLastApplicationActivity = System.currentTimeMillis();
            }
        }

        public void RequestSucceededOnResponseWorkerThread() {
            int len = 0;
            if (this.m_jsonDocument != null) {
                String responseError = "";
                try {
                    responseError = this.m_jsonDocument.getString(C2DMReceiverService.EXTRA_ERROR);
                } catch (JSONException e) {
                }
                if (E_OK.equals(responseError) || E_Timeout.equals(responseError)) {
                    boolean bNeedSecurePoll = false;
                    int nSecurePollMessageId = this.m_cmd.m_nMessage;
                    if (this.m_jsonDocument.has("messages")) {
                        JSONArray arrMsgs = null;
                        try {
                            arrMsgs = this.m_jsonDocument.getJSONArray("messages");
                        } catch (JSONException e2) {
                        }
                        long localTimeMS = System.currentTimeMillis();
                        int timestampServerMS = this.m_jsonDocument.optInt("timestamp", 0);
                        if (arrMsgs != null) {
                            len = arrMsgs.length();
                        }
                        for (int jj = 0; jj < len; jj++) {
                            JSONObject objJsonMsg = null;
                            try {
                                objJsonMsg = arrMsgs.getJSONObject(jj);
                            } catch (JSONException e3) {
                            }
                            if (objJsonMsg != null) {
                                if (!isSecure() && objJsonMsg.has("secure_message_id")) {
                                    nSecurePollMessageId = objJsonMsg.optInt("secure_message_id", this.m_cmd.m_nMessage);
                                    bNeedSecurePoll = true;
                                    break;
                                }
                                notifyMessage(objJsonMsg, localTimeMS, timestampServerMS);
                            }
                        }
                    }
                    DispatchIncomingMessagesNotifications();
                    DispatchPersonaStateNotifications();
                    DispatchRelationshipChangesNotifications();
                    UmqCommand cmd = new UmqCommand();
                    cmd.m_eCommand = UmqCommState.UMQ_POLL_STATUS;
                    cmd.m_sOAuthToken = this.m_cmd.m_sOAuthToken;
                    cmd.m_sMySteamID = this.m_cmd.m_sMySteamID;
                    cmd.m_nMessage = this.m_cmd.m_nMessage;
                    if (bNeedSecurePoll) {
                        cmd.m_nMessage = nSecurePollMessageId;
                        cmd.m_eCommand = UmqCommState.UMQ_POLL;
                    } else if (this.m_jsonDocument.has("messagelast")) {
                        try {
                            cmd.m_nMessage = this.m_jsonDocument.getInt("messagelast");
                        } catch (JSONException e4) {
                        }
                    }
                    if (this.m_jsonDocument.has("nextsectimeout")) {
                        try {
                            cmd.m_nTimeout = this.m_jsonDocument.getInt("nextsectimeout");
                        } catch (JSONException e5) {
                        }
                    }
                    SteamUmqCommunicationService.this.AddCommandToQueue(cmd);
                    SteamUmqCommunicationService.this.CurrentJobDone(this.m_cmd);
                    return;
                }
            }
            onThisRequestFailed(C2DMReceiverService.EXTRA_ERROR);
        }

        public void RequestFailedOnResponseWorkerThread() {
            onThisRequestFailed(C2DMReceiverService.EXTRA_ERROR);
        }

        private void onThisRequestFailed(String pollStatus) {
            UmqCommand cmd = new UmqCommand();
            cmd.m_eCommand = UmqCommState.UMQ_LOGON;
            cmd.m_sOAuthToken = this.m_cmd.m_sOAuthToken;
            cmd.m_sMySteamID = this.m_cmd.m_sMySteamID;
            cmd.m_nMessage = this.m_cmd.m_nMessage;
            SteamUmqCommunicationService.this.AddCommandToQueue(cmd);
            onThisRequestFinished(pollStatus);
        }

        private void onThisRequestFinished(String pollStatus) {
            SteamUmqCommunicationService.this.CurrentJobDone(this.m_cmd);
        }

        private void notifyMessage(JSONObject obj, long localTimeMS, int timestampServerMS) {
            try {
                String jsonType = obj.getString("type");
                if (jsonType != null) {
                    if (jsonType.equals("saytext") || jsonType.equals("my_saytext") || jsonType.equals("emote") || jsonType.equals("my_emote") || jsonType.equals("typing")) {
                        boolean bIncomingMsg = !jsonType.startsWith("my_");
                        if (!bIncomingMsg) {
                            jsonType = jsonType.substring(FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER);
                        }
                        Message msg = new Message();
                        msg.sMySteamID = this.m_cmd.m_sMySteamID;
                        msg.sWithSteamID = obj.getString("steamid_from");
                        msg.bIncoming = bIncomingMsg;
                        msg.bUnread = bIncomingMsg;
                        if (timestampServerMS != 0) {
                            int timestampMsgMS = obj.optInt("timestamp", 0);
                            if (timestampMsgMS != 0 && timestampMsgMS < timestampServerMS) {
                                localTimeMS = (((long) timestampMsgMS) + localTimeMS) - ((long) timestampServerMS);
                                if (localTimeMS < s_lastTimestampOnMessage) {
                                    localTimeMS = s_lastTimestampOnMessage;
                                }
                            }
                        }
                        msg.msgtime = Calendar.getInstance();
                        msg.msgtime.setTimeInMillis(localTimeMS);
                        s_lastTimestampOnMessage = localTimeMS;
                        Long longWithSteamID = Long.valueOf(msg.sWithSteamID);
                        FriendInfo friendInfoInDB = (FriendInfo) SteamCommunityApplication.GetInstance().GetFriendInfoDB().GetItemsMap().get(longWithSteamID);
                        if (friendInfoInDB != null && friendInfoInDB.m_relationship == FriendRelationship.friend) {
                            msg.msgtype = jsonType;
                            msg.bindata = "";
                            if (!jsonType.equals("typing")) {
                                msg.bindata = obj.getString("text");
                                SteamUmqCommunicationService.this.m_db.insertMessage(msg);
                            }
                            if (this.m_mapMessagesFromSteamIds == null) {
                                this.m_mapMessagesFromSteamIds = new HashMap();
                            }
                            Intent intentNotify = (Intent) this.m_mapMessagesFromSteamIds.get(longWithSteamID);
                            if (intentNotify == null) {
                                intentNotify = new Intent(INTENT_ACTION);
                                intentNotify.putExtra("type", "chatmsg");
                                intentNotify.putExtra("action", "incoming");
                                intentNotify.putExtra("steamid", msg.sWithSteamID);
                                this.m_mapMessagesFromSteamIds.put(longWithSteamID, intentNotify);
                            }
                            if (!jsonType.equals("typing")) {
                                String keyIncoming = bIncomingMsg ? "incoming" : "my_incoming";
                                intentNotify.putExtra(keyIncoming, intentNotify.getIntExtra(keyIncoming, 0) + 1);
                                if (bIncomingMsg) {
                                    intentNotify.putExtra("notifymsgtext", msg.bindata);
                                }
                            }
                            if (bIncomingMsg) {
                                intentNotify.putExtra("typing", jsonType.equals("typing") ? 1 : 0);
                            }
                            if (msg.id > 0 && !intentNotify.hasExtra("msgidFirst")) {
                                intentNotify.putExtra("msgidFirst", msg.id);
                            }
                            if (msg.id > 0) {
                                intentNotify.putExtra("msgidLast", msg.id);
                            }
                        }
                    } else if (jsonType.equals("personastate")) {
                        steamid = obj.getString("steamid_from");
                        if (this.m_arrSteamIdsPersonaState == null) {
                            this.m_arrSteamIdsPersonaState = new ArrayList();
                        }
                        this.m_arrSteamIdsPersonaState.add(steamid);
                        SteamUmqCommunicationService.this.UpdateAndroidNotificationNameMap(steamid, obj.optString("persona_name"));
                    } else if (jsonType.equals("personarelationship")) {
                        steamid = obj.getString("steamid_from");
                        if (this.m_arrSteamIdsRelationshipChanges == null) {
                            this.m_arrSteamIdsRelationshipChanges = new ArrayList();
                        }
                        this.m_arrSteamIdsRelationshipChanges.add(steamid);
                    } else if (jsonType.equals("push2poll")) {
                        if (this.m_arrSteamIdsRelationshipChanges == null) {
                            this.m_arrSteamIdsRelationshipChanges = new ArrayList();
                        }
                        this.m_arrSteamIdsRelationshipChanges.add("");
                    }
                }
            } catch (JSONException e) {
            }
        }
    }

    private class RequestUMQServerInfo extends BaseUMQRequest {
        private int localTime;

        public RequestUMQServerInfo() {
            super(SteamDBService.JOB_QUEUE_UMQ_DEVICEINFO);
            this.localTime = (int) (System.currentTimeMillis() / 1000);
            SetUriAndDocumentType(URI_RequestUMQServerInfo, RequestDocumentType.JSON);
            SetRequestAction(RequestActionType.DoHttpRequestNoCache);
        }

        public void RequestSucceededOnResponseWorkerThread() {
            int serverTime = this.m_jsonDocument.optInt("servertime");
            if (serverTime == 0) {
                RequestFailedOnResponseWorkerThread();
            } else {
                SteamUmqCommunicationService.this.m_steamServerInfo.UpdateServerInfo(this.localTime, serverTime);
            }
        }

        public void RequestFailedOnResponseWorkerThread() {
            synchronized (SteamUmqCommunicationService.this.m_steamServerInfo) {
                SteamUmqCommunicationService.this.m_steamServerInfo.m_steamServerInfoRequired = true;
            }
        }
    }

    private class ScreenStateReceiver extends BroadcastReceiver {
        public boolean m_bIsScreenActive;
        public long m_timeLastApplicationActivity;
        public long m_timeScreenStateChanged;

        private ScreenStateReceiver() {
            this.m_bIsScreenActive = true;
            this.m_timeScreenStateChanged = System.currentTimeMillis();
            this.m_timeLastApplicationActivity = System.currentTimeMillis();
        }

        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals("android.intent.action.SCREEN_OFF")) {
                this.m_bIsScreenActive = false;
                this.m_timeScreenStateChanged = System.currentTimeMillis();
            } else if (intent.getAction().equals("android.intent.action.SCREEN_ON")) {
                this.m_bIsScreenActive = true;
                this.m_timeScreenStateChanged = System.currentTimeMillis();
                SteamUmqCommunicationService.this.SvcReq_UmqActivity(null);
            }
        }
    }

    private static class SteamServerInfo {
        private int m_reqLocalTimestamp;
        private int m_reqRemoteTimestamp;
        public boolean m_steamServerInfoRequired;

        public SteamServerInfo() {
            this.m_steamServerInfoRequired = true;
            this.m_reqLocalTimestamp = 0;
            this.m_reqRemoteTimestamp = 0;
            try {
                SharedPreferences prefs = SteamCommunityApplication.GetInstance().getApplicationContext().getSharedPreferences("steamumqcommunication", 0);
                this.m_reqLocalTimestamp = prefs.getInt("serverInfo_LocalTime", 0);
                this.m_reqRemoteTimestamp = prefs.getInt("serverInfo_RemoteTime", 0);
            } catch (Exception e) {
                this.m_reqLocalTimestamp = 0;
                this.m_reqRemoteTimestamp = 0;
            }
        }

        public synchronized void UpdateServerInfo(int localTime, int serverTime) {
            try {
                Editor edt = SteamCommunityApplication.GetInstance().getApplicationContext().getSharedPreferences("steamumqcommunication", 0).edit();
                edt.putInt("serverInfo_LocalTime", this.m_reqLocalTimestamp);
                edt.putInt("serverInfo_RemoteTime", this.m_reqRemoteTimestamp);
                edt.commit();
                this.m_reqLocalTimestamp = localTime;
                this.m_reqRemoteTimestamp = serverTime;
            } catch (Exception e) {
            }
        }

        public synchronized int GetCurrentServerTime() {
            int i;
            if (this.m_reqLocalTimestamp == 0 || this.m_reqRemoteTimestamp == 0) {
                i = 0;
            } else {
                i = this.m_reqRemoteTimestamp + (((int) (System.currentTimeMillis() / 1000)) - this.m_reqLocalTimestamp);
            }
            return i;
        }
    }

    private class ThreadPoolQueueCounter {
        private int m_nThreadPoolQueueId;
        private String m_sThreadPoolQueueId;

        private ThreadPoolQueueCounter() {
            this.m_nThreadPoolQueueId = 0;
            this.m_sThreadPoolQueueId = SteamDBService.JOB_QUEUE_UMQ_POLL;
        }

        public String GetThreadPoolQueueId() {
            return this.m_sThreadPoolQueueId;
        }

        public void SwitchToNextThreadPoolQueueId() {
            RequestBase r = new RequestBase(GetThreadPoolQueueId());
            r.SetCallerIntent(SteamDBService.JOB_INTENT_SHUTDOWN_JOB_QUEUE);
            SteamUmqCommunicationService.this.m_svc.SubmitRequest(r);
            this.m_nThreadPoolQueueId++;
            this.m_sThreadPoolQueueId = SteamDBService.JOB_QUEUE_UMQ_POLL + this.m_nThreadPoolQueueId;
        }
    }

    public static class UmqCommand {
        public UmqCommState m_eCommand;
        public int m_nMessage;
        public int m_nTimeout;
        public RequestBase m_request;
        public String m_sMySteamID;
        public String m_sOAuthToken;
    }

    public enum UmqCommState {
        UMQ_NONE,
        UMQ_LOGON,
        UMQ_LOGON_RETRY,
        UMQ_POLL_STATUS,
        UMQ_POLL,
        UMQ_LOGOFF
    }

    public enum UmqConnectionState {
        offline,
        invalidcredentials,
        connecting,
        reconnecting,
        idle,
        active;

        public boolean isConnected() {
            return this == active;
        }

        public int getStringResid() {
            switch (AnonymousClass_2.$SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[ordinal()]) {
                case m_nRetryLogonSleepSecondsMin:
                    return R.string.service_umq_foreground_invalidcredentials;
                case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                    return R.string.service_umq_foreground_connecting;
                case AccessibilityNodeInfoCompat.ACTION_SELECT:
                    return R.string.service_umq_foreground_reconnecting;
                case MotionEventCompat.ACTION_POINTER_DOWN:
                    return R.string.Snooze;
                case MotionEventCompat.ACTION_POINTER_UP:
                    return R.string.Online;
                default:
                    return R.string.Offline;
            }
        }
    }

    SteamUmqCommunicationService(SteamDBService svc) {
        this.m_c2dmIntentReceiver = new C2DM();
        this.m_screenStateReceiver = new ScreenStateReceiver();
        this.m_steamServerInfo = new SteamServerInfo();
        this.m_eMyConnectionState = UmqConnectionState.offline;
        this.m_lockNotificationMessages = new Object();
        this.m_notificationsMessages = new HashMap();
        this.m_settingOngoingEnabled = true;
        this.m_bRunningInForeground = false;
        this.m_lastAndroidNotificationResID = 0;
        this.m_cancelOngoingNotificationThread = null;
        this.m_ThreadPoolCounter = new ThreadPoolQueueCounter();
        this.m_arrCommands = new ArrayList();
        this.m_cmdCurrent = null;
        this.m_cmdCurrentLock = new ReentrantLock();
        this.m_cmdCurrentCond = this.m_cmdCurrentLock.newCondition();
        this.m_cmdRetryLogonCond = this.m_cmdCurrentLock.newCondition();
        this.m_nRetryLogonSleepSeconds = 2;
        this.m_thread = null;
        this.m_svc = svc;
        this.m_db = new SteamUmqCommunicationDatabase(svc.getApplicationContext());
        this.m_c2dmIntentReceiver.m_c2dmRegistrationId = C2DMessaging.getRegistrationId(this.m_svc);
        this.m_c2dmIntentReceiver.m_c2dmRegistrationLanguage = SteamCommunityApplication.GetInstance().getString(R.string.DO_NOT_LOCALIZE_COOKIE_Steam_Language);
        this.m_svc.registerReceiver(this.m_c2dmIntentReceiver, new IntentFilter(C2DMProcessor.INTENT_C2DM_REGISTRATION_READY));
        IntentFilter screenStateFilter = new IntentFilter("android.intent.action.SCREEN_OFF");
        screenStateFilter.addAction("android.intent.action.SCREEN_ON");
        this.m_svc.registerReceiver(this.m_screenStateReceiver, screenStateFilter);
    }

    public ISteamUmqCommunicationDatabase getDB() {
        return this.m_db;
    }

    private void SetUmqConnectionState(UmqConnectionState eNewState) {
        if ((this.m_eMyConnectionState != UmqConnectionState.invalidcredentials || eNewState != UmqConnectionState.offline) && this.m_eMyConnectionState != eNewState) {
            UpdateAndroidNotificationPanelFromUmqState(eNewState, false);
            Intent intent = new Intent(INTENT_ACTION);
            intent.putExtra("type", "umqstate");
            intent.putExtra("state", eNewState.toString());
            intent.putExtra("old_state", this.m_eMyConnectionState.toString());
            this.m_eMyConnectionState = eNewState;
            this.m_svc.getApplicationContext().sendBroadcast(intent);
        }
    }

    public UmqConnectionState SvcReq_GetUmqConnectionState() {
        return this.m_eMyConnectionState;
    }

    public int SvcReq_GetUmqCurrentServerTime() {
        return this.m_steamServerInfo.GetCurrentServerTime();
    }

    private void UpdateAndroidNotificationNameMap(String steamid, String name) {
        if (name != null && !name.equals("")) {
            UmqInfo info = new UmqInfo();
            info.steamid = steamid;
            info.name = name;
            this.m_db.insertInfo(info);
        }
    }

    private void UpdateAndroidNotificationMessage(String steamid, String text, int numMsgs) {
        try {
            synchronized (this.m_lockNotificationMessages) {
                NotificationManager nm = (NotificationManager) this.m_svc.getSystemService("notification");
                MessageNotifyInfo mni = (MessageNotifyInfo) this.m_notificationsMessages.get(steamid);
                boolean bFirstMessage = false;
                StringBuilder sb;
                if (mni == null) {
                    mni = new MessageNotifyInfo();
                    bFirstMessage = true;
                    mni.num = numMsgs;
                    mni.msg = text;
                    if (text.length() > 40) {
                        sb = new StringBuilder(43);
                        sb.append(text.substring(0, 40));
                        sb.append("...");
                        mni.msg = sb.toString();
                    }
                    this.m_notificationsMessages.put(steamid, mni);
                } else {
                    mni.num += numMsgs;
                    String sPresentation = this.m_svc.getText(R.string.notification_chat_msgs).toString().replace("#", String.valueOf(mni.num));
                    mni.msg = text;
                    int multimsgMaxLen = 40 - sPresentation.length();
                    if (text.length() > multimsgMaxLen) {
                        sb = new StringBuilder((sPresentation.length() + multimsgMaxLen) + 3);
                        sb.append(sPresentation);
                        sb.append(text.substring(0, multimsgMaxLen));
                        sb.append("...");
                        mni.msg = sb.toString();
                    } else {
                        sb = new StringBuilder(sPresentation.length() + text.length());
                        sb.append(sPresentation);
                        sb.append(text);
                        mni.msg = sb.toString();
                    }
                }
                if (nm != null) {
                    Notification notification = new Notification(2130837539, mni.msg, System.currentTimeMillis());
                    notification.flags |= 16;
                    SteamCommunityApplication.GetInstance().GetSettingInfoDB().configureNotificationObject(notification, bFirstMessage);
                    UmqInfo umqinfo = this.m_db.selectInfo(steamid);
                    String name = umqinfo != null ? umqinfo.name : null;
                    if (name == null) {
                        name = this.m_svc.getText(R.string.Chat).toString();
                    }
                    notification.setLatestEventInfo(this.m_svc, name, mni.msg, SteamCommunityApplication.GetInstance().GetIntentToChatWithSteamID(steamid));
                    nm.notify(steamid, R.string.Chat, notification);
                }
            }
        } catch (Exception e) {
        }
    }

    private void UpdateAndroidNotificationMessageRemove(String steamid) {
        try {
            synchronized (this.m_lockNotificationMessages) {
                if (((MessageNotifyInfo) this.m_notificationsMessages.remove(steamid)) == null) {
                    return;
                }
                NotificationManager nm = (NotificationManager) this.m_svc.getSystemService("notification");
                if (nm != null) {
                    nm.cancel(steamid, R.string.Chat);
                }
            }
        } catch (Exception e) {
        }
    }

    private void UpdateAndroidNotificationMessageRemoveAll() {
        try {
            synchronized (this.m_lockNotificationMessages) {
                NotificationManager nm = (NotificationManager) this.m_svc.getSystemService("notification");
                if (nm != null) {
                    for (String steamid : this.m_notificationsMessages.keySet()) {
                        nm.cancel(steamid, R.string.Chat);
                    }
                }
                this.m_notificationsMessages.clear();
            }
        } catch (Exception e) {
        }
    }

    public void SvcReq_MarkReadMessages(REQ_ACT_MARKREADMESSAGES_DATA data) {
        this.m_db.updateMarkReadMessagesWithUser(data.mysteamid, data.withsteamid, data.deleteAllMessages);
        if (data.withsteamid != null) {
            UpdateAndroidNotificationMessageRemove(data.withsteamid);
        } else {
            UpdateAndroidNotificationMessageRemoveAll();
        }
    }

    public void SvcReq_SettingChange(REQ_ACT_SETTINGCHANGE_DATA data) {
        boolean settingOngoingEnabled = true;
        if (data.sSettingKey.equals(SettingInfoDB.KEY_NOTIFICATIONS_IM)) {
            this.m_cmdCurrentLock.lock();
            this.m_c2dmIntentReceiver.m_c2dmShouldBeSentToServer = true;
            this.m_cmdCurrentLock.unlock();
        } else if (data.sSettingKey.equals(SettingInfoDB.KEY_NOTIFICATIONS_ONGOING)) {
            if (data.sNewValue == null || data.sNewValue.equals("")) {
                settingOngoingEnabled = false;
            }
            if (this.m_settingOngoingEnabled != settingOngoingEnabled) {
                this.m_cmdCurrentLock.lock();
                this.m_settingOngoingEnabled = settingOngoingEnabled;
                if (this.m_settingOngoingEnabled) {
                    UpdateAndroidNotificationPanelFromUmqState(this.m_eMyConnectionState, true);
                } else {
                    cancelOngoingNotification();
                }
                this.m_cmdCurrentLock.unlock();
            }
        }
    }

    private void UpdateAndroidNotificationPanelFromUmqState(UmqConnectionState eState, boolean bForce) {
        if (bForce) {
            this.m_lastAndroidNotificationResID = 0;
        }
        if (!(this.m_bRunningInForeground || bForce || eState == UmqConnectionState.offline)) {
            this.m_settingOngoingEnabled = SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingForegroundService.getBooleanValue(SteamCommunityApplication.GetInstance().getApplicationContext());
        }
        switch (AnonymousClass_2.$SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqConnectionState[eState.ordinal()]) {
            case UriData.RESULT_INVALID_CONTENT:
                UpdateAndroidNotificationPanel(0);
            case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                if (!bForce && this.m_eMyConnectionState == UmqConnectionState.active) {
                    return;
                }
                if (bForce || this.m_eMyConnectionState != UmqConnectionState.reconnecting) {
                    UpdateAndroidNotificationPanel(R.string.service_umq_foreground_connecting);
                }
            default:
                UpdateAndroidNotificationPanel(eState.getStringResid());
        }
    }

    private void UpdateAndroidNotificationPanel(int resid) {
        if (resid != this.m_lastAndroidNotificationResID) {
            if (resid == 0 || !SteamCommunityApplication.GetInstance().m_bApplicationExiting) {
                int icon_resid = (resid == 2131165274 || resid == 2131165216) ? R.drawable.notification_default : R.drawable.notification_offline;
                try {
                    CharSequence text;
                    Notification notification;
                    if (this.m_lastAndroidNotificationResID == 0) {
                        text = this.m_svc.getText(resid);
                        notification = new Notification(icon_resid, text, System.currentTimeMillis());
                        notification.setLatestEventInfo(this.m_svc, this.m_svc.getText(R.string.Steam), text, SteamCommunityApplication.GetInstance().GetIntentForActivityClassFromService(CommunityActivity.class));
                        this.m_svc.startForeground(R.string.service_umq_foreground, notification);
                        this.m_lastAndroidNotificationResID = resid;
                        this.m_bRunningInForeground = true;
                    } else if (resid == 0) {
                        if (this.m_bRunningInForeground) {
                            this.m_svc.stopForeground(true);
                            this.m_bRunningInForeground = false;
                        }
                        this.m_lastAndroidNotificationResID = 0;
                        UpdateAndroidNotificationMessageRemoveAll();
                        return;
                    } else {
                        NotificationManager nm = (NotificationManager) this.m_svc.getSystemService("notification");
                        if (nm != null) {
                            text = this.m_svc.getText(resid);
                            notification = new Notification(icon_resid, text, System.currentTimeMillis());
                            notification.setLatestEventInfo(this.m_svc, this.m_svc.getText(R.string.Steam), text, SteamCommunityApplication.GetInstance().GetIntentForActivityClassFromService(CommunityActivity.class));
                            if (this.m_bRunningInForeground) {
                                nm.notify(R.string.service_umq_foreground, notification);
                            } else {
                                this.m_svc.startForeground(R.string.service_umq_foreground, notification);
                            }
                            this.m_bRunningInForeground = true;
                            this.m_lastAndroidNotificationResID = resid;
                        }
                    }
                } catch (Exception e) {
                }
                if (this.m_cancelOngoingNotificationThread == null && !this.m_settingOngoingEnabled && this.m_bRunningInForeground) {
                    this.m_cancelOngoingNotificationThread = new CancelOngoingNotificationThread();
                    this.m_cancelOngoingNotificationThread.setName("CancelOngoingNotificationThread");
                    this.m_cancelOngoingNotificationThread.start();
                }
                if (this.m_cancelOngoingNotificationThread == null) {
                    return;
                }
                if (this.m_settingOngoingEnabled) {
                    this.m_cancelOngoingNotificationThread.m_timeToClearOngoingNotification = 0;
                } else {
                    this.m_cancelOngoingNotificationThread.m_timeToClearOngoingNotification = System.currentTimeMillis() + 3000;
                }
            }
        }
    }

    private void cancelOngoingNotification() {
        if (this.m_bRunningInForeground) {
            try {
                this.m_svc.stopForeground(true);
                this.m_bRunningInForeground = false;
            } catch (Exception e) {
            }
        }
    }

    static {
        m_sUMQID = null;
        URI_RequestUMQLogon = Config.URL_WEBAPI_BASE + "/ISteamWebUserPresenceOAuth/Logon/v0001";
        URI_RequestUMQPollStatus = Config.URL_WEBAPI_BASE_INSECURE + "/ISteamWebUserPresenceOAuth/PollStatus/v0001";
        URI_RequestUMQPoll = Config.URL_WEBAPI_BASE + "/ISteamWebUserPresenceOAuth/Poll/v0001";
        s_lastTimestampOnMessage = System.currentTimeMillis();
        URI_RequestUMQMessage = Config.URL_WEBAPI_BASE + "/ISteamWebUserPresenceOAuth/Message/v0001";
        URI_RequestUMQLogoff = Config.URL_WEBAPI_BASE + "/ISteamWebUserPresenceOAuth/Logoff/v0001";
        URI_RequestUMQDeviceInfo = Config.URL_WEBAPI_BASE + "/ISteamWebUserPresenceOAuth/DeviceInfo/v0001";
        URI_RequestUMQServerInfo = Config.URL_WEBAPI_BASE + "/ISteamWebAPIUtil/GetServerInfo/v0001";
    }

    public void SvcReq_SetUmqCommunicationSettings(REQ_ACT_LOGININFO_DATA data) {
        UmqCommand cmd = new UmqCommand();
        cmd.m_eCommand = UmqCommState.UMQ_LOGON;
        cmd.m_sOAuthToken = data.sOAuthToken;
        cmd.m_sMySteamID = data.sSteamID;
        AddCommandToQueue(cmd, true);
    }

    public void SvcReq_SendMessage(REQ_ACT_SENDMESSAGE_DATA data) {
        RequestUMQMessage r = new RequestUMQMessage();
        String sPostData = "?access_token=" + data.mylogin.sOAuthToken;
        if (m_sUMQID != null) {
            sPostData = sPostData + "&umqid=" + m_sUMQID;
        }
        sPostData = (sPostData + "&type=" + data.msg.msgtype) + "&steamid_dst=" + data.msg.sWithSteamID;
        if (!(data.msg.bindata == null || data.msg.bindata.equals(""))) {
            if (data.msg.msgtype.equals("saytext") || data.msg.msgtype.equals("emote")) {
                sPostData = sPostData + "&text=" + Uri.encode(data.msg.bindata);
            }
        }
        r.SetObjData(data);
        r.SetPostData(sPostData);
        Intent intent = new Intent(INTENT_ACTION);
        intent.putExtra("type", "chatmsg");
        intent.putExtra("action", "send");
        intent.putExtra("send", "starting");
        intent.putExtra("steamid", data.msg.sWithSteamID);
        if (data.intentcontext != null) {
            intent.putExtra("intentcontext", data.intentcontext);
        }
        this.m_svc.getApplicationContext().sendBroadcast(intent);
        this.m_svc.SubmitRequest(r);
    }

    public void SvcReq_UmqActivity(REQ_ACT_UMQACTIVITY_DATA data) {
        this.m_screenStateReceiver.m_timeLastApplicationActivity = System.currentTimeMillis();
        RetryLogonSignalMoreJobsAvailable(m_nRetryLogonSleepSecondsMin);
    }

    private void RetryLogonWaitForMoreJobs() {
        long lWaitStart = System.currentTimeMillis();
        while (this.m_arrCommands.isEmpty()) {
            try {
                long lTimeLeft = (lWaitStart + ((long) (this.m_nRetryLogonSleepSeconds * 1000))) - System.currentTimeMillis();
                if (lTimeLeft > 0) {
                    this.m_cmdRetryLogonCond.awaitNanos(1000000 * lTimeLeft);
                } else {
                    return;
                }
            } catch (Exception e) {
            }
        }
    }

    private void RetryLogonSignalMoreJobsAvailable(int nRetryLogonSleepSeconds) {
        this.m_cmdCurrentLock.lock();
        this.m_nRetryLogonSleepSeconds = nRetryLogonSleepSeconds;
        this.m_cmdRetryLogonCond.signalAll();
        this.m_cmdCurrentLock.unlock();
    }

    private void CurrentJobDone(UmqCommand cmd) {
        this.m_cmdCurrentLock.lock();
        try {
            CurrentJobDoneNonLocking(cmd);
            this.m_cmdCurrentLock.unlock();
        } catch (Exception e) {
            this.m_cmdCurrentLock.unlock();
        }
    }

    private void CurrentJobDoneNonLocking(UmqCommand cmd) {
        cmd.m_request = null;
        this.m_cmdCurrentCond.signalAll();
    }

    private boolean ProcessCommandsThreadedBlocking() {
        this.m_cmdCurrentLock.lock();
        try {
            if (this.m_cmdCurrent != null && this.m_cmdCurrent.m_request != null) {
                this.m_cmdCurrentCond.await();
                this.m_cmdCurrentLock.unlock();
                return true;
            } else if (this.m_arrCommands.isEmpty()) {
                this.m_thread = null;
                this.m_cmdCurrentLock.unlock();
                return false;
            } else {
                this.m_cmdCurrent = (UmqCommand) this.m_arrCommands.get(0);
                this.m_arrCommands.remove(0);
                ProcessNextCommand();
                this.m_cmdCurrentLock.unlock();
                return true;
            }
        } catch (Exception e) {
            this.m_cmdCurrentLock.unlock();
            return true;
        }
    }

    private void ProcessNextCommand() {
        RequestUMQLogon r;
        String sPostData;
        switch (AnonymousClass_2.$SwitchMap$com$valvesoftware$android$steam$community$SteamUmqCommunicationService$UmqCommState[this.m_cmdCurrent.m_eCommand.ordinal()]) {
            case UriData.RESULT_INVALID_CONTENT:
                SetUmqConnectionState(UmqConnectionState.reconnecting);
                RetryLogonWaitForMoreJobs();
                if (this.m_cmdCurrent.m_sOAuthToken != null) {
                    SetUmqConnectionState(UmqConnectionState.offline);
                    CurrentJobDoneNonLocking(this.m_cmdCurrent);
                }
                SetUmqConnectionState(UmqConnectionState.connecting);
                r = new RequestUMQLogon(this.m_cmdCurrent);
                sPostData = "?access_token=" + this.m_cmdCurrent.m_sOAuthToken;
                if (m_sUMQID == null) {
                    m_sUMQID = SteamCommunityApplication.GetInstance().getApplicationContext().getSharedPreferences("steamumqcommunication", 0).getString("umqid", null);
                }
                if (m_sUMQID != null) {
                    sPostData = sPostData + "&umqid=" + m_sUMQID;
                }
                r.SetPostData(sPostData);
                r.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
                this.m_svc.SubmitRequest(r);
            case m_nRetryLogonSleepSecondsMin:
                if (this.m_cmdCurrent.m_sOAuthToken != null) {
                    SetUmqConnectionState(UmqConnectionState.connecting);
                    r = new RequestUMQLogon(this.m_cmdCurrent);
                    sPostData = "?access_token=" + this.m_cmdCurrent.m_sOAuthToken;
                    if (m_sUMQID == null) {
                        m_sUMQID = SteamCommunityApplication.GetInstance().getApplicationContext().getSharedPreferences("steamumqcommunication", 0).getString("umqid", null);
                    }
                    if (m_sUMQID != null) {
                        sPostData = sPostData + "&umqid=" + m_sUMQID;
                    }
                    r.SetPostData(sPostData);
                    r.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
                    this.m_svc.SubmitRequest(r);
                }
                SetUmqConnectionState(UmqConnectionState.offline);
                CurrentJobDoneNonLocking(this.m_cmdCurrent);
            case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
            case AccessibilityNodeInfoCompat.ACTION_SELECT:
                if (this.m_cmdCurrent.m_eCommand == UmqCommState.UMQ_POLL_STATUS && !this.m_screenStateReceiver.m_bIsScreenActive && ((int) ((System.currentTimeMillis() - this.m_screenStateReceiver.m_timeScreenStateChanged) / 1000)) > 60 && ((int) ((System.currentTimeMillis() - this.m_screenStateReceiver.m_timeLastApplicationActivity) / 1000)) > 60) {
                    boolean bGoingIdle = true;
                    switch (SteamCommunityApplication.GetInstance().GetSettingInfoDB().m_settingNotificationsIMs2.getRadioSelectorItemValue(SteamCommunityApplication.GetInstance().getApplicationContext()).value) {
                        case UriData.RESULT_INVALID_CONTENT:
                            if (this.m_c2dmIntentReceiver.m_c2dmClientPushIsActive) {
                                this.m_nRetryLogonSleepSeconds = 1800;
                            }
                            this.m_nRetryLogonSleepSeconds = 15;
                            bGoingIdle = false;
                            break;
                        case m_nRetryLogonSleepSecondsMin:
                            this.m_nRetryLogonSleepSeconds = 15;
                            bGoingIdle = false;
                            break;
                        default:
                            this.m_nRetryLogonSleepSeconds = 604800;
                            break;
                    }
                    if (bGoingIdle && ((int) ((System.currentTimeMillis() - this.m_screenStateReceiver.m_timeScreenStateChanged) / 1000)) < 900) {
                        this.m_screenStateReceiver.m_timeScreenStateChanged = System.currentTimeMillis() - 960000;
                    }
                    RetryLogonWaitForMoreJobs();
                    this.m_screenStateReceiver.m_timeLastApplicationActivity = System.currentTimeMillis();
                    this.m_nRetryLogonSleepSeconds = 2;
                }
                SetUmqConnectionState(UmqConnectionState.active);
                RequestUMQPoll r2 = new RequestUMQPoll(this.m_cmdCurrent);
                if (this.m_cmdCurrent.m_eCommand == UmqCommState.UMQ_POLL_STATUS) {
                    sPostData = "?steamid=" + this.m_cmdCurrent.m_sMySteamID;
                } else {
                    sPostData = "?access_token=" + this.m_cmdCurrent.m_sOAuthToken;
                }
                if (m_sUMQID != null) {
                    sPostData = sPostData + "&umqid=" + m_sUMQID;
                }
                sPostData = sPostData + "&message=" + this.m_cmdCurrent.m_nMessage;
                if (this.m_cmdCurrent.m_nTimeout > 0) {
                    sPostData = sPostData + "&sectimeout=" + this.m_cmdCurrent.m_nTimeout;
                }
                if (!this.m_screenStateReceiver.m_bIsScreenActive) {
                    sPostData = sPostData + "&secidletime=" + ((int) ((System.currentTimeMillis() - this.m_screenStateReceiver.m_timeScreenStateChanged) / 1000));
                }
                r2.SetPostData(sPostData);
                r2.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
                this.m_svc.SubmitRequest(r2);
                if (!(!this.m_c2dmIntentReceiver.m_c2dmShouldBeSentToServer || this.m_c2dmIntentReceiver.m_c2dmRegistrationId == null || this.m_c2dmIntentReceiver.m_c2dmRegistrationId.equals(""))) {
                    RequestUMQDeviceInfo r22 = null;
                    synchronized (this.m_c2dmIntentReceiver) {
                        if (this.m_c2dmIntentReceiver.m_c2dmShouldBeSentToServer) {
                            this.m_c2dmIntentReceiver.m_c2dmShouldBeSentToServer = false;
                            String sDeviceInfoPostData = "?access_token=" + this.m_cmdCurrent.m_sOAuthToken;
                            if (m_sUMQID != null) {
                                sDeviceInfoPostData = sDeviceInfoPostData + "&umqid=" + m_sUMQID;
                            }
                            r22 = new RequestUMQDeviceInfo(sDeviceInfoPostData);
                        }
                    }
                    if (r22 != null) {
                        this.m_svc.SubmitRequest(r22);
                    }
                }
                if (this.m_steamServerInfo.m_steamServerInfoRequired) {
                    RequestUMQServerInfo r23 = null;
                    synchronized (this.m_steamServerInfo) {
                        if (this.m_steamServerInfo.m_steamServerInfoRequired) {
                            this.m_steamServerInfo.m_steamServerInfoRequired = false;
                            r23 = new RequestUMQServerInfo();
                        }
                    }
                    if (r23 != null) {
                        this.m_svc.SubmitRequest(r23);
                    }
                }
            case MotionEventCompat.ACTION_POINTER_DOWN:
                SetUmqConnectionState(UmqConnectionState.offline);
                RequestUMQLogoff r3 = new RequestUMQLogoff(this.m_cmdCurrent);
                sPostData = "?access_token=" + this.m_cmdCurrent.m_sOAuthToken;
                if (m_sUMQID != null) {
                    sPostData = sPostData + "&umqid=" + m_sUMQID;
                }
                r3.SetPostData(sPostData);
                r3.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
                this.m_svc.SubmitRequest(r3);
            default:
                CurrentJobDoneNonLocking(this.m_cmdCurrent);
        }
    }

    private void AddCommandToQueue(UmqCommand cmd) {
        AddCommandToQueue(cmd, false);
    }

    private void AddCommandToQueue(UmqCommand cmd, boolean bCanChangeToken) {
        this.m_cmdCurrentLock.lock();
        UmqCommand cmdLast = this.m_arrCommands.isEmpty() ? this.m_cmdCurrent : (UmqCommand) this.m_arrCommands.get(this.m_arrCommands.size() - 1);
        if (cmdLast == null || cmdLast.m_sOAuthToken == null || cmd.m_sOAuthToken == null || !cmd.m_sOAuthToken.equals(cmdLast.m_sOAuthToken)) {
            if (!bCanChangeToken) {
                this.m_cmdCurrentLock.unlock();
                return;
            } else if (cmd.m_eCommand != UmqCommState.UMQ_LOGON) {
                this.m_cmdCurrentLock.unlock();
                return;
            } else if (cmdLast != null) {
                if (cmdLast.m_sOAuthToken != null) {
                    if (this.m_cmdCurrent != null) {
                        CurrentJobDoneNonLocking(this.m_cmdCurrent);
                    }
                    this.m_arrCommands.clear();
                    SetUmqConnectionState(UmqConnectionState.offline);
                    this.m_ThreadPoolCounter.SwitchToNextThreadPoolQueueId();
                    UmqCommand off = new UmqCommand();
                    off.m_eCommand = UmqCommState.UMQ_LOGOFF;
                    off.m_sOAuthToken = cmdLast.m_sOAuthToken;
                    off.m_sMySteamID = cmdLast.m_sMySteamID;
                    this.m_arrCommands.add(off);
                }
            }
        }
        if (cmd.m_eCommand.equals(UmqCommState.UMQ_LOGON_RETRY)) {
            this.m_arrCommands.add(0, cmd);
            if (this.m_nRetryLogonSleepSeconds * 4 < 600) {
                this.m_nRetryLogonSleepSeconds *= 4;
            } else {
                this.m_nRetryLogonSleepSeconds += 600;
            }
            if (this.m_nRetryLogonSleepSeconds > 3600) {
                this.m_nRetryLogonSleepSeconds = 3600;
            }
        } else {
            this.m_arrCommands.add(cmd);
            this.m_nRetryLogonSleepSeconds = 2;
        }
        if (this.m_thread == null) {
            this.m_thread = new Thread() {
                public void run() {
                    do {
                    } while (SteamUmqCommunicationService.this.ProcessCommandsThreadedBlocking());
                }
            };
            this.m_thread.setName("UmqCommunicationThread");
            this.m_thread.start();
        } else {
            this.m_cmdRetryLogonCond.signalAll();
        }
        this.m_cmdCurrentLock.unlock();
    }
}
