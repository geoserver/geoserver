/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web;

import java.util.logging.Logger;
import org.apache.wicket.markup.html.WebPage;
import org.apache.wicket.request.resource.PackageResourceReference;
import org.geotools.util.logging.Logging;

/**
 * Extension point for contributing additional css and/or javascript to a page.
 *
 * <p>Use of this extension point is more suited toward bulk updating of multiple existing pages in
 * the GeoServer application. For contributing css or javascript to a single page one should use the
 * existing Wicket approach.
 *
 * <p>Instances of this class are registered in the spring context. Example:
 *
 * <pre>
 * &lt;bean id="myHeaderContribution" class="org.geoserver.web.HeaderContribution">
 *   &lt;property name="scope" value="com.acme.MyClass"/>
 *   &lt;property name="cssFilename=" value="mycss.css"/>
 *   &lt;property name="javaScriptFilename=" value="myjavascript.js"/>
 * &lt;/bean>
 * </pre>
 *
 * Would correspond to a package structure like:
 *
 * <pre>
 * src/
 *   com.acme/
 *      MyClass.java
 *      mycss.css
 *      myjavascript.css
 *
 * </pre>
 *
 * @author Justin Deoliveira, OpenGeo
 */
public class HeaderContribution {

    static Logger LOGGER = Logging.getLogger("org.geoserver.web");

    Class<?> scope;
    String cssFilename, javaScriptFilename, faviconFilename;

    public Class<?> getScope() {
        return scope;
    }

    public void setScope(Class<?> scope) {
        this.scope = scope;
    }

    public String getCSSFilename() {
        return cssFilename;
    }

    public void setCSSFilename(String filename) {
        this.cssFilename = filename;
    }

    public String getJavaScriptFilename() {
        return javaScriptFilename;
    }

    public void setJavaScriptFilename(String javaScriptFilename) {
        this.javaScriptFilename = javaScriptFilename;
    }

    public String getFaviconFilename() {
        return faviconFilename;
    }

    public void setFaviconFilename(String faviconName) {
        this.faviconFilename = faviconName;
    }

    /**
     * Determines if the header contribution should apply to a particular page or not.
     *
     * <p>This implementation always returns true, if clients need a more flexible mechanism for
     * determining which pages apply they should subclass and override this method.
     */
    public boolean appliesTo(WebPage page) {
        return true;
    }

    /**
     * Returns the resource reference to the css for the header contribution, or null if there is no
     * css contribution.
     */
    public PackageResourceReference getCSS() {
        if (scope != null && cssFilename != null) {
            return new PackageResourceReference(scope, cssFilename);
        }

        return null;
    }

    /**
     * Returns the resource reference to the javascript for the header contribution, or null if
     * there is no javascript contribution.
     */
    public PackageResourceReference getJavaScript() {
        if (scope != null && javaScriptFilename != null) {
            return new PackageResourceReference(scope, javaScriptFilename);
        }

        return null;
    }

    /**
     * Returns the resource reference to a replacement favicon for the header contribution, or null
     * if there is no favicon replacement
     */
    public PackageResourceReference getFavicon() {
        if (scope != null && faviconFilename != null) {
            return new PackageResourceReference(scope, faviconFilename);
        }

        return null;
    }
}
