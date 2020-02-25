/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.test.onlineTest;

import java.io.File;
import java.io.IOException;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

/** Solr index data setup utility class */
public class SolrIndexSetup {

    public static final String SCHEMA_PATH = "/schema";
    public static final String UPDATE_PATH = "/update/json/docs?commit=true";
    public static final String USER_AGENT = "Mozilla/5.0";
    private String url;

    public SolrIndexSetup(String url) {
        this.url = url;
    }

    /** initialize index data on solr */
    public void init() {
        // post geom type to "http://localhost:8983/solr/stations/schema"
        postJson(url + SCHEMA_PATH, "geomType.txt");
        // post fields
        postJson(url + SCHEMA_PATH, "createFields.txt");
        // post data
        postJson(url + UPDATE_PATH, "insertData.txt");
    }

    /**
     * utility method to send a json post request to a web API
     *
     * @return status code
     */
    protected int postJson(String url, String fileName) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost post = new HttpPost(url);
            post.setHeader("User-Agent", USER_AGENT);
            // inject content post data
            File jsonFile =
                    new File(
                            getClass()
                                    .getClassLoader()
                                    .getResource("test-data/" + fileName)
                                    .getFile());
            FileEntity entity = new FileEntity(jsonFile, ContentType.APPLICATION_JSON);
            post.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(post)) {
                return response.getStatusLine().getStatusCode();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
