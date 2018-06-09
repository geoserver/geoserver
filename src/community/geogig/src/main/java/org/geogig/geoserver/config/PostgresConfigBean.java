/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import com.google.common.base.Preconditions;
import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.MultiValueMap;
import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Simple bean that contains PostgreSQL specific configuration parameters for connecting to a GeoGig
 * PostgreSQL backend. Instances of this bean can be wrapped inside a Wicket IModel implementation
 * and used to build GeoGig repository URI location in a PostgreSQL database. Note that the URI
 * location cannot be built solely from an instance of this bean, but must be supplied a repository
 * ID.
 *
 * @see org.geogig.geoserver.web.repository.GeoGigRepositoryInfoFormComponent
 */
public class PostgresConfigBean implements Serializable {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresConfigBean.class);

    private static final String SCHEME = "postgresql";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SLASH = "/";
    private static final String UTF8 = StandardCharsets.UTF_8.name();

    private String host = "localhost", database, schema = "public", username = "postgres", password;
    private Integer port = 5432;

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    public String getSchema() {
        return schema;
    }

    public void setSchema(String schema) {
        this.schema = schema;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getPort() {
        return port;
    }

    public void setPort(Integer port) {
        this.port = port;
    }

    public URI buildUriForRepo(String repoId) {
        UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
        // set the schema
        builder.scheme(SCHEME);
        // set the host
        builder.host(host);
        // set the port
        if (port > 0) {
            builder.port(port);
        }
        // build the path in the form of "/databaseName/schema/repoID"
        StringBuilder sb = new StringBuilder(128);
        sb.append(SLASH).append(database);
        if (null != schema) {
            sb.append(SLASH).append(schema);
        }
        sb.append(SLASH).append(repoId);
        builder.path(sb.toString());
        // set the query parameters
        String encodedUsername = username;
        String encodedPassword = password;
        try {
            // try to URLEncode the username value, using UTF-8 encoding
            encodedUsername = URLEncoder.encode(username, UTF8);
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn(
                    String.format(
                            "Error encoding PostgreSQL username in UTF-8, attempting to use unencoded value: %s",
                            username),
                    uee);
        }
        try {
            encodedPassword = URLEncoder.encode(password, UTF8);
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn(
                    "Error encoding PostgreSQL password value, attempting to use unencoded value",
                    uee);
        }
        builder.queryParam(USER, encodedUsername);
        builder.queryParam(PASSWORD, encodedPassword);
        // return the URI
        return builder.build(true).toUri();
    }

    public static PostgresConfigBean newInstance() {
        return new PostgresConfigBean();
    }

    public static PostgresConfigBean from(URI location) {
        Preconditions.checkNotNull(location, "Cannot parse NULL URI location");
        Preconditions.checkNotNull(location.getScheme(), "Cannot parse NULL URI scheme");
        if (!"postgresql".equals(location.getScheme())) {
            // don't parse, return new object
            return newInstance();
        }
        UriComponents uri = UriComponentsBuilder.fromUri(location).build();
        // build a bean from the parts
        String host = uri.getHost();
        int port = uri.getPort();
        // get the path and parse database, repo and schema
        String uriPath = uri.getPath();
        // Path might have a leading '/'. If it does, skip it
        int startIndex = uriPath.startsWith(SLASH) ? 1 : 0;
        String[] paths = uriPath.substring(startIndex).split(SLASH);
        // first is always the database
        String database = paths[0];
        // second part is repoId if no other parts exist, otherwise it's schema
        String schema = null;
        if (paths.length > 2) {
            schema = paths[1];
        }
        // get the query parameters and pull out user and password
        MultiValueMap<String, String> queryParams = uri.getQueryParams();
        String username = queryParams.getFirst(USER);
        String password = queryParams.getFirst(PASSWORD);
        try {
            // username should be URLEncoded, decode it here
            username = URLDecoder.decode(username, UTF8);
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn(
                    String.format(
                            "Error decoding PostgreSQL username value, attempting to use undecoded value: %s",
                            username),
                    uee);
        }
        try {
            // password should be URLEncoded, decode it here
            password = URLDecoder.decode(password, UTF8);
        } catch (UnsupportedEncodingException uee) {
            LOGGER.warn(
                    "Error decoding PostgreSQL password value, attempting to use undecoded value",
                    uee);
        }
        PostgresConfigBean bean = new PostgresConfigBean();
        bean.setHost(host);
        bean.setPort(port);
        bean.setDatabase(database);
        bean.setSchema(schema);
        bean.setUsername(username);
        bean.setPassword(password);
        return bean;
    }

    public static String parseRepoId(URI location) {
        // get the path and parse database, repo and schema
        String uriPath = location.getPath();
        // URI might have a leading '/'. If it does, skip it
        int startIndex = uriPath.startsWith(SLASH) ? 1 : 0;
        String[] paths = uriPath.substring(startIndex).split(SLASH);
        // last part is the repoID
        return paths[paths.length - 1];
    }

    @Override
    public int hashCode() {
        // hash all the fields, if they aren't null, otherwise use some prime numbers as place
        // holders
        return (host != null)
                ? host.hashCode()
                : 17
                        ^ ((port != null) ? port.hashCode() : 37)
                        ^ ((username != null) ? username.hashCode() : 57)
                        ^ ((schema != null) ? schema.hashCode() : 97)
                        ^ ((password != null) ? password.hashCode() : 137)
                        ^ ((database != null) ? database.hashCode() : 197);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final PostgresConfigBean other = (PostgresConfigBean) obj;
        if (!Objects.equals(this.host, other.host)) {
            return false;
        }
        if (!Objects.equals(this.database, other.database)) {
            return false;
        }
        if (!Objects.equals(this.schema, other.schema)) {
            return false;
        }
        if (!Objects.equals(this.username, other.username)) {
            return false;
        }
        if (!Objects.equals(this.password, other.password)) {
            return false;
        }
        if (!Objects.equals(this.port, other.port)) {
            return false;
        }
        return true;
    }
}
