package org.geoserver.featurestemplating.configuration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.List;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class TemplateInfoDaoTest extends GeoServerSystemTestSupport {

    @Test
    public void testSaveFindUpdateDelete() {
        TemplateInfoDAO dao = TemplateInfoDAO.get();
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName("template_name_json");
        info.setExtension("json");
        info.setWorkspace(MockData.CITE_PREFIX);
        dao.saveOrUpdate(info);
        TemplateInfo found = dao.findById(info.getIdentifier());
        assertEquals(info, found);
        assertEquals(1, dao.findAll().size());
        found.setFeatureType(MockData.FIFTEEN.getLocalPart());
        dao.saveOrUpdate(found);
        assertEquals(
                MockData.FIFTEEN.getLocalPart(),
                dao.findById(info.getIdentifier()).getFeatureType());
        dao.delete(found);
        assertEquals(0, dao.findAll().size());
    }

    @Test
    public void testFindFeatureTypeRelatedTemplateInfo() {
        TemplateInfoDAO dao = TemplateInfoDAO.get();
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName("template_name_json");
        info.setExtension("json");
        info.setWorkspace(MockData.CITE_PREFIX);
        dao.saveOrUpdate(info);

        info = new TemplateInfo();
        info.setTemplateName("template_name_xml");
        info.setExtension("xml");
        dao.saveOrUpdate(info);

        info = new TemplateInfo();
        info.setTemplateName("template_name_xml");
        info.setExtension("xml");
        info.setWorkspace(MockData.CDF_PREFIX);
        info.setFeatureType(MockData.FIFTEEN.getLocalPart());
        dao.saveOrUpdate(info);

        FeatureTypeInfo fti = getCatalog().getFeatureTypeByName(MockData.FIFTEEN.getLocalPart());
        List<TemplateInfo> infos = dao.findByFeatureTypeInfo(fti);

        assertFalse(
                infos.stream()
                        .anyMatch(
                                i ->
                                        i.getWorkspace() != null
                                                && i.getWorkspace().equals(MockData.CITE_PREFIX)));
    }
}
