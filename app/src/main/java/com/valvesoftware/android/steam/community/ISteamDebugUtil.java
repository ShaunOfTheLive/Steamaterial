package com.valvesoftware.android.steam.community;

import android.content.Intent;
import com.valvesoftware.android.steam.community.ISteamDebugUtil.IDebugUtilRecord;

public interface ISteamDebugUtil {

    public static abstract class Helper implements ISteamDebugUtil {
        public IDebugUtilRecord newDebugUtilRecord(String key, String value) {
            return newDebugUtilRecord(null, key, value);
        }

        public IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord parent, String key, Intent intent) {
            if (intent == null) {
                return newDebugUtilRecord(parent, key, "intent==null");
            }
            IDebugUtilRecord r = newDebugUtilRecord(parent, key, intent.toString());
            if (intent.getExtras() == null) {
                return r;
            }
            for (String exkey : intent.getExtras().keySet()) {
                newDebugUtilRecord(r, exkey, intent.getExtras().get(exkey).toString());
            }
            return r;
        }

        public IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord parent, String key, Throwable ex) {
            if (ex == null) {
                return newDebugUtilRecord(parent, key, "ex==null");
            }
            IDebugUtilRecord r = newDebugUtilRecord(parent, key, ex.toString());
            newDebugUtilRecord(r, "class", ex.getClass().getName());
            newDebugUtilRecord(r, "msg", ex.getMessage());
            newDebugUtilRecord(r, "locmsg", ex.getLocalizedMessage());
            newDebugUtilRecord(r, "stacktrace", ex.getStackTrace());
            return r;
        }

        public IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord parent, String key, StackTraceElement[] trace) {
            if (trace == null) {
                return newDebugUtilRecord(parent, key, "trace==null");
            }
            if (trace.length <= 0) {
                return newDebugUtilRecord(parent, key, "Empty Stack Trace");
            }
            parent = newDebugUtilRecord(parent, key, trace[0].toString() + " // " + trace.length + " stack trace element(s)");
            for (StackTraceElement aTrace : trace) {
                String str;
                String str2 = aTrace.getClassName() + "." + aTrace.getMethodName();
                if (aTrace.isNativeMethod()) {
                    str = "NativeMethod";
                } else {
                    str = (aTrace.getFileName() != null ? aTrace.getFileName() : "unknown") + (aTrace.getLineNumber() >= 0 ? ":" + aTrace.getLineNumber() : "");
                }
                parent = newDebugUtilRecord(parent, str2, str);
            }
            return parent;
        }
    }

    public static interface IDebugUtilRecord {
        long getId();
    }

    String getSessionId();

    IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord iDebugUtilRecord, String str, Intent intent);

    IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord iDebugUtilRecord, String str, String str2);

    IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord iDebugUtilRecord, String str, Throwable th);

    IDebugUtilRecord newDebugUtilRecord(IDebugUtilRecord iDebugUtilRecord, String str, StackTraceElement[] stackTraceElementArr);

    IDebugUtilRecord newDebugUtilRecord(String str, String str2);
}
