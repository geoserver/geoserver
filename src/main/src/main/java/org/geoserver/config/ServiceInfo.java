/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.config;

import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.geoserver.catalog.Info;
import org.geoserver.catalog.KeywordInfo;
import org.geoserver.catalog.MetadataLinkInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.api.util.InternationalString;
import org.geotools.util.Version;

/**
 * Generic / abstract service configuration.
 *
 * @author Justin Deoliveira, The Open Planning Project
 */
public interface ServiceInfo extends Info {

    /** Identifier. */
    @Override
    String getId();

    /**
     * Service type, used to identify services for availability (example {@code WMS}, {@code WFS}, ...).
     *
     * <p>Service type is used internally by GeoServer, and is not subject to change by user like {@link #getName()} and
     * {@link #getTitle()}.
     *
     * <p>See {@link org.geoserver.catalog.ServiceResourceProvider#getServicesForResource(ResourceInfo)} and
     * {@link ResourceInfo#getDisabledServices()} for example use.
     *
     * @return Service type.
     */
    String getType();

    /**
     * Name of the service, unique within all instances of ServiceInfo within global services (or within workspace
     * services).
     *
     * <p>The name is used in GetCapabilities output and may user supplied (do not assume this to be {@code WMS},
     * {@code WFS}, {@code WCS} - an example may be {@code GeoServer WMS}.
     *
     * @return Name of service
     * @uml.property name="name"
     */
    String getName();

    /**
     * Sets the name of the service.
     *
     * @uml.property name="name"
     */
    void setName(String name);

    /** The workspace the service is specific or local to, or {@code null} if the service is global. */
    WorkspaceInfo getWorkspace();

    /** Sets the workspace the service is specific or local to. */
    void setWorkspace(WorkspaceInfo workspace);

    /**
     * The global geoserver configuration.
     *
     * @uml.property name="geoServer"
     * @uml.associationEnd inverse="service:org.geoserver.config.GeoServerInfo"
     */
    GeoServer getGeoServer();

    /**
     * Sets the global geoserver configuration.
     *
     * @uml.property name="geoServer"
     */
    void setGeoServer(GeoServer geoServer);

    /** @uml.property name="citeCompliant" */
    boolean isCiteCompliant();

    /** @uml.property name="citeCompliant" */
    void setCiteCompliant(boolean citeCompliant);

    /** @uml.property name="enabled" */
    boolean isEnabled();

    /** @uml.property name="enabled" */
    void setEnabled(boolean enabled);

    /** @uml.property name="onlineResource" */
    String getOnlineResource();

    /** @uml.property name="onlineResource" */
    void setOnlineResource(String onlineResource);

    /** @uml.property name="title" */
    String getTitle();

    /** @uml.property name="title" */
    void setTitle(String title);

    /** @uml.property name="internationalTitle" */
    InternationalString getInternationalTitle();

    /** @uml.property name="internationalTitle" */
    void setInternationalTitle(InternationalString title);

    /** @uml.property name="abstract" */
    String getAbstract();

    /** @uml.property name="abstract" */
    void setAbstract(String abstrct);

    /** @uml.property name="internationalAbstract" */
    InternationalString getInternationalAbstract();

    /** @uml.property name="internationalAbstract" */
    void setInternationalAbstract(InternationalString title);

    /** @uml.property name="maintainer" */
    String getMaintainer();

    /** @uml.property name="maintainer" */
    void setMaintainer(String maintainer);

    /** @uml.property name="fees" */
    String getFees();

    /** @uml.property name="fees" */
    void setFees(String fees);

    /** @uml.property name="accessConstraints" */
    String getAccessConstraints();

    /** @uml.property name="accessConstraints" */
    void setAccessConstraints(String accessConstraints);

    /**
     * The versions of the service that are available.
     *
     * <p>This list contains objects of type {@link Version}.
     *
     * @uml.property name="versions"
     */
    List<Version> getVersions();

    /**
     * Keywords associated with the service.
     *
     * @uml.property name="keywords"
     */
    List<KeywordInfo> getKeywords();

    /** List of keyword values, derived from {@link #getKeywords()}. */
    List<String> keywordValues();

    /**
     * Exception formats the service can provide.
     *
     * @uml.property name="exceptionFormats"
     */
    List<String> getExceptionFormats();

    /**
     * The service metadata link.
     *
     * @uml.property name="metadataLink"
     * @uml.associationEnd inverse="service:org.geoserver.catalog.MetadataLinkInfo"
     */
    MetadataLinkInfo getMetadataLink();

    /**
     * Setter of the property <tt>metadataLink</tt>
     *
     * @uml.property name="metadataLink"
     */
    void setMetadataLink(MetadataLinkInfo metadataLink);

    /**
     * Sets the output strategy used by the service.
     *
     * <p>This value is an identifier which indicates how the output of a response should behave. An example might be
     * "performance", indicating that the response should be encoded as quickly as possible.
     */
    String getOutputStrategy();

    /** Sets the output strategy. */
    void setOutputStrategy(String outputStrategy);

    /** The base url for the schemas describing the service. */
    String getSchemaBaseURL();

    /** Sets the base url for the schemas describing the service. */
    void setSchemaBaseURL(String schemaBaseURL);

    /** Flag indicating if the service should be verbose or not. */
    boolean isVerbose();

    /** Sets the flag indicating if the service should be verbose or not. */
    void setVerbose(boolean verbose);

    /** @uml.property name="metadata" */
    MetadataMap getMetadata();

    Map<Object, Object> getClientProperties();

    Locale getDefaultLocale();

    void setDefaultLocale(Locale locale);
}
