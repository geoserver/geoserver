package org.geoserver.smartdataloader.data.store;

import static org.junit.Assert.assertEquals;

import java.sql.DatabaseMetaData;
import java.util.ArrayList;
import java.util.List;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.domain.DomainModelBuilder;
import org.geoserver.smartdataloader.domain.DomainModelConfig;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.junit.Test;

/** Tests related to ExclusionsDomainModelVisitor. */
public abstract class JDBCExclusionsDomainModelVisitorTest
        extends AbstractJDBCSmartDataLoaderTestSupport {

    public JDBCExclusionsDomainModelVisitorTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
    }

    @Test
    public void testDomainModelVisitWithExclusions() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_stations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);
        DomainModel dm = dmb.buildDomainModel();
        List<String> exclusions = new ArrayList<>();
        exclusions.add("meteo_stations.code");
        exclusions.add("meteo_stations.common_name");
        exclusions.add("meteo_stations.position");
        exclusions.add("meteo_observations.value");
        exclusions.add("meteo_parameters.param_unit");
        exclusions.add("meteo_parameters.param_name");
        DomainModel newDomainModel = ExclusionsDomainModelVisitor.buildDomainModel(dm, exclusions);
        assertEquals(1, newDomainModel.getRootEntity().getAttributes().size()); // only id
        assertEquals("id", newDomainModel.getRootEntity().getAttributes().get(0).getName());
        assertEquals(
                2,
                newDomainModel
                        .getRootEntity()
                        .getRelations()
                        .size()); // meteo_stations.meteo_observations
        assertEquals(
                "meteo_observations",
                newDomainModel
                        .getRootEntity()
                        .getRelations()
                        .get(0)
                        .getDestinationEntity()
                        .getName()); // meteo_observations
        assertEquals(
                "meteo_stations_maintainers",
                newDomainModel
                        .getRootEntity()
                        .getRelations()
                        .get(1)
                        .getDestinationEntity()
                        .getName()); // meteo_stations_maintainers
        assertEquals(
                1,
                newDomainModel
                        .getRootEntity()
                        .getRelations()
                        .get(0)
                        .getDestinationEntity()
                        .getRelations()
                        .size()); // meteo_parameters
    }
}
