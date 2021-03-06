/*
 * Copyright (c) 2013 EMC Corporation
 * All Rights Reserved
 */

package com.emc.storageos.security.authentication;

import com.emc.storageos.db.client.model.BaseToken;
import com.emc.storageos.db.client.model.StorageOSUserDAO;
import com.emc.storageos.svcs.errorhandling.resources.InternalException;

/**
 * Interface for token validation
 */

public interface TokenValidator {
    /**
     * 
     * Decodes and validates a auth token. If valid, return user record.
     * This is a compound operation of decode + verifyToken + resolveUser
     * and supports tokens from remote VDCs
     * 
     * @param token
     * @return storageosuser data model object
     * @throws InternalException
     */
    public StorageOSUserDAO validateToken(String token);

    /**
     * Decodes and validates a token but does not resolve the user.
     * 
     * @param token (signed token on wire)
     * @return Token object if valid
     */
    public BaseToken verifyToken(String token);

    /**
     * Retrieves a user record from a valid token
     * 
     * @param token
     * @return user record
     */
    public StorageOSUserDAO resolveUser(BaseToken token);

}
