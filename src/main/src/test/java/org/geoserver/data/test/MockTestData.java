/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import static org.easymock.EasyMock.expect;
import static org.easymock.classextension.EasyMock.*;
import static org.geoserver.security.SecurityUtils.toBytes;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.TreeSet;
import java.util.UUID;

import javax.crypto.spec.SecretKeySpec;
import javax.xml.namespace.QName;

import org.apache.commons.io.FileUtils;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.easymock.IArgumentMatcher;
import org.easymock.internal.LastControl;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.ResourcePool;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.impl.CatalogFactoryImpl;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.data.util.IOUtils;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerAuthenticationProvider;
import org.geoserver.security.GeoServerRoleStore;
import org.geoserver.security.GeoServerSecurityFilterChain;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.GeoServerUserGroupStore;
import org.geoserver.security.KeyStoreProvider;
import org.geoserver.security.KeyStoreProviderImpl;
import org.geoserver.security.MasterPasswordProvider;
import org.geoserver.security.config.PasswordPolicyConfig;
import org.geoserver.security.config.SecurityAuthProviderConfig;
import org.geoserver.security.config.SecurityFilterConfig;
import org.geoserver.security.config.SecurityUserGroupServiceConfig;
import org.geoserver.security.impl.GeoServerRole;
import org.geoserver.security.impl.GeoServerUser;
import org.geoserver.security.impl.GeoServerUserGroup;
import org.geoserver.security.password.GeoServerDigestPasswordEncoder;
import org.geoserver.security.password.GeoServerEmptyPasswordEncoder;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.security.password.GeoServerPasswordEncoder;
import org.geoserver.security.password.GeoServerPlainTextPasswordEncoder;
import org.geoserver.security.password.PasswordEncodingType;
import org.geoserver.security.password.PasswordValidator;
import org.geoserver.security.validation.PasswordValidatorImpl;
import org.geoserver.security.xml.XMLRoleService;
import org.geoserver.security.xml.XMLUserGroupService;
import org.geoserver.test.GeoServerMockTestSupport;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.data.FeatureSource;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.Version;
import org.opengis.feature.type.FeatureType;
import org.springframework.context.ApplicationContext;

/**
 * Test setup uses for GeoServer mock tests.
 * <p>
 * This is the default test setup used by {@link GeoServerMockTestSupport}. During setup this 
 * class creates a catalog whose contents contain all the layers defined by {@link CiteTestData}
 * </p>
 * <p>
 * Customizing the setup, adding layers, etc... is done from 
 * {@link GeoServerSystemTestSupport#setUpTestData}. 
 * </p>
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class MockTestData extends CiteTestData {

    File data;
    Catalog catalog;
    GeoServerSecurityManager secMgr;
    MockCreator mockCreator;
    boolean includeRaster;

    public MockTestData() throws IOException {
        // setup the root
        data = IOUtils.createRandomDirectory("./target", "mock", "data");
        data.delete();
        data.mkdir();

        mockCreator = new MockCreator();
    }

    public void setMockCreator(MockCreator mockCreator) {
        this.mockCreator = mockCreator;
    }

    public boolean isInludeRaster() {
        return includeRaster;
    }

    public void setIncludeRaster(boolean includeRaster) {
        this.includeRaster = includeRaster;
    }

    public Catalog getCatalog() {
        if (catalog == null) {
            try {
                catalog = mockCreator.createCatalog(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return catalog;
    }

    public GeoServerSecurityManager getSecurityManager() {
        if (secMgr == null) {
            try {
                secMgr = mockCreator.createSecurityManager(this);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
        return secMgr;
    }

    @Override
    public void setUp() throws Exception {
    }
 
    @Override
    public void tearDown() throws Exception {
        FileUtils.deleteDirectory(data);
    }
    
    @Override
    public File getDataDirectoryRoot() {
        return data;
    }
    
    @Override
    public boolean isTestDataAvailable() {
        return true;
    }
}
