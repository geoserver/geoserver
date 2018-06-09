/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.ows;

import java.net.URL;
import java.util.List;
import java.util.Map;

/**
 * Bean containing the properties of an Open Web Service (OWS).
 *
 * <p>The properties of this bean are meant to configure the behaviour of an open web service. The
 * bean is a property java bean, only getters and setters apply.
 *
 * <p>This class contains properties common to many types of open web services. It is intended to be
 * subclassed by services who require addional properties.
 *
 * @author Justin Deoliveira, The Open Planning Project, jdeolive@openplans.org
 * @deprecated use {@link org.geoserver.config.ServiceInfo}
 */
public interface OWS {
    /// **
    // * id of the service
    // */
    // private String id;
    //
    /// **
    // * Service enabled flag
    // */
    // private boolean enabled;
    //
    /// **
    // * Url pointing to the service
    // */
    // private URL onlineResource;
    //
    /// **
    // * Name
    // */
    // private String name;
    //
    /// **
    // * Title
    // */
    // private String title;
    //
    /// **
    // * Abstract
    // */
    // private String serverAbstract;
    //
    /// **
    // * Fees for using the service.
    // */
    // private String fees;
    //
    /// **
    // * Service access constraints.
    // */
    // private String accessConstraints;
    //
    /// **
    // * Maintainer of the service.
    // */
    // private String maintainer;
    //
    /// **
    // * List of keywords associated with the service.
    // */
    // private List keywords;
    //
    /// **
    // * Client properties
    // */
    // private Map clientProperties;
    //
    /// **
    // * Flag indicating wether the service should be verbose or not.
    // */
    // private boolean isVerbose;
    //
    /// **
    // * Number of decimals used when encoding data.
    // */
    // private int numDecimals;
    //
    /// **
    // * Locationz used to look up schemas.
    // */
    // private String schemaBaseURL = "http://schemas.opengis.net";

    /// **
    // * Creates a new OWS.
    // *
    // * @param id The id of the service.
    // */
    // public OWS(String id) {
    //    this.id = id;
    // }

    /// **
    // * Protected constructor for subclass use.
    // */
    // protected OWS() {
    // }

    /** @return The identifier of the service */
    public String getId();
    // {
    //    return id;
    // }

    /** id setter for subclasses. */
    // protected void setId(String id)
    // {
    //    this.id = id;
    // }

    /**
     * Returns whether is service is enabled.
     *
     * @return true when enabled.
     */
    public boolean isEnabled();
    // {
    //    return enabled;
    // }

    public void setEnabled(boolean enabled);
    // {
    //    this.enabled = enabled;
    // }

    /**
     * Returns the Online Resource for this Service.
     *
     * @return URL The Online resource.
     */
    public URL getOnlineResource();
    // {
    //    return onlineResource;
    // }

    public void setOnlineResource(URL onlineResource);
    // {
    //    this.onlineResource = onlineResource;
    // }

    /**
     * A description of this service.
     *
     * @return String This Service's abstract.
     */
    public String getAbstract();
    // {
    //    return serverAbstract;
    // }

    public void setAbstract(String serverAbstract);
    // {
    //    this.serverAbstract = serverAbstract;
    // }

    /**
     * A description of this service's access constraints.
     *
     * @return String This service's access constraints.
     */
    public String getAccessConstraints();
    // {
    //    return accessConstraints;
    // }

    public void setAccessConstraints(String accessConstraints);
    // {
    //    this.accessConstraints = accessConstraints;
    // }

    /**
     * A description of the fees for this service.
     *
     * @return String the fees for this service.
     */
    public String getFees();
    // {
    //    return fees;
    // }

    public void setFees(String fees);
    // {
    //    this.fees = fees;
    // }

    /**
     * A list of the keywords for this service.
     *
     * @return List keywords for this service
     */
    public List getKeywords();
    // {
    //    return keywords;
    // }

    // public void setKeywords(List keywords) {
    //    this.keywords = keywords;
    // }

    /**
     * The name of the maintainer for this service.
     *
     * @return String maintainer for this service.
     */
    public String getMaintainer();
    // {
    //    return maintainer;
    // }

    public void setMaintainer(String maintainer);
    // {
    //    this.maintainer = maintainer;
    // }

    /**
     * The name for this service.
     *
     * @return String the service's name.
     */
    public String getName();
    // {
    //    return name;
    // }

    public void setName(String name);
    // {
    //    this.name = name;
    // }

    /**
     * The title for this service.
     *
     * @return String the service's title.
     */
    public String getTitle();
    // {
    //    return title;
    // }

    public void setTitle(String title);
    // {
    //    this.title = title;
    // }

    /** Client properties for the service. */
    public Map getClientProperties();
    // {
    //    return clientProperties;
    // }

    // public void setClientProperties(Map clientProperties) {
    //    this.clientProperties = clientProperties;
    // }

    /**
     * Flag indicating wether the service should be verbose, for things like responding to requests,
     * etc...
     *
     * @return True if verboseness on, other wise false.
     */
    public boolean isVerbose();
    // {
    //    return isVerbose;
    // }

    public void setVerbose(boolean verbose);
    // {
    //    this.isVerbose = isVerbose;
    // }

    /**
     * Sets the base url from which to locate schemas from.
     *
     * @param schemaBaseURL
     */
    public void setSchemaBaseURL(String schemaBaseURL);
    // {
    //    this.schemaBaseURL = schemaBaseURL;
    // }

    /** @return The base url from which to locate schemas from. */
    public String getSchemaBaseURL();
    // {
    //    return schemaBaseURL;
    // }
}
