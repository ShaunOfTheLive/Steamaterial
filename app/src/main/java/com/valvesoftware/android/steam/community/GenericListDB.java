package com.valvesoftware.android.steam.community;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import com.valvesoftware.android.steam.community.GenericListDB.ListItemUpdatedListener;
import com.valvesoftware.android.steam.community.SteamDBService.TaskDoRequest;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase.DiskCacheInfo;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestDocumentType;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import org.json.JSONException;
import org.json.JSONObject;
import com.valvesoftware.android.steam.community.GenericListDB.GenericListItem.RequestForAvatarImage;

public abstract class GenericListDB extends BroadcastReceiver {
    protected boolean m_bAutoRefreshIfDataMightBeStale;
    protected boolean m_bDataMightBeStale;
    private boolean m_bHasLiveItemData;
    protected boolean m_bRefreshListCached;
    private final ArrayList<WeakReference<ListItemUpdatedListener>> m_callBacks;
    protected EventBroadcaster m_eventBroadcaster;
    protected final ConcurrentHashMap<Long, GenericListItem> m_itemsMap;
    private Object m_mtxAvatarRequests;
    private int m_numIssuedAvatarRequests;
    private GenericListItem[] m_pendingAvatarRequests;
    protected final TreeMap<Integer, RequestBase> m_reqItemDetails;
    protected RequestBase m_reqRefreshList;
    private final TreeMap<Integer, RequestForAvatarImage> m_submittedAvatarRequests;

    public static abstract class GenericListItem {
        private static Bitmap m_avatarSmallPlaceholder;
        private String m_LoadedAvatarSmallURI;
        private Bitmap m_avatarSmall;
        public String m_avatarSmallURL;
        private String m_sTriedLoadingBitmapFromCache;
        public Long m_steamID;

        public class RequestForAvatarImage extends RequestBase {
            private GenericListDB m_db;
            private String m_sOriginalRequestURI;

            public RequestForAvatarImage(String jobqueue, GenericListDB db, String uriToImage) {
                super(jobqueue);
                this.m_db = db;
                this.m_sOriginalRequestURI = uriToImage;
                SetRequestAction(RequestActionType.GetFromCacheOrDoHttpRequestAndCacheResults);
                SetUriAndDocumentType(uriToImage, RequestDocumentType.Bitmap);
            }

            com.valvesoftware.android.steam.community.GenericListDB.GenericListItem getItem() {
                return com.valvesoftware.android.steam.community.GenericListDB.GenericListItem.this;
            }

            public void RequestSucceededOnResponseWorkerThread() {
                SubmitNextRequest();
            }

            public void RequestFailedOnResponseWorkerThread() {
                SubmitNextRequest();
            }

            private void SubmitNextRequest() {
                if (this.m_db != null) {
                    this.m_db.RequestForAvatarImageFinishedOnWorkerThread();
                }
            }
        }

        public abstract boolean HasPresentationData();

        protected abstract RequestForAvatarImage RequestAvatarImage(GenericListDB genericListDB);

        static {
            m_avatarSmallPlaceholder = null;
        }

        public Bitmap GetAvatarSmall() {
            return this.m_avatarSmall != null ? this.m_avatarSmall : GetPlaceholderAvatar();
        }

        private void SetAvatarSmall(Bitmap bmp, String sAvatarURI) {
            this.m_avatarSmall = bmp;
            this.m_LoadedAvatarSmallURI = sAvatarURI;
        }

        public boolean IsAvatarSmallLoaded() {
            return ((this.m_LoadedAvatarSmallURI == null || !this.m_LoadedAvatarSmallURI.equals(this.m_avatarSmallURL) || this.m_avatarSmall == null) && this.m_avatarSmallURL != null) ? TryLoadingAvatarFromCache() : true;
        }

        private boolean TryLoadingAvatarFromCache() {
            if (this.m_sTriedLoadingBitmapFromCache != null && this.m_sTriedLoadingBitmapFromCache.equals(this.m_avatarSmallURL)) {
                return false;
            }
            this.m_sTriedLoadingBitmapFromCache = this.m_avatarSmallURL;
            RequestForAvatarImage req = RequestAvatarImage(null);
            req.SetRequestAction(RequestActionType.GetFromCacheOnly);
            WorkerThreadPool.RunTask(new TaskDoRequest(req));
            return HandleAvatarImageResponse(req);
        }

