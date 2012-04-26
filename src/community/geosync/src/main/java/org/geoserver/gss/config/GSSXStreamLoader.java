/* Copyright (c) 2011 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gss.config;

import java.util.ArrayList;
import java.util.Arrays;

import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.Version;

import com.thoughtworks.xstream.XStream;

/**
 * Loads the GSS configuration from the GeoServer data directory.
 * 
 * @author Gabriel Roldan
 * 
 */
public class GSSXStreamLoader extends XStreamServiceLoader<GSSInfo> {

    public GSSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "gss");
    }

    @Override
    protected GSSInfo createServiceFromScratch(GeoServer gs) {
        return new GSSInfoImpl();
    }

    public Class<GSSInfo> getServiceClass() {
        return GSSInfo.class;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        XStream xs = xp.getXStream();
        xs.alias("gss", GSSInfo.class, GSSInfoImpl.class);
    }

    @Override
    protected GSSInfo initialize(GSSInfo service) {
        GSSInfoImpl impl = (GSSInfoImpl) service;
        if (service.getVersions() == null) {
            impl.setVersions(new ArrayList<Version>());
        }
        if (service.getVersions().isEmpty()) {
            service.getVersions().add(new Version("1.0.0"));
        }
        if (service.getTitle() == null) {
            service.setTitle("GeoServer GeoSynchronization Service");
        }
        if (service.getAbstract() == null) {
            service.setAbstract("");
        }
        if (service.getAccessConstraints() == null) {
            service.setAccessConstraints("NONE");
        }
        if (service.getFees() == null) {
            service.setFees("NONE");
        }
        if (service.getKeywords() == null) {
            impl.setKeywords(new ArrayList<KeywordInfo>(Arrays.asList(new Keyword("GeoServer"),
                    new Keyword("GSS"), new Keyword("GeoSync"))));
        }
        return service;
    }
}
