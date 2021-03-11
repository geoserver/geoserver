package org.geoserver.smartdataloader.utils;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.geoserver.smartdataloader.domain.DomainModelVisitorImpl;
import org.geoserver.smartdataloader.domain.entities.DomainEntity;
import org.geoserver.smartdataloader.domain.entities.DomainEntitySimpleAttribute;
import org.geoserver.smartdataloader.domain.entities.DomainModel;
import org.geoserver.smartdataloader.domain.entities.DomainRelation;
import org.geoserver.smartdataloader.metadata.DataStoreMetadata;
import org.geotools.util.logging.Logging;

/** Dummy DomainModelVisitor that logs visited items in a StringBuilder */
public class LoggerDomainModelVisitor extends DomainModelVisitorImpl {

    private static final Logger LOGGER = Logging.getLogger(LoggerDomainModelVisitor.class);
    private StringBuilder internalLogger = new StringBuilder();

    @Override
    public void visitDataStoreMetadata(DataStoreMetadata dataStoreMetadata) {
        String ds = dataStoreMetadata.getDataStoreMetadataConfig().toString();
        LOGGER.log(Level.INFO, ds);
        internalLogger.append(ds + "\n");
    }

    @Override
    public void visitDomainModel(DomainModel domainModel) {
        String dm = domainModel.getClass().getName();
        LOGGER.log(Level.INFO, dm);
        internalLogger.append(dm + "\n");
    }

    @Override
    public void visitDomainRootEntity(DomainEntity entity) {
        String de = entity.getName();
        LOGGER.log(Level.INFO, de);
        internalLogger.append(de + "\n");
    }

    @Override
    public void visitDomainChainedEntity(DomainEntity entity) {
        String de = entity.getName();
        LOGGER.log(Level.INFO, de);
        internalLogger.append(de + "\n");
    }

    @Override
    public void visitDomainEntitySimpleAttribute(DomainEntitySimpleAttribute domainAttribute) {
        String da = domainAttribute.getName();
        LOGGER.log(Level.INFO, domainAttribute.getName());
        internalLogger.append(da + "\n");
    }

    @Override
    public void visitDomainRelation(DomainRelation domainRelation) {
        String dr =
                domainRelation.getContainingEntity().getName()
                        + " -> "
                        + domainRelation.getDestinationEntity().getName();
        LOGGER.log(Level.INFO, dr);
        internalLogger.append(dr + "\n");
    }

    public String getLog() {
        return internalLogger.toString();
    }
}
