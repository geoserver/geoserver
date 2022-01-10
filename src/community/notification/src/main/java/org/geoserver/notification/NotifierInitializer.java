/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package org.geoserver.notification;

import com.thoughtworks.xstream.XStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geoserver.notification.common.NotificationConfiguration;
import org.geoserver.notification.common.NotificationXStreamInitializer;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.Resources;
import org.geoserver.util.IOUtils;
import org.geotools.util.logging.Logging;

public class NotifierInitializer implements GeoServerInitializer {

    static Logger LOGGER = Logging.getLogger(NotifierInitializer.class);

    public static final String PROPERTYFILENAME = "notifier.xml";
    public static final String THREAD_NAME = "MessageMultiplexer";

    private GeoServerResourceLoader loader;

    public NotifierInitializer(GeoServerResourceLoader loader) {
        this.loader = loader;
    }

    public void initialize(GeoServer geoServer) throws Exception {

        XStream xs = new XStream();
        List<NotificationXStreamInitializer> xstreamInitializers =
                GeoServerExtensions.extensions(NotificationXStreamInitializer.class);
        for (NotificationXStreamInitializer ni : xstreamInitializers) {
            ni.init(xs);
        }
        NotificationConfiguration cfg = getConfiguration(xs);
        MessageMultiplexer mm = new MessageMultiplexer(cfg);

        List<INotificationCatalogListener> catalogListeners =
                GeoServerExtensions.extensions(INotificationCatalogListener.class);
        for (INotificationCatalogListener cl : catalogListeners) {
            cl.setMessageMultiplexer(mm);
            geoServer.getCatalog().addListener(cl);
        }

        List<INotificationTransactionListener> transactionListeners =
                GeoServerExtensions.extensions(INotificationTransactionListener.class);
        for (INotificationTransactionListener tl : transactionListeners) {
            tl.setMessageMultiplexer(mm);
        }

        (new Thread(mm, THREAD_NAME)).start();
    }

    private NotificationConfiguration getConfiguration(XStream xs) {
        NotificationConfiguration nc = null;
        try {
            Resource f = this.loader.get(Paths.path("notifier", PROPERTYFILENAME));
            if (!Resources.exists(f)) {
                /*
                 * Copy and use the sample notifier
                 */
                IOUtils.copy(
                        getClass()
                                .getClassLoader()
                                .getResourceAsStream(NotifierInitializer.PROPERTYFILENAME),
                        f.file());
            }
            nc = (NotificationConfiguration) xs.fromXML(f.in());
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
        }
        return nc;
    }
}
