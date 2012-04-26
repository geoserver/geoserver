package org.geoserver.data.gss;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.xml.stream.XMLInputFactory;

import org.apache.commons.io.IOUtils;
import org.geoserver.bxml.CatalogProvider;
import org.geoserver.bxml.atom.FeedDecoder;
import org.geoserver.gss.functional.v_1_0_0.GSSFunctionalTestSupport;
import org.geoserver.gss.impl.GSS;
import org.geoserver.gss.internal.atom.EntryImpl;
import org.geoserver.gss.internal.atom.FeedImpl;
import org.geoserver.platform.GeoServerExtensions;
import org.geotools.data.FeatureSource;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.NameImpl;
import org.gvsig.bxml.adapt.sax.XmlToBxmlSaxConverter;
import org.gvsig.bxml.adapt.stax.XmlStreamReaderAdapter;
import org.gvsig.bxml.stream.BxmlFactoryFinder;
import org.gvsig.bxml.stream.BxmlInputFactory;
import org.gvsig.bxml.stream.BxmlStreamReader;
import org.gvsig.bxml.util.ProgressListener;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.feature.type.FeatureType;
import org.opengis.feature.type.Name;
import org.opengis.filter.Filter;
import org.opengis.filter.FilterFactory2;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;

public class ReplicationReceiverTest extends GSSFunctionalTestSupport {

    private GeometryFactory gf = new GeometryFactory();

    private FilterFactory2 ff;

    private GSS gss;

    private boolean isBinary;

    @Override
    public void oneTimeSetUp() throws Exception {
        super.oneTimeSetUp();
        gf = new GeometryFactory();
        ff = CommonFactoryFinder.getFilterFactory2(null);

        gss = GeoServerExtensions.bean(GSS.class, applicationContext);

        List<String> properties = Arrays.asList("NAME", "the_geom");
        List<Object> newValues = Arrays.asList("Cam Bridge",
                (Object) gf.createPoint(new Coordinate(4, 5)));
        String commitMessage = "Change Cam Bridge";
        Filter filter = Filter.INCLUDE;
        recordUpdateCommit(gss, CITE_BRIDGES, filter, properties, newValues, commitMessage);

    }

    public void testUpdate() throws Exception {
        BxmlStreamReader reader = getReader("replicationReceiverUpdate");

        FeedDecoder decoder = new FeedDecoder();

        reader.nextTag();
        FeedImpl feed = decoder.decode(reader);

        List<String> properties = Arrays.asList("NAME", "FID", "the_geom");
        List<Object> values = Arrays.asList("Cam Bridge", "110",
                (Object) gf.createPoint(new Coordinate(4, 5)));

        assertFeature(CITE_BRIDGES, "Bridges.1107531599613", properties, values);

        ReplicationReceiver replicationReceiver = new ReplicationReceiver();
        replicationReceiver.receive(feed, new CatalogProvider(getCatalog()));

        List<Object> newValues = Arrays.asList("Cam Bridge 1", "150",
                (Object) gf.createPoint(new Coordinate(5, 6)));
        assertFeature(CITE_BRIDGES, "Bridges.1107531599613", properties, newValues);
    }

    public void testDelete() throws Exception {
        BxmlStreamReader reader = getReader("replicationReceiverDelete");

        FeedDecoder decoder = new FeedDecoder();

        reader.nextTag();
        FeedImpl feed = decoder.decode(reader);

        Feature feature = getFeature(CITE_BRIDGES, "Bridges.1107531599613");
        assertNotNull(feature);

        ReplicationReceiver replicationReceiver = new ReplicationReceiver();
        replicationReceiver.receive(feed, new CatalogProvider(getCatalog()));

        feature = getFeature(CITE_BRIDGES, "Bridges.1107531599613");
        assertNull(feature);

    }

    public void testInsert() throws Exception {
        BxmlStreamReader reader = getReader("replicationReceiverInsert");

        FeedDecoder decoder = new FeedDecoder();

        reader.nextTag();
        FeedImpl feed = decoder.decode(reader);

        assertNull(getFeature(CITE_BRIDGES, "Bridges.555"));

        ReplicationReceiver replicationReceiver = new ReplicationReceiver();
        EntryImpl entry = replicationReceiver.receive(feed, new CatalogProvider(getCatalog()));
        assertNotNull(entry);
        
        List<String> properties = Arrays.asList("the_geom", "FID", "NAME");
        List<Object> values = Arrays.asList((Object) gf.createPoint(new Coordinate(9, 10)), "230",
                "Cam Bridge 25");

        assertFeature(CITE_BRIDGES, "Bridges.555", properties, values);
    }

