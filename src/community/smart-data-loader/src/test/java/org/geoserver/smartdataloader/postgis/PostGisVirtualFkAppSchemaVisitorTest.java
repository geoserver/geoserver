/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.postgis;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import javax.sql.DataSource;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.data.store.virtualfk.Relationships;
import org.geoserver.smartdataloader.data.store.virtualfk.RelationshipsXmlParser;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.DataStoreMetadataFactory;
import org.geoserver.smartdataloader.metadata.jdbc.DefaultJdbcHelper;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcDataStoreMetadataConfig;
import org.geoserver.smartdataloader.metadata.jdbc.JdbcHelper;
import org.geoserver.smartdataloader.metadata.jdbc.VirtualFkJdbcHelper;
import org.geoserver.smartdataloader.visitors.appschema.AppSchemaVisitor;
import org.geotools.jdbc.JDBCTestSetup;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Validates generation of App-Schema mappings when virtual relationships are provided and the destination entity is
 * backed by a database view.
 */
public class PostGisVirtualFkAppSchemaVisitorTest extends AbstractJDBCSmartDataLoaderTestSupport {

    private static final String VIRTUAL_RELATIONSHIPS_XML = "<relationships>"
            + "<relationship name=\"stations_to_observations_view\" cardinality=\"1:n\">"
            + "<source schema=\"smartappschematest\" entity=\"meteo_stations\" kind=\"table\">"
            + "<key column=\"id\"/>"
            + "</source>"
            + "<target schema=\"smartappschematest\" entity=\"v_meteo_observations_parameters\" kind=\"view\">"
            + "<key column=\"station_id\"/>"
            + "</target>"
            + "</relationship>"
            + "</relationships>";

    private static final Relationships VIRTUAL_RELATIONSHIPS;

    static {
        try {
            VIRTUAL_RELATIONSHIPS = RelationshipsXmlParser.parse(VIRTUAL_RELATIONSHIPS_XML);
        } catch (Exception e) {
            throw new ExceptionInInitializerError(e);
        }
    }

    public PostGisVirtualFkAppSchemaVisitorTest() {
        this(new PostGisFixtureHelper());
    }

    private PostGisVirtualFkAppSchemaVisitorTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
    }

    @Override
    protected JDBCTestSetup createTestSetup() {
        return new MeteoStationsViewsPostGISTestSetUp();
    }

    @Override
    protected DataStoreMetadata getDataStoreMetadata(DatabaseMetaData metaData) throws Exception {
        JdbcHelper helper = new VirtualFkJdbcHelper(new DefaultJdbcHelper(), VIRTUAL_RELATIONSHIPS);
        DataStoreMetadataConfig config =
                new JdbcDataStoreMetadataConfig(ONLINE_DB_SCHEMA, metaData.getConnection(), null, ONLINE_DB_SCHEMA);
        return new DataStoreMetadataFactory().getDataStoreMetadata(config, helper);
    }

    @Test
    public void testVirtualRelationshipsAreIncludedInAppSchema() throws Exception {
        DataSource source = this.dataSource;
        Connection connection = source.getConnection();
        try {
            DatabaseMetaData metaData = connection.getMetaData();
            DataStoreMetadata dsm = getDataStoreMetadata(metaData);
            DomainModelConfig config = new DomainModelConfig();
            config.setRootEntityName("meteo_stations");
            DomainModelBuilder builder = new DomainModelBuilder(dsm, config);
            DomainModel model = builder.buildDomainModel();

            AppSchemaVisitor visitor =
                    new AppSchemaVisitor(NAMESPACE_PREFIX, TARGET_NAMESPACE, "meteo-stations-virtual-gml.xsd");
            model.accept(visitor);
            Document doc = visitor.getDocument();

            assertStationsMappingContainsViewRelation(doc);
            assertViewFeatureTypeMapping(doc);
        } finally {
            connection.close();
        }
    }

    private void assertStationsMappingContainsViewRelation(Document doc) {
        Element mapping = findFeatureTypeMapping(doc, "mt:MeteoStationsFeature");
        assertNotNull("Expected mapping for meteo_stations to be present", mapping);

        Element attributeMappings = getFirstChildElementByName(mapping, "attributeMappings");
        assertNotNull("Attribute mappings missing for meteo_stations", attributeMappings);

        boolean relationFound = false;
        for (Node node = attributeMappings.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && "AttributeMapping".equals(node.getNodeName())) {
                Element attributeMapping = (Element) node;
                String targetAttribute = getChildTextContent(attributeMapping, "targetAttribute");
                if ("vMeteoObservationsParameters".equals(targetAttribute)) {
                    Element sourceExpression = getFirstChildElementByName(attributeMapping, "sourceExpression");
                    assertNotNull("sourceExpression missing for virtual relationship", sourceExpression);
                    assertEquals(
                            "mt:VMeteoObservationsParametersFeature",
                            getChildTextContent(sourceExpression, "linkElement"));
                    assertEquals("FEATURE_LINK[1]", getChildTextContent(sourceExpression, "linkField"));
                    assertEquals("id", getChildTextContent(sourceExpression, "OCQL"));
                    relationFound = true;
                    break;
                }
            }
        }

        assertTrue(
                "Virtual relationship between meteo_stations and v_meteo_observations_parameters not found",
                relationFound);
    }

    private void assertViewFeatureTypeMapping(Document doc) {
        Element mapping = findFeatureTypeMapping(doc, "mt:VMeteoObservationsParametersFeature");
        assertNotNull("Expected mapping for v_meteo_observations_parameters view to be present", mapping);
        assertEquals("v_meteo_observations_parameters", getChildTextContent(mapping, "sourceType"));

        Element attributeMappings = getFirstChildElementByName(mapping, "attributeMappings");
        assertNotNull("Attribute mappings missing for view mapping", attributeMappings);

        boolean linkBackFound = false;
        for (Node node = attributeMappings.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && "AttributeMapping".equals(node.getNodeName())) {
                Element attributeMapping = (Element) node;
                String targetAttribute = getChildTextContent(attributeMapping, "targetAttribute");
                if ("FEATURE_LINK[1]".equals(targetAttribute)) {
                    Element sourceExpression = getFirstChildElementByName(attributeMapping, "sourceExpression");
                    assertNotNull("sourceExpression missing for view link back", sourceExpression);
                    if ("station_id".equals(getChildTextContent(sourceExpression, "OCQL"))) {
                        linkBackFound = true;
                        break;
                    }
                }
            }
        }

        assertTrue("View mapping does not expose the virtual FK link back to meteo_stations", linkBackFound);
    }

    private Element findFeatureTypeMapping(Document doc, String targetElementValue) {
        Node typeMappings = doc.getElementsByTagName("typeMappings").item(0);
        if (typeMappings == null) {
            return null;
        }
        for (Node node = typeMappings.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && "FeatureTypeMapping".equals(node.getNodeName())) {
                Element mapping = (Element) node;
                String targetElement = getChildTextContent(mapping, "targetElement");
                if (targetElementValue.equals(targetElement)) {
                    return mapping;
                }
            }
        }
        return null;
    }

    private Element getFirstChildElementByName(Element parent, String name) {
        for (Node node = parent.getFirstChild(); node != null; node = node.getNextSibling()) {
            if (node instanceof Element && name.equals(node.getNodeName())) {
                return (Element) node;
            }
        }
        return null;
    }

    private String getChildTextContent(Element parent, String childName) {
        if (parent == null) {
            return null;
        }
        Element element = getFirstChildElementByName(parent, childName);
        return element != null ? element.getTextContent() : null;
    }
}
