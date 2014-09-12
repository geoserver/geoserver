package org.geoserver.cluster.hazelcast;

import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.expectLastCall;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.same;
import static org.easymock.EasyMock.verify;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.junit.Test;

public class RemovedObjectProxyTest {
	

	@Test
	public void testVisitsCatalogInfoSubclasses() throws Exception {
		
		// Find all visit methods on CatalogVisitor that take a single parameter that's a subclass
		// of CatalogInfo
		for (Method method: CatalogVisitor.class.getMethods()) {
			if (!method.getName().equals("visit")) continue;
			if (method.getParameterTypes().length!=1) continue;
			Class<?> clazz = method.getParameterTypes()[0];
			if (CatalogInfo.class.isAssignableFrom(method.getParameterTypes()[0])){
			
				@SuppressWarnings("unchecked")
				InvocationHandler handler = new RemovedObjectProxy("Test", "Test", (Class<? extends CatalogInfo>)clazz);
				CatalogInfo info = (CatalogInfo) Proxy.newProxyInstance(
						clazz.getClassLoader(),
						new Class[] { clazz },
						handler);
				
				// Check that the method is called by accept when the proxy is mimicking the appropriate type
				CatalogVisitor visitor = createMock(CatalogVisitor.class);
				System.err.println(method);
				method.invoke(visitor, same(info));expectLastCall();
				
				replay(visitor);
				info.accept(visitor);
				verify(visitor);

			}
		}
		
	}

}
