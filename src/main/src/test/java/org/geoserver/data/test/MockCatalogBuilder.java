/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.data.test;

import static org.easymock.EasyMock.*;
import static org.easymock.EasyMock.createMock;
import static org.easymock.EasyMock.createNiceMock;
import static org.easymock.EasyMock.replay;
import static org.geoserver.data.test.CiteTestData.DEFAULT_LATLON_ENVELOPE;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;
import javax.xml.namespace.QName;
import org.easymock.EasyMock;
import org.easymock.IAnswer;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.CoverageStoreInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.Keyword;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.impl.CatalogImpl;
import org.geoserver.ows.util.OwsUtils;
import org.geoserver.util.IOUtils;
import org.geotools.coverage.grid.io.AbstractGridFormat;
import org.geotools.coverage.grid.io.GridCoverage2DReader;
import org.geotools.coverage.grid.io.GridFormatFinder;
import org.geotools.data.DataAccess;
import org.geotools.data.DataStore;
import org.geotools.data.FeatureSource;
import org.geotools.data.property.PropertyDataStore;
import org.geotools.data.property.PropertyDataStoreFactory;
import org.geotools.feature.NameImpl;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.URLs;
import org.geotools.util.Version;
import org.opengis.feature.type.FeatureType;

public class MockCatalogBuilder {
    public static interface Callback {
        void onWorkspace(String name, WorkspaceInfo ws, MockCatalogBuilder b);

        void onStore(String name, StoreInfo st, WorkspaceInfo ws, MockCatalogBuilder b);

        void onResource(String name, ResourceInfo r, StoreInfo s, MockCatalogBuilder b);

        void onLayer(String name, LayerInfo l, MockCatalogBuilder b);

        void onStyle(String name, StyleInfo s, MockCatalogBuilder b);

        void onLayerGroup(String name, LayerGroupInfo lg, MockCatalogBuilder b);
    }

    Catalog catalog;
    File dataDirRoot;
    Callback callback;

    LinkedList<WorkspaceInfo> workspaces = new LinkedList();
    LinkedList<NamespaceInfo> namespaces = new LinkedList();
    LinkedList<DataStoreInfo> dataStores = new LinkedList();
    LinkedList<CoverageStoreInfo> coverageStores = new LinkedList();
    LinkedList<FeatureTypeInfo> featureTypes = new LinkedList();
    LinkedList<CoverageInfo> coverages = new LinkedList();
    LinkedList<LayerInfo> layers = new LinkedList();
    LinkedList<StyleInfo> styles = new LinkedList();
    LinkedList<LayerGroupInfo> layerGroups = new LinkedList();

    LinkedList<FeatureTypeInfo> featureTypesByNamespace = new LinkedList();
    LinkedList<FeatureTypeInfo> featureTypesAll = new LinkedList();
    LinkedList<CoverageInfo> coveragesByNamespace = new LinkedList();
    LinkedList<CoverageInfo> coveragesAll = new LinkedList();
    LinkedList<DataStoreInfo> dataStoresAll = new LinkedList();
    LinkedList<CoverageStoreInfo> coverageStoresAll = new LinkedList();

    public MockCatalogBuilder(Catalog catalog, File dataDirRoot) {
        this.catalog = catalog;
        this.dataDirRoot = dataDirRoot;
        this.callback = createNiceMock(Callback.class);
    }

    public void setCallback(Callback callback) {
        this.callback = callback;
    }

    public Catalog getCatalog() {
        return catalog;
    }