    public void testInsert2() throws Exception {
        BxmlStreamReader reader = getReader("replicationReceiverInsert2");

        FeedDecoder decoder = new FeedDecoder();

        reader.nextTag();
        FeedImpl feed = decoder.decode(reader);

        assertNull(getFeature(CITE_BRIDGES, "Bridges.556"));
        assertNull(getFeature(CITE_BRIDGES, "Bridges.557"));
        assertNull(getFeature(CITE_BRIDGES, "Bridges.558"));

        ReplicationReceiver replicationReceiver = new ReplicationReceiver();
        replicationReceiver.receive(feed, new CatalogProvider(getCatalog()));

        List<String> properties = Arrays.asList("the_geom", "FID", "NAME");

        List<Object> values = Arrays.asList((Object) gf.createPoint(new Coordinate(11, 12)), "10",
                "Bridge 01");
        assertFeature(CITE_BRIDGES, "Bridges.556", properties, values);

        values = Arrays.asList((Object) gf.createPoint(new Coordinate(13, 14)), "11", "Bridge 02");
        assertFeature(CITE_BRIDGES, "Bridges.557", properties, values);

        values = Arrays.asList((Object) gf.createPoint(new Coordinate(15, 16)), "12", "Bridge 03");
        assertFeature(CITE_BRIDGES, "Bridges.558", properties, values);
    }

    private void assertFeature(Name featureName, String featureId, List<String> properties,
            List<Object> values) throws IOException {
        Feature feature = getFeature(featureName, featureId);
        assertNotNull(feature);
        for (int i = 0; i < properties.size(); i++) {
            Property propertyName = feature.getProperty(new NameImpl(properties.get(i)));
            assertEquals(values.get(i), propertyName.getValue());
        }
    }

    protected Feature getFeature(Name featureName, String featureId) throws IOException {

        FeatureSource<? extends FeatureType, ? extends Feature> featureSource;
        featureSource = getCatalog().getFeatureTypeByName(featureName).getFeatureSource(null, null);

        Filter filter = ff.id(Collections.singleton(ff.featureId(featureId)));

        FeatureCollection<? extends FeatureType, ? extends Feature> features;
        features = featureSource.getFeatures(filter);
        FeatureIterator<? extends Feature> iterator = features.features();
        try {
            if (iterator.hasNext()) {
                return iterator.next();
            } else {
                return null;
            }
        } finally {
            iterator.close();
        }
    }

    protected BxmlStreamReader getReader(final String resource) throws Exception {

        final String xmlResource = resource + ".xml";
        final String bxmlResource = resource + ".bxml";

        final InputStream input;
        if (isBinary) {
            if (null == getClass().getResourceAsStream(bxmlResource)) {
                LOGGER.warning(" ----------- BXML resource " + bxmlResource + " not found by "
                        + getClass().getName() + ", encoding XML resource...");
                input = getClass().getResourceAsStream(xmlResource);
            } else {
                input = getClass().getResourceAsStream(bxmlResource);
            }
        } else {
            input = getClass().getResourceAsStream(xmlResource);
        }

        return getReader(input);
    }

    protected BxmlStreamReader getReader(final InputStream in) throws Exception {
        final ByteArrayOutputStream buff = new ByteArrayOutputStream();
        IOUtils.copy(in, buff);

        final boolean dataIsBinary;
        if (1 == buff.toByteArray()[0]) {
            dataIsBinary = true;
        } else {
            dataIsBinary = false;
        }

        BxmlStreamReader reader;

        if (dataIsBinary) {
            reader = getBxmlReader(new ByteArrayInputStream(buff.toByteArray()));
        } else {
            reader = getXmlReader(new ByteArrayInputStream(buff.toByteArray()));
        }
        if (isBinary && !dataIsBinary) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            new XmlToBxmlSaxConverter().convert(new ByteArrayInputStream(buff.toByteArray()), out,
                    ProgressListener.NULL, true);
            reader = getBxmlReader(new ByteArrayInputStream(out.toByteArray()));
        }

        return reader;
    }

    private BxmlStreamReader getBxmlReader(final InputStream input) throws Exception {
        BxmlInputFactory factory = BxmlFactoryFinder.newInputFactory();

        factory.setNamespaceAware(true);
        BxmlStreamReader reader = factory.createScanner(input);
        return reader;
    }

    private BxmlStreamReader getXmlReader(final InputStream input) throws Exception {
        XMLInputFactory factory = XMLInputFactory.newInstance();

        factory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.TRUE);
        BxmlStreamReader reader = new XmlStreamReaderAdapter(factory, input);
        return reader;
    }

}
