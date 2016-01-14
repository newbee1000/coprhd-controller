/*
 * Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.systemservices.impl.restore;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.curator.framework.recipes.locks.InterProcessLock;
import org.apache.commons.io.FileUtils;

import com.emc.storageos.coordinator.client.service.NodeListener;
import com.emc.storageos.coordinator.common.Service;
import com.emc.vipr.model.sys.backup.BackupRestoreStatus;
import com.emc.vipr.model.sys.backup.BackupRestoreStatus.Status;
import com.emc.storageos.svcs.errorhandling.resources.APIException;
import com.emc.storageos.management.backup.BackupConstants;
import com.emc.storageos.management.backup.util.FtpClient;
import com.emc.storageos.management.backup.BackupOps;
import com.emc.storageos.systemservices.impl.jobs.backupscheduler.SchedulerConfig;
import com.emc.storageos.systemservices.impl.client.SysClientFactory;

public final class DownloadExecutor implements  Runnable {
    private static final Logger log = LoggerFactory.getLogger(DownloadExecutor.class);

    private FtpClient client;
    private String remoteBackupFileName;
    private BackupOps backupOps;
    private boolean notifyOthers;
    private String localHostName;
    private BackupRestoreStatus restoreStatus;
    //private DownloadListener downloadListener = new DownloadListener();
    private DownloadListener downloadListener;
    private CancelListener  cancelListener;
    private boolean isGeo = false; // true if the backupset is from GEO env
    private volatile  boolean isCanceled2=false;

    public static DownloadExecutor create(SchedulerConfig cfg, String backupZipFileName, BackupOps backupOps, boolean notifyOthers) {

        DownloadExecutor downloader = new DownloadExecutor(cfg, backupZipFileName, backupOps, notifyOthers);

        return downloader;
    }

    private DownloadExecutor(SchedulerConfig cfg, String backupZipFileName, BackupOps backupOps, boolean notifyOthers) {
        if (cfg.uploadUrl == null) {
            try {
                cfg.reload();
            }catch (Exception e) {
                log.error("Failed to reload cfg e=", e);
                throw new RuntimeException(e);
            }
        }

        client = new FtpClient(cfg.uploadUrl, cfg.uploadUserName, cfg.getUploadPassword());
        remoteBackupFileName = backupZipFileName;
        this.backupOps = backupOps;
        this.notifyOthers = notifyOthers;
    }

    public void registerListener() {
        try {
            //log.info("lbyd add download and cancel listener");
            log.info("lbyd add download listener");
            downloadListener = new DownloadListener(Thread.currentThread());
            //downloadListener = new DownloadListener();
            backupOps.addRestoreListener(downloadListener);
            /*
            cancelListener = new CancelListener(Thread.currentThread());
            backupOps.addRestoreListener(cancelListener);
            */
        }catch (Exception e) {
            log.error("Fail to add download listener e=", e);
            throw APIException.internalServerErrors.addListenerFailed();
        }
    }

    class DownloadListener implements NodeListener {
        private Thread downloadingThread;

        public DownloadListener(Thread t) {
            downloadingThread = t;
        }

        /*
        public DownloadListener() {
        }
        */

        @Override
        public String getPath() {
            String prefix = backupOps.getBackupConfigPrefix();
            return prefix + "/" + remoteBackupFileName;
        }

        /**
         * called when user modify restore status, procedure or node status
         */
        @Override
        public void nodeChanged() {
            log.info("The restore status changed");
            onRestoreStatusChange();
        }

        private void onRestoreStatusChange() {
            BackupRestoreStatus status = backupOps.queryBackupRestoreStatus(remoteBackupFileName, false);
            log.info("lbye Restore status={}", status);
            Status s = status.getStatus();

            if (s == Status.DOWNLOAD_CANCELLED) {
                log.info("lbye cancel downloading thread");
                isCanceled2 = true;
                downloadingThread.interrupt();
            }

            File downloadedDir = backupOps.getDownloadDirectory(remoteBackupFileName);
            if (s.removeDownloadFiles()) {
                //Remove downloaded backup data
                log.info("lbye 2 To remove downloaded {} exist={}", downloadedDir, downloadedDir.exists());
                try {
                    FileUtils.deleteDirectory(downloadedDir);
                }catch (IOException e) {
                    log.error("Failed to remove {} e=", downloadedDir.getAbsolutePath(), e );
                }
            }

            log.info("Removed downloaded {} exist={}", downloadedDir, downloadedDir.exists());
            if (s.removeListener()) {
                try {
                    //log.info("Remove download and restore listener");
                    log.info("Remove download listener");
                    backupOps.removeRestoreListener(downloadListener);
                    // backupOps.removeRestoreListener(cancelListener);
                }catch (Exception e) {
                    log.warn("Failed to remove download and restore listener");
                }
            }
        }

        /**
         * called when connection state changed.
         */
        @Override
        public void connectionStateChanged(State state) {
            log.info("Restore status connection state changed to {}", state);
            if (state.equals(State.CONNECTED)) {
                log.info("Curator (re)connected.");
                onRestoreStatusChange();
            }
        }
    }

    class CancelListener implements NodeListener {
        private Thread downloadingThread;

        public CancelListener(Thread t) {
            downloadingThread = t;
        }

        @Override
        public String getPath() {
            String path = backupOps.getCurrentRestoreInfoPath();
            log.info("lbyc cancel path={}", path);

            return path;
        }

        /**
         * called when user modify restore status, procedure or node status
         */
        @Override
        public void nodeChanged() {
            log.info("The current restore info changed");
            onCurrentBackupInfoChange();
        }

        private void onCurrentBackupInfoChange() {
            BackupRestoreStatus status = backupOps.queryBackupRestoreStatus(remoteBackupFileName, false);
            Map<String, String> map  = backupOps.getCurrentBackupInfo();
            log.info("lbyc current restore info: {}", map);
            boolean isCanceled = Boolean.parseBoolean(map.get(BackupConstants.CURRENT_DOWNLOADING_IS_CANCELED_KEY));

            if (isCanceled) {
                isCanceled2 = true;
                log.info("To stop downloading thread {} isCanceled=true", downloadingThread.getName());
                downloadingThread.interrupt();
            }
        }

        /**
         * called when connection state changed.
         */
        @Override
        public void connectionStateChanged(State state) {
            log.info("Restore status connection state changed to {}", state);
            if (state.equals(State.CONNECTED)) {
                log.info("Curator (re)connected.");
                onCurrentBackupInfoChange();
            }
        }
    }

    public synchronized void setDownloadStatus(String backupName, BackupRestoreStatus.Status status, long backupSize, long increasedSize) {
        log.info("Set download status backupName={} status={} backupSize={} increasedSize={}",
                new Object[] {backupName, status, backupSize, increasedSize});
        restoreStatus = backupOps.queryBackupRestoreStatus(backupName, false);
        if (status != null && status == Status.DOWNLOAD_CANCELLED ) {
            if (!restoreStatus.getStatus().canBeCanceled()) {
                return;
            }
        }

        restoreStatus.setBackupName(backupName);

        if (status != null) {
            restoreStatus.setStatus(status);

            if (status == Status.DOWNLOAD_SUCCESS) {
                restoreStatus.increaseNodeCompleted();
            }
        }

        if (backupSize > 0) {
            restoreStatus.setBackupSize(backupSize);
        }

        if (increasedSize > 0) {
            long newSize = restoreStatus.getDownoadSize() + increasedSize;
            restoreStatus.setDownoadSize(newSize);
        }

        backupOps.persistBackupRestoreStatus(restoreStatus, false);
    }

    public void cancelDownload() {
        Map<String, String> map = backupOps.getCurrentBackupInfo();
        log.info("lbye To cancel current download {}", map);
        String backupName = map.get(BackupConstants.CURRENT_DOWNLOADING_BACKUP_NAME_KEY);
        boolean isLocal = Boolean.parseBoolean(map.get(BackupConstants.CURRENT_DOWNLOADING_BACKUP_ISLOCAL_KEY));
        log.info("lbye backupname={}, isLocal={}", backupName, isLocal);
        if (isLocal == false) {
            setDownloadStatus(backupName, BackupRestoreStatus.Status.DOWNLOAD_CANCELLED, 0, 0);
            log.info("lbye Persist cancel flag into zk");
        }
    }

    public void updateDownloadSize(long size) {
        log.info("Increase download size ={}", size);
        setDownloadStatus(remoteBackupFileName, null, 0, size);
    }

    @Override
    public void run() {
        InterProcessLock lock = null;
        boolean cleanCurrentBackupInfo = false;
        try {
            lock = backupOps.getLock(BackupConstants.RESTORE_LOCK,
                    -1, TimeUnit.MILLISECONDS); // -1= no timeout

            registerListener();
            localHostName = InetAddress.getLocalHost().getHostName();
            download();
            notifyOtherNodes();
        }catch (InterruptedException e) {
            log.info("lbye the downloading thread has been interrupted");
            restoreStatus = backupOps.queryBackupRestoreStatus(remoteBackupFileName, false);
            Status s = restoreStatus.getStatus();
            if (s.canBeCanceled()) {
                log.info("lbye the downloading is canceled");
                /*
                restoreStatus.setStatus(Status.DOWNLOAD_CANCELLED);
                backupOps.persistBackupRestoreStatus(restoreStatus, false);
                */
                setDownloadStatus(remoteBackupFileName, Status.DOWNLOAD_CANCELLED, 0, 0);
                cleanCurrentBackupInfo = true;
            }
        }catch (Exception e) {
            restoreStatus = backupOps.queryBackupRestoreStatus(remoteBackupFileName, false);
            log.info("lbye isCanceled={}", isCanceled2);
            Status s = Status.DOWNLOAD_FAILED;
            if (isCanceled2) {
                s = Status.DOWNLOAD_CANCELLED;

                File downloadedDir = backupOps.getDownloadDirectory(remoteBackupFileName);
                //Remove downloaded backup data
                log.info("lbye 1 To remove downloaded {} exist={}", downloadedDir, downloadedDir.exists());
                try {
                    FileUtils.deleteDirectory(downloadedDir);
                }catch (IOException ex) {
                    log.error("Failed to remove {} e=", downloadedDir.getAbsolutePath(), ex);
                }
            }

            // restoreStatus.setStatus(s);
            setDownloadStatus(remoteBackupFileName, s, 0, 0);
            // backupOps.persistBackupRestoreStatus(restoreStatus, false);
            cleanCurrentBackupInfo = true;
            log.error("lbye Failed to download e=", e);
        }finally {
            try {
                backupOps.releaseLock(lock);
            }catch (Exception e) {
                log.error("Failed to remove listener e=",e);
            }
        }

        if (cleanCurrentBackupInfo) {
            backupOps.clearCurrentBackupInfo();
        }
    }

    private void download() throws IOException, InterruptedException {
        log.info("download start");

        log.info("lbya persist current backup info {}", remoteBackupFileName);
        backupOps.persistCurrentBackupInfo(remoteBackupFileName, false, false);

        ZipInputStream zin = getDownloadStream();

        File backupFolder= backupOps.getDownloadDirectory(remoteBackupFileName);

        if (hasDownloaded(backupFolder)) {
            log.info("The backup {} for this node has already been downloaded", remoteBackupFileName);
            return; //already downloaded, no need to download again
        }

        if (notifyOthers) {
            // This is the first node to download backup files, get the whole zip file size first
            long size = client.getFileSize(remoteBackupFileName);

            setDownloadStatus(remoteBackupFileName, BackupRestoreStatus.Status.DOWNLOADING, size, 0);
        }

        ZipEntry zentry = zin.getNextEntry();
        while (zentry != null) {
            if (isMyBackupFile(zentry)) {
                downloadMyBackupFile(backupFolder, zentry.getName(), zin);
            }
            zentry = zin.getNextEntry();
        }

        // fix the download size
        postDownload(BackupRestoreStatus.Status.DOWNLOAD_SUCCESS);

        try {
            zin.closeEntry();
            zin.close();
        }catch (IOException e) {
            log.debug("Failed to close the stream e", e);
            // it's a known issue to use curl
            //it's safe to ignore this exception here.
        }
    }

    private boolean hasDownloaded(File backupFolder) {
        FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                return name.endsWith(".zip");
            }
        };

        File[] zipFiles = backupFolder.listFiles(filter);

        if (zipFiles == null) {
            return false;
        }

        for (File zipFile : zipFiles) {
            if (backupOps.checkMD5(zipFile) == false) {
                log.info("MD5 of {} does not match its md5 file", zipFile.getAbsolutePath());
                return false;
            }
        }

        return true;
    }

    private void postDownload(BackupRestoreStatus.Status status) {
        restoreStatus = backupOps.queryBackupRestoreStatus(remoteBackupFileName, false);
        /*
        restoreStatus.increaseNodeCompleted();
        int completedNodes = restoreStatus.getNodeCompleted();
        */

        Status s = null;
        if (restoreStatus.getStatus() == BackupRestoreStatus.Status.DOWNLOADING) {
            long nodeNumber = backupOps.getHosts().size();
            /*
            if (completedNodes == nodeNumber) {
                restoreStatus.setStatus(BackupRestoreStatus.Status.DOWNLOAD_SUCCESS);
            }
            */

            if (restoreStatus.getNodeCompleted() == nodeNumber -1 ) {
                //restoreStatus.setStatus(BackupRestoreStatus.Status.DOWNLOAD_SUCCESS);
                s = Status.DOWNLOAD_SUCCESS;
            }
        }

        //backupOps.persistBackupRestoreStatus(restoreStatus, false);
        setDownloadStatus(remoteBackupFileName, s, 0, 0);
    }

    private boolean isMyBackupFile(ZipEntry backupEntry) throws UnknownHostException {
        String filename = backupEntry.getName();

        String localHostName = InetAddress.getLocalHost().getHostName();
        return filename.contains(localHostName) ||
                filename.contains(BackupConstants.BACKUP_INFO_SUFFIX) ||
                filename.contains(BackupConstants.BACKUP_ZK_FILE_SUFFIX);
    }

    private long downloadMyBackupFile(File downloadDir, String backupFileName, ZipInputStream zin) throws IOException {
        long downloadSize = 0;

        if (isGeo == false) {
            isGeo = backupOps.isGeoBackup(backupFileName);

            if (isGeo) {
                /*
                BackupRestoreStatus status = backupOps.queryBackupRestoreStatus(remoteBackupFileName, false);
                status.setIsGeo(true);
                log.info("This is a Geo backup status={}", status);
                backupOps.persistBackupRestoreStatus(status, false);
                */
                backupOps.setGeoFlag(remoteBackupFileName, false);
            }
        }

        File file = new File(downloadDir, backupFileName);
        if (!file.exists()) {
            log.info("lbyb create new file {}", file.getAbsolutePath());
            file.getParentFile().mkdirs();
            file.createNewFile();
        }

        byte[] buf = new byte[BackupConstants.DOWNLOAD_BUFFER_SIZE];
        int length;

        try (FileOutputStream out = new FileOutputStream(file)) {
            while ((length = zin.read(buf)) > 0) {
                out.write(buf, 0, length);
                downloadSize += length;
                updateDownloadSize(length);
            }
        } catch(IOException e) {
            log.error("Failed to download {} from server e=", backupFileName, e);
            setDownloadStatus(remoteBackupFileName,
                    BackupRestoreStatus.Status.DOWNLOAD_FAILED, 0, 0);
            throw e;
        }

        return downloadSize;
    }

    private void notifyOtherNodes() {
        log.info("Notify other nodes");
        if (notifyOthers == false) {
            return;
        }

        URI pushUri = SysClientFactory.URI_NODE_BACKUPS_PUSH;

        List<Service> sysSvcs = backupOps.getAllSysSvc();
        for (Service svc : sysSvcs) {
            URI endpoint = svc.getEndpoint();
            log.info("Notify {} hostname={}", endpoint, svc.getNodeName());

            if (localHostName.equals(svc.getNodeName())) {
                continue;
            }

            SysClientFactory.SysClient sysClient = SysClientFactory.getSysClient(endpoint);
            sysClient.post(pushUri, null, remoteBackupFileName);
        }
    }

    private ZipInputStream getDownloadStream() throws IOException {
        InputStream in = client.download(remoteBackupFileName);
        ZipInputStream zin = new ZipInputStream(in);

        return zin;
    }
}
