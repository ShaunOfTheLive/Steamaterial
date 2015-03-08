package com.valvesoftware.android.steam.community;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.support.v4.view.accessibility.AccessibilityNodeInfoCompat;
import com.valvesoftware.android.steam.community.ISteamUmqCommunicationDatabase.Message;
import com.valvesoftware.android.steam.community.SteamDBDiskCache.IndefiniteCache;
import com.valvesoftware.android.steam.community.SteamDBService.REQ_ACT_LOGININFO_DATA;
import com.valvesoftware.android.steam.community.SteamDBService.TaskDoRequest;
import com.valvesoftware.android.steam.community.SteamDBService.UriCacheState;
import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.SteamDBService.UriDataState;
import com.valvesoftware.android.steam.community.SteamUmqCommunicationService.UmqConnectionState;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase.DiskCacheInfo;
import com.valvesoftware.android.steam.community.WorkerThreadPool.AddBackToPoolException;
import com.valvesoftware.android.steam.community.WorkerThreadPool.ShutdownJobQueueException;
import com.valvesoftware.android.steam.community.WorkerThreadPool.Task;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;

public class SteamDBService extends Service {
    public static final String JOB_INTENT_SHUTDOWN_JOB_QUEUE = "JOB_INTENT_SHUTDOWN_JOB_QUEUE";
    public static final String JOB_QUEUE_APPS_DB = "JobQueueAppsDB";
    public static final String JOB_QUEUE_AVATARS_FRIENDS = "JobQueueAvatarsFriends";
    public static final String JOB_QUEUE_AVATARS_GROUPS = "JobQueueAvatarsGroups";
    public static final String JOB_QUEUE_CATALOG = "JobQueueCatalog";
    public static final String JOB_QUEUE_DISKCACHE = "JobQueueDiskCache";
    public static final String JOB_QUEUE_FRIENDS_DB = "JobQueueFriendsDB";
    public static final String JOB_QUEUE_GROUPS_DB = "JobQueueGroupsDB";
    public static final String JOB_QUEUE_SIMPLEACTION = "JobQueueSimpleAction";
    public static final String JOB_QUEUE_UMQ_DEVICEINFO = "JobQueueUMQdeviceinfo";
    public static final String JOB_QUEUE_UMQ_POLL = "JobQueueUMQpoll";
    public static final String JOB_QUEUE_UMQ_SENDMESSAGE = "JobQueueUMQsendmsg";
    public static final String REQ_ACT_LOGININFO = "SetLoginInformation";
    public static final String REQ_ACT_MARKREADMESSAGES = "MarkReadMessages";
    public static final String REQ_ACT_SENDMESSAGE = "SendMessage";
    public static final String REQ_ACT_SETTINGCHANGE = "SettingChange";
    public static final String REQ_ACT_UMQACTIVITY = "UmqActivity";
    private static final String TAG = "SteamDataBase";
    private final IBinder binder;
    private ISteamDebugUtil m_steamDebugUtil;
    private SteamUmqCommunicationService m_umqCommunicationService;
    private final WorkerThreadPool m_workerThreadPool;

