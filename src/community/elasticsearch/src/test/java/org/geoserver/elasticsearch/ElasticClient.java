/* (c) 2023 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.elasticsearch;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.geotools.data.elasticsearch.ElasticResponse;

/** ElasticSearch client interface */
interface ElasticClient extends Closeable {

    String RUN_AS = "es-security-runas-user";

    double getVersion();

    List<String> getTypes(String indexName) throws IOException;

    Map<String, Object> getMapping(String indexName, String type) throws IOException;

    ElasticResponse search(String searchIndices, String type, ElasticRequest request)
            throws IOException;

    ElasticResponse scroll(String scrollId, Integer scrollTime) throws IOException;

    @Override
    void close() throws IOException;

    void clearScroll(Set<String> scrollIds) throws IOException;

    void addTextAttribute(String index, String attributeName) throws IOException;
}
