/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer;

import java.util.Objects;

/** See @{@link ImporterInfo} */
public class ImporterInfoImpl implements ImporterInfo {

    String uploadRoot;
    int maxSynchronousImports;
    int maxAsynchronousImports;
    double contextExpiration = 1440;

    public ImporterInfoImpl() {}

    public ImporterInfoImpl(ImporterInfo configuration) {
        this.uploadRoot = configuration.getUploadRoot();
        this.maxSynchronousImports = configuration.getMaxSynchronousImports();
        this.maxAsynchronousImports = configuration.getMaxAsynchronousImports();
        this.contextExpiration = configuration.getContextExpiration();
    }

    @Override
    public int getMaxSynchronousImports() {
        return maxSynchronousImports;
    }

    @Override
    public void setMaxSynchronousImports(int maxSynchronousImports) {
        this.maxSynchronousImports = maxSynchronousImports;
    }

    @Override
    public int getMaxAsynchronousImports() {
        return maxAsynchronousImports;
    }

    @Override
    public void setMaxAsynchronousImports(int maxAsynchronousImports) {
        this.maxAsynchronousImports = maxAsynchronousImports;
    }

    @Override
    public String getUploadRoot() {
        return uploadRoot;
    }

    @Override
    public void setUploadRoot(String uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    @Override
    public double getContextExpiration() {
        return contextExpiration;
    }

    @Override
    public void setContextExpiration(double contextExpiration) {
        this.contextExpiration = contextExpiration;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImporterInfoImpl that = (ImporterInfoImpl) o;
        return maxSynchronousImports == that.maxSynchronousImports
                && maxAsynchronousImports == that.maxAsynchronousImports
                && contextExpiration == that.contextExpiration
                && Objects.equals(uploadRoot, that.uploadRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
                uploadRoot, maxSynchronousImports, maxAsynchronousImports, contextExpiration);
    }
}
