/* (c) 2024 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.ogcapi.provider;

import com.thoughtworks.xstream.XStream;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;
import org.springframework.stereotype.Component;

@Component
public class TestCaseInfoXStreamLoader extends XStreamServiceLoader<TestCaseInfo> {

    public TestCaseInfoXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "tc");
    }

    @Override
    public void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        initXStreamPersister(xp);
    }

    /** Sets up aliases and allowed types for the xstream persister */
    public static void initXStreamPersister(XStreamPersister xp) {
        XStream xs = xp.getXStream();
        xs.alias("tc", TestCaseInfo.class, TestCaseInfoImpl.class);
        xs.allowTypes(new Class[] {TestCaseInfo.class});
    }

    @Override
    protected TestCaseInfo createServiceFromScratch(GeoServer gs) {
        TestCaseInfoImpl testCaseInfo = new TestCaseInfoImpl();
        testCaseInfo.setName("tc");
        return (TestCaseInfo) testCaseInfo;
    }

    @Override
    public Class<TestCaseInfo> getServiceClass() {
        return TestCaseInfo.class;
    }

    @Override
    protected TestCaseInfo initialize(TestCaseInfo service) {
        super.initialize(service);
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.2"));
        }
        if (service.getTitle() == null) {
            service.setTitle("TestCaseService");
            service.setAbstract("OGCAPI-TestCase");
        }
        return service;
    }
}