    static /* synthetic */ class AnonymousClass_1 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType = new int[RequestActionType.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[RequestActionType.GetFromCacheOnly.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[RequestActionType.GetFromCacheOrDoHttpRequestAndCacheResults.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[RequestActionType.DoHttpRequestNoCache.ordinal()] = 3;
            } catch (NoSuchFieldError e3) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[RequestActionType.DoHttpRequestAndCacheResults.ordinal()] = 4;
            } catch (NoSuchFieldError e4) {
            }
        }
    }

    public static class REQ_ACT_LOGININFO_DATA {
        public String sOAuthToken;
        public String sSteamID;
    }

    public static class REQ_ACT_MARKREADMESSAGES_DATA {
        public boolean deleteAllMessages;
        public String mysteamid;
        public String withsteamid;
    }

    public static class REQ_ACT_SENDMESSAGE_DATA {
        public String intentcontext;
        public Message msg;
        public REQ_ACT_LOGININFO_DATA mylogin;
    }

    public static class REQ_ACT_SETTINGCHANGE_DATA {
        public String sNewValue;
        public String sSettingKey;
        public String sSteamID;
    }

    public static class REQ_ACT_UMQACTIVITY_DATA {
    }

    public class SteamDBTempBinder extends Binder {
        SteamDBService getService() {
            return SteamDBService.this;
        }
    }

    private static abstract class TaskDiskCacheBase implements Task {
        protected DiskCacheInfo m_cache;

        protected TaskDiskCacheBase(DiskCacheInfo cache) {
            this.m_cache = cache;
        }

        public String GetRequestQueue() {
            return JOB_QUEUE_DISKCACHE;
        }
    }

    private static class TaskDiskCacheDelete extends TaskDiskCacheBase {
        TaskDiskCacheDelete(DiskCacheInfo cache) {
            super(cache);
        }

        public void Run() {
            if (this.m_cache.disk instanceof IndefiniteCache) {
                ((IndefiniteCache) this.m_cache.disk).Delete(this.m_cache.uri);
            }
        }
    }

    private static class TaskDiskCacheWrite extends TaskDiskCacheBase {
        private byte[] m_data;

        TaskDiskCacheWrite(DiskCacheInfo cache, byte[] data) {
            super(cache);
            this.m_data = null;
            this.m_data = data;
        }

        public void Run() {
            this.m_cache.disk.Write(this.m_cache.uri, this.m_data);
        }
    }

    public static class TaskDoRequest implements Task {
        protected RequestBase m_request;

        public TaskDoRequest(RequestBase request) {
            this.m_request = null;
            this.m_request = request;
        }

        protected void WriteToCacheInBackground(UriData data) {
        }

        private UriData ReadFromMemoryOrCache() {
            UriData liveData = new UriData();
            DiskCacheInfo cache = this.m_request.GetDiskCacheInfo();
            byte[] data = cache.disk.Read(cache.uri);
            if (data != null) {
                liveData.SetRequestIsLive(data);
            }
            return data != null ? liveData : liveData;
        }

        private UriData FetchFromHttp() throws Exception {
            HttpResponse httpResponse = this.m_request.FetchHttpResponseOnWorkerThread();
            HttpEntity responseEntity = httpResponse.getEntity();
            StatusLine statusLine = httpResponse.getStatusLine();
            if (statusLine.getStatusCode() != 200 || responseEntity == null) {
                UriData data = new UriData();
                data.SetRequestError(statusLine.getStatusCode());
                return data;
            }
            InputStream inputStream = responseEntity.getContent();
            UriData liveData = new UriData();
            ByteArrayOutputStream baos = SteamDBService.ConvertInputStreamToByteArray(inputStream);
            if (this.m_request.IsRequestForByteData()) {
                liveData.SetRequestIsLive(baos.toByteArray());
                return liveData;
            }
            liveData.SetRequestIsLive(baos.toString());
            return liveData;
        }

        public String GetRequestQueue() {
            switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[this.m_request.GetRequestAction().ordinal()]) {
                case UriData.RESULT_INVALID_CONTENT:
                case UriData.RESULT_HTTP_EXCEPTION:
                    return JOB_QUEUE_DISKCACHE;
                default:
                    return this.m_request.GetRequestQueue();
            }
        }

        public void Run() {
            Throwable th;
            if (this.m_request.OnTaskReadyToRunOnJobQueue()) {
                String sCallerIntent = this.m_request.GetCallerIntent();
                if (sCallerIntent == null || !JOB_INTENT_SHUTDOWN_JOB_QUEUE.equals(sCallerIntent)) {
                    boolean success = false;
                    UriData clonedData = null;
                    switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[this.m_request.GetRequestAction().ordinal()]) {
                        case UriData.RESULT_INVALID_CONTENT:
                            clonedData = ReadFromMemoryOrCache();
                            break;
                        case UriData.RESULT_HTTP_EXCEPTION:
                            clonedData = ReadFromMemoryOrCache();
                            if (clonedData.GetRequestCacheState() == UriCacheState.CacheIsMissing) {
                                this.m_request.SetRequestAction(RequestActionType.DoHttpRequestAndCacheResults);
                                throw new AddBackToPoolException();
                            }
                    }
                    Intent intent;
                    try {
                        switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$SteamWebApi$RequestActionType[this.m_request.GetRequestAction().ordinal()]) {
                            case FragmentManagerImpl.ANIM_STYLE_CLOSE_ENTER:
                                clonedData = FetchFromHttp();
                                break;
                            case AccessibilityNodeInfoCompat.ACTION_SELECT:
                                clonedData = FetchFromHttp();
                                if (clonedData.IsRequestLive()) {
                                    WriteToCacheInBackground(clonedData);
                                }
                                break;
                        }
                        if (clonedData.GetRequestCacheState() != UriCacheState.CacheIsMissing) {
                            this.m_request.OnResponseWorkerThread(clonedData);
                            success = true;
                        }
                        if (!success) {
                            this.m_request.OnFailureWorkerThread(clonedData);
                        }
                        if (sCallerIntent != null) {
                            intent = new Intent(sCallerIntent);
                            intent.putExtra("intent_id", this.m_request.GetIntentId());
                            SteamCommunityApplication.GetInstance().sendBroadcast(intent);
                            return;
                        }
                        return;
                    } catch (Exception e) {
                        UriData clonedData2 = clonedData;
                        try {
                            clonedData = new UriData();
                            try {
                                clonedData.SetRequestError(UriData.RESULT_HTTP_EXCEPTION);
                                if (null == null) {
                                    this.m_request.OnFailureWorkerThread(clonedData);
                                }
                                if (sCallerIntent != null) {
                                    intent = new Intent(sCallerIntent);
                                    intent.putExtra("intent_id", this.m_request.GetIntentId());
                                    SteamCommunityApplication.GetInstance().sendBroadcast(intent);
                                }
                            } catch (Throwable th2) {
                                th = th2;
                                if (null == null) {
                                    this.m_request.OnFailureWorkerThread(clonedData);
                                }
                                if (sCallerIntent != null) {
                                    intent = new Intent(sCallerIntent);
                                    intent.putExtra("intent_id", this.m_request.GetIntentId());
                                    SteamCommunityApplication.GetInstance().sendBroadcast(intent);
                                }
                                throw th;
                            }
                        } catch (Throwable th3) {
                            th = th3;
                            clonedData = clonedData2;
                            if (null == null) {
                                this.m_request.OnFailureWorkerThread(clonedData);
                            }
                            if (sCallerIntent != null) {
                                intent = new Intent(sCallerIntent);
                                intent.putExtra("intent_id", this.m_request.GetIntentId());
                                SteamCommunityApplication.GetInstance().sendBroadcast(intent);
                            }
                            throw th;
                        }
                    }
                }
                throw new ShutdownJobQueueException();
            }
        }
    }

    public class TaskDoRequestWithCache extends TaskDoRequest {
        public TaskDoRequestWithCache(RequestBase request) {
            super(request);
        }

        protected void WriteToCacheInBackground(UriData data) {
            SteamDBService.this.m_workerThreadPool.AddTask(new TaskDiskCacheWrite(this.m_request.GetDiskCacheInfo(), data.GetByteData()));
        }
    }

    public enum UriCacheState {
        CacheIsLive,
        CacheIsStale,
        CacheIsMissing
    }

    public static class UriData {
        public static final int RESULT_HTTP_EXCEPTION = 2;
        public static final int RESULT_INVALID_CONTENT = 1;
        public int m_httpResult;
        private byte[] m_resultByteData;
        private String m_resultDocument;
        public UriDataState m_state;

        public UriData() {
            this.m_state = UriDataState.RequestError;
        }

        public void SetRequestIsLive(String document) {
            this.m_state = UriDataState.RequestIsLive;
            this.m_httpResult = 200;
            this.m_resultDocument = document;
            this.m_resultByteData = null;
        }

        public void SetRequestIsLive(byte[] data) {
            this.m_state = UriDataState.RequestIsLive;
            this.m_httpResult = 200;
            this.m_resultDocument = null;
            this.m_resultByteData = data;
        }

        public void SetRequestError(int httpResult) {
            this.m_state = UriDataState.RequestError;
            this.m_httpResult = httpResult;
        }

        public boolean IsRequestLive() {
            return this.m_state == UriDataState.RequestIsLive;
        }

        public UriCacheState GetRequestCacheState() {
            if (this.m_state == UriDataState.RequestIsLive) {
                return UriCacheState.CacheIsLive;
            }
            return (this.m_resultDocument == null && this.m_resultByteData == null) ? UriCacheState.CacheIsMissing : UriCacheState.CacheIsStale;
        }

        public String GetDocument() {
            if (this.m_resultDocument == null && this.m_resultByteData != null) {
                this.m_resultDocument = new String(this.m_resultByteData);
            }
            return this.m_resultDocument;
        }

        public byte[] GetByteData() {
            if (this.m_resultByteData == null && this.m_resultDocument != null) {
                this.m_resultByteData = this.m_resultDocument.getBytes();
            }
            return this.m_resultByteData;
        }
    }

    public enum UriDataState {
        RequestIsLive,
        RequestError
    }

    public SteamDBService() {
        this.m_umqCommunicationService = null;
        this.m_steamDebugUtil = new SteamDebugUtil(null);
        this.m_workerThreadPool = new WorkerThreadPool();
        this.binder = new SteamDBTempBinder();
    }

    private static ByteArrayOutputStream ConvertInputStreamToByteArray(InputStream inputStream) throws Exception {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] data = new byte[1024];
        while (true) {
            int nRead = inputStream.read(data, 0, data.length);
            if (nRead != -1) {
                bos.write(data, 0, nRead);
            } else {
                bos.flush();
                inputStream.close();
                return bos;
            }
        }
    }

    public ISteamUmqCommunicationDatabase getSteamUmqCommunicationServiceDB() {
        return this.m_umqCommunicationService.getDB();
    }

    public UmqConnectionState getSteamUmqConnectionState() {
        return this.m_umqCommunicationService.SvcReq_GetUmqConnectionState();
    }

    public int getSteamUmqCurrentServerTime() {
        return this.m_umqCommunicationService.SvcReq_GetUmqCurrentServerTime();
    }

    public ISteamDebugUtil getDebugUtil() {
        return this.m_steamDebugUtil;
    }

    public void onCreate() {
        this.m_umqCommunicationService = new SteamUmqCommunicationService(this);
        if (this.m_steamDebugUtil instanceof SteamDebugUtil) {
            ((SteamDebugUtil) this.m_steamDebugUtil).SetServiceContext(this);
        }
        SteamCommunityApplication.m_CrashHandler.m_dbgUtil = this.m_steamDebugUtil;
        SteamCommunityApplication.m_CrashHandler.register();
        C2DMProcessor.refreshAppC2DMRegistrationState(getApplicationContext());
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    public IBinder onBind(Intent intent) {
        return this.binder;
    }

    public void SubmitRequest(RequestBase request) {
        if (!JOB_QUEUE_SIMPLEACTION.equals(request.GetRequestQueue())) {
            this.m_workerThreadPool.AddTask(new TaskDoRequestWithCache(request));
        } else if (REQ_ACT_LOGININFO.equals(request.GetUri()) && (request.GetObjData() instanceof REQ_ACT_LOGININFO_DATA)) {
            this.m_umqCommunicationService.SvcReq_SetUmqCommunicationSettings((REQ_ACT_LOGININFO_DATA) request.GetObjData());
        } else if (REQ_ACT_SENDMESSAGE.equals(request.GetUri()) && (request.GetObjData() instanceof REQ_ACT_SENDMESSAGE_DATA)) {
            this.m_umqCommunicationService.SvcReq_SendMessage((REQ_ACT_SENDMESSAGE_DATA) request.GetObjData());
        } else if (REQ_ACT_UMQACTIVITY.equals(request.GetUri()) && (request.GetObjData() instanceof REQ_ACT_UMQACTIVITY_DATA)) {
            this.m_umqCommunicationService.SvcReq_UmqActivity((REQ_ACT_UMQACTIVITY_DATA) request.GetObjData());
        } else if (REQ_ACT_MARKREADMESSAGES.equals(request.GetUri()) && (request.GetObjData() instanceof REQ_ACT_MARKREADMESSAGES_DATA)) {
            this.m_umqCommunicationService.SvcReq_MarkReadMessages((REQ_ACT_MARKREADMESSAGES_DATA) request.GetObjData());
        } else if (REQ_ACT_SETTINGCHANGE.equals(request.GetUri()) && (request.GetObjData() instanceof REQ_ACT_SETTINGCHANGE_DATA)) {
            this.m_umqCommunicationService.SvcReq_SettingChange((REQ_ACT_SETTINGCHANGE_DATA) request.GetObjData());
        }
    }

    public void DeleteIndefiniteCached(RequestBase request) {
        this.m_workerThreadPool.AddTask(new TaskDiskCacheDelete(request.GetDiskCacheInfo()));
    }
}
