package org.geoserver.wps.process;

import static org.custommonkey.xmlunit.XMLAssert.*;

import java.util.ArrayList;
import java.util.List;

import junit.framework.Test;

import org.geoserver.config.GeoServer;
import org.geoserver.data.test.MockData;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geoserver.wps.WPSTestSupport;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.process.feature.gs.FeatureGSProcessFactory;
import org.opengis.feature.type.Name;
import org.w3c.dom.Document;

/**
 * Same as {@link ProcessFilterTest} but using the WPS configuration this time
 * @author aaime
 *
 */
public class WPSConfigProcessFilterTest extends AbstractProcessFilterTest {
    
    //read-only test
    public static Test suite() {
        return new OneTimeTestSetup(new WPSConfigProcessFilterTest());
    }
    
    @Override
    protected void setUpInternal() throws Exception {
        super.setUpInternal();
        
        GeoServer gs = getGeoServer();
        WPSInfo wps = gs.getService(WPSInfo.class);
        
        // remove all jts processes but buffer
        NameImpl bufferName = new NameImpl("JTS", "buffer");
        ProcessFactory jts = Processors.createProcessFactory(bufferName);
        ProcessGroupInfo jtsGroup = new ProcessGroupInfoImpl();
        jtsGroup.setFactoryClass(jts.getClass());
        jtsGroup.setEnabled(true);
        List<Name> jtsNames = new ArrayList<Name>(jts.getNames());
        jtsNames.remove(bufferName);
        jtsGroup.getFilteredProcesses().addAll(jtsNames);
        List<ProcessGroupInfo> pgs = wps.getProcessGroups();
        pgs.clear();
        pgs.add(jtsGroup);
        
        // remove the feature gs factory
        ProcessGroupInfo gsGroup = new ProcessGroupInfoImpl();
        gsGroup.setFactoryClass(FeatureGSProcessFactory.class);
        gsGroup.setEnabled(false);
        pgs.add(gsGroup);
        
        gs.save(wps);
        
    }
    
    
    
}
