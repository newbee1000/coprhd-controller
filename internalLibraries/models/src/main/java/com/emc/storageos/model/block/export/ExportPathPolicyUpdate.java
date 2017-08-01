/*
 * Copyright (c) 2016 EMC Corporation
 * All Rights Reserved
 */
package com.emc.storageos.model.block.export;

import java.net.URI;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

import com.emc.storageos.model.valid.Range;

@XmlRootElement(name = "export_path_policy")
public class ExportPathPolicyUpdate {
    
    private Integer maxPaths;
    private Integer pathsPerInitiator;
    private Integer minPaths;
    private String description;
    private Boolean portGroupFlag;
    private String name;
    private Integer maxInitiatorsPerPort;
    
    private List<URI> portsToAdd;
    private List<URI> portsToRemove;
    
    @XmlElement(name = "max_paths")
    @Range(min = 1, max = 65535)
    /**
     * The maximum number of storage paths (ports) that will be provisioned.
     */
    public Integer getMaxPaths() {
        return maxPaths;
    }
    public void setMaxPaths(Integer maxPaths) {
        this.maxPaths = maxPaths;
    }
    
    @XmlElement(name = "paths_per_initiator")
    @Range(min = 1, max = 65535)
    /**
     * The number of storage paths (ports) that will be assigned and zoned to each Initiator.
     */
    public Integer getPathsPerInitiator() {
        return pathsPerInitiator;
    }
    public void setPathsPerInitiator(Integer pathsPerInitiator) {
        this.pathsPerInitiator = pathsPerInitiator;
    }
    
    @XmlElement(name = "min_paths")
    @Range(min = 1, max = 65535)
    /**
     * The minimum number of storage paths that must be
     * provisioned for a successful export.
     */
    public Integer getMinPaths() {
        return minPaths;
    }
    public void setMinPaths(Integer minPaths) {
        this.minPaths = minPaths;
    }
    
    @XmlElement(name = "description")
    public String getDescription() {
        return description;
    }
    public void setDescription(String description) {
        this.description = description;
    }
    
    @XmlElement(name = "is_port_group")
    public Boolean getPortGroupFlag() {
        return portGroupFlag;
    }
    public void setPortGroupFlag(Boolean portGroupFlag) {
        this.portGroupFlag = portGroupFlag;
    }
    
    @XmlElement(name = "name")
    public String getName() {
        return name;
    }
    public void setName(String name) {
        this.name = name;
    }
    
    /**
     * The number of Initiators that will be assigned and zoned to each Port.
     */
    @XmlElement(name = "max_initiators_per_port", required = false)
    @Range(min = 1, max = 65535)
    public Integer getMaxInitiatorsPerPort() {
        return maxInitiatorsPerPort;
    }
    public void setMaxInitiatorsPerPort(Integer maxInitiatorsPerPort) {
        this.maxInitiatorsPerPort = maxInitiatorsPerPort;
    }
    
    @XmlElementWrapper(name = "add_storage_ports", required = false)
    @XmlElement(name = "storage_port", required = false)
    public List<URI> getPortsToAdd() {
        return portsToAdd;
    }
    public void setPortsToAdd(List<URI> portsToAdd) {
        this.portsToAdd = portsToAdd;
    }
    @XmlElementWrapper(name = "remove_storage_ports", required = false)
    @XmlElement(name = "storage_port", required = false)
    public List<URI> getPortsToRemove() {
        return portsToRemove;
    }
    public void setPortsToRemove(List<URI> portsToRemove) {
        this.portsToRemove = portsToRemove;
    }
    
    
    
}