package org.geoserver.data.versioning;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.geogit.api.GeoGIT;
import org.geoserver.geogit.GEOGIT;
import org.geotools.data.DataUtilities;
import org.geotools.data.DefaultTransaction;
import org.geotools.data.Query;
import org.geotools.data.Transaction;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.Hints;
import org.geotools.feature.FeatureCollection;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.Id;
import org.opengis.filter.identity.FeatureId;
import org.opengis.filter.identity.ResourceId;

public class VersioningFeatureStoreTest extends VersioningTestSupport {

    final FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(null);

    @Override
    protected void setUpInternal() throws Exception {
        GEOGIT mock = mock(GEOGIT.class);
        GeoGIT geoGit = new GeoGIT(getRepository());
        when(mock.getGeoGit()).thenReturn(geoGit);
        GEOGIT.set(mock);
        when(mock.getRepository()).thenReturn(getRepository());

        super.setUpInternal();
    }

    @Override
    protected void tearDownInternal() throws Exception {
        GEOGIT.set(null);
    }

    public void testAddFeatures() throws Exception {

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection;

        collection = DataUtilities.collection(Arrays.asList((SimpleFeature) points1,
                (SimpleFeature) points2, (SimpleFeature) points3));

        try {
            points.addFeatures(collection);
            fail("Expected UnsupportedOperationException on AUTO_COMMIT");
        } catch (UnsupportedOperationException e) {
            assertTrue(e.getMessage().contains("AUTO_COMMIT"));
        }

        assertEquals(0, points.getCount(Query.ALL));
        assertEquals(0, points.getFeatures().size());

        Transaction tx = new DefaultTransaction();
        points.setTransaction(tx);
        assertSame(tx, points.getTransaction());
        try {
            List<FeatureId> addedFeatures = points.addFeatures(collection);
            assertNotNull(addedFeatures);
            assertEquals(3, addedFeatures.size());

            for (FeatureId id : addedFeatures) {
                assertFalse(id instanceof ResourceId);
                assertNotNull(id.getFeatureVersion());
            }

            // assert transaction isolation

            assertEquals(3, points.getFeatures().size());
            assertEquals(0, versioningStore.getFeatureSource(pointsTypeName).getFeatures().size());

            tx.commit();

            assertEquals(3, versioningStore.getFeatureSource(pointsTypeName).getFeatures().size());
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
    }

    public void testUseProvidedFIDSupported() throws Exception {

        assertTrue(points.getQueryCapabilities().isUseProvidedFIDSupported());

        FeatureCollection<SimpleFeatureType, SimpleFeature> collection;
        collection = DataUtilities.collection(Arrays.asList((SimpleFeature) points1,
                (SimpleFeature) points2, (SimpleFeature) points3));

        Transaction tx = new DefaultTransaction();
        points.setTransaction(tx);
        try {
            List<FeatureId> newFids = points.addFeatures(collection);
            assertNotNull(newFids);
            assertEquals(3, newFids.size());

            FeatureId fid1 = newFids.get(0);
            FeatureId fid2 = newFids.get(1);
            FeatureId fid3 = newFids.get(2);

            // new ids should have been generated...
            assertFalse(idP1.equals(fid1.getID()));
            assertFalse(idP1.equals(fid1.getID()));
            assertFalse(idP1.equals(fid1.getID()));

            // now force the use of provided feature ids
            points1.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
            points2.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);
            points3.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.TRUE);

            List<FeatureId> providedFids = points.addFeatures(collection);
            assertNotNull(providedFids);
            assertEquals(3, providedFids.size());

            FeatureId fid11 = providedFids.get(0);
            FeatureId fid21 = providedFids.get(1);
            FeatureId fid31 = providedFids.get(2);

            // ids should match provided
            assertEquals(idP1, fid11.getID());
            assertEquals(idP2, fid21.getID());
            assertEquals(idP3, fid31.getID());

            tx.commit();

            assertEquals(1, points.getFeatures(ff.id(Collections.singleton(fid1))).size());
            assertEquals(1, points.getFeatures(ff.id(Collections.singleton(fid2))).size());
            assertEquals(1, points.getFeatures(ff.id(Collections.singleton(fid3))).size());

            assertEquals(1, points.getFeatures(ff.id(Collections.singleton(fid11))).size());
            assertEquals(1, points.getFeatures(ff.id(Collections.singleton(fid21))).size());
            assertEquals(1, points.getFeatures(ff.id(Collections.singleton(fid31))).size());

        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
    }