        private boolean HandleAvatarImageResponse(RequestForAvatarImage req) {
            if (req.GetBitmap() != null) {
                SetAvatarSmall(req.GetBitmap(), req.m_sOriginalRequestURI);
                return true;
            } else if (req.GetHttpResponseUriData().m_httpResult != 1) {
                return false;
            } else {
                SetAvatarSmall(GetAvatarSmall(), req.m_sOriginalRequestURI);
                return true;
            }
        }

        public RequestForAvatarImage GetRequestForAvatarImage(String jobqueue, String uriToImage, String steamID, GenericListDB db) {
            return new RequestForAvatarImage(jobqueue, db, uriToImage);
        }

        protected Bitmap GetPlaceholderAvatar() {
            if (m_avatarSmallPlaceholder == null) {
                m_avatarSmallPlaceholder = BitmapFactory.decodeResource(SteamCommunityApplication.GetInstance().getApplicationContext().getResources(), GetPlaceholderAvatarResourceId());
            }
            return m_avatarSmallPlaceholder;
        }

        protected int GetPlaceholderAvatarResourceId() {
            return R.drawable.placeholder_contact;
        }
    }

    public static interface ListItemUpdatedListener {
        void OnListItemInfoUpdateError(RequestBase requestBase);

        void OnListItemInfoUpdated(ArrayList<Long> arrayList, boolean z);

        void OnListRefreshError(RequestBase requestBase, boolean z);

        void OnListRequestsInProgress(boolean z);
    }

    class EventBroadcaster implements ListItemUpdatedListener {
        EventBroadcaster() {
        }

        public void OnListItemInfoUpdated(ArrayList<Long> updatedSteamIDs, boolean requireRefresh) {
            Iterator i$ = GenericListDB.this.m_callBacks.iterator();
            while (i$.hasNext()) {
                WeakReference<ListItemUpdatedListener> weakRef = (WeakReference) i$.next();
                ListItemUpdatedListener listener = (ListItemUpdatedListener) weakRef.get();
                if (listener != null) {
                    listener.OnListItemInfoUpdated(updatedSteamIDs, requireRefresh);
                } else {
                    GenericListDB.this.m_callBacks.remove(weakRef);
                }
            }
        }

        public void OnListItemInfoUpdateError(RequestBase req) {
            Iterator i$ = GenericListDB.this.m_callBacks.iterator();
            while (i$.hasNext()) {
                WeakReference<ListItemUpdatedListener> weakRef = (WeakReference) i$.next();
                ListItemUpdatedListener listener = (ListItemUpdatedListener) weakRef.get();
                if (listener != null) {
                    listener.OnListItemInfoUpdateError(req);
                } else {
                    GenericListDB.this.m_callBacks.remove(weakRef);
                }
            }
        }

        public void OnListRefreshError(RequestBase req, boolean cached) {
            Iterator i$ = GenericListDB.this.m_callBacks.iterator();
            while (i$.hasNext()) {
                WeakReference<ListItemUpdatedListener> weakRef = (WeakReference) i$.next();
                ListItemUpdatedListener listener = (ListItemUpdatedListener) weakRef.get();
                if (listener != null) {
                    listener.OnListRefreshError(req, cached);
                } else {
                    GenericListDB.this.m_callBacks.remove(weakRef);
                }
            }
        }

        public void OnListRequestsInProgress(boolean isRefreshing) {
            Iterator i$ = GenericListDB.this.m_callBacks.iterator();
            while (i$.hasNext()) {
                WeakReference<ListItemUpdatedListener> weakRef = (WeakReference) i$.next();
                ListItemUpdatedListener listener = (ListItemUpdatedListener) weakRef.get();
                if (listener != null) {
                    listener.OnListRequestsInProgress(isRefreshing);
                } else {
                    GenericListDB.this.m_callBacks.remove(weakRef);
                }
            }
        }
    }

    protected static class WriteItemSummaryDocumentToCache extends RequestBase {
        byte[] m_data;

        public WriteItemSummaryDocumentToCache(String steamid, byte[] data) {
            super(SteamDBService.JOB_QUEUE_DISKCACHE);
            SetIndefiniteCacheFilename(steamid);
            this.m_data = data;
        }