    public MockCatalogBuilder workspace(String name, String uri) {
        String wsId = newId();
        String nsId = newId();

        final WorkspaceInfo ws = createNiceMock(WorkspaceInfo.class);
        workspaces.add(ws);
        expect(ws.getId()).andReturn(wsId).anyTimes();
        expect(ws.getName()).andReturn(name).anyTimes();
        expect(ws.getMetadata()).andReturn(new MetadataMap()).anyTimes();

        expect(catalog.getWorkspace(wsId)).andReturn(ws).anyTimes();
        expect(catalog.getWorkspaceByName(name)).andReturn(ws).anyTimes();

        final NamespaceInfo ns = createNiceMock(NamespaceInfo.class);
        namespaces.add(ns);

        expect(ns.getId()).andReturn(nsId).anyTimes();
        expect(ns.getName()).andReturn(name).anyTimes();
        expect(ns.getPrefix()).andReturn(name).anyTimes();
        expect(ns.getMetadata()).andReturn(new MetadataMap()).anyTimes();
        expect(ns.getURI()).andReturn(uri).anyTimes();

        expect(catalog.getNamespace(nsId)).andReturn(ns).anyTimes();
        expect(catalog.getNamespaceByPrefix(name)).andReturn(ns).anyTimes();
        expect(catalog.getNamespaceByURI(uri)).andReturn(ns).anyTimes();

        ws.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(ws);
                            }
                        })
                .anyTimes();

        ns.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(ns);
                            }
                        })
                .anyTimes();

        callback.onWorkspace(name, ws, this);

        replay(ws, ns);
        return this;
    }

    public MockCatalogBuilder dataStore(String name) {
        String dsId = newId();
        final WorkspaceInfo ws = workspaces.peekLast();
        final NamespaceInfo ns = namespaces.peekLast();

        final DataStoreInfo ds = createNiceMock(DataStoreInfo.class);
        dataStores.add(ds);

        initStore(ds, DataStoreInfo.class, dsId, name, ws);

        // setup the property data store
        final File propDir = new File(dataDirRoot, name);

        HashMap cxParams = new HashMap();
        cxParams.put(PropertyDataStoreFactory.DIRECTORY.key, propDir);
        cxParams.put(PropertyDataStoreFactory.NAMESPACE.key, ns.getURI());
        expect(ds.getConnectionParameters()).andReturn(cxParams).anyTimes();

        try {
            expect(ds.getDataStore(null))
                    .andAnswer(
                            (IAnswer)
                                    new IAnswer<DataAccess>() {
                                        @Override
                                        public DataAccess answer() throws Throwable {
                                            return new PropertyDataStore(propDir, ns.getURI());
                                        }
                                    })
                    .anyTimes();
        } catch (IOException e) {
        }

        expect(catalog.getDataStore(dsId)).andReturn(ds).anyTimes();
        expect(catalog.getDataStoreByName(name)).andReturn(ds).anyTimes();
        expect(catalog.getDataStoreByName(ws.getName(), name)).andReturn(ds).anyTimes();
        expect(catalog.getDataStoreByName(ws, name)).andReturn(ds).anyTimes();

        ds.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(ds);
                            }
                        })
                .anyTimes();

        callback.onStore(name, ds, ws, this);
        replay(ds);
        return this;
    }

    public MockCatalogBuilder coverageStore(String name, String filename, String format) {
        String csId = newId();
        WorkspaceInfo ws = workspaces.peekLast();
        NamespaceInfo ns = namespaces.peekLast();

        final CoverageStoreInfo cs = createNiceMock(CoverageStoreInfo.class);
        coverageStores.add(cs);

        initStore(cs, CoverageStoreInfo.class, csId, name, ws);

        File covDir = new File(dataDirRoot, name);
        final File covFile = new File(covDir, filename);
        expect(cs.getURL()).andReturn(URLs.fileToUrl(covFile).toString()).anyTimes();
        expect(cs.getType())
                .andAnswer(
                        new IAnswer<String>() {
                            @Override
                            public String answer() throws Throwable {
                                return lookupGridFormat(covFile).getName();
                            }
                        })
                .anyTimes();
        expect(cs.getFormat())
                .andAnswer(
                        new IAnswer<AbstractGridFormat>() {
                            @Override
                            public AbstractGridFormat answer() throws Throwable {
                                return lookupGridFormat(covFile);
                            }
                        })
                .anyTimes();
        expect(cs.getConnectionParameters()).andReturn(new HashMap()).anyTimes();

        expect(catalog.getCoverageStore(csId)).andReturn(cs).anyTimes();
        expect(catalog.getCoverageStoreByName(name)).andReturn(cs).anyTimes();
        expect(catalog.getCoverageStoreByName(ws.getName(), name)).andReturn(cs).anyTimes();
        expect(catalog.getCoverageStoreByName(ws, name)).andReturn(cs).anyTimes();

        cs.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(cs);
                            }
                        })
                .anyTimes();

        callback.onStore(name, cs, ws, this);
        replay(cs);
        return this;
    }

    AbstractGridFormat lookupGridFormat(Object obj) {
        AbstractGridFormat format = GridFormatFinder.findFormat(obj);
        if (format == null) {
            throw new RuntimeException("No format for " + obj);
        }
        return format;
    }

    <T extends StoreInfo> void initStore(
            T s, Class<T> clazz, String sId, String name, WorkspaceInfo ws) {
        expect(s.getId()).andReturn(sId).anyTimes();
        expect(s.getName()).andReturn(name).anyTimes();
        expect(s.getWorkspace()).andReturn(ws).anyTimes();
        expect(s.getCatalog()).andReturn(catalog).anyTimes();
        expect(s.isEnabled()).andReturn(true).anyTimes();

        expect(catalog.getStore(sId, clazz)).andReturn(s).anyTimes();
        expect(catalog.getStore(sId, StoreInfo.class)).andReturn(s).anyTimes();

        expect(catalog.getStoreByName(name, clazz)).andReturn(s).anyTimes();
        expect(catalog.getStoreByName(name, StoreInfo.class)).andReturn(s).anyTimes();

        expect(catalog.getStoreByName(ws.getName(), name, clazz)).andReturn(s).anyTimes();
        expect(catalog.getStoreByName(ws.getName(), name, StoreInfo.class)).andReturn(s).anyTimes();

        expect(catalog.getStoreByName(ws, name, clazz)).andReturn(s).anyTimes();
        expect(catalog.getStoreByName(ws, name, StoreInfo.class)).andReturn(s).anyTimes();
    }

    public MockCatalogBuilder featureType(String name) {
        return featureType(name, null, ProjectionPolicy.NONE, null, DEFAULT_LATLON_ENVELOPE);
    }

    public MockCatalogBuilder featureType(
            final String name,
            String srs,
            ProjectionPolicy projPolicy,
            ReferencedEnvelope envelope,
            ReferencedEnvelope latLonEnvelope) {

        String ftId = newId();
        final DataStoreInfo ds = dataStores.peekLast();
        NamespaceInfo ns = namespaces.peekLast();

        final FeatureTypeInfo ft = createNiceMock(FeatureTypeInfo.class);
        featureTypes.add(ft);

        initResource(
                ft,
                FeatureTypeInfo.class,
                ftId,
                name,
                ds,
                ns,
                srs,
                projPolicy,
                envelope,
                latLonEnvelope);

        expect(ft.getNumDecimals()).andReturn(8);

        // setup the property file data
        File propDir = new File(dataDirRoot, ds.getName());
        propDir.mkdirs();

        String fileName = name + ".properties";
        try {
            IOUtils.copy(getClass().getResourceAsStream(fileName), new File(propDir, fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try {
            expect(ft.getFeatureType())
                    .andAnswer(
                            new IAnswer<FeatureType>() {
                                @Override
                                public FeatureType answer() throws Throwable {
                                    return ((DataStore) ds.getDataStore(null)).getSchema(name);
                                }
                            })
                    .anyTimes();
            expect(ft.getFeatureSource(null, null))
                    .andAnswer(
                            (IAnswer)
                                    new IAnswer<FeatureSource>() {
                                        @Override
                                        public FeatureSource answer() throws Throwable {
                                            return ((DataStore) ds.getDataStore(null))
                                                    .getFeatureSource(name);
                                        }
                                    })
                    .anyTimes();
        } catch (IOException e) {
        }

        expect(catalog.getFeatureTypeByName(or(eq(name), eq(ns.getPrefix() + ":" + name))))
                .andReturn(ft)
                .anyTimes();

        expect(
                        catalog.getFeatureTypeByName(
                                or(
                                        eq(new NameImpl(ns.getPrefix(), name)),
                                        eq(new NameImpl(ns.getURI(), name)))))
                .andReturn(ft)
                .anyTimes();
        expect(catalog.getFeatureTypeByName(ns, name)).andReturn(ft).anyTimes();

        expect(catalog.getFeatureTypeByName(ns.getPrefix(), name)).andReturn(ft).anyTimes();
        // expect(catalog.getFeatureTypeByName(or(eq(ns.getPrefix()), eq(ns.getURI())), name))
        //    .andReturn(ft).anyTimes();

        expect(catalog.getFeatureTypeByDataStore(ds, name)).andReturn(ft).anyTimes();

        ft.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(ft);
                            }
                        })
                .anyTimes();

        callback.onResource(name, ft, ds, this);
        replay(ft, createLayer(ft, name, ns));
        return this;
    }

    public MockCatalogBuilder coverage(QName qName, String fileName, String srs, Class scope) {
        scope = scope != null ? scope : getClass();

        String cId = newId();
        final CoverageStoreInfo cs = coverageStores.peekLast();
        NamespaceInfo ns = namespaces.peekLast();

        final String name = qName.getLocalPart();
        File dir = new File(dataDirRoot, name);
        dir.mkdir();

        try {
            IOUtils.copy(scope.getResourceAsStream(fileName), new File(dir, fileName));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        // initialize the mock by actually building a real one first
        CatalogBuilder cb = new CatalogBuilder(new CatalogImpl());
        cb.setStore(cs);

        GridCoverage2DReader reader = cs.getFormat().getReader(cs.getURL());
        if (reader == null) {
            throw new RuntimeException("No reader for " + cs.getURL());
        }

        CoverageInfo real = null;
        try {
            real = cb.buildCoverage(reader, null);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        final CoverageInfo c = createNiceMock(CoverageInfo.class);
        coverages.add(c);
        final List<CoverageInfo> coverageList = coverages;

        if (srs == null) {
            srs = real.getSRS();
        }
        initResource(
                c,
                CoverageInfo.class,
                cId,
                name,
                cs,
                ns,
                srs,
                real.getProjectionPolicy(),
                real.getNativeBoundingBox(),
                real.getLatLonBoundingBox());

        expect(c.getDefaultInterpolationMethod())
                .andReturn(real.getDefaultInterpolationMethod())
                .anyTimes();
        expect(c.getDimensions()).andReturn(real.getDimensions()).anyTimes();
        expect(c.getGrid()).andReturn(real.getGrid()).anyTimes();

        expect(c.getInterpolationMethods()).andReturn(real.getInterpolationMethods()).anyTimes();
        expect(c.getRequestSRS()).andReturn(real.getRequestSRS()).anyTimes();
        expect(c.getResponseSRS()).andReturn(real.getResponseSRS()).anyTimes();

        try {
            expect(c.getGridCoverageReader(null, null)).andReturn(reader).anyTimes();
        } catch (IOException e) {
        }

        expect(catalog.getCoverageByName(or(eq(name), eq(ns.getPrefix() + ":" + name))))
                .andReturn(c)
                .anyTimes();
        expect(
                        catalog.getCoverageByName(
                                or(
                                        eq(new NameImpl(ns.getPrefix(), name)),
                                        eq(new NameImpl(ns.getURI(), name)))))
                .andReturn(c)
                .anyTimes();
        expect(catalog.getCoverageByName(ns, name)).andReturn(c).anyTimes();

        expect(catalog.getCoverageByName(ns.getPrefix(), name)).andReturn(c).anyTimes();
        // expect(catalog.getFeatureTypeByName(or(eq(ns.getPrefix()), eq(ns.getURI())), name))
        //    .andReturn(ft).anyTimes();

        // expect(catalog.getCoverageByStore(cs, name)).andReturn(c).anyTimes();
        expect(catalog.getCoveragesByStore(cs)).andReturn(coverageList).anyTimes();
        expect(catalog.getCoverageByCoverageStore(cs, name)).andReturn(c).anyTimes();

        c.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(c);
                            }
                        })
                .anyTimes();

        callback.onResource(name, c, cs, this);
        replay(c, createLayer(c, name, ns));
        return this;
    }

    <T extends ResourceInfo> void initResource(
            T r,
            Class<T> clazz,
            String rId,
            String name,
            StoreInfo s,
            NamespaceInfo ns,
            String srs,
            ProjectionPolicy projPolicy,
            ReferencedEnvelope envelope,
            ReferencedEnvelope latLonEnvelope) {

        expect(r.getId()).andReturn(rId).anyTimes();
        expect(r.getName()).andReturn(name).anyTimes();
        expect(r.getQualifiedName()).andReturn(new NameImpl(ns.getURI(), name)).anyTimes();
        expect(r.getNativeName()).andReturn(name).anyTimes();
        expect(r.getQualifiedNativeName()).andReturn(new NameImpl(ns.getURI(), name)).anyTimes();
        expect(r.getTitle()).andReturn(name).anyTimes();
        expect(r.getAbstract()).andReturn("abstract about " + name).anyTimes();
        expect(r.getStore()).andReturn(s).anyTimes();
        expect(r.getNamespace()).andReturn(ns).anyTimes();

        srs = srs != null ? srs : "EPSG:4326";
        expect(r.getSRS()).andReturn(srs).anyTimes();
        try {
            expect(r.getNativeCRS()).andReturn(CRS.decode(srs));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        expect(r.getKeywords()).andReturn((List) Arrays.asList(new Keyword(name))).anyTimes();
        expect(r.isEnabled()).andReturn(true).anyTimes();
        expect(r.isAdvertised()).andReturn(true).anyTimes();
        expect(r.getProjectionPolicy()).andReturn(projPolicy).anyTimes();
        expect(r.getLatLonBoundingBox()).andReturn(latLonEnvelope).anyTimes();
        ;
        expect(r.getNativeBoundingBox()).andReturn(envelope).anyTimes();

        expect(catalog.getResource(rId, clazz)).andReturn(r).anyTimes();

        expect(catalog.getResourceByName(ns.getPrefix() + ":" + name, clazz))
                .andReturn(r)
                .anyTimes();
        expect(catalog.getResourceByName(ns.getPrefix() + ":" + name, ResourceInfo.class))
                .andReturn(r)
                .anyTimes();

        expect(catalog.getResourceByName(name, clazz)).andReturn(r).anyTimes();
        expect(catalog.getResourceByName(name, ResourceInfo.class)).andReturn(r).anyTimes();

        expect(catalog.getResourceByName(new NameImpl(ns.getPrefix(), name), clazz))
                .andReturn(r)
                .anyTimes();
        expect(catalog.getResourceByName(new NameImpl(ns.getPrefix(), name), ResourceInfo.class))
                .andReturn(r)
                .anyTimes();

        expect(catalog.getResourceByName(new NameImpl(ns.getURI(), name), clazz))
                .andReturn(r)
                .anyTimes();
        expect(catalog.getResourceByName(new NameImpl(ns.getURI(), name), ResourceInfo.class))
                .andReturn(r)
                .anyTimes();

        expect(catalog.getResourceByName(ns, name, clazz)).andReturn(r).andReturn(r).anyTimes();
        expect(catalog.getResourceByName(ns, name, ResourceInfo.class))
                .andReturn(r)
                .andReturn(r)
                .anyTimes();

        expect(catalog.getResourceByName(ns.getPrefix(), name, clazz)).andReturn(r).anyTimes();
        expect(catalog.getResourceByName(ns.getPrefix(), name, ResourceInfo.class))
                .andReturn(r)
                .anyTimes();
        // expect(catalog.getResourceByName(or(eq(ns.getPrefix()), eq(ns.getURI())), name,
        //    clazz)).andReturn(r).anyTimes();
        expect(catalog.getResourceByStore(s, name, clazz)).andReturn(r).anyTimes();
        expect(catalog.getResourceByStore(s, name, ResourceInfo.class)).andReturn(r).anyTimes();
    }

    LayerInfo createLayer(ResourceInfo r, String name, NamespaceInfo ns) {
        String lId = newId();
        StyleInfo s = styles.peekLast();

        final LayerInfo l = createNiceMock(LayerInfo.class);
        layers.add(l);

        expect(l.getId()).andReturn(lId).anyTimes();
        expect(l.getName()).andReturn(name).anyTimes();
        expect(l.getType()).andReturn(PublishedType.VECTOR).anyTimes();
        expect(l.getResource()).andReturn(r).anyTimes();
        expect(l.getDefaultStyle()).andReturn(s).anyTimes();
        expect(l.getStyles()).andReturn(Collections.singleton(s)).anyTimes();
        expect(l.isEnabled()).andReturn(true).anyTimes();
        expect(l.isAdvertised()).andReturn(true).anyTimes();

        expect(catalog.getLayer(lId)).andReturn(l).anyTimes();
        expect(catalog.getLayerByName(name)).andReturn(l).anyTimes();
        expect(catalog.getLayerByName(ns.getPrefix() + ":" + name)).andReturn(l).anyTimes();
        expect(catalog.getLayerByName(new NameImpl(ns.getPrefix(), name))).andReturn(l).anyTimes();
        expect(catalog.getLayerByName(new NameImpl(ns.getURI(), name))).andReturn(l).anyTimes();
        expect(catalog.getLayers(r)).andReturn(Arrays.asList(l)).anyTimes();
        l.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(l);
                            }
                        })
                .anyTimes();

        callback.onLayer(name, l, this);
        return l;
    }

    public MockCatalogBuilder style(String name) {
        String filename = name + ".sld";
        if (getClass().getResourceAsStream(filename) == null) {
            return this;
        }

        String sId = newId();
        String format = SLDHandler.FORMAT;
        Version version = SLDHandler.VERSION_10;

        final StyleInfo s = createNiceMock(StyleInfo.class);
        styles.add(s);

        expect(s.getId()).andReturn(sId);
        expect(s.getName()).andReturn(name).anyTimes();
        expect(s.getFilename()).andReturn(filename).anyTimes();
        expect(s.getFormatVersion()).andReturn(version).anyTimes();
        try {
            expect(s.getStyle())
                    .andReturn(
                            Styles.style(
                                    new SLDHandler()
                                            .parse(
                                                    getClass().getResourceAsStream(filename),
                                                    version,
                                                    null,
                                                    null)))
                    .anyTimes();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        expect(catalog.getStyle(sId)).andReturn(s).anyTimes();
        expect(catalog.getStyleByName(name)).andReturn(s).anyTimes();

        s.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(s);
                            }
                        })
                .anyTimes();

        callback.onStyle(name, s, this);
        replay(s);
        return this;
    }

    public MockCatalogBuilder layerGroup(
            String name, List<String> layerNames, List<String> styleNames) {

        final LayerGroupInfo lg = createMock(LayerGroupInfo.class);
        layerGroups.add(lg);

        expect(lg.getId()).andReturn(newId()).anyTimes();
        expect(lg.getName()).andReturn(name).anyTimes();

        List<PublishedInfo> grpLayers = new ArrayList<PublishedInfo>();
        List<StyleInfo> grpStyles = new ArrayList<StyleInfo>();
        for (int i = 0; i < layerNames.size(); i++) {
            String layerName = layerNames.get(i);
            LayerInfo l = null;
            for (LayerInfo layer : layers) {
                if (layerName.equals(layer.getName())) {
                    l = layer;
                    break;
                }
            }

            if (l == null) {
                throw new RuntimeException("No such layer: " + layerName);
            }

            grpLayers.add(l);

            StyleInfo s = null;
            if (styleNames != null) {
                String styleName = styleNames.get(i);
                for (StyleInfo style : styles) {
                    if (styleName.equals(style.getName())) {
                        s = style;
                        break;
                    }
                }
            }

            grpStyles.add(s);
        }
        expect(lg.getLayers()).andReturn(grpLayers).anyTimes();
        expect(lg.getStyles()).andReturn(grpStyles).anyTimes();

        expect(lg.getRootLayer()).andReturn(null).anyTimes();
        expect(lg.getRootLayerStyle()).andReturn(null).anyTimes();

        lg.accept((CatalogVisitor) anyObject());
        expectLastCall()
                .andAnswer(
                        new VisitAnswer() {
                            @Override
                            protected void doVisit(CatalogVisitor visitor) {
                                visitor.visit(lg);
                            }
                        })
                .anyTimes();

        expect(catalog.getLayerGroupByName(name)).andReturn(lg).anyTimes();

        callback.onLayerGroup(name, lg, this);
        replay(lg);
        return this;
    }

    public MockCatalogBuilder commit() {
        if (!featureTypes.isEmpty() || !coverages.isEmpty()) {
            if (!featureTypes.isEmpty()) {
                DataStoreInfo ds = dataStores.peekLast();

                expect(catalog.getResourcesByStore(ds, FeatureTypeInfo.class))
                        .andReturn(featureTypes)
                        .anyTimes();
                expect(catalog.getResourcesByStore(ds, ResourceInfo.class))
                        .andReturn((List) featureTypes)
                        .anyTimes();
                expect(catalog.getFeatureTypesByDataStore(ds)).andReturn(featureTypes).anyTimes();
            }

            if (!coverages.isEmpty()) {
                CoverageStoreInfo cs = coverageStores.peekLast();

                expect(catalog.getResourcesByStore(cs, CoverageInfo.class))
                        .andReturn(coverages)
                        .anyTimes();
                expect(catalog.getResourcesByStore(cs, ResourceInfo.class))
                        .andReturn((List) coverages)
                        .anyTimes();
                expect(catalog.getCoveragesByCoverageStore(cs)).andReturn(coverages).anyTimes();
            }

            // clear out local lists but push up to be included when this workspace is complete
            featureTypesByNamespace.addAll(featureTypes);
            featureTypes = new LinkedList<FeatureTypeInfo>();

            coveragesByNamespace.addAll(coverages);
            coverages = new LinkedList<CoverageInfo>();

        } else if (!dataStores.isEmpty() || !coverageStores.isEmpty()) {
            WorkspaceInfo ws = workspaces.peekLast();
            NamespaceInfo ns = namespaces.peekLast();

            expect(catalog.getStoresByWorkspace(ws.getName(), DataStoreInfo.class))
                    .andReturn(dataStores)
                    .anyTimes();
            expect(catalog.getStoresByWorkspace(ws, DataStoreInfo.class))
                    .andReturn(dataStores)
                    .anyTimes();
            expect(catalog.getDataStoresByWorkspace(ws.getName())).andReturn(dataStores).anyTimes();
            expect(catalog.getDataStoresByWorkspace(ws)).andReturn(dataStores).anyTimes();

            expect(catalog.getStoresByWorkspace(ws.getName(), CoverageStoreInfo.class))
                    .andReturn(coverageStores)
                    .anyTimes();
            expect(catalog.getStoresByWorkspace(ws, CoverageStoreInfo.class))
                    .andReturn(coverageStores)
                    .anyTimes();
            expect(catalog.getCoverageStoresByWorkspace(ws.getName()))
                    .andReturn(coverageStores)
                    .anyTimes();
            expect(catalog.getCoverageStoresByWorkspace(ws)).andReturn(coverageStores).anyTimes();

            List<StoreInfo> l = new LinkedList<StoreInfo>(dataStores);
            l.addAll(coverageStores);

            expect(catalog.getStoresByWorkspace(ws.getName(), StoreInfo.class))
                    .andReturn(l)
                    .anyTimes();
            expect(catalog.getStoresByWorkspace(ws, StoreInfo.class)).andReturn(l).anyTimes();

            expect(catalog.getStylesByWorkspace(ws.getName())).andReturn(styles).anyTimes();
            expect(catalog.getStylesByWorkspace(ws)).andReturn(styles).anyTimes();

            expect(catalog.getLayerGroupsByWorkspace(ws.getName()))
                    .andReturn(layerGroups)
                    .anyTimes();
            expect(catalog.getLayerGroupsByWorkspace(ws)).andReturn(layerGroups).anyTimes();

            // add all the resources for this workspace
            List<ResourceInfo> m = new LinkedList(featureTypesByNamespace);
            m.addAll(coveragesByNamespace);
            expect(catalog.getResourcesByNamespace(ns, ResourceInfo.class)).andReturn(m).anyTimes();
            // expect(catalog.getResourcesByNamespace(ns.getPrefix(),
            // ResourceInfo.class)).andReturn(m).anyTimes();
            expect(catalog.getResourcesByNamespace(ns, FeatureTypeInfo.class))
                    .andReturn(featureTypesByNamespace)
                    .anyTimes();
            // expect(catalog.getResourcesByNamespace(ns.getPrefix(), FeatureTypeInfo.class))
            //    .andReturn(featureTypesByNamespace).anyTimes();
            expect(catalog.getResourcesByNamespace(ns, CoverageInfo.class))
                    .andReturn(coveragesByNamespace)
                    .anyTimes();
            // expect(catalog.getResourcesByNamespace(ns.getPrefix(), CoverageInfo.class))
            //    .andReturn(coveragesByNamespace).anyTimes();

            dataStoresAll.addAll(dataStores);
            dataStores = new LinkedList();

            coverageStoresAll.addAll(coverageStores);
            coverageStores = new LinkedList();

            featureTypesAll.addAll(featureTypesByNamespace);
            featureTypesByNamespace = new LinkedList();

            coveragesAll.addAll(coveragesByNamespace);
            coveragesByNamespace = new LinkedList();
        } else if (!workspaces.isEmpty()) {

            // all the resources
            List<ResourceInfo> l = new LinkedList<ResourceInfo>(featureTypesAll);
            l.addAll(coveragesAll);
            expect(catalog.getResources(ResourceInfo.class)).andReturn(l).anyTimes();
            expect(catalog.getResources(FeatureTypeInfo.class))
                    .andReturn(featureTypesAll)
                    .anyTimes();
            expect(catalog.getResources(CoverageInfo.class)).andReturn(coverages).anyTimes();
            expect(catalog.getFeatureTypes()).andReturn(featureTypesAll).anyTimes();
            expect(catalog.getCoverages()).andReturn(coveragesAll).anyTimes();

            // add all the stores
            List<StoreInfo> m = new LinkedList<StoreInfo>(dataStoresAll);
            m.addAll(coverageStoresAll);
            expect(catalog.getStores(StoreInfo.class)).andReturn(m).anyTimes();
            expect(catalog.getStores(DataStoreInfo.class)).andReturn(dataStoresAll).anyTimes();
            expect(catalog.getStores(CoverageStoreInfo.class))
                    .andReturn(coverageStoresAll)
                    .anyTimes();

            // add all the layers
            expect(catalog.getLayers()).andReturn(layers).anyTimes();

            // add all the styles
            expect(catalog.getStyles()).andReturn(styles).anyTimes();

            // add all the layer groups
            expect(catalog.getLayerGroups()).andReturn(layerGroups).anyTimes();

            // add all the workspaces/namespaces
            expect(catalog.getWorkspaces()).andReturn(workspaces).anyTimes();
            expect(catalog.getNamespaces()).andReturn(namespaces).anyTimes();

            // default workspace/namespace
            expect(catalog.getDefaultWorkspace()).andReturn(workspaces.peekFirst()).anyTimes();
            expect(catalog.getDefaultNamespace()).andReturn(namespaces.peekFirst()).anyTimes();

            replay(catalog);

            featureTypesAll = new LinkedList();
            coveragesAll = new LinkedList();
            dataStoresAll = new LinkedList();
            coverageStoresAll = new LinkedList();
            styles = new LinkedList();
            workspaces = new LinkedList();
            namespaces = new LinkedList();
        }

        return this;
    }

    WorkspaceInfo findWorkspace(String name) {
        return find(name, workspaces);
    }

    DataStoreInfo findDataStore(String name) {
        return find(name, dataStoresAll);
    }

    CoverageStoreInfo findCoverageStore(String name) {
        return find(name, coverageStoresAll);
    }

    FeatureTypeInfo findFeatureType(String name) {
        return find(name, featureTypesAll);
    }

    CoverageInfo findCoverage(String name) {
        return find(name, coveragesAll);
    }

    StyleInfo findStyle(String name) {
        return find(name, styles);
    }

    LayerInfo findLayer(String name) {
        return find(name, layers);
    }

    LayerGroupInfo findLayerGroup(String name) {
        return find(name, layerGroups);
    }

    <T extends CatalogInfo> T find(final String name, List<T> list) {
        return Iterables.find(
                list,
                new Predicate<T>() {
                    @Override
                    public boolean apply(T input) {
                        return name.equals(OwsUtils.get(input, "name"));
                    }
                });
    }

    protected String newId() {
        return UUID.randomUUID().toString();
    }

    abstract static class VisitAnswer implements IAnswer<Object> {
        @Override
        public Object answer() throws Throwable {
            CatalogVisitor visitor = (CatalogVisitor) EasyMock.getCurrentArguments()[0];
            doVisit(visitor);
            return null;
        }

        protected abstract void doVisit(CatalogVisitor visitor);
    }
}