    @SuppressWarnings("deprecation")
    public void testModifyFeatures() throws Exception {
        initFull();

        Id filter = ff.id(Collections.singleton(ff.featureId(idP1)));
        Transaction tx = new DefaultTransaction();
        points.setTransaction(tx);
        try {
            // initial value
            for (Iterator<SimpleFeature> it = unversionedStore.getFeatureSource(pointsTypeName)
                    .getFeatures().iterator(); it.hasNext();) {
                SimpleFeature next = it.next();
                System.out.println(next.getID());
            }
            assertEquals("StringProp1_1", unversionedStore.getFeatureSource(pointsTypeName)
                    .getFeatures(filter).iterator().next().getAttribute("sp"));

            assertEquals("StringProp1_1", points.getFeatures(filter).iterator().next()
                    .getAttribute("sp"));
            // modify
            points.modifyFeatures("sp", "modified", filter);
            // modified value before commit
            assertEquals("modified", points.getFeatures(filter).iterator().next()
                    .getAttribute("sp"));
            // unmodified value before commit on another store instance (tx isolation)
            assertEquals("StringProp1_1", versioningStore.getFeatureSource(pointsTypeName)
                    .getFeatures(filter).iterator().next().getAttribute("sp"));

            tx.commit();

            // modified value after commit on another store instance
            assertEquals("modified",
                    versioningStore.getFeatureSource(pointsTypeName).getFeatures(filter).iterator()
                            .next().getAttribute("sp"));
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
        points.setTransaction(Transaction.AUTO_COMMIT);
        SimpleFeature modified = points.getFeatures(filter).iterator().next();
        assertEquals("modified", modified.getAttribute("sp"));
    }

    private void initFull() throws IOException {
        setUseProvidedFidHint(true, lines1, lines2, lines3, points1, points2, points3);
        {
            Transaction tx = new DefaultTransaction();
            lines.setTransaction(tx);
            points.setTransaction(tx);
            List<FeatureId> addFeatures = lines.addFeatures(DataUtilities.collection(Arrays.asList(
                    (SimpleFeature) lines1, (SimpleFeature) lines2, (SimpleFeature) lines3)));

            List<FeatureId> addFeatures2 = points.addFeatures(DataUtilities.collection(Arrays
                    .asList((SimpleFeature) points1, (SimpleFeature) points2,
                            (SimpleFeature) points3)));

            tx.commit();
            assertEquals(3, points.getCount(Query.ALL));
            assertEquals(3, lines.getCount(Query.ALL));
        }
    }

    private void setUseProvidedFidHint(boolean useProvidedFid, Feature... features) {
        for (Feature f : features) {
            f.getUserData().put(Hints.USE_PROVIDED_FID, Boolean.valueOf(useProvidedFid));
        }
    }

    public void testRemoveFeatures() throws Exception {
        initFull();

        Id filter = ff.id(Collections.singleton(ff.featureId(idP1)));
        Transaction tx = new DefaultTransaction();
        points.setTransaction(tx);
        try {
            // initial # of features
            assertEquals(3, points.getFeatures().size());
            // remove feature
            points.removeFeatures(filter);

            // #of features before commit on the same store
            assertEquals(2, points.getFeatures().size());

            // #of features before commit on a different store instance
            assertEquals(3, versioningStore.getFeatureSource(pointsTypeName).getFeatures().size());

            tx.commit();

            // #of features after commit on a different store instance
            assertEquals(2, versioningStore.getFeatureSource(pointsTypeName).getFeatures().size());
        } catch (Exception e) {
            tx.rollback();
            throw e;
        } finally {
            tx.close();
        }
        points.setTransaction(Transaction.AUTO_COMMIT);
        assertEquals(2, points.getFeatures().size());
        assertEquals(0, points.getFeatures(filter).size());
    }

}
