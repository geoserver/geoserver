package org.geoserver.wps.process;

import java.util.List;


public class ProcessFilterTest extends AbstractProcessFilterTest {
    
    @Override
    protected void setUpSpring(List<String> springContextLocations) {
        super.setUpSpring(springContextLocations);
        springContextLocations.add("classpath*:/processFilterContext.xml");
    }
        
}
