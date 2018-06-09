/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.geopkg;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import net.opengis.wfs.GetFeatureType;
import net.opengis.wfs.WfsFactory;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.platform.Operation;
import org.geoserver.wfs.WFSTestSupport;
import org.geoserver.wfs.request.FeatureCollectionResponse;
import org.geotools.data.FeatureSource;
import org.geotools.data.memory.MemoryFeatureCollection;
import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.data.simple.SimpleFeatureReader;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.geopkg.FeatureEntry;
import org.geotools.geopkg.GeoPackage;
import org.junit.Before;
import org.junit.Test;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.FeatureType;
import org.opengis.filter.FilterFactory;
import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Test for WFS GetFeature OutputFormat for GeoPackage
 *
 * @author Niels Charlier
 */
public class GeoPackageGetFeatureOutputFormatTest extends WFSTestSupport {

    protected static FilterFactory ff = CommonFactoryFinder.getFilterFactory();

    protected GeoPackageGetFeatureOutputFormat format;

    protected Operation op;

    protected GetFeatureType gft;

    @Before
    public void init() {
        gft = WfsFactory.eINSTANCE.createGetFeatureType();
        format = new GeoPackageGetFeatureOutputFormat(getGeoServer());
        op = new Operation("GetFeature", getServiceDescriptor10(), null, new Object[] {gft});
    }

    @Test
    public void testGetFeatureOneType() throws IOException {
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        ;
        fct.getFeature().add(fs.getFeatures());

        testGetFeature(fct, false);
    }

    @Test
    public void testGetFeatureTwoTypes() throws IOException {
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.LAKES);
        fct.getFeature().add(fs.getFeatures());

        fs = getFeatureSource(SystemTestData.STREAMS);
        fct.getFeature().add(fs.getFeatures());

        testGetFeature(fct, false);
    }

    @Test
    public void testGetFeatureWithFilter() throws IOException {
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.LAKES);
        fct.getFeature().add(fs.getFeatures());

        fs = getFeatureSource(SystemTestData.STREAMS);
        FeatureCollection coll =
                fs.getFeatures(ff.equals(ff.property("NAME"), ff.literal("Cam Stream")));
        assertEquals(1, coll.size());

        fct.getFeature().add(coll);
        testGetFeature(fct, false);
    }

    @Test
    public void testGetFeatureWithSpatialIndex() throws IOException {
        System.setProperty(GeoPackageGetFeatureOutputFormat.PROPERTY_INDEXED, "true");
        FeatureCollectionResponse fct =
                FeatureCollectionResponse.adapt(WfsFactory.eINSTANCE.createFeatureCollectionType());

        FeatureSource<? extends FeatureType, ? extends Feature> fs =
                getFeatureSource(SystemTestData.BASIC_POLYGONS);
        fct.getFeature().add(fs.getFeatures());

        testGetFeature(fct, true);

        System.getProperties().remove(GeoPackageGetFeatureOutputFormat.PROPERTY_INDEXED);
    }

    @Test
    public void testHttpStuff() throws Exception {
        String layerName = SystemTestData.BASIC_POLYGONS.getLocalPart();
        MockHttpServletResponse resp =
                getAsServletResponse(
                        "wfs?request=getfeature&typename="
                                + layerName
                                + "&outputformat=geopackage");
        assertEquals(GeoPkg.MIME_TYPE, resp.getContentType());

        assertEquals(
                "attachment; filename=" + layerName + ".gpkg",
                resp.getHeader("Content-Disposition"));
    }

    public void testGetFeature(FeatureCollectionResponse fct, boolean indexed) throws IOException {
        // FileOutputStream fos = new FileOutputStream(new File("/home/niels/Temp/geopkg.db"));
        // format.write(fct, fos, op);

        ByteArrayOutputStream os = new ByteArrayOutputStream();
        format.write(fct, os, op);

        GeoPackage geopkg = createGeoPackage(os.toByteArray());

        // compare all feature collections
        for (FeatureCollection collection : fct.getFeatures()) {
            FeatureEntry e = new FeatureEntry();
            e.setTableName(collection.getSchema().getName().getLocalPart());
            e.setGeometryColumn(
                    collection.getSchema().getGeometryDescriptor().getName().getLocalPart());

            SimpleFeatureReader reader = geopkg.reader(e, null, null);

            SimpleFeatureCollection sCollection = (SimpleFeatureCollection) collection;

            // spatial index
            assertEquals(indexed, geopkg.hasSpatialIndex(e));

            // compare type
            SimpleFeatureType type1 = reader.getFeatureType();
            SimpleFeatureType type2 = sCollection.getSchema();
            assertEquals(type1.getDescriptors().size(), type2.getDescriptors().size());
            for (int i = 0; i < type1.getDescriptors().size(); i++) {
                assertEquals(type1.getDescriptor(i).getName(), type2.getDescriptor(i).getName());
                assertEquals(type1.getDescriptor(i).getType(), type2.getDescriptor(i).getType());
            }

            // compare data
            MemoryFeatureCollection memCollection = new MemoryFeatureCollection(type2);
            while (reader.hasNext()) {
                memCollection.add(reader.next());
            }

            assertEquals(sCollection.size(), memCollection.size());

            SimpleFeatureIterator it = sCollection.features();
            while (it.hasNext()) {
                SimpleFeature sf = it.next();
                for (int i = 0; i < type1.getDescriptors().size(); i++) {
                    assertTrue(findFeatureAttribute(memCollection, i, sf.getAttribute(i)));
                }
            }

            reader.close();
        }

        geopkg.close();
    }

    protected boolean findFeatureAttribute(
            SimpleFeatureCollection collection, int indexProp, Object value) {
        SimpleFeatureIterator it = collection.features();
        while (it.hasNext()) {
            SimpleFeature sf = it.next();
            if (sf.getAttribute(indexProp).equals(value)) {
                return true;
            }
        }
        return false;
    }

    protected GeoPackage createGeoPackage(byte[] inMemory) throws IOException {

        File f = File.createTempFile("temp", ".gpkg", new File("target"));
        FileOutputStream fout = new FileOutputStream(f);
        fout.write(inMemory);
        fout.flush();
        fout.close();

        return new GeoPackage(f);
    }
}
