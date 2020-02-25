/* (c) 2014 - 2015 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2014 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms;

import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.MarshallingContext;
import com.thoughtworks.xstream.converters.UnmarshallingContext;
import com.thoughtworks.xstream.io.HierarchicalStreamReader;
import com.thoughtworks.xstream.io.HierarchicalStreamWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.config.GeoServer;
import org.geoserver.config.util.AuthorityURLInfoInfoListConverter;
import org.geoserver.config.util.LayerIdentifierInfoListConverter;
import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersister.ServiceInfoConverter;
import org.geoserver.config.util.XStreamServiceLoader;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.wms.WMSInfo.WMSInterpolation;
import org.geotools.util.Version;

/**
 * Loads and persist the {@link WMSInfo} object to and from xstream persistence.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class WMSXStreamLoader extends XStreamServiceLoader<WMSInfo> {

    public WMSXStreamLoader(GeoServerResourceLoader resourceLoader) {
        super(resourceLoader, "wms");
    }

    public Class<WMSInfo> getServiceClass() {
        return WMSInfo.class;
    }

    protected WMSInfo createServiceFromScratch(GeoServer gs) {
        WMSInfo wms = new WMSInfoImpl();
        wms.setName("WMS");
        return wms;
    }

    @Override
    protected void initXStreamPersister(XStreamPersister xp, GeoServer gs) {
        super.initXStreamPersister(xp, gs);
        initXStreamPersister(xp);
    }

    /** Sets up aliases and allowed types for the xstream persister */
    public static void initXStreamPersister(XStreamPersister xp) {
        XStream xs = xp.getXStream();
        xs.alias("wms", WMSInfo.class, WMSInfoImpl.class);
        xs.registerConverter(new WMSInfoConverter(xp));
        xs.addDefaultImplementation(WatermarkInfoImpl.class, WatermarkInfo.class);
        xs.allowTypes(
                new Class[] {
                    WatermarkInfo.class, WatermarkInfoImpl.class, CacheConfiguration.class
                });
    }

    @Override
    protected WMSInfo initialize(WMSInfo service) {
        super.initialize(service);

        final Version version_1_1_1 = WMS.VERSION_1_1_1;
        final Version version_1_3_0 = WMS.VERSION_1_3_0;

        if (!service.getVersions().contains(version_1_1_1)) {
            service.getVersions().add(version_1_1_1);
        }
        if (!service.getVersions().contains(version_1_3_0)) {
            service.getVersions().add(version_1_3_0);
        }
        if (service.getSRS() == null) {
            ((WMSInfoImpl) service).setSRS(new ArrayList<String>());
        }
        if (service.getGetFeatureInfoMimeTypes() == null) {
            ((WMSInfoImpl) service).setGetFeatureInfoMimeTypes(new HashSet<String>());
        }
        if (service.getGetMapMimeTypes() == null) {
            ((WMSInfoImpl) service).setGetMapMimeTypes(new HashSet<String>());
        }
        if (service.getInterpolation() == null) {
            service.setInterpolation(WMSInterpolation.Nearest);
        }
        return service;
    }

    /**
     * Converter for WMSInfo, stores authority urls and identifiers under metadata map in the 2.1.x
     * series.
     *
     * @since 2.1.3
     */
    static class WMSInfoConverter extends ServiceInfoConverter {

        public WMSInfoConverter(XStreamPersister xp) {
            xp.super(WMSInfo.class);
        }

        @Override
        public boolean canConvert(Class type) {
            return WMSInfo.class.isAssignableFrom(type);
        }

        /** @since 2.1.3 */
        @Override
        protected void doMarshal(
                Object source, HierarchicalStreamWriter writer, MarshallingContext context) {

            //            WMSInfo service = (WMSInfo) source;
            //            {
            //                String authUrlsSerializedForm =
            // AuthorityURLInfoInfoListConverter.toString(service
            //                        .getAuthorityURLs());
            //                if (null != authUrlsSerializedForm) {
            //                    service.getMetadata().put("authorityURLs",
            // authUrlsSerializedForm);
            //                }
            //            }
            //
            //            {
            //                String identifiersSerializedForm = LayerIdentifierInfoListConverter
            //                        .toString(service.getIdentifiers());
            //                if (null != identifiersSerializedForm) {
            //                    service.getMetadata().put("identifiers",
            // identifiersSerializedForm);
            //                }
            //            }

            super.doMarshal(source, writer, context);
        }

        @Override
        public Object doUnmarshal(
                Object result, HierarchicalStreamReader reader, UnmarshallingContext context) {

            WMSInfoImpl service = (WMSInfoImpl) super.doUnmarshal(result, reader, context);
            MetadataMap metadata = service.getMetadata();
            // for backwards compatibility with 2.1.3+ data directories, check if the auth urls and
            // identifiers are stored in the metadata map
            if (service.getAuthorityURLs() == null && metadata != null) {
                String serialized = metadata.get("authorityURLs", String.class);
                List<AuthorityURLInfo> authorities;
                if (serialized == null) {
                    authorities = new ArrayList<AuthorityURLInfo>(1);
                } else {
                    authorities = AuthorityURLInfoInfoListConverter.fromString(serialized);
                }
                service.setAuthorityURLs(authorities);
            }
            if (service.getIdentifiers() == null && metadata != null) {
                String serialized = metadata.get("identifiers", String.class);
                List<LayerIdentifierInfo> identifiers;
                if (serialized == null) {
                    identifiers = new ArrayList<LayerIdentifierInfo>(1);
                } else {
                    identifiers = LayerIdentifierInfoListConverter.fromString(serialized);
                }
                service.setIdentifiers(identifiers);
            }
            return service;
        }
    }
}
