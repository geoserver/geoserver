package org.geoserver.bxml.wfs_1_1;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import net.opengis.wfs.InsertElementType;

import org.geoserver.bxml.BxmlTestSupport;
import org.geoserver.bxml.CatalogProvider;
import org.geoserver.bxml.Context;
import org.geoserver.bxml.FeatureTypeProvider;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geotools.feature.NameImpl;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.AttributeType;
import org.opengis.feature.type.Name;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;

public class InsertDecoderTest extends BxmlTestSupport {

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        Class[] columnTypes = new Class[] { Long.class, Integer.class, String.class, String.class,
                String.class, Geometry.class };
        Catalog catalog = buildCatalog(columnTypes);
        FeatureTypeProvider catalogProvider = new CatalogProvider(catalog);
        Context.put(FeatureTypeProvider.class, catalogProvider);
    }

    @Override
    protected void tearDown() throws Exception {
        Context.clear();
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    protected Catalog buildCatalog(Class[] columnTypes) throws Exception {
        Name typeName = new NameImpl("http://opengeo.org/osm", "planet_osm_point");
        int featureCount = columnTypes.length;

        SimpleFeatureType mockFeatureType = mock(SimpleFeatureType.class);
        for (int i = 0; i < featureCount; i++) {
            AttributeDescriptor mockAttributeDescriptor = mock(AttributeDescriptor.class);

            AttributeType mockAttributeType = mock(AttributeType.class);

            when(mockAttributeType.getBinding()).thenReturn((Class) columnTypes[i]);
            when(mockAttributeDescriptor.getType()).thenReturn(mockAttributeType);
            when(mockFeatureType.getDescriptor(i)).thenReturn(mockAttributeDescriptor);
        }

        when(mockFeatureType.getAttributeCount()).thenReturn(featureCount);
        FeatureTypeInfo mockFeatureTypeInfo = mock(FeatureTypeInfo.class);
        when(mockFeatureTypeInfo.getFeatureType()).thenReturn(mockFeatureType);
        Catalog mockCatalog = mock(Catalog.class);
        when(
                mockCatalog.getFeatureTypeByName(eq(typeName.getNamespaceURI()),
                        eq(typeName.getLocalPart()))).thenReturn(mockFeatureTypeInfo);
        when(mockCatalog.getFeatureTypeByName(eq(typeName))).thenReturn(mockFeatureTypeInfo);
        return mockCatalog;
    }

    @SuppressWarnings({ "rawtypes" })
    public void testInsertPoint() throws Exception {

        BxmlStreamReader reader = super.getReader("insert_point");
        reader.nextTag();
        InsertElementTypeDecoder decoder = new InsertElementTypeDecoder();
        InsertElementType insertElement = (InsertElementType) decoder.decode(reader);
        assertNotNull(insertElement);

        SimpleFeature feature = (SimpleFeature) insertElement.getFeature().get(0);

        assertEquals("planet_osm_point.1", feature.getID());
        assertEquals(158607824l, feature.getAttribute(0));
        assertEquals(166, feature.getAttribute(1));
        assertEquals("New Center Historical Place", feature.getAttribute(2));
        assertEquals("", feature.getAttribute(3));
        assertEquals("hamlet", feature.getAttribute(4));
        testPoint(new double[] { -8426871.63467172, 4991071.85937995 },
                (Point) feature.getAttribute(5));

        SimpleFeature feature1 = (SimpleFeature) insertElement.getFeature().get(1);

        assertEquals("planet_osm_point.2", feature1.getID());
        assertEquals(98765l, feature1.getAttribute(0));
        assertEquals(255, feature1.getAttribute(1));
        assertEquals("Another name", feature1.getAttribute(2));
        assertEquals("abc", feature1.getAttribute(3));
        assertEquals("burgos", feature1.getAttribute(4));
        testPoint(new double[] { -255.33, 365.12 }, (Point) feature1.getAttribute(5));

        reader.close();
    }

}
