/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.config;

/**
 * Bean object to make importing existing repositories easier. Importing an existing GeoGig repo
 * will either consist of a Repository Name AND a {@link PostgresConfigBean}, OR just a single
 * Repository directory.
 */
public class ImportRepositoryFormBean {

    private String repoName;
    private String repoDirectory;
    private PostgresConfigBean pgBean = new PostgresConfigBean();

    public String getRepoName() {
        return repoName;
    }

    public void setRepoName(String repoName) {
        this.repoName = repoName;
    }

    public String getRepoDirectory() {
        return repoDirectory;
    }

    public void setRepoDirectory(String repoDirectory) {
        this.repoDirectory = repoDirectory;
    }

    public PostgresConfigBean getPgBean() {
        return pgBean;
    }

    public void setPgBean(PostgresConfigBean pgBean) {
        this.pgBean = pgBean;
    }
}
