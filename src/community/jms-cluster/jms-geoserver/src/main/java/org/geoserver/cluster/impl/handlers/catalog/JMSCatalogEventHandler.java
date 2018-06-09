/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import com.thoughtworks.xstream.XStream;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import javax.jms.JMSException;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.cluster.JMSEventHandler;
import org.geoserver.cluster.JMSEventHandlerSPI;

/**
 * Abstract class which use Xstream as message serializer/de-serializer. We extend this class to
 * implementing synchronize method.
 *
 * @author Carlo Cancellieri - carlo.cancellieri@geo-solutions.it
 */
public abstract class JMSCatalogEventHandler extends JMSEventHandler<String, CatalogEvent> {
    public JMSCatalogEventHandler(
            final XStream xstream, Class<JMSEventHandlerSPI<String, CatalogEvent>> clazz) {
        super(xstream, clazz);
        // omit not serializable fields
        omitFields();
    }

    /**
     * omit not serializable fields
     *
     * @see {@link XStream}
     */
    private void omitFields() {
        // omit not serializable fields
        xstream.omitField(CatalogImpl.class, "listeners");
        xstream.omitField(CatalogImpl.class, "facade");
        xstream.omitField(CatalogImpl.class, "resourcePool");
        xstream.omitField(CatalogImpl.class, "resourceLoader");
    }

    @Override
    public String serialize(CatalogEvent event) throws Exception {
        return xstream.toXML(removeCatalogProperties(event));
    }

    @Override
    public CatalogEvent deserialize(String s) throws Exception {

        final Object source = xstream.fromXML(s);
        if (source instanceof CatalogEvent) {
            final CatalogEvent ev = (CatalogEvent) source;
            if (LOGGER.isLoggable(Level.FINE)) {
                final CatalogInfo info = ev.getSource();
                LOGGER.fine("Incoming message event of type CatalogEvent: " + info.getId());
            }
            return ev;
        } else {
            throw new JMSException("Unable to deserialize the following object:\n" + s);
        }
    }

    /** Make sure that properties of type catalog are not serialized for catalog modified events. */
    private CatalogEvent removeCatalogProperties(CatalogEvent event) {
        if (!(event instanceof CatalogModifyEvent)) {
            // not a modify event so nothing to do
            return event;
        }
        CatalogModifyEvent modifyEvent = (CatalogModifyEvent) event;
        // index all the properties that are not of catalog type
        List<Integer> indexes = new ArrayList<>();
        int totalProperties = modifyEvent.getPropertyNames().size();
        for (int i = 0; i < totalProperties; i++) {
            // we only need to check the new values
            Object value = modifyEvent.getNewValues().get(i);
            if (!(value instanceof Catalog)) {
                // not a property of type catalog
                indexes.add(i);
            }
        }
        // let's see if we need to do anything
        if (indexes.size() == totalProperties) {
            // no properties of type catalog, we can use the original event
            return event;
        }
        // well we need to create a new modify event and ignore the properties of catalog type
        List<String> properties = new ArrayList<>();
        List<Object> oldValues = new ArrayList<>();
        List<Object> newValues = new ArrayList<>();
        for (int index : indexes) {
            // add all the properties that are not of catalog type
            properties.add(modifyEvent.getPropertyNames().get(index));
            oldValues.add(modifyEvent.getOldValues().get(index));
            newValues.add(modifyEvent.getNewValues().get(index));
        }
        // crete the new event
        CatalogModifyEventImpl newEvent = new CatalogModifyEventImpl();
        newEvent.setPropertyNames(properties);
        newEvent.setOldValues(oldValues);
        newEvent.setNewValues(newValues);
        newEvent.setSource(modifyEvent.getSource());
        return newEvent;
    }
}
