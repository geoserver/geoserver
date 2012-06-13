package org.geoserver.security;

import java.util.Arrays;

public class GeoServerCustomSecurityProviderTest extends GeoServerSecurityTestSupport {
    @Override
    protected String[] getSpringContextLocations() {
        String[] locations = super.getSpringContextLocations();
        String[] locationsWithExtra = Arrays.copyOf(super.getSpringContextLocations(), locations.length + 1);
        locationsWithExtra[locationsWithExtra.length - 1] = getClass().getResource(getClass().getSimpleName() + "-context.xml").toString(); 
        return locationsWithExtra;
    }
    
    @Override
    protected void oneTimeTearDown() throws Exception {
		// since we are testing a shutdown hook, we need to be able to avoid
		// double-deleting the spring context after calling it manually in
		// testThatDestroyIsCalled()
    	if (applicationContext != null) super.oneTimeTearDown();
    }
    
    public static class SecurityProvider extends GeoServerSecurityProvider {
        static boolean initCalled = false;
        static boolean destroyCalled = false;
        
        @Override
        public void init(GeoServerSecurityManager manager) {
            initCalled = true;
        }

        @Override
        public void destroy(GeoServerSecurityManager manager) {
            destroyCalled = true;
        }
    }
    
    public void testThatInitIsCalled() {
        assertTrue("The Security provider's init method should be called", SecurityProvider.initCalled);
    }

    public void testThatDestroyIsCalled() throws Exception {
    	super.oneTimeTearDown();
        assertTrue("The Security provider's destroy method should be called", SecurityProvider.destroyCalled);
    }
}
