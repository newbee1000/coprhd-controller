package com.emc.sa.engine.service;

import com.emc.storageos.vasa.async.TaskInfo;

public interface ExternalTaskApdapterInterface {

	
	public void init() throws Exception ;

	public void precheck() throws Exception ;


	public void preLuanch() throws Exception ;

	public TaskInfo execute() throws Exception ;

	public void postcheck() throws Exception ;

	public void postLuanch() throws Exception;

	public void getStatus() throws Exception ;

	public void destroy() ;

}
