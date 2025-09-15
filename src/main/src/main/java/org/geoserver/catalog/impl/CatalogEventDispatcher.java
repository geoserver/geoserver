/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogBeforeAddEvent;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.platform.ExtensionPriority;
import org.geotools.util.logging.Logging;

public class CatalogEventDispatcher {
    /** logger */
    private static final Logger LOGGER = Logging.getLogger(CatalogImpl.class);

    /** listeners */
    protected List<CatalogListener> listeners = new CopyOnWriteArrayList<>();

    public Collection<CatalogListener> getListeners() {
        return Collections.unmodifiableCollection(listeners);
    }

    public void addListener(CatalogListener listener) {
        listeners.add(listener);
        Collections.sort(listeners, ExtensionPriority.COMPARATOR);
    }

    public void removeListener(CatalogListener listener) {
        listeners.remove(listener);
    }

    public void removeListeners(Class<? extends CatalogListener> listenerClass) {
        new ArrayList<>(listeners)
                .stream().filter(l -> listenerClass.isInstance(l)).forEach(l -> listeners.remove(l));
    }

    public void dispatch(CatalogEvent event) {
        CatalogException toThrow = null;

        for (CatalogListener listener : listeners) {
            try {
                if (event instanceof CatalogAddEvent addEvent1) {
                    listener.handleAddEvent(addEvent1);
                } else if (event instanceof CatalogRemoveEvent removeEvent) {
                    listener.handleRemoveEvent(removeEvent);
                } else if (event instanceof CatalogModifyEvent modifyEvent1) {
                    listener.handleModifyEvent(modifyEvent1);
                } else if (event instanceof CatalogPostModifyEvent modifyEvent) {
                    listener.handlePostModifyEvent(modifyEvent);
                } else if (event instanceof CatalogBeforeAddEvent addEvent) {
                    listener.handlePreAddEvent(addEvent);
                }
            } catch (Throwable t) {
                if (t instanceof CatalogException exception && toThrow == null) {
                    toThrow = exception;
                } else if (LOGGER.isLoggable(Level.WARNING)) {
                    LOGGER.log(Level.WARNING, "Catalog listener threw exception handling event.", t);
                }
            }
        }

        if (toThrow != null) {
            throw toThrow;
        }
    }

    public void syncTo(CatalogEventDispatcher dispatcher) {
        dispatcher.listeners = listeners;
    }
}
