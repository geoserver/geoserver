/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.monitor;

import com.maxmind.geoip.Location;
import com.maxmind.geoip.LookupService;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geotools.util.logging.Logging;

public class GeoIPPostProcessor implements RequestPostProcessor {

    static Logger LOGGER = Logging.getLogger("org.geoserver.montior");

    /** cached geoip lookup service */
    static volatile LookupService geoIPLookup;

    static final String PROCESSOR_NAME = "geoIp";

    // TODO: cache by IP address

    GeoServerResourceLoader loader;
    AtomicBoolean warned = new AtomicBoolean(false);

    public GeoIPPostProcessor(GeoServerResourceLoader loader) {
        this.loader = loader;
    }

    public void run(RequestData data, HttpServletRequest request, HttpServletResponse response) {
        if (data.getRemoteAddr() == null) {
            LOGGER.info("Request data did not contain ip address. Unable to perform GeoIP lookup.");
            return;
        }

        if (geoIPLookup == null) {
            synchronized (this) {
                if (geoIPLookup == null) {
                    geoIPLookup = lookupGeoIPDatabase();
                }
            }
        }

        if (geoIPLookup == null) {
            return;
        }

        Location loc = geoIPLookup.getLocation(data.getRemoteAddr());
        if (loc == null) {
            LOGGER.fine("Unable to obtain location for " + data.getRemoteAddr());
            return;
        }

        data.setRemoteCountry(loc.countryName);
        data.setRemoteCity(loc.city);
        data.setRemoteLat(loc.latitude);
        data.setRemoteLon(loc.longitude);
    }

    LookupService lookupGeoIPDatabase() {
        try {
            File f = loader.find("monitoring", "GeoLiteCity.dat");
            if (f != null) {
                return new LookupService(f);
            }

            if (!warned.get()) {
                warned.set(true);

                String path =
                        new File(loader.getBaseDirectory(), "monitoring/GeoLiteCity.dat")
                                .getAbsolutePath();
                LOGGER.warning(
                        "GeoIP database "
                                + path
                                + " is not available. "
                                + "Please install the file to enable GeoIP lookups.");
            }
            return null;

        } catch (IOException e) {
            LOGGER.log(Level.WARNING, "Error occured looking up GeoIP database", e);
            return null;
        }
    }

    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }
}
