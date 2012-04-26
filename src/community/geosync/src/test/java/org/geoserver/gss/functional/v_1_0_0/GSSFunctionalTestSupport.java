package org.geoserver.gss.functional.v_1_0_0;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.custommonkey.xmlunit.SimpleNamespaceContext;
import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geogit.api.RevCommit;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.data.geogit.GeoToolsCommitStateResolver;
import org.geoserver.data.test.MockData;
import org.geoserver.data.versioning.decorator.FeatureStoreDecorator;
import org.geoserver.gss.config.GSSInfo;
import org.geoserver.gss.config.GSSXStreamLoader;
import org.geoserver.gss.impl.GSS;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.test.GeoServerTestSupport;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory;
import org.opengis.filter.FilterFactory2;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.io.WKTReader;

/**
 * 
 * Upon startup, the repository contains the following commits:
 * 
 * <ul>
 * <li>Insert of:
 * <code>Bridges.1107531599613[the_geom=POINT (0.0002 0.0007), FID="110",NAME="Cam Bridge"]</code>
 * <li>Insert of:
 * <ul>
 * <li>
 * <code>Buildings.1107531701010[the_geom=MULTIPOLYGON (((0.0008 0.0005, 0.0008 0.0007, 0.0012 0.0007, 0.0012 0.0005, 0.0008 0.0005))),FID="113", ADDRESS="123 Main Street"]</code>
 * </li>
 * <li>
 * <code>Buildings.1107531701011[the_geom=MULTIPOLYGON (((0.002 0.0008, 0.002 0.001, 0.0024 0.001, 0.0024 0.0008, 0.002 0.0008))), FID="114", ADDRESS="215 Main Street"]</code>
 * </li>
 * </ul>
 * </li>
 * <li>Commit Message: "Change Cam Bridge", Update of:
 * <code>Bridges.1107531599613[the_geom=POINT (0.0001 0.0006), NAME="Cam Bridge2"]</code></li>
 * <li>Commit Message: "Moved building", Update of
 * <code>Buildings.1107531701011[the_geom=MULTIPOLYGON (((0.002 0.0007, 0.0024 0.0007, 0.0024 0.0005, 0.002 0.0005, 0.002 0.0007)))]</code>
 * </li>
 * <li>Commit Message: "Deleted building", Delete of <code>Buildings.1107531701010</code></li>
 * </ul>
 * 
 * @author groldan
 * 
 */
public abstract class GSSFunctionalTestSupport extends GeoServerTestSupport {

    protected static final Logger LOGGER = Logging.getLogger("org.geoserver.gss.functional");

    protected static final Name CITE_BRIDGES = new NameImpl(MockData.BRIDGES);

    protected static final Name CITE_BUILDINGS = new NameImpl(MockData.BUILDINGS);

    protected static XpathEngine xpath;

    protected FilterFactory ff = CommonFactoryFinder.getFilterFactory(null);

    @Override
    public void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();

        Logging.ALL.forceMonolineConsoleOutput();

        // configure the GSS service
        GeoServer gs = getGeoServer();
        GSSXStreamLoader loader = (GSSXStreamLoader) applicationContext.getBean("gssLoader");
        GSSInfo gssInfo = loader.load(gs);
        assertNotNull(gssInfo);
        loader.save(gssInfo, gs);
        gs.add(gssInfo);

        // init xmlunit
        Map<String, String> namespaces = new HashMap<String, String>();
        namespaces.put("gss", "http://www.opengis.net/gss/1.0");
        namespaces.put("wfs", "http://www.opengis.net/wfs");
        namespaces.put("ows", "http://www.opengis.net/ows/1.1");
        namespaces.put("fes", "http://www.opengis.net/ogc");
        namespaces.put("gml", "http://www.opengis.net/gml");
        namespaces.put("sf", "http://www.openplans.org/spearfish");
        namespaces.put("xs", "http://www.w3.org/2001/XMLSchema");
        namespaces.put("app", "http://www.w3.org/2007/app");
        namespaces.put("atom", "http://www.w3.org/2005/Atom");
        namespaces.put("georss", "http://www.georss.org/georss");
        namespaces.put("os", "http://a9.com/-/spec/opensearch/1.1/");

        namespaces.put("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");

        XMLUnit.setXpathNamespaceContext(new SimpleNamespaceContext(namespaces));

        xpath = XMLUnit.newXpathEngine();

        GSS gss = GeoServerExtensions.bean(GSS.class, applicationContext);

