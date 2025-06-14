/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.capture;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.eq;
import static org.easymock.EasyMock.expect;
import static org.easymock.EasyMock.isNull;
import static org.easymock.EasyMock.replay;
import static org.easymock.EasyMock.verify;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.security.CatalogMode;
import org.geoserver.security.ResourceAccessManager;
import org.geoserver.security.SecureCatalogImpl;
import org.geoserver.security.VectorAccessLimits;
import org.geoserver.security.WorkspaceAccessLimits;
import org.geotools.api.filter.Filter;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecureCatalogImplFilterTest {
    Authentication anonymous = new TestingAuthenticationToken("anonymous", null);
    ResourceAccessManager manager;

    static <T> List<T> collectAndClose(CloseableIterator<T> it) throws IOException {
        if (it == null) return null;
        try (it) {
            LinkedList<T> list = new LinkedList<>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        }
    }

    static <T> CloseableIterator<T> makeCIterator(List<T> source, Filter f) {
        return CloseableIteratorAdapter.filter(source.iterator(), f);
    }

    FeatureTypeInfo createMockFeatureType(
            String name, WorkspaceInfo ws, CatalogMode mode, Filter mockFilter, boolean read, boolean write) {
        DataStoreInfo mockStoreInfo = createMock(DataStoreInfo.class);
        FeatureTypeInfo mockFTInfo = createMock(FeatureTypeInfo.class);
        expect(mockFTInfo.getName()).andStubReturn(name);
        expect(mockFTInfo.getStore()).andStubReturn(mockStoreInfo);
        expect(mockStoreInfo.getWorkspace()).andStubReturn(ws);
        replay(mockStoreInfo);
        expect(manager.getAccessLimits(eq(anonymous), eq(mockFTInfo)))
                .andStubReturn(new VectorAccessLimits(mode, null, null, null, null));
        expect(mockFilter.evaluate(mockFTInfo)).andStubReturn(read || mode == CatalogMode.CHALLENGE);
        return mockFTInfo;
    }

    static Matcher<FeatureTypeInfo> matchFT(String name, WorkspaceInfo ws) {
        return allOf(hasProperty("name", is(name)), hasProperty("store", hasProperty("workspace", is(ws))));
    }

    @Test
    public void testFeatureTypeList() throws Exception {
        Catalog catalog = createMock(Catalog.class);

        manager = createMock(ResourceAccessManager.class);

        Filter mockFilter = createMock(Filter.class);
        expect(manager.getSecurityFilter(eq(anonymous), eq(FeatureTypeInfo.class)))
                .andStubReturn(mockFilter); // TODO

        final Capture<Filter> filterCapture = Capture.newInstance(CaptureType.LAST);

        final List<FeatureTypeInfo> source = new ArrayList<>();

        WorkspaceInfo mockWSInfo = createMock(WorkspaceInfo.class);
        expect(manager.getAccessLimits(eq(anonymous), eq(mockWSInfo)))
                .andStubReturn(new WorkspaceAccessLimits(CatalogMode.HIDE, true, false, false));

        FeatureTypeInfo mockFTInfo =
                createMockFeatureType("foo", mockWSInfo, CatalogMode.HIDE, mockFilter, true, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        mockFTInfo = createMockFeatureType("bar", mockWSInfo, CatalogMode.HIDE, mockFilter, false, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        mockFTInfo = createMockFeatureType("baz", mockWSInfo, CatalogMode.CHALLENGE, mockFilter, false, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        expect(catalog.list(eq(FeatureTypeInfo.class), capture(filterCapture), isNull(), isNull(), isNull()))
                .andStubAnswer(() -> {
                    Filter filter = filterCapture.getValue();
                    return CloseableIteratorAdapter.filter(source.iterator(), filter);
                });

        replay(catalog, manager, mockFilter);

        @SuppressWarnings("serial")
        SecureCatalogImpl sc = new SecureCatalogImpl(catalog, manager) {
            // Calls static method we can't mock
            @Override
            protected boolean isAdmin(Authentication authentication) {
                return false;
            }

            // Not relevant to the test ad complicates things due to static calls
            @Override
            protected <T extends CatalogInfo> T checkAccess(
                    Authentication user, T info, MixedModeBehavior mixedModeBehavior) {
                return info;
            }
        };

        // use no user at all
        SecurityContextHolder.getContext().setAuthentication(anonymous);

        List<FeatureTypeInfo> ftResult = collectAndClose(sc.list(FeatureTypeInfo.class, Predicates.acceptAll()));
        ftResult.get(0).getStore().getWorkspace();
        assertThat(ftResult, contains(matchFT("foo", mockWSInfo), matchFT("baz", mockWSInfo)));

        verify(catalog, manager);
    }
}
