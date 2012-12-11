/* Copyright (c) 2012 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wfs;

import org.geoserver.config.GeoServer;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wfs.GMLInfo.SrsNameStyle;
import org.geotools.util.Version;

/**
 * Loads and persist the {@link WFSInfo} object to and from xstream 
 * persistence.
 * 
 * @author Justin Deoliveira, The Open Planning Project
 *
 */
public class WFSXStreamLoader extends XStreamServiceLoader<WFSInfo> {

    public WFSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "wfs");
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        xp.getXStream().alias( "wfs", WFSInfo.class, WFSInfoImpl.class );
        xp.getXStream().alias( "version", WFSInfo.Version.class);
        xp.getXStream().alias( "gml", GMLInfo.class, GMLInfoImpl.class );
    }
    
    protected WFSInfo createServiceFromScratch(GeoServer gs) {
        WFSInfoImpl wfs = new WFSInfoImpl();
        wfs.setName("WFS");
        wfs.setMaxFeatures(1000000);

        //gml2
        GMLInfoImpl gml2 = new GMLInfoImpl();
        gml2.setSrsNameStyle( GMLInfo.SrsNameStyle.XML );
        gml2.setOverrideGMLAttributes(true);
        wfs.getGML().put( WFSInfo.Version.V_10 , gml2 );
        
        //gml3
        GMLInfoImpl gml3 = new GMLInfoImpl();
        gml3.setSrsNameStyle( GMLInfo.SrsNameStyle.URN );
        gml3.setOverrideGMLAttributes(false);
        wfs.getGML().put( WFSInfo.Version.V_11 , gml3);
        
        return wfs;
    }

    public Class<WFSInfo> getServiceClass() {
        return WFSInfo.class;
    }
    
    @Override
    protected WFSInfo initialize(WFSInfo service) {
        super.initialize(service);
        if ( service.getVersions().isEmpty() ) {
            service.getVersions().add(WFSInfo.Version.V_10.getVersion());
            service.getVersions().add(WFSInfo.Version.V_11.getVersion());
        }

        if (!service.getVersions().contains(WFSInfo.Version.V_20.getVersion())) {
            service.getVersions().add(WFSInfo.Version.V_20.getVersion());
        }

        //set the defaults for GMLInfo if they are not set
        GMLInfo gml = service.getGML().get(WFSInfo.Version.V_10);
        if (gml.getOverrideGMLAttributes() == null) {
            gml.setOverrideGMLAttributes(true);
        }
        gml = service.getGML().get(WFSInfo.Version.V_11);
        if (gml.getOverrideGMLAttributes() == null) {
            gml.setOverrideGMLAttributes(false);
        }
        gml = service.getGML().get(WFSInfo.Version.V_20);
        if (gml == null) {
            gml = new GMLInfoImpl();
            gml.setOverrideGMLAttributes(false);
            gml.setSrsNameStyle(SrsNameStyle.URN2);
            service.getGML().put(WFSInfo.Version.V_20, gml);
        }
        return service;
    }

}
