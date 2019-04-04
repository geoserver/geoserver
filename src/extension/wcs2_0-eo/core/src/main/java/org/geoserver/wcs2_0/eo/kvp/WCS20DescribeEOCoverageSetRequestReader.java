/* (c) 2014 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wcs2_0.eo.kvp;

import java.util.Arrays;
import java.util.Map;
import net.opengis.ows11.SectionsType;
import net.opengis.wcs20.DescribeEOCoverageSetType;
import net.opengis.wcs20.Section;
import net.opengis.wcs20.Sections;
import net.opengis.wcs20.Wcs20Factory;
import org.geoserver.ows.kvp.EMFKvpRequestReader;
import org.geoserver.platform.OWS20Exception.OWSExceptionCode;
import org.geoserver.wcs2_0.exception.WCS20Exception;

/**
 * Parses a DescribeEOCoverageSet request for WCS EO into the correspondent model object
 *
 * @author Andrea Aime - GeoSolutions
 */
public class WCS20DescribeEOCoverageSetRequestReader extends EMFKvpRequestReader {

    public WCS20DescribeEOCoverageSetRequestReader() {
        super(DescribeEOCoverageSetType.class, Wcs20Factory.eINSTANCE);
    }

    @Override
    public Object read(Object request, Map kvp, Map rawKvp) throws Exception {
        SectionsType owsSections = (SectionsType) kvp.get("sections");
        if (owsSections != null) {
            Sections sections = Wcs20Factory.eINSTANCE.createSections();
            for (Object o : owsSections.getSection()) {
                String sectionName = (String) o;
                Section section = Section.get(sectionName);
                if (section == null) {
                    throw new WCS20Exception(
                            "Invalid sections value "
                                    + sectionName
                                    + ", supported values are "
                                    + Arrays.asList(Section.values()),
                            OWSExceptionCode.InvalidParameterValue,
                            "sections");
                }
                sections.getSection().add(section);
            }
            kvp.put("sections", sections);
        }
        // the kvp param is subset, the objet field is dimensionTrim....
        Object subset = kvp.get("subset");
        if (subset != null) {
            kvp.put("dimensionTrim", subset);
        }
        // the kvp param is containment, the object field is containmentType
        Object containment = kvp.get("containment");
        if (containment != null) {
            kvp.put("containmentType", containment);
        }
        return super.read(request, kvp, rawKvp);
    }
}