        public boolean OnTaskReadyToRunOnJobQueue() {
            DiskCacheInfo dci = GetDiskCacheInfo();
            dci.disk.Write(dci.uri, this.m_data);
            return false;
        }
    }

    protected abstract void HandleItemSummaryDocument(RequestBase requestBase);

    protected abstract void HandleItemSummaryDocument(JSONObject jSONObject, String str);

    protected abstract void HandleListRefreshDocument(RequestBase requestBase, boolean z);

    protected abstract RequestBase IssueFullListRefreshRequest(boolean z);

    protected abstract RequestBase IssueSingleItemSummaryRequest(String[] strArr);

    public boolean HasLiveItemData() {
        return this.m_bHasLiveItemData;
    }

    GenericListDB() {
        this.m_bHasLiveItemData = false;
        this.m_itemsMap = new ConcurrentHashMap();
        this.m_reqRefreshList = null;
        this.m_bRefreshListCached = true;
        this.m_reqItemDetails = new TreeMap();
        this.m_mtxAvatarRequests = new Object();
        this.m_submittedAvatarRequests = new TreeMap();
        this.m_pendingAvatarRequests = null;
        this.m_numIssuedAvatarRequests = 0;
        this.m_callBacks = new ArrayList();
        this.m_eventBroadcaster = new EventBroadcaster();
        this.m_bAutoRefreshIfDataMightBeStale = false;
        this.m_bDataMightBeStale = false;
        SteamCommunityApplication.GetInstance().registerReceiver(this, new IntentFilter(getClass().getName()));
    }

    public ConcurrentHashMap<Long, GenericListItem> GetItemsMap() {
        return this.m_itemsMap;
    }

    public void ClearItems() {
        this.m_itemsMap.clear();
        this.m_pendingAvatarRequests = null;
        this.m_numIssuedAvatarRequests = 0;
        this.m_eventBroadcaster.OnListItemInfoUpdated(null, true);
    }

    public GenericListItem GetListItem(Long steamID) {
        return (GenericListItem) this.m_itemsMap.get(steamID);
    }

    protected void RemoveOldItemList(ArrayList<Long> oldSet) {
        if (!oldSet.isEmpty()) {
            Iterator i$ = oldSet.iterator();
            while (i$.hasNext()) {
                this.m_itemsMap.remove((Long) i$.next());
            }
            this.m_eventBroadcaster.OnListItemInfoUpdated(oldSet, true);
        }
    }

    protected void IssueItemSummaryRequest(String[] steamIDs, boolean cached) {
        if (!SteamWebApi.IsLoggedIn()) {
            return;
        }
        String[] arr$;
        int len$;
        int i$;
        String id;
        if (cached) {
            ArrayList<Long> updatedUsers = new ArrayList();
            arr$ = steamIDs;
            len$ = arr$.length;
            i$ = 0;
            while (i$ < len$) {
                id = arr$[i$];
                byte[] data = SteamCommunityApplication.GetInstance().GetDiskCacheIndefinite().Read(id);
                if (data != null) {
                    if (data != null) {
                        try {
                            HandleItemSummaryDocument(new JSONObject(new String(data)), id);
                            updatedUsers.add(Long.valueOf(id));
                        } catch (JSONException e) {
                        }
                    }
                    i$++;
                } else {
                    if (data != null) {
                        HandleItemSummaryDocument(new JSONObject(new String(data)), id);
                        updatedUsers.add(Long.valueOf(id));
                    }
                    i$++;
                }
            }
            if (!updatedUsers.isEmpty()) {
                this.m_eventBroadcaster.OnListItemInfoUpdated(updatedUsers, true);
                return;
            }
            return;
        }
        boolean bStartedRefreshRequests = false;
        String[] param = new String[100];
        arr$ = steamIDs;
        len$ = arr$.length;
        i$ = 0;
        int numParams = 0;
        while (i$ < len$) {
            int numParams2;
            RequestBase req;
            id = arr$[i$];
            if (this.m_itemsMap.containsKey(Long.valueOf(id))) {
                numParams2 = numParams + 1;
                param[numParams] = id;
                if (numParams2 >= 100) {
                    req = IssueSingleItemSummaryRequest(param);
                    if (req != null) {
                        req.SetCallerIntent(getClass().getName());
                        SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(req);
                        this.m_reqItemDetails.put(Integer.valueOf(req.GetIntentId()), req);
                        bStartedRefreshRequests = true;
                    }
                    numParams2 = 0;
                }
            } else {
                numParams2 = numParams;
            }
            i$++;
            numParams = numParams2;
        }
        if (numParams > 0) {
            if (numParams < 100) {
                param[numParams] = null;
            }
            req = IssueSingleItemSummaryRequest(param);
            if (req != null) {
                req.SetCallerIntent(getClass().getName());
                SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(req);
                this.m_reqItemDetails.put(Integer.valueOf(req.GetIntentId()), req);
                bStartedRefreshRequests = true;
            }
        } else {
            numParams2 = numParams;
        }
        if (bStartedRefreshRequests) {
            this.m_eventBroadcaster.OnListRequestsInProgress(true);
        }
    }

