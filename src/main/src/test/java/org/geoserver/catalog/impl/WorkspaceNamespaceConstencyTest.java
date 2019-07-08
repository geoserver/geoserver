/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog.impl;

import static org.easymock.EasyMock.*;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import org.easymock.EasyMock;
import org.easymock.IArgumentMatcher;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.NamespaceWorkspaceConsistencyListener;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.junit.Test;

public class WorkspaceNamespaceConstencyTest {

    @Test
    public void testChangeWorkspace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener((CatalogListener) anyObject());
        expectLastCall();

        NamespaceInfo ns = createMock(NamespaceInfo.class);
        ns.setPrefix("abcd");
        expectLastCall();

        expect(cat.getNamespaceByPrefix("gs")).andReturn(ns);

        cat.save(ns);
        expectLastCall();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(ws).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("name"));
        expect(e.getOldValues()).andReturn((List) Arrays.asList("gs"));
        expect(e.getNewValues()).andReturn((List) Arrays.asList("abcd"));

        replay(e, ws, ns, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);
        verify(ns, cat);
    }

    @Test
    public void testChangeNamespace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener((CatalogListener) anyObject());
        expectLastCall();

        WorkspaceInfo ws = createMock(WorkspaceInfo.class);
        ws.setName("abcd");
        expectLastCall();

        expect(cat.getWorkspaceByName("gs")).andReturn(ws);

        cat.save(ws);
        expectLastCall();

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(ns).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("prefix"));
        expect(e.getOldValues()).andReturn((List) Arrays.asList("gs"));
        expect(e.getNewValues()).andReturn((List) Arrays.asList("abcd"));

        replay(e, ws, ns, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);
        verify(ws, cat);
    }

    @Test
    public void testChangeDefaultWorkspace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener((CatalogListener) anyObject());
        expectLastCall();

        NamespaceInfo def = createNiceMock(NamespaceInfo.class);
        expect(cat.getDefaultNamespace()).andReturn(def);

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(cat.getNamespaceByPrefix("abcd")).andReturn(ns);

        cat.setDefaultNamespace(ns);
        expectLastCall();

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(ws.getName()).andReturn("abcd");

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(cat).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("defaultWorkspace"));
        expect(e.getNewValues()).andReturn((List) Arrays.asList(ws));

        replay(ns, ws, e, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);

        verify(ns, ws, cat);
    }

    @Test
    public void testChangeDefaultNamespace() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener((CatalogListener) anyObject());
        expectLastCall();

        WorkspaceInfo def = createNiceMock(WorkspaceInfo.class);
        expect(cat.getDefaultWorkspace()).andReturn(def);

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(cat.getWorkspaceByName("abcd")).andReturn(ws);

        cat.setDefaultWorkspace(ws);
        expectLastCall();

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(ns.getPrefix()).andReturn("abcd");

        CatalogModifyEvent e = createNiceMock(CatalogModifyEvent.class);
        expect(e.getSource()).andReturn(cat).anyTimes();
        expect(e.getPropertyNames()).andReturn(Arrays.asList("defaultNamespace"));
        expect(e.getNewValues()).andReturn((List) Arrays.asList(ns));

        replay(ns, ws, e, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handleModifyEvent(e);

        verify(ns, ws, cat);
    }

    @Test
    public void testChangeNamespaceURI() {
        Catalog cat = createMock(Catalog.class);
        cat.addListener((CatalogListener) anyObject());
        expectLastCall();

        NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        expect(ns.getPrefix()).andReturn("foo");
        expect(ns.getURI()).andReturn("http://foo.org");

        WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        expect(cat.getWorkspaceByName("foo")).andReturn(ws);

        DataStoreInfo ds = createNiceMock(DataStoreInfo.class);

        expect(cat.getDataStoresByWorkspace(ws)).andReturn(Arrays.asList(ds));

        HashMap params = new HashMap();
        params.put("namespace", "http://bar.org");
        expect(ds.getConnectionParameters()).andReturn(params).anyTimes();

        cat.save(hasNamespace("http://foo.org"));
        expectLastCall();

        CatalogPostModifyEvent e = createNiceMock(CatalogPostModifyEvent.class);
        expect(e.getSource()).andReturn(ns).anyTimes();
        expect(ns.getPrefix()).andReturn("foo");
        expect(cat.getWorkspaceByName("foo")).andReturn(ws);

        replay(ds, ws, ns, e, cat);

        new NamespaceWorkspaceConsistencyListener(cat).handlePostModifyEvent(e);
        verify(cat);
    }

    protected StoreInfo hasNamespace(final String namespace) {
        EasyMock.reportMatcher(
                new IArgumentMatcher() {
                    @Override
                    public boolean matches(Object argument) {
                        return namespace.equals(
                                ((StoreInfo) argument).getConnectionParameters().get("namespace"));
                    }

                    @Override
                    public void appendTo(StringBuffer buffer) {
                        buffer.append("hasNamespace '").append(namespace).append("'");
                    }
                });
        return null;
    }
}
