package com.emc.storageos.vmax.restapi;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

import com.emc.storageos.db.client.model.StorageProvider;
import com.emc.storageos.vmax.VMAXRestUtils;
import com.emc.storageos.vmax.restapi.model.response.NDMMigrationEnvironmentResponse;

public class VMAXRestClientTest {
	
	private static VMAXApiClient apiClient;
	
	 @BeforeClass
	    public static void setup() throws Exception {
		 VMAXApiRestClientFactory apiClientFactory = new VMAXApiRestClientFactory();
		 apiClientFactory.setConnectionTimeoutMs(30000);
		 apiClientFactory.setConnManagerTimeout(60000);
		 apiClientFactory.setMaxConnections(300);
		 apiClientFactory.setMaxConnectionsPerHost(100);
		 apiClientFactory.setNeedCertificateManager(true);
		 apiClientFactory.setSocketConnectionTimeoutMs(3600000);
		 
		 apiClientFactory.init();
		 StorageProvider provider = new StorageProvider();
		 provider.setUseSSL(true);
		 provider.setIPAddress("lglw7150.lss.emc.com");
		 provider.setPortNumber(8443);
		 apiClient = (VMAXApiClient) apiClientFactory.getRESTClient(VMAXRestUtils.getUnisphereRestServerInfo(provider), "smc", "smc", true);
	 }

	@Test
	public void apiClientTest() throws Exception {
		assertNotNull("Api Client object is null", apiClient);
		NDMMigrationEnvironmentResponse response = apiClient.getMigrationEnvironment("", "");
		assertEquals("Not the correct state", "OK", response.getState());
		assertEquals("Not the correct other symm id",  "000196701405", response.getOtherSymmetrixId());
		assertEquals("Not the correct symm id", "000195701430", response.getSymmetrixId());
		assertFalse("Not the correct status", response.isInvalid());
//		fail("Not yet implemented");
	}

}
