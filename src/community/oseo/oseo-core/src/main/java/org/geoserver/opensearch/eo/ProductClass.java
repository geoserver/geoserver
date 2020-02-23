/* (c) 2018 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.opensearch.eo;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.geoserver.config.GeoServer;

public class ProductClass implements Serializable, Cloneable {

    /** The generic product class */
    public static final ProductClass GENERIC =
            new ProductClass("eop_generic", "eop", "http://www.opengis.net/eop/2.1");

    /** Optical products */
    public static final ProductClass OPTICAL =
            new ProductClass("optical", "opt", "http://www.opengis.net/opt/2.1");

    /** Radar products */
    public static final ProductClass RADAR =
            new ProductClass("radar", "sar", "http://www.opengis.net/sar/2.1");

    /** Altimetric products */
    public static final ProductClass ALTIMETRIC =
            new ProductClass("altimetric", "alt", "http://www.opengis.net/alt/2.1");

    /** Atmospheric products */
    public static final ProductClass ATMOSPHERIC =
            new ProductClass("atmospheric", "atm", "http://www.opengis.net/atm/2.1");

    /** Limb products */
    public static final ProductClass LIMB =
            new ProductClass("limb", "lmb", "http://www.opengis.net/lmb/2.1");

    /** SSP products */
    public static final ProductClass SSP =
            new ProductClass("ssp", "ssp", "http://www.opengis.net/ssp/2.1");

    /** The default, built-in product classes */
    public static final List<ProductClass> DEFAULT_PRODUCT_CLASSES =
            Collections.unmodifiableList(
                    Arrays.asList(GENERIC, OPTICAL, RADAR, ALTIMETRIC, ATMOSPHERIC, LIMB, SSP));

    public static ProductClass getProductClassFromName(GeoServer geoServer, String name) {
        for (ProductClass pc : getProductClasses(geoServer)) {
            if (name.equalsIgnoreCase(pc.getName())) {
                return pc;
            }
        }
        throw new IllegalArgumentException("Could not locate a product class named " + name);
    }

    /**
     * Searches a product by name (search is case insensitive)
     *
     * @param oseo reference to the service configuration
     * @param name the product class name
     */
    public static ProductClass getProductClassFromName(OSEOInfo oseo, String name) {
        for (ProductClass pc : getProductClasses(oseo)) {
            if (name.equalsIgnoreCase(pc.getName())) {
                return pc;
            }
        }
        throw new IllegalArgumentException("Could not locate a product class named " + name);
    }

    /**
     * Searches a product by prefix (search is case insensitive)
     *
     * @param oseo reference to the service configuration
     * @param name the product class prefix
     */
    public static ProductClass getProductClassFromPrefix(OSEOInfo oseo, String prefix) {
        for (ProductClass pc : getProductClasses(oseo)) {
            if (prefix.equalsIgnoreCase(pc.getPrefix())) {
                return pc;
            }
        }
        throw new IllegalArgumentException(
                "Could not locate a product class with prefix " + prefix);
    }

    /**
     * Checks if the given prefix matches a product class
     *
     * @param oseo reference to the service configuration
     * @param name the product class name
     */
    public static boolean isProductClass(OSEOInfo oseo, String prefix) {
        for (ProductClass pc : getProductClasses(oseo)) {
            if (pc.getPrefix().equals(prefix)) {
                return true;
            }
        }
        return false;
    }

    private String name;

    private String namespace;

    private String prefix;

    /**
     * Builds a new product class
     *
     * @param name The name of the class, used as sensor type in the collection model
     * @param prefix The prefix, used in database mappings and XML/JSON outputs
     * @param namespace The namespace, used in XML outputs
     */
    public ProductClass(String name, String prefix, String namespace) {
        this.name = name;
        this.prefix = prefix;
        this.namespace = namespace;
    }

    public String getNamespace() {
        return namespace;
    }

    public String getPrefix() {
        return prefix;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setNamespace(String namespace) {
        this.namespace = namespace;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ProductClass that = (ProductClass) o;
        return Objects.equals(name, that.name)
                && Objects.equals(namespace, that.namespace)
                && Objects.equals(prefix, that.prefix);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, namespace, prefix);
    }

    @Override
    public String toString() {
        return "ProductClass{"
                + "name='"
                + name
                + '\''
                + ", namespace='"
                + namespace
                + '\''
                + ", prefix='"
                + prefix
                + '\''
                + '}';
    }

    /**
     * Returns the configured product classes
     *
     * @param geoServer A GeoServer reference used to retrieve the OpenSearch configuration
     */
    public static List<ProductClass> getProductClasses(GeoServer geoServer) {
        if (geoServer != null) {
            OSEOInfo oseo = geoServer.getService(OSEOInfo.class);
            return getProductClasses(oseo);
        } else {
            return DEFAULT_PRODUCT_CLASSES;
        }
    }

    /**
     * Returns the configured product classes
     *
     * @param oseo The OpenSearch configuration
     */
    public static List<ProductClass> getProductClasses(OSEOInfo oseo) {
        if (oseo == null) {
            return DEFAULT_PRODUCT_CLASSES;
        } else {
            return oseo.getProductClasses();
        }
    }

    @Override
    public Object clone() throws CloneNotSupportedException {
        return new ProductClass(this.name, this.prefix, this.namespace);
    }
}
