/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wps.process;

import java.util.ArrayList;
import java.util.List;
import org.geoserver.config.GeoServer;
import org.geoserver.wps.ProcessGroupInfo;
import org.geoserver.wps.ProcessGroupInfoImpl;
import org.geoserver.wps.ProcessInfo;
import org.geoserver.wps.ProcessInfoImpl;
import org.geoserver.wps.WPSInfo;
import org.geotools.feature.NameImpl;
import org.geotools.process.ProcessFactory;
import org.geotools.process.Processors;
import org.geotools.process.vector.VectorProcessFactory;
import org.junit.After;
import org.junit.Before;
import org.opengis.feature.type.Name;

/**
 * Same as {@link ProcessFilterTest} but using the WPS configuration this time
 *
 * @author aaime
 */
public class WPSConfigProcessFilterTest extends AbstractProcessFilterTest {

    @Before
    public void setUpInternal() throws Exception {

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
        for (Name jtsName : jtsNames) {
            ProcessInfo pai = new ProcessInfoImpl();
            pai.setName(jtsName);
            pai.setEnabled(false);
            jtsGroup.getFilteredProcesses().add(pai);
        }
        List<ProcessGroupInfo> pgs = wps.getProcessGroups();
        pgs.clear();
        pgs.add(jtsGroup);

        // remove the feature gs factory
        ProcessGroupInfo gsGroup = new ProcessGroupInfoImpl();
        gsGroup.setFactoryClass(VectorProcessFactory.class);
        gsGroup.setEnabled(false);
        pgs.add(gsGroup);

        gs.save(wps);
    }

    @After
    public void cleanup() {
        GeoServer gs = getGeoServer();
        WPSInfo wps = gs.getService(WPSInfo.class);
        wps.getProcessGroups().clear();
        gs.save(wps);
    }
}