        // insert the single bridge in cite:Bridges
        assertTrue(makeVersioned(gss, CITE_BRIDGES) instanceof RevCommit);

        // insert the two buildings in cite:Buildings
        assertTrue(makeVersioned(gss, CITE_BUILDINGS) instanceof RevCommit);

        GeometryFactory gf = new GeometryFactory();

        Filter filter;
        List<String> properties;
        List<Object> newValues;
        String commitMessage;

        // update the bridge
        properties = Arrays.asList("NAME", "the_geom");
        newValues = Arrays.asList("Cam Bridge2",
                (Object) gf.createPoint(new Coordinate(0.0001, 0.0006)));
        commitMessage = "Change Cam Bridge";
        filter = Filter.INCLUDE;
        recordUpdateCommit(gss, CITE_BRIDGES, filter, properties, newValues, commitMessage);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        // update second building
        filter = ff.id(Collections.singleton(ff.featureId("Buildings.1107531701011")));
        Geometry movedBuilding = new WKTReader()
                .read("MULTIPOLYGON (((0.002 0.0007, 0.0024 0.0007, 0.0024 0.0005, 0.002 0.0005, 0.002 0.0007)))");

        properties = Arrays.asList("the_geom");
        newValues = Arrays.asList((Object) movedBuilding);
        commitMessage = "Moved building";
        recordUpdateCommit(gss, CITE_BUILDINGS, filter, properties, newValues, commitMessage);

        // delete first building
        filter = ff.id(Collections.singleton(ff.featureId("Buildings.1107531701010")));
        recordDeleteCommit(gss, CITE_BUILDINGS, filter, "Deleted building");

    }

    protected void recordDeleteCommit(final GSS gss, final Name typeName, final Filter filter,
            final String commitMessage) throws Exception {

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(typeName);
        SimpleFeatureStore store = (SimpleFeatureStore) typeInfo.getFeatureSource(null, null);
        assertTrue(store instanceof FeatureStoreDecorator);
        Transaction tx = new DefaultTransaction();
        store.setTransaction(tx);
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_MESSAGE, commitMessage);
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_AUTHOR, "admin");
        try {
            @SuppressWarnings("rawtypes")
            FeatureCollection affectedFeatures = store.getFeatures(filter);
            assertTrue("affectedFeatures" + affectedFeatures.size(), affectedFeatures.size() > 0);

            LOGGER.info("Creating commit '" + commitMessage + "'");

            store.removeFeatures(filter);

            tx.commit();
            LOGGER.info("Delete committed");
        } catch (Exception e) {
            tx.rollback();
        } finally {
            tx.close();
        }
    }

    protected void recordUpdateCommit(final GSS gss, final Name typeName, final Filter filter,
            final List<String> properties, final List<Object> newValues, final String commitMessage)
            throws Exception {

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(typeName);
        SimpleFeatureStore store = (SimpleFeatureStore) typeInfo.getFeatureSource(null, null);
        assertTrue(store instanceof FeatureStoreDecorator);

        Transaction tx = new DefaultTransaction();
        store.setTransaction(tx);
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_MESSAGE, commitMessage);
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_AUTHOR, "admin");
        try {
            @SuppressWarnings("rawtypes")
            FeatureCollection affectedFeatures = store.getFeatures(filter);
            assertTrue("affectedFeatures" + affectedFeatures.size(), affectedFeatures.size() > 0);

            store.modifyFeatures(properties.toArray(new String[properties.size()]),
                    newValues.toArray(), filter);

            LOGGER.info("Creating commit '" + commitMessage + "'");
            tx.commit();

            LOGGER.info("Update committed");
        } catch (Exception e) {
            tx.rollback();
        } finally {
            tx.close();
        }
    }

    protected Object makeVersioned(final GSS gss, final Name featureTypeName) throws Exception {
        LOGGER.info("Importing FeatureType as versioned: " + featureTypeName);
        Future<?> future = gss.initialize(featureTypeName);
        future.get();// lock until imported
        assertTrue(gss.isReplicated(featureTypeName));
        return future.get();
    }

    protected List<String> evaluateAll(final String xpathStr, final Document dom) throws Exception {
        NodeList matchingNodes = xpath.getMatchingNodes(xpathStr, dom);
        int length = matchingNodes.getLength();
        List<String> matches = new ArrayList<String>(length);
        for (int i = 0; i < length; i++) {
            Node item = matchingNodes.item(i);
            String nodeValue = item.getTextContent();
            matches.add(nodeValue);
        }
        return matches;
    }

}
