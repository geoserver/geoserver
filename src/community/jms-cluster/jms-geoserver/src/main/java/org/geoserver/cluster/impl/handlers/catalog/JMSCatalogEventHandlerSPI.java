/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.cluster.JMSEventHandlerSPI;
import org.geoserver.cluster.events.ToggleSwitch;

/** @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it */
public abstract class JMSCatalogEventHandlerSPI extends JMSEventHandlerSPI<String, CatalogEvent> {

    protected final Catalog catalog;
    protected final XStream xstream;
    protected final ToggleSwitch producer;

    public JMSCatalogEventHandlerSPI(
            int priority, Catalog catalog, XStream xstream, ToggleSwitch producer) {
        super(priority);
        this.catalog = catalog;
        this.xstream = xstream;
        this.producer = producer;
    }
}
