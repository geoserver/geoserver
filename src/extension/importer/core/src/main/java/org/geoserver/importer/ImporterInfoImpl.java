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

    public ImporterInfoImpl() {}

    public ImporterInfoImpl(ImporterInfo configuration) {
        this.uploadRoot = configuration.getUploadRoot();
        this.maxSynchronousImports = configuration.getMaxSynchronousImports();
        this.maxAsynchronousImports = configuration.getMaxAsynchronousImports();
    }

    public int getMaxSynchronousImports() {
        return maxSynchronousImports;
    }

    public void setMaxSynchronousImports(int maxSynchronousImports) {
        this.maxSynchronousImports = maxSynchronousImports;
    }

    @Override
    public int getMaxAsynchronousImports() {
        return maxAsynchronousImports;
    }

    public void setMaxAsynchronousImports(int maxAsynchronousImports) {
        this.maxAsynchronousImports = maxAsynchronousImports;
    }

    @Override
    public String getUploadRoot() {
        return uploadRoot;
    }

    public void setUploadRoot(String uploadRoot) {
        this.uploadRoot = uploadRoot;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ImporterInfoImpl that = (ImporterInfoImpl) o;
        return maxSynchronousImports == that.maxSynchronousImports
                && maxAsynchronousImports == that.maxAsynchronousImports
                && Objects.equals(uploadRoot, that.uploadRoot);
    }

    @Override
    public int hashCode() {
        return Objects.hash(uploadRoot, maxSynchronousImports, maxAsynchronousImports);
    }
}
