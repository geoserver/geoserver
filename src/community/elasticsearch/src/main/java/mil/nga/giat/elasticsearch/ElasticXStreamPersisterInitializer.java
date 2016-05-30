package mil.nga.giat.elasticsearch;

import org.geoserver.config.util.XStreamPersister;
import org.geoserver.config.util.XStreamPersisterInitializer;

public class ElasticXStreamPersisterInitializer implements XStreamPersisterInitializer {

    @Override
    public void init(XStreamPersister persister) {
        persister.getXStream().allowTypes(new String[] {
                "mil.nga.giat.data.elasticsearch.ElasticAttribute"
        });
    }
}
