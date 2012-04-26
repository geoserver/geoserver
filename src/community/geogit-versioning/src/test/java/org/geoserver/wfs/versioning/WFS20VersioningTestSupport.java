package org.geoserver.wfs.versioning;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import org.custommonkey.xmlunit.XMLUnit;
import org.custommonkey.xmlunit.XpathEngine;
import org.geogit.api.GeoGIT;
import org.geogit.api.ObjectId;
import org.geogit.api.Ref;
import org.geogit.api.RevCommit;
import org.geogit.api.RevObject.TYPE;
import org.geogit.api.RevTree;
import org.geogit.api.TreeVisitor;
import org.geogit.repository.Repository;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.geogit.GeoToolsCommitStateResolver;
import org.geoserver.data.test.MockData;
import org.geoserver.geogit.GEOGIT;
import org.geoserver.wfs.v2_0.WFS20TestSupport;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Transaction;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureStore;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.identity.FeatureId;
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
 * <li><b>Commit 1</b>, Insert of:
 * <code>Bridges.1107531599613[the_geom=POINT (0.0002 0.0007), FID="110",NAME="Cam Bridge"]</code>
 * <li><b>Commit 2</b>, Insert of:
 * <ul>
 * <li>
 * <code>Buildings.1107531701010[the_geom=MULTIPOLYGON (((0.0008 0.0005, 0.0008 0.0007, 0.0012 0.0007, 0.0012 0.0005, 0.0008 0.0005))),FID="113", ADDRESS="123 Main Street"]</code>
 * </li>
 * <li>
 * <code>Buildings.1107531701011[the_geom=MULTIPOLYGON (((0.002 0.0008, 0.002 0.001, 0.0024 0.001, 0.0024 0.0008, 0.002 0.0008))), FID="114", ADDRESS="215 Main Street"]</code>
 * </li>
 * </ul>
 * </li>
 * <li><b>Commit 3</b>, commit message: "Change Cam Bridge", Update of:
 * <code>Bridges.1107531599613[the_geom=POINT (0.0001 0.0006), NAME="Cam Bridge2"]</code></li>
 * <li><b>Commit 4</b>, commit Message: "Moved building", Update of
 * <code>Buildings.1107531701011[the_geom=MULTIPOLYGON (((0.002 0.0007, 0.0024 0.0007, 0.0024 0.0005, 0.002 0.0005, 0.002 0.0007)))]</code>
 * </li>
 * <li><b>Commit 5</b>, commit message: "Deleted building", Delete of
 * <code>Buildings.1107531701010</code></li>
 * </ul>
 * 
 * @author groldan
 * 
 */
public abstract class WFS20VersioningTestSupport extends WFS20TestSupport {

    protected static final Logger LOGGER = Logging.getLogger(WFS20VersioningTestSupport.class);

    protected static final Name CITE_BRIDGES = new NameImpl(MockData.BRIDGES);

    protected static final Name CITE_BUILDINGS = new NameImpl(MockData.BUILDINGS);

    protected static final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    protected static XpathEngine xpath;

    // holds all the feature identifiers in the repository after the first initial commit (in the
    // form <feature id>@<version id>
    protected static Set<String> commit1FeatureIdentifiers;

    // holds all the feature identifiers in the repository after the second initial commit (in the
    // form <feature id>@<version id>
    protected static Set<String> commit2FeatureIdentifiers;

    // holds all the feature identifiers in the repository after the third initial commit (in the
    // form <feature id>@<version id>
    protected static Set<String> commit3FeatureIdentifiers;

    // holds all the feature identifiers in the repository after the third initial commit (in the
    // form <feature id>@<version id>
    protected static Set<String> commit4FeatureIdentifiers;

    // holds all the feature identifiers in the repository after the third initial commit (in the
    // form <feature id>@<version id>
    protected static Set<String> commit5FeatureIdentifiers;

    /**
     * These are the five commits recorded. Commit timestamps are mocked up and have the values
     * 10000,20000,30000,4000, and 50000, respectively
     */
    protected static RevCommit commit1, commit2, commit3, commit4, commit5;

    protected GEOGIT ggitFacade;

    protected static class MockCommitStateResolver extends GeoToolsCommitStateResolver {
        private final long timestamp;

        public MockCommitStateResolver(long timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public long getCommitTimeMillis() {
            return timestamp;
        }
    }

    @Override
    public final void oneTimeTearDown() throws Exception {
        GeoGIT.setCommitStateResolver(null);
    }

    @Override
    public final void oneTimeSetUp() throws Exception {

        super.oneTimeSetUp();
        xpath = XMLUnit.newXpathEngine();

        ggitFacade = GEOGIT.get();

        // insert the single bridge in cite:Bridges
        GeoGIT.setCommitStateResolver(new MockCommitStateResolver(10000));
        assertTrue(makeVersioned(ggitFacade, CITE_BRIDGES) instanceof RevCommit);
        commit1FeatureIdentifiers = getCurrentResourIds(ggitFacade);
        commit1 = getCurrentCommit(ggitFacade);

        // insert the two buildings in cite:Buildings
        GeoGIT.setCommitStateResolver(new MockCommitStateResolver(20000));
        assertTrue(makeVersioned(ggitFacade, CITE_BUILDINGS) instanceof RevCommit);
        commit2FeatureIdentifiers = getCurrentResourIds(ggitFacade);
        commit2 = getCurrentCommit(ggitFacade);

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
        GeoGIT.setCommitStateResolver(new MockCommitStateResolver(30000));
        recordUpdateCommit(ggitFacade, CITE_BRIDGES, filter, properties, newValues, commitMessage);
        commit3FeatureIdentifiers = getCurrentResourIds(ggitFacade);
        assertNotNull(commit3FeatureIdentifiers);
        commit3 = getCurrentCommit(ggitFacade);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);
        // update second building
        filter = ff.id(Collections.singleton(ff.featureId("Buildings.1107531701011")));
        Geometry movedBuilding = new WKTReader()
                .read("MULTIPOLYGON (((0.002 0.0007, 0.0024 0.0007, 0.0024 0.0005, 0.002 0.0005, 0.002 0.0007)))");

