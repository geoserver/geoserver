/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.ContactInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerFactory;
import org.geoserver.config.GeoServerInfo;
import org.geoserver.config.JAIInfo;
import org.geoserver.config.LoggingInfo;
import org.geoserver.config.ServiceInfo;
import org.geoserver.config.impl.JAIInfoImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.util.logging.Logging;

/**
 * Imports configuration from a legacy "services.xml" file into a geoserver configuration instance.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public class LegacyConfigurationImporter {

    /** logger */
    static Logger LOGGER = Logging.getLogger("org.geoserver.confg");

    /** configuration */
    GeoServer geoServer;

    /**
     * Creates the importer.
     *
     * @param geoServer The configuration to import into.
     */
    public LegacyConfigurationImporter(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /**
     * No argument constructor.
     *
     * <p>Calling code should use {@link #setConfiguration(GeoServer)} when using this constructor.
     */
    public LegacyConfigurationImporter() {}

    /** Sets teh configuration to import into. */
    public void setConfiguration(GeoServer geoServer) {
        this.geoServer = geoServer;
    }

    /** The configuration being imported into. */
    public GeoServer getConfiguration() {
        return geoServer;
    }

    /**
     * Imports configuration from a geoserver data directory into the configuration.
     *
     * @param dir The root of the data directory.
     */
    public void imprt(File dir) throws Exception {

        // TODO: this routine needs to be safer about accessing parameters,
        // wrapping in null checks

        GeoServerFactory factory = geoServer.getFactory();

        // services.xml
        File servicesFile = new File(dir, "services.xml");
        if (!servicesFile.exists()) {
            throw new FileNotFoundException(
                    "Could not find services.xml under:" + dir.getAbsolutePath());
        }

        LegacyServicesReader reader = new LegacyServicesReader();
        reader.read(servicesFile);

        //
        // global
        //
        GeoServerInfo info = factory.createGlobal();
        Map<String, Object> global = reader.global();

        // info.setMaxFeatures( get( global, "maxFeatures", Integer.class ) );
        info.getSettings().setVerbose(get(global, "verbose", boolean.class));
        info.getSettings().setVerboseExceptions(get(global, "verboseExceptions", boolean.class));
        info.getSettings().setNumDecimals(get(global, "numDecimals", int.class, 4));
        info.getSettings().setCharset((String) global.get("charSet"));
        info.setUpdateSequence(get(global, "updateSequence", int.class).longValue());
        info.getSettings().setOnlineResource(get(global, "onlineResource", String.class));
        info.getSettings().setProxyBaseUrl(get(global, "ProxyBaseUrl", String.class));

        // contact
        Map<String, Object> contact = reader.contact();
        ContactInfo contactInfo = factory.createContact();

        contactInfo.setContactPerson((String) contact.get("ContactPerson"));
        contactInfo.setContactOrganization((String) contact.get("ContactOrganization"));
        contactInfo.setContactVoice((String) contact.get("ContactVoiceTelephone"));
        contactInfo.setContactFacsimile((String) contact.get("ContactFacsimileTelephone"));
        contactInfo.setContactPosition((String) contact.get("ContactPosition"));
        contactInfo.setContactEmail((String) contact.get("ContactElectronicMailAddress"));

        contactInfo.setAddress((String) contact.get("Address"));
        contactInfo.setAddressType((String) contact.get("AddressType"));
        contactInfo.setAddressCity((String) contact.get("City"));
        contactInfo.setAddressCountry((String) contact.get("Country"));
        contactInfo.setAddressState((String) contact.get("StateOrProvince"));
        contactInfo.setAddressPostalCode((String) contact.get("PostCode"));
        info.getSettings().setContact(contactInfo);

        // jai
        JAIInfo jai = new JAIInfoImpl();
        jai.setMemoryCapacity(
                (Double)
                        value(global.get("JaiMemoryCapacity"), JAIInfoImpl.DEFAULT_MemoryCapacity));
        jai.setMemoryThreshold(
                (Double)
                        value(
                                global.get("JaiMemoryThreshold"),
                                JAIInfoImpl.DEFAULT_MemoryThreshold));
        jai.setTileThreads(
                (Integer) value(global.get("JaiTileThreads"), JAIInfoImpl.DEFAULT_TileThreads));
        jai.setTilePriority(
                (Integer) value(global.get("JaiTilePriority"), JAIInfoImpl.DEFAULT_TilePriority));
        jai.setJpegAcceleration(
                (Boolean) value(global.get("JaiJPEGNative"), JAIInfoImpl.DEFAULT_JPEGNative));
        if (Boolean.TRUE.equals(value(global.get("JaiPNGNative"), JAIInfoImpl.DEFAULT_PNGNative))) {
            jai.setPngEncoderType(JAIInfo.PngEncoderType.NATIVE);
        }
        jai.setRecycling(
                (Boolean) value(global.get("JaiRecycling"), JAIInfoImpl.DEFAULT_Recycling));
        jai.setAllowNativeMosaic(
                (Boolean) value(global.get("JaiMosaicNative"), JAIInfoImpl.DEFAULT_MosaicNative));
        info.setJAI(jai);

        geoServer.setGlobal(info);

        // logging
        LoggingInfo logging = factory.createLogging();

        logging.setLevel((String) global.get("log4jConfigFile"));
        logging.setLocation((String) global.get("logLocation"));

        if (global.get("suppressStdOutLogging") != null) {
            logging.setStdOutLogging(!get(global, "suppressStdOutLogging", Boolean.class));
        } else {
            logging.setStdOutLogging(true);
        }
        geoServer.setLogging(logging);

        // read services
        for (LegacyServiceLoader sl : GeoServerExtensions.extensions(LegacyServiceLoader.class)) {
            try {
                sl.setReader(reader);

                ServiceInfo service = sl.load(geoServer);
                if (service != null) {
                    LOGGER.info("Loading service '" + service.getId() + "'");
                    geoServer.add(service);
                }
            } catch (Exception e) {
                String msg =
                        "Error occured loading service: " + sl.getServiceClass().getSimpleName();
                LOGGER.warning(msg);
                LOGGER.log(Level.INFO, "", e);
            }
        }
    }

    Object value(Object value, Object def) {
        return value != null ? value : def;
    }

    protected <T extends Object> T get(Map map, String key, Class<T> clazz, T def) {
        Object o = map.get(key);
        if (o == null) {
            if (def != null) {
                return def;
            }

            // check for primitive type
            if (clazz.isPrimitive()) {
                if (clazz == int.class) {
                    return (T) Integer.valueOf(0);
                }
                if (clazz == double.class) {
                    return (T) Double.valueOf(0d);
                }
                if (clazz == boolean.class) {
                    return (T) Boolean.FALSE;
                }
            }
            return null;
        }

        return (T) o;
    }

    protected <T extends Object> T get(Map map, String key, Class<T> clazz) {
        return get(map, key, clazz, null);
    }
}
