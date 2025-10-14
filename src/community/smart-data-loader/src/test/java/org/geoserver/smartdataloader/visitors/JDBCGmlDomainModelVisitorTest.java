package org.geoserver.smartdataloader.visitors;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.sql.DatabaseMetaData;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.commons.lang3.StringUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.visitors.gml.GmlSchemaVisitor;
import org.junit.Test;
import org.w3c.dom.Document;

/** Tests related to GMLDomainModelVisitor */
public abstract class JDBCGmlDomainModelVisitorTest extends AbstractJDBCSmartDataLoaderTestSupport {

    private final String entityPrefix;

    public JDBCGmlDomainModelVisitorTest(JDBCFixtureHelper fixtureHelper) {
        this(fixtureHelper, null);
    }

    public JDBCGmlDomainModelVisitorTest(JDBCFixtureHelper fixtureHelper, String entityPrefix) {
        super(fixtureHelper);
        NAMESPACE_PREFIX = "mt";
        TARGET_NAMESPACE = "http://www.geo-solutions.it/smartappschema/1.0";
        this.entityPrefix = StringUtils.isBlank(entityPrefix) ? "" : entityPrefix.trim();
    }

    @Test
    public void testObservationsRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_observations");
        dmc.setEntitiesPrefix(this.entityPrefix);
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        GmlSchemaVisitor dmv = new GmlSchemaVisitor(NAMESPACE_PREFIX, TARGET_NAMESPACE);
        dm.accept(dmv);

        try (InputStream is =
                JDBCGmlDomainModelVisitorTest.class.getResourceAsStream(entityPrefix + "meteo-observations-gml.xsd")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertTrue(d.similar());
        }
    }

    @Test
    public void testStationsRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_stations");
        dmc.setEntitiesPrefix(this.entityPrefix);
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        GmlSchemaVisitor dmv = new GmlSchemaVisitor(NAMESPACE_PREFIX, TARGET_NAMESPACE);
        dm.accept(dmv);

        try (InputStream is =
                JDBCGmlDomainModelVisitorTest.class.getResourceAsStream(entityPrefix + "meteo-stations-gml.xsd")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());
            assertTrue(d.similar());
        }
    }

    @Test
    public void testParametersRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_parameters");
        dmc.setEntitiesPrefix(this.entityPrefix);
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        GmlSchemaVisitor dmv = new GmlSchemaVisitor(NAMESPACE_PREFIX, TARGET_NAMESPACE);
        dm.accept(dmv);

        try (InputStream is =
                JDBCGmlDomainModelVisitorTest.class.getResourceAsStream(entityPrefix + "meteo-parameters-gml.xsd")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertTrue(d.similar());
        }
    }

    @Test
    public void testMaintainersRootEntity() throws Exception {
        DatabaseMetaData metaData = dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_maintainers");
        dmc.setEntitiesPrefix(this.entityPrefix);
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        GmlSchemaVisitor dmv = new GmlSchemaVisitor(NAMESPACE_PREFIX, TARGET_NAMESPACE);
        dm.accept(dmv);

        try (InputStream is =
                JDBCGmlDomainModelVisitorTest.class.getResourceAsStream(entityPrefix + "meteo-maintainers-gml.xsd")) {
            DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
            DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
            Document control = dBuilder.parse(is);
            XMLUnit.setIgnoreWhitespace(true);
            XMLUnit.setIgnoreComments(true);

            Diff d = XMLUnit.compareXML(control, dmv.getDocument());

            assertTrue(d.similar());
        }
    }
}
