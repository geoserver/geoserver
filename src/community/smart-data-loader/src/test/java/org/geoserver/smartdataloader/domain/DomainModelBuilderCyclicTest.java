/* (c) 2025 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.smartdataloader.domain;

import java.sql.DatabaseMetaData;
import org.geoserver.smartdataloader.AbstractJDBCSmartDataLoaderTestSupport;
import org.geoserver.smartdataloader.JDBCFixtureHelper;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.junit.Assert;
import org.junit.Test;

public abstract class DomainModelBuilderCyclicTest extends AbstractJDBCSmartDataLoaderTestSupport {

    public DomainModelBuilderCyclicTest(JDBCFixtureHelper fixtureHelper) {
        super(fixtureHelper);
    }

    @Test
    public void testDomainModelBuilderWithStationsAsRoot() throws Exception {
        DatabaseMetaData metaData = this.dataSource.getConnection().getMetaData();
        DataStoreMetadata dsm = this.getDataStoreMetadata(metaData);
        DomainModelConfig dmc = new DomainModelConfig();
        dmc.setRootEntityName("meteo_stations");
        DomainModelBuilder dmb = new DomainModelBuilder(dsm, dmc);

        DomainModel dm = dmb.buildDomainModel();
        DomainEntity meteoObservationsDomainEntity = dm.getRootEntity().getRelations().stream()
                .map(DomainRelation::getDestinationEntity)
                .filter(destinationEntity -> "meteo_observations".equals(destinationEntity.getName()))
                .findFirst()
                .orElseThrow();
        DomainEntity testEntity = meteoObservationsDomainEntity.getRelations().stream()
                .map(DomainRelation::getDestinationEntity)
                .filter(destinationEntity -> "meteo_test".equals(destinationEntity.getName()))
                .findFirst()
                .orElseThrow();
        Assert.assertNotNull(testEntity);
    }
}
