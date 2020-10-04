/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.regex.Pattern;
import org.geoserver.web.data.resource.BasicResourceConfig;
import org.geotools.referencing.CRS;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.util.factory.Hints;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CRSAuthorityFactory;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.vfny.geoserver.crs.GeoserverCustomWKTFactory;

/**
 * Provides a filtered, sorted view over the available EPSG coordinate reference systems.
 *
 * @author Gabriel Roldan - OpenGeo
 * @author Andrea Aime - OpenGeo
 */
public class SRSProvider extends GeoServerDataProvider<SRSProvider.SRS> {

    private static final long serialVersionUID = 3731647638872356912L;

    /**
     * Spots integral numbers
     *
     * @see #buildCodeList()
     */
    private static Pattern NUMERIC = Pattern.compile("\\d+");

    /**
     * A lightweight bean to carry over the code and description of a {@link
     * CoordinateReferenceSystem}
     *
     * @author Gabriel Roldan - OpenGeo
     */
    public static class SRS implements Serializable, Comparable<SRS> {

        private static final long serialVersionUID = -4155644876049747585L;

        private String code;

        private transient String description;

        public SRS(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            // lazy loading of description
            if (description == null) {
                // grab the description
                String desc = "-";
                try {
                    // REVISIT: as far as I know the EPSG names are not localized? anyway, if
                    // they're revisit to use the page locale
                    // description = CRS.getAuthorityFactory(true).getDescriptionText("EPSG:" +
                    // code)
                    // .toString(getLocale()).toUpperCase();
                    String epsgcode = code;
                    if (!epsgcode.startsWith(BasicResourceConfig.URN_OGC_PREFIX))
                        epsgcode = "EPSG:" + epsgcode;
                    desc = CRS.getAuthorityFactory(true).getDescriptionText(epsgcode).toString();
                } catch (Exception e) {
                    // no problem
                }
                description = desc;
            }
            return description;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof SRS)) {
                return false;
            }
            return code.equals(((SRS) o).code);
        }

        @Override
        public int hashCode() {
            return 17 * code.hashCode();
        }

        public int compareTo(SRS o) {
            return code.compareTo(o.code);
        }
    }

    /**
     * custom geoserver crs factory which loads codes from epsg.properties file in data directory
     */
    private static CRSAuthorityFactory customFactory =
            ReferencingFactoryFinder.getCRSAuthorityFactory(
                    "EPSG",
                    new Hints(Hints.CRS_AUTHORITY_FACTORY, GeoserverCustomWKTFactory.class));

    public static final Property<SRS> CODE =
            new BeanProperty<SRS>("code", "code") {

                private static final long serialVersionUID = -1638823520421390286L;

                @Override
                public Comparator<SRS> getComparator() {
                    return new CodeComparator();
                }
            };

    public static final Property<SRS> DESCRIPTION =
            new BeanProperty<SRS>("description", "description") {

                private static final long serialVersionUID = 3549074714488486991L;

                @Override
                public Comparator<SRS> getComparator() {
                    return new Comparator<SRS>() {

                        public int compare(SRS o1, SRS o2) {
                            return String.CASE_INSENSITIVE_ORDER.compare(
                                    o1.getDescription(), o2.getDescription());
                        }
                    };
                }
            };

    private static final ArrayList<Property<SRS>> PROPERTIES =
            new ArrayList<Property<SRS>>(Arrays.asList(CODE, DESCRIPTION));

    private volatile List<SRS> items;

    public SRSProvider() {}

    // a constructor to pass custom list of SRS list
    public SRSProvider(List<String> srsList) {
        List<SRS> otherSRS = new ArrayList<>();
        for (String srs : srsList) {

            if (srs.startsWith(BasicResourceConfig.EPSG_PREFIX))
                srs = srs.substring(BasicResourceConfig.EPSG_PREFIX.length());

            otherSRS.add(new SRS(srs));
        }

        this.items = otherSRS;
    }

    @Override
    protected List<SRS> getItems() {
        if (items == null) {
            synchronized (this) {
                if (items == null) {
                    items = buildCodeList();
                }
            }
        }
        return items;
    }

    @Override
    protected List<Property<SRS>> getProperties() {
        return PROPERTIES;
    }

    static List<SRS> buildCodeList() {
        // long t = System.currentTimeMillis();
        Set<String> codes = CRS.getSupportedCodes("EPSG");

        try {
            codes.addAll(customFactory.getAuthorityCodes(CoordinateReferenceSystem.class));
        } catch (FactoryException e) {
            LOGGER.log(Level.WARNING, "Error occurred while trying to gather custom CRS codes", e);
        }

        // make a set with each code
        Set<SRS> idSet = new HashSet<SRS>();
        for (String code : codes) {
            // make sure we're using just the non prefix part
            String id = code.substring(code.indexOf(':') + 1);
            // avoid WGS84DD and eventual friends, as we're still not able to handle them,
            // if they are chosen exceptions arises everywhere
            if (NUMERIC.matcher(id).matches()) {
                idSet.add(new SRS(id));
            }
        }

        List<SRS> srsList = new ArrayList<SRS>(idSet);
        Collections.sort(srsList, new CodeComparator()); // sort to get them in order
        return srsList;
    }

    /**
     * Compares the codes so that most of the codes ger compared as numbers, but unfortunately some
     * non numeric ones can sneak in...
     *
     * @author Andrea Aime - TOPP
     */
    private static class CodeComparator implements Comparator<SRS> {

        public int compare(SRS srs1, SRS srs2) {
            String s1 = srs1.getCode();
            String s2 = srs2.getCode();
            Integer c1 = null, c2 = null;
            try {
                c1 = Integer.parseInt(s1);
            } catch (NumberFormatException e) {
                //
            }
            try {
                c2 = Integer.parseInt(s2);
            } catch (NumberFormatException e) {
                //
            }
            if (c1 == null) {
                if (c2 == null) return s1.compareTo(s2);
                else return -1;
            } else {
                if (c2 == null) return 1;
                else return c1 - c2;
            }
        }
    }
}
