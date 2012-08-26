package org.geoserver.wps.process;

import junit.framework.Test;

public class ProcessFilterTest extends AbstractProcessFilterTest {
    
    //read-only test
    public static Test suite() {
        return new OneTimeTestSetup(new ProcessFilterTest());
    }
    
    /**
     * Returns the spring context locations to be used in order to build the GeoServer Spring
     * context. Subclasses might want to provide extra locations in order to test extension points.
     * @return
     */
    protected String[] getSpringContextLocations() {
        return new String[] {
                "classpath*:/applicationContext.xml",
                "classpath*:/applicationSecurityContext.xml",
                "classpath*:/processFilterContext.xml"
            };
    }
    

    
}
