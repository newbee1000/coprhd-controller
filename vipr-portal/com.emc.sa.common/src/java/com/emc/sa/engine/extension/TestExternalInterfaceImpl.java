package com.emc.sa.engine.extension;

import com.emc.sa.descriptor.TestExternalInterface;

public class TestExternalInterfaceImpl implements TestExternalInterface{

	@Override
	public String sayHello() {
		String msg= "MANOJ External Loader";
		return msg;
	}

}