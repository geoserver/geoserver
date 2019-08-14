/* (c) 2017 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.records;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.util.Collection;
import java.util.List;
import org.geoserver.csw.records.iso.MetaDataDescriptor;
import org.geoserver.csw.store.internal.iso.MDTestSupport;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;
import org.locationtech.jts.geom.MultiPolygon;
import org.opengis.feature.Feature;
import org.opengis.feature.Property;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.PropertyName;
import org.opengis.geometry.MismatchedDimensionException;

public class MetaDataTest extends MDTestSupport {

    FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2();

    @Test
    public void testConfirmTypeBuilt() {
        assertNotNull(MetaDataDescriptor.METADATA_TYPE);
        assertNotNull(MetaDataDescriptor.METADATA_DESCRIPTOR);
    }

    @Test
    public void testBuildMDRecord() throws MismatchedDimensionException, Exception {
        GenericRecordBuilder rb = new GenericRecordBuilder(MetaDataDescriptor.getInstance());
        rb.addElement("fileIdentifier.CharacterString", "00180e67-b7cf-40a3-861d-b3a09337b195");
        rb.addElement(
                "identificationInfo.AbstractMD_Identification.citation.CI_Citation.title.CharacterString",
                "Image2000 Product 1 (at1) Multispectral");
        rb.addElement("dateStamp.Date", "2004-10-04 00:00:00");
        rb.addElement(
                "identificationInfo.AbstractMD_Identification.abstract.CharacterString",
                "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        rb.addElement("hierarchyLevel.MD_ScopeCode.@codeListValue", "dataset");
        rb.addElement(
                "identificationInfo.AbstractMD_Identification.descriptiveKeywords.MD_Keywords.keyword.CharacterString",
                "imagery",
                "baseMaps",
                "earthCover");
        rb.addElement(
                "contact.CI_ResponsibleParty.individualName.CharacterString", "Niels Charlier");
        rb.addBoundingBox(
                new ReferencedEnvelope(14.05, 17.24, 46.46, 28.42, DefaultGeographicCRS.WGS84));
        Feature f = rb.build(null);

        assertRecordElement(
                f,
                "gmd:fileIdentifier/gco:CharacterString",
                "00180e67-b7cf-40a3-861d-b3a09337b195");
        assertRecordElement(
                f,
                "gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:citation/gmd:CI_Citation/gmd:title/gco:CharacterString",
                "Image2000 Product 1 (at1) Multispectral");
        assertRecordElement(f, "gmd:dateStamp/gco:Date", "2004-10-04 00:00:00");
        assertRecordElement(
                f,
                "gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:abstract/gco:CharacterString",
                "IMAGE2000 product 1 individual orthorectified scenes. IMAGE2000 was  produced from ETM+ Landsat 7 satellite data and provides a consistent European coverage of individual orthorectified scenes in national map projection systems.");
        assertRecordElement(f, "gmd:hierarchyLevel/gmd:MD_ScopeCode/@codeListValue", "dataset");
        assertRecordElement(
                f,
                "gmd:identificationInfo/gmd:AbstractMD_Identification/gmd:descriptiveKeywords/gmd:MD_Keywords/gmd:keyword/gco:CharacterString",
                "imagery",
                "baseMaps",
                "earthCover");
        assertBBox(
                f, new ReferencedEnvelope(14.05, 17.24, 46.46, 28.42, DefaultGeographicCRS.WGS84));
    }

    private void assertBBox(Feature f, ReferencedEnvelope... envelopes) throws Exception {
        PropertyName bbox =
                ff.property(
                        "gmd:identificationInfo/gmd:MD_DataIdentification/gmd:extent/gmd:EX_Extent/gmd:geographicElement/gmd:EX_GeographicBoundingBox",
                        MetaDataDescriptor.NAMESPACES);
        Property p = (Property) bbox.evaluate(f);
        MultiPolygon geometry = (MultiPolygon) p.getValue();
        List<ReferencedEnvelope> featureEnvelopes =
                (List<ReferencedEnvelope>)
                        p.getUserData().get(GenericRecordBuilder.ORIGINAL_BBOXES);
        ReferencedEnvelope total = null;
        for (int i = 0; i < envelopes.length; i++) {
            assertEquals(envelopes[i], featureEnvelopes.get(i));
            ReferencedEnvelope re = envelopes[i].transform(CSWRecordDescriptor.DEFAULT_CRS, true);
            if (total == null) {
                total = re;
            } else {
                total.expandToInclude(re);
            }
        }

        assertTrue(total.contains(geometry.getEnvelopeInternal()));
    }

    private void assertRecordElement(Feature f, String elementName, Object... values) {
        PropertyName pn = ff.property(elementName, MetaDataDescriptor.NAMESPACES);

        Object value = pn.evaluate(f);

        if (value instanceof Collection) {
            Collection<Property> propertyList = (Collection<Property>) value;
            Property[] properties =
                    (Property[]) propertyList.toArray(new Property[propertyList.size()]);
            assertEquals(properties.length, values.length);
            for (int i = 0; i < properties.length; i++) {
                Property property = (Property) properties[i];
                assertEquals(values[i], property.getValue());
            }
        } else {
            Property property = (Property) value;
            assertEquals(1, values.length);
            assertEquals(values[0], property.getValue());
        }
    }
}
