/*
 * Copyright (c) 2015 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.model.compute;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "compute_imageserver_update")
public class ComputeImageServerUpdate {
    private String imageServerIp;

    private String imageServerSecondIp;

    private String imageServerUser;

    private String imageServerPassword;

    private String tftpbootDir;

    private Integer osInstallTimeoutMs;

    public ComputeImageServerUpdate() {

    }

    public ComputeImageServerUpdate(String imageServerAddress,
            String imageServerSecondIp, String imageServerUser,
            String imageServerPassword, String tftpbootDir,
            Integer osInstallTimeoutMs) {
        super();
        this.imageServerIp = imageServerAddress;
        this.imageServerSecondIp = imageServerSecondIp;
        this.imageServerUser = imageServerUser;
        this.imageServerPassword = imageServerPassword;
        this.tftpbootDir = tftpbootDir;
        this.osInstallTimeoutMs = osInstallTimeoutMs;
    }

    /**
     * @return the imageServerIp
     */
    @XmlElement(required = true, name = "imageServerIp")
    public String getImageServerIp() {
        return imageServerIp;
    }

    /**
     * @param imageServerIp
     *            the imageServerIp to set
     */
    public void setImageServerIp(String imageServerIp) {
        this.imageServerIp = imageServerIp;
    }

    /**
     * @return the imageServerSecondIp
     */
    @XmlElement(required = true, name = "imageServerSecondIp")
    public String getImageServerSecondIp() {
        return imageServerSecondIp;
    }

    /**
     * @param imageServerSecondIp
     *            the imageServerSecondIp to set
     */
    public void setImageServerSecondIp(String imageServerSecondIp) {
        this.imageServerSecondIp = imageServerSecondIp;
    }

    /**
     * @return the imageServerUser
     */
    @XmlElement(required = true, name = "imageServerUser")
    public String getImageServerUser() {
        return imageServerUser;
    }

    /**
     * @param imageServerUser
     *            the imageServerUser to set
     */
    public void setImageServerUser(String imageServerUser) {
        this.imageServerUser = imageServerUser;
    }

    /**
     * @return the password
     */
    @XmlElement(required = true)
    public String getImageServerPassword() {
        return imageServerPassword;
    }

    /**
     * @param password
     *            the password to set
     */
    public void setImageServerPassword(String imageServerPassword) {
        this.imageServerPassword = imageServerPassword;
    }

    /**
     * @return the tftpbootDir
     */
    @XmlElement(required = true, name = "tftpbootDir")
    public String getTftpbootDir() {
        return tftpbootDir;
    }

    /**
     * @param tftpbootDir
     *            the tftpbootDir to set
     */
    public void setTftpbootDir(String tftpbootDir) {
        this.tftpbootDir = tftpbootDir;
    }

    /**
     * @return the osInstallTimeoutMs
     */
    @XmlElement(required = true, name = "osInstallTimeoutMs")
    public long getOsInstallTimeoutMs() {
        return osInstallTimeoutMs;
    }

    /**
     * @param osInstallTimeoutMs
     *            the osInstallTimeoutMs to set
     */
    public void setOsInstallTimeoutMs(Integer osInstallTimeoutMs) {
        this.osInstallTimeoutMs = osInstallTimeoutMs;
    }
}
