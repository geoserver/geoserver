/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.catalog;

/**
 * The AttributionInfo interface describes a data provider for attribution, such as in the WMS
 * Capabilities document.
 *
 * @author David Winslow <dwinslow@opengeo.org>
 */
public interface AttributionInfo extends Info {
    /**
     * Get the title field of the attribution, providing a human-readable name for the attribution.
     *
     * @return a String containing the title
     */
    String getTitle();

    /**
     * Get the href field of the attribution, indicating a link that users can follow for more
     * information about the providing organization.
     *
     * @return a String containing the href
     */
    String getHref();

    /**
     * Get the logoURL field of the attribution, providing a URL for an image representing the
     * providing organization.
     *
     * @return a String containing the logoURL
     */
    String getLogoURL();

    /**
     * Get the mimetype field of the attribution, indicating the mimetype of the logo image pointed
     * to by the logoURL.
     *
     * @return a String containing the logoType
     */
    String getLogoType();

    /**
     * Get the width field of the attribution, indicating the width of the logo image pointed to by
     * the logoURL.
     *
     * @return the logoWidth as an int
     */
    int getLogoWidth();

    /**
     * Get the height field of the attribution, indicating the height of the logo image pointed to
     * by the logoURL.
     *
     * @return the logoHeight as an int
     */
    int getLogoHeight();

    /**
     * Set the title field of the attribution, providing a human-readable name for the attribution.
     *
     * @param title a String containing the new title value
     */
    void setTitle(String title);

    /**
     * Set the href field of the attribution, indicating a link that users can follow for more
     * information about the providing organization.
     *
     * @param href a String containing the new href value
     */
    void setHref(String href);

    /**
     * Set the logoURL field of the attribution, providing a URL for an image representing the
     * providing organization.
     *
     * @param logoURL a String containing the new logoURL value
     */
    void setLogoURL(String logoURL);

    /**
     * Set the mimetype field of the attribution, indicating the mimetype of the logo image pointed
     * to by the logoURL.
     *
     * @param logoType a String containing the new logoType value
     */
    void setLogoType(String logoType);

    /**
     * Set the width field of the attribution, indicating the width of the logo image pointed to by
     * the logoURL.
     *
     * @param logoWidth the new logoWidth value
     */
    void setLogoWidth(int logoWidth);

    /**
     * Set the height field of the attribution, indicating the height of the logo image pointed to
     * by the logoURL.
     *
     * @param logoHeight the new logoHeight value
     */
    void setLogoHeight(int logoHeight);
}