    public void RequestAvatarImage(GenericListItem[] required) {
        synchronized (this.m_mtxAvatarRequests) {
            this.m_pendingAvatarRequests = required;
            this.m_numIssuedAvatarRequests = 0;
        }
        IssueQueuedAvatarImageRequests(false);
    }

    protected void RequestAvatarImageImmediately(GenericListItem x) {
        if (x != null) {
            synchronized (this.m_mtxAvatarRequests) {
                RequestForAvatarImage req = x.RequestAvatarImage(this);
                if (req == null) {
                    return;
                }
                this.m_submittedAvatarRequests.put(Integer.valueOf(req.GetIntentId()), req);
                if (req != null) {
                    req.SetCallerIntent(getClass().getName());
                    req.SetRequestAction(RequestActionType.DoHttpRequestAndCacheResults);
                    SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(req);
                }
            }
        }
    }

    protected void IssueQueuedAvatarImageRequests(boolean onWorkerThread) {
        RequestForAvatarImage req = null;
        synchronized (this.m_mtxAvatarRequests) {
            if (!onWorkerThread) {
                if (!this.m_submittedAvatarRequests.isEmpty()) {
                    return;
                }
            }
            if (this.m_pendingAvatarRequests == null) {
                return;
            }
            while (this.m_numIssuedAvatarRequests >= 0 && this.m_numIssuedAvatarRequests < this.m_pendingAvatarRequests.length) {
                GenericListItem[] genericListItemArr = this.m_pendingAvatarRequests;
                int i = this.m_numIssuedAvatarRequests;
                this.m_numIssuedAvatarRequests = i + 1;
                GenericListItem x = genericListItemArr[i];
                if (x != null) {
                    req = x.RequestAvatarImage(this);
                    if (req != null) {
                        this.m_submittedAvatarRequests.put(Integer.valueOf(req.GetIntentId()), req);
                        break;
                    }
                }
            }
            if (req != null) {
                req.SetCallerIntent(getClass().getName());
                req.SetRequestAction(RequestActionType.DoHttpRequestAndCacheResults);
                SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(req);
            }
        }
    }

    private void HandleAvatarImageResponse(RequestForAvatarImage req) {
        GenericListItem myEntry = req.getItem();
        if (myEntry != null && myEntry.HandleAvatarImageResponse(req)) {
            ArrayList<Long> updatedAvatarSteamIDs = new ArrayList();
            updatedAvatarSteamIDs.add(myEntry.m_steamID);
            this.m_eventBroadcaster.OnListItemInfoUpdated(updatedAvatarSteamIDs, false);
        }
        IssueQueuedAvatarImageRequests(false);
    }

    private void RequestForAvatarImageFinishedOnWorkerThread() {
        IssueQueuedAvatarImageRequests(true);
    }

    public void RefreshFromCacheAndHttp() {
        IssueRefreshRequest(true);
    }

    public void RefreshFromHttpOnly() {
        IssueRefreshRequest(false);
    }

    private void IssueRefreshRequest(boolean bStartCached) {
        if (SteamWebApi.IsLoggedIn()) {
            this.m_bRefreshListCached = bStartCached;
            this.m_reqRefreshList = IssueFullListRefreshRequest(this.m_bRefreshListCached);
            if (this.m_reqRefreshList == null && this.m_bRefreshListCached) {
                this.m_bRefreshListCached = false;
                this.m_reqRefreshList = IssueFullListRefreshRequest(this.m_bRefreshListCached);
            }
            if (this.m_reqRefreshList != null) {
                this.m_reqRefreshList.SetCallerIntent(getClass().getName());
                SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(this.m_reqRefreshList);
                this.m_eventBroadcaster.OnListRequestsInProgress(true);
            }
        }
    }

