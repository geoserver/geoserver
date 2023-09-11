/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.wicket;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.geoserver.crs.CapabilitiesCRSProvider;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.gml2.SrsSyntax;
import org.geotools.referencing.CRS;

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

        private String identifier;

        private transient String description;

        public SRS(String identifier) {
            this.identifier = identifier;
        }

        public String getIdentifier() {
            return identifier;
        }

        public String getDescription() {
            // lazy loading of description
            if (description == null) {
                // grab the description
                String desc = "-";
                try {
                    desc = CRS.getAuthorityFactory(true).getDescriptionText(identifier).toString();
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
            return identifier.equals(((SRS) o).identifier);
        }

        @Override
        public int hashCode() {
            return 17 * identifier.hashCode();
        }

        @Override
        public int compareTo(SRS o) {
            return identifier.compareTo(o.identifier);
        }
    }

    public static final Property<SRS> IDENTIFIER =
            new BeanProperty<SRS>("identifier", "identifier") {

                private static final long serialVersionUID = -1638823520421390286L;

                @Override
                public Comparator<SRS> getComparator() {
                    return new SRSIdentifierComparator();
                }
            };

    public static final Property<SRS> DESCRIPTION =
            new BeanProperty<SRS>("description", "description") {

                private static final long serialVersionUID = 3549074714488486991L;

                @Override
                public Comparator<SRS> getComparator() {
                    return (o1, o2) ->
                            String.CASE_INSENSITIVE_ORDER.compare(
                                    o1.getDescription(), o2.getDescription());
                }
            };

    private static final ArrayList<Property<SRS>> PROPERTIES =
            new ArrayList<>(Arrays.asList(IDENTIFIER, DESCRIPTION));

    private volatile List<SRS> items;

    public SRSProvider() {}

    // a constructor to pass custom list of SRS list
    public SRSProvider(List<String> srsList) {
        List<SRS> otherSRS = new ArrayList<>();
        for (String srs : srsList) {
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
        return new CapabilitiesCRSProvider()
                .getCodes().stream()
                        .map(s -> new SRS(s))
                        .sorted(new SRSIdentifierComparator())
                        .collect(Collectors.toList());
    }

    /** Compares the CRS identifiers */
    static class SRSIdentifierComparator implements Comparator<SRS> {

        @Override
        public int compare(SRS srs1, SRS srs2) {
            String s1 = SrsSyntax.AUTH_CODE.getSRS(srs1.getIdentifier());
            String s2 = SrsSyntax.AUTH_CODE.getSRS(srs2.getIdentifier());

            // if not in the expected format, place them at the beginning, and sort alphabetically
            int idx1 = s1.indexOf(':');
            int idx2 = s2.indexOf(':');
            if (idx1 <= 0 && idx2 <= 0) return s1.compareTo(s2);
            if (idx1 <= 0 && idx2 > 0) return -1;
            if (idx1 > 0 && idx2 <= 0) return 1;

            // both are in form AUTH:CODE
            String a1 = s1.substring(0, idx1);
            String a2 = s2.substring(0, idx2);
            String c1 = s1.substring(idx1 + 1);
            String c2 = s2.substring(idx2 + 1);

            int authComparison = a1.compareTo(a2);
            if (authComparison != 0) return authComparison;

            // do a numerical comparison if possible
            boolean numeric1 = NUMERIC.matcher(c1).matches();
            boolean numeric2 = NUMERIC.matcher(c2).matches();
            if (numeric1 && numeric2) {
                return (int) Math.signum(Integer.parseInt(c1) - Integer.parseInt(c2));
            } else if (numeric1) {
                return 1;
            } else if (numeric2) {
                return -1;
            } else {
                return c1.compareTo(c2);
            }
        }
    }
}
