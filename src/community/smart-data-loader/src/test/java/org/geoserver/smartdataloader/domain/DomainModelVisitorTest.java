package org.geoserver.smartdataloader.domain;

import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.DatabaseMetaData;
import org.apache.commons.io.IOUtils;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geoserver.smartdataloader.utils.LoggerDomainModelVisitor;
import org.junit.Test;

/** Tests related to a simple DomainModelVisitor (which logs DomainModel visited nodes) */
public abstract class DomainModelVisitorTest extends AbstractJDBCSmartDataLoaderTestSupport {

    public DomainModelVisitorTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
    }

    @Test
    public void testDomainModelVisitWithStations() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_stations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        LoggerDomainModelVisitor dmv = new LoggerDomainModelVisitor();
        dm.accept(dmv);

        try (InputStream is =
                DomainModelVisitorTest.class.getResourceAsStream("meteo-stations-logvisitor.txt")) {
            String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertTrue(dmv.getLog().contains(expected));
        }
    }

    @Test
    public void testDomainModelVisitWithObservations() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_observations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        LoggerDomainModelVisitor dmv = new LoggerDomainModelVisitor();
        dm.accept(dmv);

        try (InputStream is =
                DomainModelVisitorTest.class.getResourceAsStream(
                        "meteo-observations-logvisitor.txt")) {
            String expected = IOUtils.toString(is, StandardCharsets.UTF_8);
            assertTrue(dmv.getLog().contains(expected));
        }
    }
}