    protected void RefreshFromHttpIfDataMightBeStale() {
        if (this.m_bDataMightBeStale && SteamWebApi.IsLoggedIn()) {
            SteamDBService dbService = SteamCommunityApplication.GetInstance().GetSteamDB();
            if (dbService != null) {
                this.m_bDataMightBeStale = !dbService.getSteamUmqConnectionState().isConnected();
            }
            RefreshFromHttpOnly();
        }
    }

    public void SetAutoRefreshIfDataMightBeStale(boolean bAutoRefresh) {
        this.m_bAutoRefreshIfDataMightBeStale = bAutoRefresh;
        if (bAutoRefresh) {
            RefreshFromHttpIfDataMightBeStale();
        }
    }

    private void checkIfNoLongerRefreshingAndNotifyListeners() {
        if (this.m_reqRefreshList == null && this.m_reqItemDetails.isEmpty()) {
            this.m_eventBroadcaster.OnListRequestsInProgress(false);
        }
    }

    public void onReceive(Context context, Intent intent) {
        if (getClass().getName().equals(intent.getAction())) {
            Integer intentId = Integer.valueOf(intent.getIntExtra("intent_id", -1));
            if (this.m_reqRefreshList == null || this.m_reqRefreshList.GetIntentId() != intentId.intValue()) {
                RequestBase requestForItemSummariesBySteamIDs = (RequestBase) this.m_reqItemDetails.remove(intentId);
                if (requestForItemSummariesBySteamIDs != null) {
                    if (requestForItemSummariesBySteamIDs.IsRequestLive()) {
                        try {
                            if (requestForItemSummariesBySteamIDs.GetRequestAction() != RequestActionType.GetFromCacheOnly) {
                                this.m_bHasLiveItemData = true;
                            }
                            HandleItemSummaryDocument(requestForItemSummariesBySteamIDs);
                        } catch (Exception e) {
                            this.m_eventBroadcaster.OnListItemInfoUpdateError(requestForItemSummariesBySteamIDs);
                        }
                    } else {
                        this.m_eventBroadcaster.OnListItemInfoUpdateError(requestForItemSummariesBySteamIDs);
                    }
                    checkIfNoLongerRefreshingAndNotifyListeners();
                    return;
                }
                RequestForAvatarImage reqAvatarImage;
                synchronized (this.m_mtxAvatarRequests) {
                    reqAvatarImage = (RequestForAvatarImage) this.m_submittedAvatarRequests.remove(intentId);
                }
                if (reqAvatarImage != null) {
                    HandleAvatarImageResponse(reqAvatarImage);
                    return;
                }
                return;
            }
            RequestBase req = this.m_reqRefreshList;
            this.m_reqRefreshList = null;
            if (req.IsRequestLive()) {
                try {
                    HandleListRefreshDocument(req, this.m_bRefreshListCached);
                } catch (Exception e2) {
                    this.m_eventBroadcaster.OnListRefreshError(req, this.m_bRefreshListCached);
                }
            } else {
                this.m_eventBroadcaster.OnListRefreshError(req, this.m_bRefreshListCached);
            }
            if (this.m_bRefreshListCached) {
                this.m_bDataMightBeStale = true;
                RefreshFromHttpIfDataMightBeStale();
            }
            checkIfNoLongerRefreshingAndNotifyListeners();
        }
    }

    protected void StoreItemSummaryDocumentInCache(String steamid, String data) {
        SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(new WriteItemSummaryDocumentToCache(steamid, data.getBytes()));
    }

    public void RegisterCallback(ListItemUpdatedListener newListener) {
        DeregisterCallback(newListener);
        this.m_callBacks.add(new WeakReference(newListener));
    }

    public void DeregisterCallback(ListItemUpdatedListener oldListener) {
        Iterator i$ = this.m_callBacks.iterator();
        while (i$.hasNext()) {
            WeakReference<ListItemUpdatedListener> weakRef = (WeakReference) i$.next();
            if (oldListener == ((ListItemUpdatedListener) weakRef.get())) {
                this.m_callBacks.remove(weakRef);
                return;
            }
        }
    }
}
