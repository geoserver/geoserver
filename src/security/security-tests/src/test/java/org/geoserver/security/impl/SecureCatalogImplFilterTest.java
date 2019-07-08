/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.security.impl;

import static org.easymock.EasyMock.*;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.easymock.Capture;
import org.easymock.CaptureType;
import org.easymock.IAnswer;
import org.geoserver.catalog.*;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.catalog.util.CloseableIteratorAdapter;
import org.geoserver.security.*;
import org.hamcrest.Matcher;
import org.junit.Test;
import org.opengis.filter.Filter;
import org.opengis.filter.sort.SortBy;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public class SecureCatalogImplFilterTest {
    Authentication anonymous = new TestingAuthenticationToken("anonymous", null);
    ResourceAccessManager manager;

    static <T> List<T> collectAndClose(CloseableIterator<T> it) throws IOException {
        if (it == null) return null;
        try {
            LinkedList<T> list = new LinkedList<T>();
            while (it.hasNext()) {
                list.add(it.next());
            }
            return list;
        } finally {
            it.close();
        }
    }

    static <T> CloseableIterator<T> makeCIterator(List<T> source, Filter f) {
        return CloseableIteratorAdapter.filter(source.iterator(), f);
    }

    FeatureTypeInfo createMockFeatureType(
            String name,
            WorkspaceInfo ws,
            CatalogMode mode,
            Filter mockFilter,
            boolean read,
            boolean write) {
        DataStoreInfo mockStoreInfo = createMock(DataStoreInfo.class);
        FeatureTypeInfo mockFTInfo = createMock(FeatureTypeInfo.class);
        expect(mockFTInfo.getName()).andStubReturn(name);
        expect(mockFTInfo.getStore()).andStubReturn(mockStoreInfo);
        expect(mockStoreInfo.getWorkspace()).andStubReturn(ws);
        replay(mockStoreInfo);
        expect(manager.getAccessLimits(eq(anonymous), eq(mockFTInfo)))
                .andStubReturn(new VectorAccessLimits(mode, null, null, null, null));
        expect(mockFilter.evaluate(mockFTInfo))
                .andStubReturn(read || mode == CatalogMode.CHALLENGE);
        return mockFTInfo;
    }

    static Matcher<FeatureTypeInfo> matchFT(String name, WorkspaceInfo ws) {
        return allOf(
                hasProperty("name", is(name)),
                hasProperty("store", hasProperty("workspace", is(ws))));
    }

    @Test
    public void testFeatureTypeList() throws Exception {
        Catalog catalog = createMock(Catalog.class);

        manager = createMock(ResourceAccessManager.class);

        Filter mockFilter = createMock(Filter.class);
        expect(manager.getSecurityFilter(eq(anonymous), eq(FeatureTypeInfo.class)))
                .andStubReturn(mockFilter); // TODO

        final Capture<Filter> filterCapture = Capture.newInstance(CaptureType.LAST);

        final List<FeatureTypeInfo> source = new ArrayList<FeatureTypeInfo>();

        WorkspaceInfo mockWSInfo = createMock(WorkspaceInfo.class);
        expect(manager.getAccessLimits(eq(anonymous), eq(mockWSInfo)))
                .andStubReturn(new WorkspaceAccessLimits(CatalogMode.HIDE, true, false, false));

        FeatureTypeInfo mockFTInfo =
                createMockFeatureType("foo", mockWSInfo, CatalogMode.HIDE, mockFilter, true, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        mockFTInfo =
                createMockFeatureType(
                        "bar", mockWSInfo, CatalogMode.HIDE, mockFilter, false, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        mockFTInfo =
                createMockFeatureType(
                        "baz", mockWSInfo, CatalogMode.CHALLENGE, mockFilter, false, false);
        source.add(mockFTInfo);
        replay(mockFTInfo);

        expect(
                        catalog.list(
                                eq(FeatureTypeInfo.class),
                                capture(filterCapture),
                                (Integer) isNull(),
                                (Integer) isNull(),
                                (SortBy) isNull()))
                .andStubAnswer(
                        new IAnswer<CloseableIterator<FeatureTypeInfo>>() {

                            @Override
                            public CloseableIterator<FeatureTypeInfo> answer() throws Throwable {
                                Filter filter = filterCapture.getValue();
                                return CloseableIteratorAdapter.filter(source.iterator(), filter);
                            }
                        });

        replay(catalog, manager, mockFilter);

        @SuppressWarnings("serial")
        SecureCatalogImpl sc =
                new SecureCatalogImpl(catalog, manager) {
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

        List<FeatureTypeInfo> ftResult =
                collectAndClose(sc.list(FeatureTypeInfo.class, Predicates.acceptAll()));
        WorkspaceInfo foo = ftResult.get(0).getStore().getWorkspace();
        assertThat(ftResult, contains(matchFT("foo", mockWSInfo), matchFT("baz", mockWSInfo)));

        verify(catalog, manager);
    }
}
