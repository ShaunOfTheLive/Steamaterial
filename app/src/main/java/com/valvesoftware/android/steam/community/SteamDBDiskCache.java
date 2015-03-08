package com.valvesoftware.android.steam.community;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.TreeMap;

public class SteamDBDiskCache {
    class DataDiskAccessByteArray implements IDataDiskAccess {
        byte[] m_data;

        DataDiskAccessByteArray(com.valvesoftware.android.steam.community.SteamDBDiskCache$1 x0) {
            super();
        }

        private DataDiskAccessByteArray() {
            super();
        }

        public void onRead(FileInputStream fis, long len) throws IOException {
            byte[] v1 = new byte[((int)len)];
            int v0 = fis.read(v1);
            if((((long)v0)) != len) {
                throw new IOException("File read produced " + v0 + " when " + len + " was expected");
            }

            this.m_data = v1;
        }

        public void onWrite(OutputStream fw) throws IOException {
            if(this.m_data != null) {
                fw.write(this.m_data);
            }
        }
    }

    public interface IDataDiskAccess {
        void onRead(FileInputStream arg1, long arg2) throws IOException;

        void onWrite(OutputStream arg1) throws IOException;
    }

    public class IndefiniteCache extends SteamDBDiskCache {
        public IndefiniteCache(File rootdir) {
            this(rootdir);
        }

        public void Delete(String uri) {
            __monitor_enter(this);
            try {
                new File(this.m_dir, this.getFileNameFromUri(uri)).delete();
            }
            catch(Throwable v0) {
                __monitor_exit(this);
                throw v0;
            }

            __monitor_exit(this);
        }
    }

    public class WebCache extends SteamDBDiskCache {
        private TreeMap m_ageMap;
        private final int m_numFilesLimit;

        public WebCache(File rootdir) {
            this(rootdir);
            this.m_ageMap = null;
            this.m_numFilesLimit = 1024;
        }

        protected String getFileNameFromUri(String uri) {
            int v0 = uri.hashCode();
            int v1 = uri.length();
            int[] v3 = new int[]{uri.lastIndexOf(61) + 1, uri.lastIndexOf(47) + 1};
            int v2 = Math.max(v3[0], v3[1]);
            if(v2 <= 0) {
                v2 = v1;
            }

            StringBuilder v5 = new StringBuilder(v1 + 13 - v2);
            v5.append(v0);
            v5.append("_");
            v5.append(uri.substring(v2));
            return v5.toString();
        }

        protected void onAfterWriteToFile(File file) {
            if(this.m_ageMap != null) {
                this.m_ageMap.put(Long.valueOf(System.currentTimeMillis()), file);
            }

            if(this.m_ageMap == null) {
                this.m_ageMap = new TreeMap();
                File[] v2 = this.m_dir.listFiles();
                int v6 = v2.length;
                int v5;
                for(v5 = 0; v5 < v6; ++v5) {
                    this.m_ageMap.put(Long.valueOf(v2[v5].lastModified()), v2[v5]);
                }
            }

            while(this.m_ageMap.size() > 1024) {
            }
        }
    }

    protected File m_dir;
    private int m_idxTmpFileName;
    private static final String s_tmpFileName = "tmpfile";

    protected SteamDBDiskCache(File rootdir) {
        super();
        this.m_idxTmpFileName = 0;
        this.m_dir = rootdir;
    }

    public boolean Read(String uri, IDataDiskAccess data) {
        boolean v1;
        try {
            v1 = this.ReadFromFile(new File(this.m_dir, this.getFileNameFromUri(uri)), data);
        }
        catch(Exception v0) {
            v1 = false;
        }

        return v1;
    }

    public byte[] Read(String uri) {
        byte[] v1_1;
        com.valvesoftware.android.steam.community.SteamDBDiskCache$1 v1 = null;
        DataDiskAccessByteArray v0 = new DataDiskAccessByteArray(v1);
        if(this.Read(uri, ((IDataDiskAccess)v0))) {
            v1_1 = v0.m_data;
        }

        return v1_1;
    }

    private boolean ReadFromFile(File file, IDataDiskAccess data) throws IOException {
        long v1;
        FileInputStream v0;
        __monitor_enter(this);
        try {
            if(!file.exists()) {
            }
            else {
                goto label_6;
            }
        }
        catch(Throwable v3) {
            goto label_25;
        }

        boolean v3_1 = false;
        goto label_4;
        try {
        label_6:
            v0 = new FileInputStream(file);
        }
        catch(Throwable v3) {
            goto label_25;
        }

        try {
            v1 = file.length();
            if(v1 > 2147483647) {
                throw new IOException("File " + file.getAbsolutePath() + " is too large: " + v1);
            }
            else {
                goto label_27;
            }

            goto label_4;
        }
        catch(Throwable v3) {
            goto label_22;
        }

    label_27:
        if(data != null) {
            try {
                data.onRead(v0, v1);
                goto label_29;
            }
            catch(Throwable v3) {
                try {
                label_22:
                    v0.close();
                    throw v3;
                label_29:
                    v0.close();
                    v3_1 = true;
                    goto label_4;
                }
                catch(Throwable v3) {
                label_25:
                    __monitor_exit(this);
                    throw v3;
                }
            }
        }

        goto label_29;
    label_4:
        __monitor_exit(this);
        return v3_1;
    }

    public void Write(String uri, IDataDiskAccess data) {
        try {
            this.WriteToFile(new File(this.m_dir, this.getFileNameFromUri(uri)), data);
        }
        catch(Exception v0) {
        }
    }

    public void Write(String uri, byte[] data) {
        DataDiskAccessByteArray v0 = new DataDiskAccessByteArray(null);
        v0.m_data = data;
        this.Write(uri, ((IDataDiskAccess)v0));
    }

    private void WriteToFile(File file, IDataDiskAccess data) throws IOException {
        OutputStream v0_1;
        FileOutputStream v1;
        FileOutputStream v0;
        File v2;
        __monitor_enter(this);
        try {
            ++this.m_idxTmpFileName;
            v2 = new File(this.m_dir, "tmpfile" + this.m_idxTmpFileName);
            v0 = null;
        }
        catch(Throwable v3) {
            goto label_30;
        }

        try {
            v1 = new FileOutputStream(v2);
            if(data == null) {
                goto label_16;
            }
        }
        catch(Throwable v3) {
            goto label_26;
        }

        try {
            data.onWrite(((OutputStream)v1));
        label_16:
            ((OutputStream)v1).flush();
            ((OutputStream)v1).close();
            v0_1 = null;
            if(0 == 0) {
                goto label_21;
            }

            goto label_20;
        }
        catch(Throwable v3) {
            v0 = v1;
        }

    label_26:
        if(v0 == null) {
            goto label_28;
        }

        try {
            ((OutputStream)v0).close();
        label_28:
            throw v3;
        label_20:
            v0_1.close();
        label_21:
            v2.renameTo(file);
            this.onAfterWriteToFile(file);
        }
        catch(Throwable v3) {
        label_30:
            __monitor_exit(this);
            throw v3;
        }

        __monitor_exit(this);
    }

    protected String getFileNameFromUri(String uri) {
        return uri;
    }

    protected void onAfterWriteToFile(File file) {
    }
}

