/*
 * Copyright (c) 2012-2016 iWave Software LLC
 * All Rights Reserved
 */
package com.iwave.ext.netappc.model;

public class SnapmirrorResp {

    public static final String SUCCESSED = "succeeded";
    public static final String FAILED = "failed";
    public static final String INPROGRESS = "in_progress";

    public Integer getResultErrorCode() {
        return resultErrorCode;
    }

    public void setResultErrorCode(Integer resultErrorCode) {
        this.resultErrorCode = resultErrorCode;
    }

    public Integer getResultJobid() {
        return resultJobid;
    }

    public void setResultJobid(Integer resultJobid) {
        this.resultJobid = resultJobid;
    }

    public String getResultStatus() {
        return resultStatus;
    }

    public void setResultStatus(String resultStatus) {
        this.resultStatus = resultStatus;
    }

    public String getResultErrorMessage() {
        return resultErrorMessage;
    }

    public void setResultErrorMessage(String resultErrorMessage) {
        this.resultErrorMessage = resultErrorMessage;
    }

    public SnapmirrorResp() {
        // TODO Auto-generated constructor stub
    }

    private Integer resultErrorCode;

    private Integer resultJobid;

    private String resultStatus;

    private String resultErrorMessage;

}
