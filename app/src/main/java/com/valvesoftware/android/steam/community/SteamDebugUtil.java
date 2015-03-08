package com.valvesoftware.android.steam.community;

import com.valvesoftware.android.steam.community.ISteamDebugUtil.Helper;
import com.valvesoftware.android.steam.community.ISteamDebugUtil.IDebugUtilRecord;
import com.valvesoftware.android.steam.community.SteamDebugUtilDatabase.CrashTrace;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestActionType;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestBase;
import com.valvesoftware.android.steam.community.SteamWebApi.RequestDocumentType;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SteamDebugUtil extends Helper {
    private ArrayList<DebugUtilRecord> m_arrCommands;
    private DebugUtilRecord m_cmdCurrent;
    private final Condition m_cmdCurrentCond;
    private final Lock m_cmdCurrentLock;
    private SteamDebugUtilDatabase m_db;
    private SteamDBService m_svc;
    private Thread m_thread;

    class AnonymousClass_1 extends Thread {
        final /* synthetic */ ArrayList val$arrTraces;

        final class UploaderRequest extends RequestBase {
            public CrashTrace m_CrashTrace;
            public boolean m_bFinished;
            public boolean m_bSucceeded;

            public UploaderRequest(CrashTrace trace) {
                super("SteamDebugUtilUploader");
                this.m_CrashTrace = trace;
            }

            public String buildPostData() {
                return this.m_CrashTrace.info;
            }

            private synchronized void onUploaderFinished(boolean bSucceeded) {
                this.m_bFinished = true;
                this.m_bSucceeded = bSucceeded;
                notifyAll();
            }

            public void RequestSucceededOnResponseWorkerThread() {
                onUploaderFinished(true);
            }

            public void RequestFailedOnResponseWorkerThread() {
                onUploaderFinished(false);
            }
        }

        AnonymousClass_1(ArrayList arrayList) {
            this.val$arrTraces = arrayList;
        }

        public void run() {
            int nSleep = 30;
            int jj = this.val$arrTraces.size();
            while (true) {
                int jj2 = jj - 1;
                if (jj > 0) {
                    UploaderRequest req = new UploaderRequest((CrashTrace) this.val$arrTraces.get(jj2));
                    req.SetUriAndDocumentType(Config.URL_MOBILE_CRASH_UPLOAD, RequestDocumentType.JSON);
                    req.SetPostData(req.buildPostData());
                    req.SetRequestAction(RequestActionType.DoHttpRequestNoCache);
                    synchronized (req) {
                        SteamCommunityApplication.GetInstance().SubmitSteamDBRequest(req);
                        while (!req.m_bFinished) {
                            try {
                                req.wait();
                            } catch (InterruptedException e) {
                            }
                        }
                        if (req.m_bSucceeded) {
                            nSleep = 30;
                            SteamDebugUtil.this.m_db.updateMarkCrashTraceUploaded(req.m_CrashTrace);
                        } else {
                            nSleep *= 2;
                            if (nSleep > 10800) {
                                nSleep = 10800;
                            }
                            jj2++;
                        }
                    }
                    try {
                        Thread.sleep((long) (nSleep * 1000));
                    } catch (InterruptedException e2) {
                    }
                    jj = jj2;
                } else {
                    return;
                }
            }
        }
    }

    class AnonymousClass_2 extends Thread {
        final /* synthetic */ boolean val$bDbReadyNow;

        AnonymousClass_2(boolean z) {
            this.val$bDbReadyNow = z;
        }

        public void run() {
            if (this.val$bDbReadyNow) {
                SteamDebugUtil.this.ProcessCrashesThreaded();
            }
            do {
            } while (SteamDebugUtil.this.ProcessCommandsThreadedBlocking());
        }
    }

    public static class DebugUtilRecord implements IDebugUtilRecord {
        long id;
        String key;
        IDebugUtilRecord parent;
        long sessionstate;
        long threadid;
        Calendar timestamp;
        String value;

        public DebugUtilRecord() {
            this.timestamp = Calendar.getInstance();
            this.threadid = Thread.currentThread().getId();
        }

        public long getId() {
            return this.id;
        }
    }

    public static class Dummy_ISteamDebugUtil extends Helper {

        private static class DebugUtilRecord implements IDebugUtilRecord {
            private DebugUtilRecord() {
            }

            public long getId() {
                return -1;
            }
        }

        public String getSessionId() {
            return "";
        }

        public IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord parent, String key, String value) {
            return new DebugUtilRecord();
        }
    }

    public SteamDebugUtil(SteamDBService svc) {
        this.m_svc = null;
        this.m_db = null;
        this.m_arrCommands = new ArrayList();
        this.m_cmdCurrent = null;
        this.m_cmdCurrentLock = new ReentrantLock();
        this.m_cmdCurrentCond = this.m_cmdCurrentLock.newCondition();
        this.m_thread = null;
        if (svc != null) {
            SetServiceContext(svc);
            return;
        }
        DebugUtilRecord r = new DebugUtilRecord();
        r.key = "SteamDebugUtil";
        r.value = "Application Started";
        r.sessionstate = Calendar.getInstance().getTimeInMillis();
        AddCommandToQueue(r);
    }

    public void SetServiceContext(SteamDBService svc) {
        this.m_db = new SteamDebugUtilDatabase(svc.getApplicationContext());
        synchronized (this) {
            this.m_svc = svc;
        }
        DebugUtilRecord r = new DebugUtilRecord();
        r.key = "SteamDebugUtil";
        r.value = "DB Started";
        AddCommandToQueue(r, true);
    }

    private boolean ProcessCommandsThreadedBlocking() {
        boolean z = false;
        this.m_cmdCurrentLock.lock();
        try {
            if (this.m_arrCommands.isEmpty()) {
                this.m_thread = null;
                this.m_cmdCurrentLock.unlock();
                return false;
            }
            this.m_cmdCurrent = (DebugUtilRecord) this.m_arrCommands.get(0);
            this.m_arrCommands.remove(0);
            z = true;
            this.m_cmdCurrentLock.unlock();
            if (z) {
                ProcessNextCommand();
            }
            return true;
        } catch (Exception e) {
            this.m_cmdCurrentLock.unlock();
        }
    }

    private void ProcessNextCommand() {
        this.m_db.insertMessage(this.m_cmdCurrent);
    }

    private void ProcessCrashesThreaded() {
        String str = "SteamDebugUtilUploader";
        ArrayList<CrashTrace> arrTraces = this.m_db.selectCrashTraces();
        if (arrTraces != null && !arrTraces.isEmpty()) {
            Thread uploader = new AnonymousClass_1(arrTraces);
            uploader.setName("SteamDebugUtilUploader");
            uploader.start();
        }
    }

    public String getSessionId() {
        return this.m_db.getDbName();
    }

    public IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord parent, String key, String value) {
        DebugUtilRecord r = new DebugUtilRecord();
        r.parent = parent;
        r.key = key;
        r.value = value;
        AddCommandToQueue(r);
        return r;
    }

    private void AddCommandToQueue(DebugUtilRecord r) {
        AddCommandToQueue(r, false);
    }

    private void AddCommandToQueue(DebugUtilRecord r, boolean bDbReadyNow) {
        this.m_cmdCurrentLock.lock();
        try {
            this.m_arrCommands.add(r);
            if (this.m_thread == null) {
                synchronized (this) {
                    if (this.m_svc == null) {
                        this.m_cmdCurrentLock.unlock();
                        return;
                    }
                    this.m_thread = new AnonymousClass_2(bDbReadyNow);
                    this.m_thread.setName("SteamDebugUtilThread");
                    this.m_thread.start();
                }
            } else {
                this.m_cmdCurrentCond.signalAll();
            }
            this.m_cmdCurrentLock.unlock();
        } catch (Throwable th) {
        }
    }
}
