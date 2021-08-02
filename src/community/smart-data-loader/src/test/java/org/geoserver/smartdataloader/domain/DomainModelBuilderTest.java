package org.geoserver.smartdataloader.domain;

import static org.junit.Assert.assertEquals;

import java.sql.DatabaseMetaData;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geotools.util.logging.Logging;
import org.junit.Test;

/** Tests for DomainModelBuilder class. */
public abstract class DomainModelBuilderTest extends AbstractJDBCSmartDataLoaderTestSupport {

    private static final Logger LOGGER = Logging.getLogger(DomainModelBuilderTest.class);

    public DomainModelBuilderTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
    }

    @Test
    public void testDomainModelBuilderWithRootEntityFailure() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_failure");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);

        DomainModel dm = null;
        try {
            dm = dmb.buildDomainModel();
            LOGGER.log(Level.INFO, dm.toString());
        } catch (RuntimeException e) {
            assertEquals(dm, null);
        }
    }

    @Test
    public void testDomainModelBuilderWithStationsAsRoot() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_stations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);

        DomainModel dm = null;
        dm = dmb.buildDomainModel();
        LOGGER.log(Level.INFO, dm.toString());

        // check build domainmodel
        int rootMeteoStationsRelationsSize = dm.getRootEntity().getRelations().size();
        DomainRelation meteoStationRelation1 = dm.getRootEntity().getRelations().get(0);
        String containingEntityNameMeteoStationsRelation1 =
                meteoStationRelation1.getContainingEntity().getName();
        String destinationEntityNameMeteoStationsRelation1 =
                meteoStationRelation1.getDestinationEntity().getName();

        DomainRelation meteoStationRelation2 = dm.getRootEntity().getRelations().get(1);
        String containingEntityNameMeteoStationsRelation2 =
                meteoStationRelation2.getContainingEntity().getName();
        String destinationEntityNameMeteoStationsRelation2 =
                meteoStationRelation2.getDestinationEntity().getName();
        int meteoObservationsRelationsSize =
                meteoStationRelation1.getDestinationEntity().getRelations().size();
        DomainRelation meteoObservationRelation =
                meteoStationRelation1.getDestinationEntity().getRelations().get(0);
        int meteoMaintainersRelationSize =
                meteoStationRelation2.getDestinationEntity().getRelations().size();
        String destinationEntityNameMeteoObservationsRelation =
                meteoObservationRelation.getDestinationEntity().getName();
        int meteoParamatersRelationsSize =
                meteoObservationRelation.getDestinationEntity().getRelations().size();

        assertEquals(4, dm.getRootEntity().getAttributes().size());
        assertEquals(2, rootMeteoStationsRelationsSize);
        assertEquals("meteo_stations", containingEntityNameMeteoStationsRelation1);
        assertEquals("meteo_observations", destinationEntityNameMeteoStationsRelation1);
        assertEquals("meteo_stations", containingEntityNameMeteoStationsRelation2);
        assertEquals("meteo_stations_maintainers", destinationEntityNameMeteoStationsRelation2);
        assertEquals(1, meteoObservationsRelationsSize);
        assertEquals(1, meteoMaintainersRelationSize);
        assertEquals("meteo_parameters", destinationEntityNameMeteoObservationsRelation);
        assertEquals(0, meteoParamatersRelationsSize);
    }

    @Test
    public void testDomainModelBuilderWithParametersAsRoot() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_parameters");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);

        DomainModel dm = null;
        dm = dmb.buildDomainModel();
        LOGGER.log(Level.INFO, dm.toString());

        // check build domainmodel
        int rootMeteoParametersRelationsSize = dm.getRootEntity().getRelations().size();
        DomainRelation meteoParameterRelation = dm.getRootEntity().getRelations().get(0);
        String containingEntityNameMeteoParametersRelation =
                meteoParameterRelation.getContainingEntity().getName();
        String destinationEntityNameMeteoParametersRelation =
                meteoParameterRelation.getDestinationEntity().getName();
        int meteoObservationsRelationsSize =
                meteoParameterRelation.getDestinationEntity().getRelations().size();
        DomainRelation meteoObservationRelation =
                meteoParameterRelation.getDestinationEntity().getRelations().get(0);
        String destinationEntityNameMeteoObservationsRelation =
                meteoObservationRelation.getDestinationEntity().getName();
        int meteoStationsRelationsSize =
                meteoObservationRelation.getDestinationEntity().getRelations().size();

        assertEquals(3, dm.getRootEntity().getAttributes().size());
        assertEquals(1, rootMeteoParametersRelationsSize);
        assertEquals("meteo_parameters", containingEntityNameMeteoParametersRelation);
        assertEquals("meteo_observations", destinationEntityNameMeteoParametersRelation);
        assertEquals(1, meteoObservationsRelationsSize);
        assertEquals("meteo_stations", destinationEntityNameMeteoObservationsRelation);
        assertEquals(1, meteoStationsRelationsSize);
    }

    @Test
    public void testDomainModelBuilderWithObservationsAsRoot() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_observations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);

        DomainModel dm = null;
        dm = dmb.buildDomainModel();
        LOGGER.log(Level.INFO, dm.toString());

        // check build domainmodel
        int rootMeteoObservationsRelationsSize = dm.getRootEntity().getRelations().size();
        // check we have 2 relations
        assertEquals(2, rootMeteoObservationsRelationsSize);
        // check relations containing and destination entity, as much as relations from that
        // entities
        List<DomainRelation> meteoObservationsRelations = dm.getRootEntity().getRelations();
        Map<String, DomainRelation> map = new HashMap<>();
        // put every relation in a map, with key based on destination entity
        for (DomainRelation dr : meteoObservationsRelations) {
            map.put(dr.getDestinationEntity().getName(), dr);
        }
        DomainRelation relationSt = map.get("meteo_stations");
        DomainRelation relationParam = map.get("meteo_parameters");
        int relationSizeSt = relationSt.getDestinationEntity().getRelations().size();
        int relationSizeParam = relationParam.getDestinationEntity().getRelations().size();
        assertEquals(1, relationSizeSt);
        assertEquals(0, relationSizeParam);
        String stContainingEntity = relationSt.getContainingEntity().getName();
        String paramContainingEntity = relationParam.getContainingEntity().getName();
        assertEquals(stContainingEntity, paramContainingEntity);
        assertEquals("meteo_observations", stContainingEntity);
        assertEquals(4, dm.getRootEntity().getAttributes().size());
    }
}
