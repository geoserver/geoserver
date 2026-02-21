/* (c) 2014 - 2025 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.gwc.layer;

import java.util.Collections;
import org.geoserver.catalog.AttributionInfo;
import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.CatalogVisitor;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.PublishedInfo;
import org.geoserver.catalog.PublishedType;

/**
 * Lightweight fallback implementation of {@link PublishedInfo} used when a PublishedInfo cannot be found in the
 * catalog. Kept package private since it is only useful within the GWC layer package.
 */
final class MissingPublishedInfo implements PublishedInfo {
    private final String id;
    private final String name;
    private final PublishedType type = PublishedType.GROUP;
    private final MetadataMap meta = new MetadataMap();

    MissingPublishedInfo(String id, String name) {
        this.id = id == null ? "missing" : id;
        this.name = name == null ? "<missing>" : name;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        // no-op
    }

    @Override
    public String prefixedName() {
        return name;
    }

    @Override
    public String getTitle() {
        return null;
    }

    @Override
    public void setTitle(String title) {
        // no-op
    }

    @Override
    public org.geotools.api.util.InternationalString getInternationalTitle() {
        return null;
    }

    @Override
    public void setInternationalTitle(org.geotools.api.util.InternationalString internationalTitle) {
        // no-op
    }

    @Override
    public String getAbstract() {
        return null;
    }

    @Override
    public void setAbstract(String abstractTxt) {
        // no-op
    }

    @Override
    public org.geotools.api.util.InternationalString getInternationalAbstract() {
        return null;
    }

    @Override
    public void setInternationalAbstract(org.geotools.api.util.InternationalString internationalAbstract) {
        // no-op
    }

    @Override
    public MetadataMap getMetadata() {
        return meta;
    }

    @Override
    public java.util.List<AuthorityURLInfo> getAuthorityURLs() {
        return Collections.emptyList();
    }

    @Override
    public java.util.List<LayerIdentifierInfo> getIdentifiers() {
        return Collections.emptyList();
    }

    @Override
    public PublishedType getType() {
        return type;
    }

    @Override
    public AttributionInfo getAttribution() {
        return null;
    }

    @Override
    public void setAttribution(AttributionInfo attribution) {
        // no-op
    }

    @Override
    public boolean isEnabled() {
        return false;
    }

    @Override
    public void setEnabled(boolean enabled) {
        // no-op
    }

    @Override
    public boolean isAdvertised() {
        return false;
    }

    @Override
    public void setAdvertised(boolean advertised) {
        // no-op
    }

    @Override
    public void accept(CatalogVisitor visitor) {
        // no-op
    }

    @Override
    public java.util.Date getDateModified() {
        return null;
    }

    @Override
    public java.util.Date getDateCreated() {
        return null;
    }

    @Override
    public void setDateCreated(java.util.Date dateCreated) {
        // no-op
    }

    @Override
    public void setDateModified(java.util.Date dateModified) {
        // no-op
    }

    @Override
    public String getModifiedBy() {
        return null;
    }

    @Override
    public void setModifiedBy(String userName) {
        // no-op
    }
}
