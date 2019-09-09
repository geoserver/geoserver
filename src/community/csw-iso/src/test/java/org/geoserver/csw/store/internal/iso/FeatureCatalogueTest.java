/* (c) 2019 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.csw.store.internal.iso;

import static org.custommonkey.xmlunit.XMLAssert.assertXpathEvaluatesTo;

import java.util.ArrayList;
import java.util.UUID;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.MetadataMap;
import org.geoserver.data.test.SystemTestData;
import org.junit.Test;
import org.opengis.feature.type.PropertyDescriptor;
import org.w3c.dom.Document;

public class FeatureCatalogueTest extends MDTestSupport {

    private static String uuid = UUID.randomUUID().toString();

    @SuppressWarnings("unchecked")
    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        // insert feature catalogue data
        FeatureTypeInfo forestInfo =
                (FeatureTypeInfo) getCatalog().getLayerByName("Forests").getResource();
        // attributes can be copied from feature type
        MetadataMap custom = new MetadataMap();
        custom.put("object-catalog/name", new ArrayList<String>());
        custom.put("object-catalog/definition", new ArrayList<String>());
        custom.put("object-catalog/type", new ArrayList<String>());
        custom.put("object-catalog/min-occurence", new ArrayList<Integer>());
        custom.put("object-catalog/max-occurence", new ArrayList<Integer>());
        custom.put("object-catalog/domain/value", new ArrayList<ArrayList<String>>());
        for (PropertyDescriptor descr : forestInfo.getFeatureType().getDescriptors()) {
            ((ArrayList<String>) custom.get("object-catalog/name"))
                    .add(descr.getName().getLocalPart());
            ((ArrayList<String>) custom.get("object-catalog/definition"))
                    .add("definition for " + descr.getName().getLocalPart());
            ((ArrayList<String>) custom.get("object-catalog/type"))
                    .add(descr.getType().getBinding().getSimpleName());
            ((ArrayList<Integer>) custom.get("object-catalog/min-occurence"))
                    .add(descr.getMinOccurs());
            ((ArrayList<Integer>) custom.get("object-catalog/max-occurence"))
                    .add(descr.getMaxOccurs());
            ((ArrayList<ArrayList<String>>) custom.get("object-catalog/domain/value"))
                    .add(new ArrayList<String>());
        }
        ((ArrayList<ArrayList<String>>) custom.get("object-catalog/domain/value")).get(0).add("a");
        ((ArrayList<ArrayList<String>>) custom.get("object-catalog/domain/value")).get(0).add("b");
        ((ArrayList<ArrayList<String>>) custom.get("object-catalog/domain/value")).get(0).add("c");
        custom.put("object-catalog/version", "1.1");
        custom.put("object-catalog/date", "01/01/2018");
        custom.put("object-catalog/uuid", uuid);
        forestInfo.getMetadata().put("custom", custom);

        getCatalog().save(forestInfo);
    }

    @Test
    public void testGetRecords() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecords&typeNames=gmd:MD_Metadata"
                        + "&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&maxRecords=100";

        Document d = getAsDOM(request);
        // print(d);

        assertXpathEvaluatesTo("1", "count(/csw:GetRecordsResponse)", d);

        assertXpathEvaluatesTo("full", "//csw:SearchResults/@elementSet", d);
        assertXpathEvaluatesTo("30", "//csw:SearchResults/@numberOfRecordsMatched", d);
        assertXpathEvaluatesTo("30", "//csw:SearchResults/@numberOfRecordsReturned", d);
        assertXpathEvaluatesTo("0", "//csw:SearchResults/@nextRecord", d);
        assertXpathEvaluatesTo("30", "count(//csw:SearchResults/*)", d);

        assertXpathEvaluatesTo("29", "count(//csw:SearchResults/gmd:MD_Metadata)", d);

        assertXpathEvaluatesTo("1", "count(//csw:SearchResults/gfc:FC_FeatureCatalogue)", d);

        assertFeatureCatalogue(d);
    }

    @Test
    public void testGetRecordById() throws Exception {
        String request =
                "csw?service=CSW&version=2.0.2&request=GetRecordById&typeNames=gmd:MD_Metadata"
                        + "&resultType=results&elementSetName=full&outputSchema=http://www.isotc211.org/2005/gmd"
                        + "&id="
                        + uuid;

        Document d = getAsDOM(request);
        print(d);

        assertXpathEvaluatesTo("1", "count(/csw:GetRecordByIdResponse)", d);
        assertXpathEvaluatesTo("1", "count(//gfc:FC_FeatureCatalogue)", d);

        assertFeatureCatalogue(d);
    }

    private void assertFeatureCatalogue(Document d) throws Exception {

        assertXpathEvaluatesTo(uuid, "//gfc:FC_FeatureCatalogue/@uuid", d);

        assertXpathEvaluatesTo(
                "ForestsType",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:typeName/gco:LocalName",
                d);
        assertXpathEvaluatesTo(
                "01/01/2018", "//gfc:FC_FeatureCatalogue/gmx:versionDate/gco:DateTime", d);
        assertXpathEvaluatesTo(
                "1.1", "//gfc:FC_FeatureCatalogue/gmx:versionNumber/gco:CharacterString", d);
        assertXpathEvaluatesTo(
                "Forests (feature catalogue)",
                "//gfc:FC_FeatureCatalogue/gmx:name/gco:CharacterString",
                d);

        assertXpathEvaluatesTo(
                "3",
                "count(//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics)",
                d);

        assertXpathEvaluatesTo(
                "MultiPolygon",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:valueType/gco:TypeName/gco:aName/gco:CharacterString",
                d);

        assertXpathEvaluatesTo(
                "the_geom",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:memberName/gco:LocalName",
                d);

        assertXpathEvaluatesTo(
                "1",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:cardinality/gco:Multiplicity/gco:range/gco:MultiplicityRange/gco:upper/gco:UnlimitedInteger",
                d);

        assertXpathEvaluatesTo(
                "false",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:cardinality/gco:Multiplicity/gco:range/gco:MultiplicityRange/gco:upper/gco:UnlimitedInteger/@isInfinite",
                d);

        assertXpathEvaluatesTo(
                "false",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:cardinality/gco:Multiplicity/gco:range/gco:MultiplicityRange/gco:upper/gco:UnlimitedInteger/@xsi:nil",
                d);

        assertXpathEvaluatesTo(
                "3",
                "count(//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:listedValue)",
                d);

        assertXpathEvaluatesTo(
                "a",
                "//gfc:FC_FeatureCatalogue/gfc:featureType/gfc:FC_FeatureType/gfc:carrierOfCharacteristics[1]/gfc:FC_FeatureAttribute/gfc:listedValue[1]/gfc:FC_ListedValue/gfc:label/gco:CharacterString",
                d);
    }
}
