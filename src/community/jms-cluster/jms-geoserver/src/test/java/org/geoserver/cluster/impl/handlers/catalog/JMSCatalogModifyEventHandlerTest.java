/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.cluster.impl.handlers.catalog;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.IsInstanceOf.instanceOf;
import static org.junit.Assert.assertThat;

import com.thoughtworks.xstream.XStream;
import java.util.Arrays;
import org.geoserver.catalog.event.CatalogEvent;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.impl.CatalogModifyEventImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.hamcrest.CoreMatchers;
import org.junit.Test;

public final class JMSCatalogModifyEventHandlerTest {

    @Test
    public void testCatalogModifyEventHandling() throws Exception {
        // create a catalog modify event that include properties of type catalog
        CatalogModifyEventImpl catalogModifyEvent = new CatalogModifyEventImpl();
        catalogModifyEvent.setPropertyNames(
                Arrays.asList("propertyA", "propertyB", "propertyC", "propertyD"));
        catalogModifyEvent.setOldValues(Arrays.asList("value", new CatalogImpl(), 50, null));
        catalogModifyEvent.setNewValues(
                Arrays.asList("new_value", new CatalogImpl(), null, new CatalogImpl()));
        // serialise the event and deserialize it
        JMSCatalogModifyEventHandlerSPI handler =
                new JMSCatalogModifyEventHandlerSPI(0, null, new XStream(), null);
        String serializedEvent = handler.createHandler().serialize(catalogModifyEvent);
        CatalogEvent newEvent = handler.createHandler().deserialize(serializedEvent);
        // check the deserialized event
        assertThat(newEvent, notNullValue());
        assertThat(newEvent, instanceOf(CatalogModifyEvent.class));
        CatalogModifyEvent newModifyEvent = (CatalogModifyEvent) newEvent;
        // check properties names
        assertThat(newModifyEvent.getPropertyNames().size(), is(2));
        assertThat(
                newModifyEvent.getPropertyNames(), CoreMatchers.hasItems("propertyA", "propertyC"));
        // check old values
        assertThat(newModifyEvent.getOldValues().size(), is(2));
        assertThat(newModifyEvent.getOldValues(), CoreMatchers.hasItems("value", 50));
        // check new values
        assertThat(newModifyEvent.getNewValues().size(), is(2));
        assertThat(newModifyEvent.getNewValues(), CoreMatchers.hasItems("new_value", null));
    }
}
