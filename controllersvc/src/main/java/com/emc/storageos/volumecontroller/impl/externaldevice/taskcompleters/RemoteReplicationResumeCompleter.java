/*
 * Copyright (c) 2017 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.volumecontroller.impl.externaldevice.taskcompleters;

import com.emc.storageos.volumecontroller.impl.externaldevice.RemoteReplicationElement;

@SuppressWarnings("serial")
public class RemoteReplicationResumeCompleter extends AbstractRemoteReplicationOperationCompleter {

    public RemoteReplicationResumeCompleter(RemoteReplicationElement remoteReplicationElement, String opId) {
        super(remoteReplicationElement, opId);
    }

}
