package com.valvesoftware.android.steam.community;

import com.valvesoftware.android.steam.community.SteamDBService.UriData;
import com.valvesoftware.android.steam.community.WorkerThreadPool.Task;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class WorkerThreadPool {
    static String TAG;
    private final TreeMap<Integer, JobQueue> m_jobQueues;

    public static interface Task {
        String GetRequestQueue();

        void Run() throws Exception;
    }

    static /* synthetic */ class AnonymousClass_1 {
        static final /* synthetic */ int[] $SwitchMap$com$valvesoftware$android$steam$community$WorkerThreadPool$TaskResult;

        static {
            $SwitchMap$com$valvesoftware$android$steam$community$WorkerThreadPool$TaskResult = new int[TaskResult.values().length];
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$WorkerThreadPool$TaskResult[TaskResult.AddBackToPool.ordinal()] = 1;
            } catch (NoSuchFieldError e) {
            }
            try {
                $SwitchMap$com$valvesoftware$android$steam$community$WorkerThreadPool$TaskResult[TaskResult.ShutdownQueue.ordinal()] = 2;
            } catch (NoSuchFieldError e2) {
            }
        }
    }

    public static class AddBackToPoolException extends RuntimeException {
        private static final long serialVersionUID = 6309647525704374L;
    }

    private class JobQueue implements Runnable {
        private String m_sName;
        private final List<Task> m_tasks;
        private Thread m_thread;

        public JobQueue(String name) {
            this.m_tasks = new ArrayList();
            this.m_sName = null;
            this.m_thread = null;
            this.m_sName = name;
        }

        public String GetName() {
            return this.m_sName;
        }

        public void EnqueueTask(Task task) {
            synchronized (this.m_tasks) {
                this.m_tasks.add(task);
                if (this.m_thread != null) {
                    this.m_tasks.notify();
                } else {
                    this.m_thread = new Thread(this, GetName());
                    this.m_thread.start();
                }
            }
        }

        private void WaitForNextTaskOrTimeout() {
            long lStartTime = System.currentTimeMillis();
            while (true) {
                try {
                    long lTimeToWait = (2000 + lStartTime) - System.currentTimeMillis();
                    if (lTimeToWait > 0) {
                        this.m_tasks.wait(lTimeToWait);
                    } else {
                        return;
                    }
                } catch (InterruptedException e) {
                }
            }
        }

        private boolean RunNextTask() {
            synchronized (this.m_tasks) {
                if (this.m_tasks.isEmpty()) {
                    WaitForNextTaskOrTimeout();
                    if (this.m_tasks.isEmpty()) {
                        this.m_thread = null;
                        return false;
                    }
                }
                int numTasks = this.m_tasks.size();
                Task task = (Task) this.m_tasks.remove(0);
                switch (AnonymousClass_1.$SwitchMap$com$valvesoftware$android$steam$community$WorkerThreadPool$TaskResult[WorkerThreadPool.RunTask(task).ordinal()]) {
                    case UriData.RESULT_INVALID_CONTENT:
                        WorkerThreadPool.this.AddTask(task);
                        return true;
                    case UriData.RESULT_HTTP_EXCEPTION:
                        synchronized (WorkerThreadPool.this.m_jobQueues) {
                            if (this != ((JobQueue) WorkerThreadPool.this.m_jobQueues.remove(Integer.valueOf(System.identityHashCode(GetName()))))) {
                            }
                        }
                        synchronized (this.m_tasks) {
                            if (this.m_tasks.isEmpty()) {
                            }
                        }
                        return false;
                    default:
                        return true;
                }
            }
        }

        public void run() {
            do {
            } while (RunNextTask());
        }
    }

    public static class ShutdownJobQueueException extends RuntimeException {
        private static final long serialVersionUID = -3634293381030210582L;
    }

    private enum TaskResult {
        Continue,
        AddBackToPool,
        ShutdownQueue
    }

    public WorkerThreadPool() {
        this.m_jobQueues = new TreeMap();
    }

    static {
        TAG = "WorkerThreadPool";
    }

    public static TaskResult RunTask(Task task) {
        try {
            task.Run();
        } catch (ShutdownJobQueueException e) {
            return TaskResult.ShutdownQueue;
        } catch (AddBackToPoolException e2) {
            return TaskResult.AddBackToPool;
        } catch (Exception e3) {
        }
        return TaskResult.Continue;
    }

    public void AddTask(Task task) {
        JobQueue jq;
        String name = task.GetRequestQueue();
        Integer key = Integer.valueOf(System.identityHashCode(name));
        TreeMap treeMap = this.m_jobQueues;
        synchronized (treeMap) {
            try {
                jq = (JobQueue) this.m_jobQueues.get(key);
                if (jq == null) {
                    JobQueue jq2 = new JobQueue(name);
                    try {
                        this.m_jobQueues.put(key, jq2);
                        jq = jq2;
                    } catch (Throwable th) {
                        Throwable th2 = th;
                        jq = jq2;
                        throw th2;
                    }
                }
            } catch (Throwable th3) {
                th2 = th3;
                throw th2;
            }
        }
        jq.EnqueueTask(task);
    }
}
