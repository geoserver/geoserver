/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

import java.io.Serializable;
import java.net.URI;
import java.util.Objects;

import com.google.common.base.Preconditions;

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

    private static final String SCHEME = "postgresql://";
    private static final String USER = "user";
    private static final String PASSWORD = "password";
    private static final String SLASH = "/";
    private static final String AMPERSAND = "&";
    private static final String QUESTION_MARK = "?";

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
        StringBuilder sb = new StringBuilder(128);
        sb.append(SCHEME).append(this.host);
        if (port > 0) {
            sb.append(":").append(port);
        }
        sb.append(SLASH).append(database);
        if (null != schema) {
            sb.append(SLASH).append(schema);
        }
        sb.append(SLASH).append(repoId);
        sb.append(QUESTION_MARK).append(USER).append("=").append(username);
        sb.append(AMPERSAND).append(PASSWORD).append("=").append(password);
        return URI.create(sb.toString());
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
        // build a bean from the parts
        String host = location.getHost();
        int port = location.getPort();
        // get the path and parse database, repo and schema
        String uriPath = location.getPath();
        // URI might have a leading '/'. If it does, skip it
        int startIndex = uriPath.startsWith(SLASH) ? 1 : 0;
        String[] paths = uriPath.substring(startIndex).split(SLASH);
        // first is always the database
        String database = paths[0];
        // second part is repoId if no other parts exist, otherwise it's schema
        String schema = null;
        if (paths.length > 2) {
            schema = paths[1];
        }
        // get the query and parse username and password
        String query = location.getQuery();
        String username = null;
        String password = null;
        for (String queryParam : query.split(AMPERSAND)) {
            int keyEnd = queryParam.indexOf("=");
            String key = queryParam.substring(0, keyEnd);
            String value = queryParam.substring(keyEnd + 1);
            if (USER.equals(key)) {
                username = value;
            } else if (PASSWORD.equals(key)) {
                password = value;
            }
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
        return paths[paths.length-1];
    }

    @Override
    public int hashCode() {
        // hs all the fields, if they aren't null, otherwise use some prime numbers as place holders
        return (host != null) ? host.hashCode() : 17 ^
                ((port != null) ? port.hashCode() : 37) ^
                ((username != null) ? username.hashCode() : 57) ^
                ((schema != null) ? schema.hashCode() : 97) ^
                ((password != null) ? password.hashCode() : 137) ^
                ((database != null) ? database.hashCode() : 197);
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
