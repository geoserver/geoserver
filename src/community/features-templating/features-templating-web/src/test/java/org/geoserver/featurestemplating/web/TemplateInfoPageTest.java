/* (c) 2021 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.featurestemplating.web;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.markup.html.form.CheckBox;
import org.geoserver.featurestemplating.configuration.TemplateFileManager;
import org.geoserver.featurestemplating.configuration.TemplateInfo;
import org.geoserver.featurestemplating.configuration.TemplateInfoDAO;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.junit.Test;

public class TemplateInfoPageTest extends GeoServerWicketTestSupport {

    private static final String JSON_TEMPLATE =
            "{"
                    + "  \"@context\": {"
                    + "    \"gsp\": \"http://www.opengis.net/ont/geosparql#\","
                    + "    \"sf\": \"http://www.opengis.net/ont/sf#\","
                    + "    \"schema\": \"https://schema.org/\","
                    + "    \"dc\": \"http://purl.org/dc/terms/\","
                    + "    \"Feature\": \"gsp:Feature\","
                    + "    \"FeatureCollection\": \"schema:Collection\","
                    + "    \"Point\": \"sf:Point\","
                    + "    \"wkt\": \"gsp:asWKT\","
                    + "    \"features\": {"
                    + "      \"@container\": \"@set\","
                    + "      \"@id\": \"schema:hasPart\""
                    + "    },"
                    + "    \"geometry\": \"sf:geometry\","
                    + "    \"description\": \"dc:description\","
                    + "    \"title\": \"dc:title\","
                    + "    \"name\": \"schema:name\""
                    + "  },"
                    + "  \"type\": \"FeatureCollection\","
                    + "  \"features\": ["
                    + "    {"
                    + "      \"$source\": \"cite:NamedPlaces\""
                    + "    },"
                    + "    {"
                    + "      \"id\": \"${cite:FID}\","
                    + "      \"@type\": ["
                    + "        \"Feature\","
                    + "        \"cite:NamedPlaces\","
                    + "        \"http://vocabulary.odm2.org/samplingfeaturetype/namedplaces\""
                    + "      ],"
                    + "      \"name\": \"${cite:NAME}\","
                    + "      \"geometry\": {"
                    + "        \"@type\": \"MultiPolygon\","
                    + "        \"wkt\": \"$${toWKT(xpath('cite:the_geom'))}\""
                    + "      }"
                    + "    }"
                    + "  ]"
                    + "}";

    private static final String GML_TEMPLATE =
            "<gft:Template>\n"
                    + "<gft:Options>\n"
                    + "  <gft:Namespaces xmlns:topp=\"http://www.openplans.org/topp\"/>\n"
                    + "  <gft:SchemaLocation xsi:schemaLocation=\"http://www.opengis.net/wfs/2.0 http://brgm-dev.geo-solutions.it/geoserver/schemas/wfs/2.0/wfs.xsd http://www.opengis.net/gml/3.2 http://schemas.opengis.net/gml/3.2.1/gml.xsd\"/>\n"
                    + "</gft:Options>\n"
                    + "  <topp:states gml:id=\"${@id}\">\n"
                    + "  \t<topp:name code=\"${STATE_ABBR}\">${STATE_NAME}</topp:name>\n"
                    + "  \t<topp:region>${SUB_REGION}</topp:region>\n"
                    + "    <topp:population>${PERSONS}</topp:population>\n"
                    + "    <topp:males>${MALE}</topp:males>\n"
                    + "  \t<topp:females>${FEMALE}</topp:females>\n"
                    + "  \t<topp:active_population>${WORKERS}</topp:active_population>\n"
                    + "  \t<topp:wkt_geom>$${toWKT(the_geom)}</topp:wkt_geom>\n"
                    + "  </topp:states>\n"
                    + "</gft:Template>";

    @Test
    public void testDelete() {

        TemplateInfo info = new TemplateInfo();
        info.setWorkspace("cite");
        info.setTemplateName("testGMLTemplate");
        info.setExtension("xml");
        info = TemplateInfoDAO.get().saveOrUpdate(info);
        TemplateFileManager.get().saveTemplateFile(info, GML_TEMPLATE);

        TemplateInfo info2 = new TemplateInfo();
        info2.setWorkspace("cite");
        info2.setTemplateName("testJSONTemplate");
        info2.setExtension("json");
        info2 = TemplateInfoDAO.get().saveOrUpdate(info2);
        TemplateFileManager.get().saveTemplateFile(info2, JSON_TEMPLATE);
        TemplateInfo info3 = new TemplateInfo();
        info3.setWorkspace("cite");
        info3.setTemplateName("testJSONTemplate2");
        info3.setExtension("json");
        info3 = TemplateInfoDAO.get().saveOrUpdate(info3);
        TemplateFileManager.get().saveTemplateFile(info3, JSON_TEMPLATE);
        login();
        tester.startPage(new TemplateInfoPage());
        CheckBox checkBox =
                (CheckBox)
                        tester.getComponentFromLastRenderedPage(
                                "tablePanel:listContainer:items:1:selectItemContainer:selectItem");
        checkBox.setModelObject(true);
        tester.getComponentFromLastRenderedPage("removeSelected").setEnabled(true);
        tester.clickLink("removeSelected");
        assertEquals(2, TemplateInfoDAO.get().findAll().size());
        assertTrue(TemplateFileManager.get().getTemplateResource(info2).file().exists());
        assertTrue(TemplateFileManager.get().getTemplateResource(info3).file().exists());
    }
}
