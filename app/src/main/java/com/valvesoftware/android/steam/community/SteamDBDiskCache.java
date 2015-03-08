package com.valvesoftware.android.steam.community;

import com.valvesoftware.android.steam.community.SteamDBDiskCache.IDataDiskAccess;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TreeMap;

public class SteamDBDiskCache {
    private static final String s_tmpFileName = "tmpfile";
    protected File m_dir;
    private int m_idxTmpFileName;

    public static interface IDataDiskAccess {
        void onRead(FileInputStream fileInputStream, long j) throws IOException;

        void onWrite(OutputStream outputStream) throws IOException;
    }

    private static class DataDiskAccessByteArray implements IDataDiskAccess {
        byte[] m_data;

        private DataDiskAccessByteArray() {
        }

        public void onWrite(OutputStream fw) throws IOException {
            if (this.m_data != null) {
                fw.write(this.m_data);
            }
        }

        public void onRead(FileInputStream fis, long len) throws IOException {
            byte[] readbytes = new byte[((int) len)];
            int numBytesRead = fis.read(readbytes);
            if (((long) numBytesRead) != len) {
                throw new IOException("File read produced " + numBytesRead + " when " + len + " was expected");
            }
            this.m_data = readbytes;
        }
    }

    public static class IndefiniteCache extends SteamDBDiskCache {
        public IndefiniteCache(File rootdir) {
            super(rootdir);
        }

        public synchronized void Delete(String uri) {
            new File(this.m_dir, getFileNameFromUri(uri)).delete();
        }
    }

    public static class WebCache extends SteamDBDiskCache {
        private TreeMap<Long, File> m_ageMap;
        private final int m_numFilesLimit;

        public WebCache(File rootdir) {
            super(rootdir);
            this.m_ageMap = null;
            this.m_numFilesLimit = 1024;
        }

        protected String getFileNameFromUri(String uri) {
            int iHash = uri.hashCode();
            int iLen = uri.length();
            int[] last = new int[]{uri.lastIndexOf(61) + 1, uri.lastIndexOf(47) + 1};
            int iMaxIdx = Math.max(last[0], last[1]);
            if (iMaxIdx <= 0) {
                iMaxIdx = iLen;
            }
            StringBuilder sb = new StringBuilder((iLen + 13) - iMaxIdx);
            sb.append(iHash);
            sb.append("_");
            sb.append(uri.substring(iMaxIdx));
            return sb.toString();
        }

        protected void onAfterWriteToFile(File file) {
            if (this.m_ageMap != null) {
                this.m_ageMap.put(Long.valueOf(System.currentTimeMillis()), file);
            }
            if (this.m_ageMap == null) {
                this.m_ageMap = new TreeMap();
                for (File fsent : this.m_dir.listFiles()) {
                    this.m_ageMap.put(Long.valueOf(fsent.lastModified()), fsent);
                }
            }
            while (this.m_ageMap.size() > 1024) {
                if (((File) this.m_ageMap.remove(this.m_ageMap.firstKey())).delete()) {
                }
            }
        }
    }

    protected SteamDBDiskCache(File rootdir) {
        this.m_idxTmpFileName = 0;
        this.m_dir = rootdir;
    }

    protected String getFileNameFromUri(String uri) {
        return uri;
    }

    public void Write(String uri, byte[] data) {
        IDataDiskAccess w = new DataDiskAccessByteArray();
        w.m_data = data;
        Write(uri, w);
    }

    public void Write(String uri, IDataDiskAccess data) {
        try {
            WriteToFile(new File(this.m_dir, getFileNameFromUri(uri)), data);
        } catch (Exception e) {
        }
    }

    public byte[] Read(String uri) {
        DataDiskAccessByteArray r = new DataDiskAccessByteArray();
        return Read(uri, r) ? r.m_data : null;
    }

    public boolean Read(String uri, IDataDiskAccess data) {
        try {
            return ReadFromFile(new File(this.m_dir, getFileNameFromUri(uri)), data);
        } catch (Exception e) {
            return false;
        }
    }

    private synchronized boolean ReadFromFile(File file, IDataDiskAccess data) throws IOException {
        boolean z;
        try {
            if (file.exists()) {
                FileInputStream fis = new FileInputStream(file);
                long len = file.length();
                if (len > 2147483647L) {
                    throw new IOException("File " + file.getAbsolutePath() + " is too large: " + len);
                }
                if (data != null) {
                    data.onRead(fis, len);
                }
                fis.close();
                z = true;
            } else {
                z = false;
            }
        } catch (Throwable th) {
        }
        return z;
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private synchronized void WriteToFile(java.io.File r7_file, com.valvesoftware.android.steam.community.SteamDBDiskCache.IDataDiskAccess r8_data) throws java.io.IOException {
        throw new UnsupportedOperationException("Method not decompiled: com.valvesoftware.android.steam.community.SteamDBDiskCache.WriteToFile(java.io.File, com.valvesoftware.android.steam.community.SteamDBDiskCache$IDataDiskAccess):void");
        /*
        this = this;
        monitor-enter(r6);
        r3 = r6.m_idxTmpFileName;	 Catch:{ all -> 0x004c }
        r3 = r3 + 1;
        r6.m_idxTmpFileName = r3;	 Catch:{ all -> 0x004c }
        r2 = new java.io.File;	 Catch:{ all -> 0x004c }
        r3 = r6.m_dir;	 Catch:{ all -> 0x004c }
        r4 = new java.lang.StringBuilder;	 Catch:{ all -> 0x004c }
        r4.<init>();	 Catch:{ all -> 0x004c }
        r5 = "tmpfile";
        r4 = r4.append(r5);	 Catch:{ all -> 0x004c }
        r5 = r6.m_idxTmpFileName;	 Catch:{ all -> 0x004c }
        r4 = r4.append(r5);	 Catch:{ all -> 0x004c }
        r4 = r4.toString();	 Catch:{ all -> 0x004c }
        r2.<init>(r3, r4);	 Catch:{ all -> 0x004c }
        r0 = 0;
        r1 = new java.io.FileOutputStream;	 Catch:{ all -> 0x0045 }
        r1.<init>(r2);	 Catch:{ all -> 0x0045 }
        if (r8 == 0) goto L_0x002e;
    L_0x002b:
        r8.onWrite(r1);	 Catch:{ all -> 0x004f }
    L_0x002e:
        r1.flush();	 Catch:{ all -> 0x004f }
        r1.close();	 Catch:{ all -> 0x004f }
        r0 = 0;
        if (r0 == 0) goto L_0x003a;
    L_0x0037:
        r0.close();	 Catch:{ all -> 0x004c }
    L_0x003a:
        r3 = r2.renameTo(r7);	 Catch:{ all -> 0x004c }
        if (r3 != 0) goto L_0x0040;
    L_0x0040:
        r6.onAfterWriteToFile(r7);	 Catch:{ all -> 0x004c }
        monitor-exit(r6);
        return;
    L_0x0045:
        r3 = move-exception;
    L_0x0046:
        if (r0 == 0) goto L_0x004b;
    L_0x0048:
        r0.close();	 Catch:{ all -> 0x004c }
    L_0x004b:
        throw r3;	 Catch:{ all -> 0x004c }
    L_0x004c:
        r3 = move-exception;
        monitor-exit(r6);
        throw r3;
    L_0x004f:
        r3 = move-exception;
        r0 = r1;
        goto L_0x0046;
        */
    }

    protected void onAfterWriteToFile(File file) {
    }
}