        properties = Arrays.asList("the_geom");
        newValues = Arrays.asList((Object) movedBuilding);
        commitMessage = "Moved building";
        GeoGIT.setCommitStateResolver(new MockCommitStateResolver(40000));
        recordUpdateCommit(ggitFacade, CITE_BUILDINGS, filter, properties, newValues, commitMessage);
        commit4FeatureIdentifiers = getCurrentResourIds(ggitFacade);
        commit4 = getCurrentCommit(ggitFacade);

        // delete first building
        filter = ff.id(Collections.singleton(ff.featureId("Buildings.1107531701010")));
        GeoGIT.setCommitStateResolver(new MockCommitStateResolver(50000));
        recordDeleteCommit(ggitFacade, CITE_BUILDINGS, filter, "Deleted building");
        commit5FeatureIdentifiers = getCurrentResourIds(ggitFacade);
        assertNotNull(commit5FeatureIdentifiers);
        commit5 = getCurrentCommit(ggitFacade);
    }

    private RevCommit getCurrentCommit(GEOGIT ggitFacade) {
        Repository repository = ggitFacade.getRepository();
        return repository.getCommit(ggitFacade.getRepository().getHead().getObjectId());
    }

    private Set<String> getCurrentResourIds(GEOGIT ggitFacade) {
        final Repository repository = ggitFacade.getGeoGit().getRepository();

        final Set<String> resourceIds = new HashSet<String>();
        class RidCollector implements TreeVisitor {
            @Override
            public boolean visitSubTree(int bucket, ObjectId treeId) {
                return true;
            }

            @Override
            public boolean visitEntry(Ref ref) {
                if (TYPE.BLOB.equals(ref.getType())) {
                    resourceIds.add(new StringBuilder(ref.getName()).append('@')
                            .append(ref.getObjectId().toString()).toString());
                } else if (TYPE.TREE.equals(ref.getType())) {
                    repository.getTree(ref.getObjectId()).accept(new RidCollector());
                }
                return true;
            }
        }
        ;

        RevTree headTree = repository.getHeadTree();
        headTree.accept(new RidCollector());

        RevCommit commit = repository.getCommit(repository.getHead().getObjectId());
        System.out.println("Resource ids in commit '" + commit.getMessage() + "':\n" + resourceIds);
        return resourceIds;
    }

    protected List<FeatureId> recordInsertCommit(final GEOGIT facade, final String commitMessage,
            final Name typeName, final SimpleFeature... features) throws Exception {

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(typeName);
        SimpleFeatureStore store = (SimpleFeatureStore) typeInfo.getFeatureSource(null, null);
        Transaction tx = new DefaultTransaction();
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_MESSAGE, commitMessage);
        store.setTransaction(tx);
        try {
            for(SimpleFeature f : features){
                f.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
            }
            SimpleFeatureCollection collection = DataUtilities.collection(Arrays.asList(features));
            List<FeatureId> addedFeatures = store.addFeatures(collection);

            LOGGER.fine("Creating commit '" + commitMessage + "'");

            tx.commit();
            LOGGER.fine("Insert committed");
            return addedFeatures;
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
    }

    private void recordDeleteCommit(final GEOGIT facade, final Name typeName, final Filter filter,
            final String commitMessage) throws Exception {

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(typeName);
        SimpleFeatureStore store = (SimpleFeatureStore) typeInfo.getFeatureSource(null, null);
        Transaction tx = new DefaultTransaction();
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_MESSAGE, commitMessage);
        store.setTransaction(tx);
        try {
            @SuppressWarnings("rawtypes")
            FeatureCollection affectedFeatures = store.getFeatures(filter);
            assertTrue("affectedFeatures" + affectedFeatures.size(), affectedFeatures.size() > 0);

            LOGGER.fine("Creating commit '" + commitMessage + "'");

            store.removeFeatures(filter);

            tx.commit();
            LOGGER.fine("Delete committed");
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
    }

    private void recordUpdateCommit(final GEOGIT facade, final Name typeName, final Filter filter,
            final List<String> properties, final List<Object> newValues, final String commitMessage)
            throws Exception {

        FeatureTypeInfo typeInfo = getCatalog().getFeatureTypeByName(typeName);
        SimpleFeatureStore store = (SimpleFeatureStore) typeInfo.getFeatureSource(null, null);
        Transaction tx = new DefaultTransaction();
        tx.putProperty(GeoToolsCommitStateResolver.GEOGIT_COMMIT_MESSAGE, commitMessage);
        store.setTransaction(tx);
        try {

            @SuppressWarnings("rawtypes")
            FeatureCollection affectedFeatures = store.getFeatures(filter);
            assertTrue("affectedFeatures" + affectedFeatures.size(), affectedFeatures.size() > 0);

            store.modifyFeatures(properties.toArray(new String[properties.size()]),
                    newValues.toArray(), filter);

            LOGGER.fine("Creating commit '" + commitMessage + "'");

            tx.commit();
            LOGGER.fine("Update committed");
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
    }

    private Object makeVersioned(final GEOGIT facade, final Name featureTypeName) throws Exception {
        LOGGER.fine("Importing FeatureType as versioned: " + featureTypeName);
        Future<?> future = facade.initialize(featureTypeName);
        future.get();// lock until imported
        assertTrue(facade.isReplicated(featureTypeName));
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
