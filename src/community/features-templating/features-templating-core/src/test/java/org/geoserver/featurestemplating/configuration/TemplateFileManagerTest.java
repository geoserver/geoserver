package org.geoserver.featurestemplating.configuration;

import static org.junit.Assert.assertTrue;

import java.io.File;
import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.junit.Test;

public class TemplateFileManagerTest extends GeoServerSystemTestSupport {

    @Test
    public void testSave() {
        TemplateFileManager fileManager = TemplateFileManager.get();
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName("template_name_json");
        info.setExtension("json");
        String voidTemplate = "{}";
        fileManager.saveTemplateFile(info, voidTemplate);
        File templateFile = fileManager.getTemplateResource(info).file();
        assertTrue(templateFile.exists());
        File dir = fileManager.getTemplateLocation(info);
        assertTrue(dir.getPath().endsWith("features-templating"));
        templateFile.delete();
    }

    @Test
    public void testSaveInWorkspace() {
        TemplateFileManager fileManager = TemplateFileManager.get();
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName("template_name_json");
        info.setWorkspace(MockData.CITE_PREFIX);
        info.setExtension("json");
        String voidTemplate = "{}";
        fileManager.saveTemplateFile(info, voidTemplate);
        File templateFile = fileManager.getTemplateResource(info).file();
        assertTrue(templateFile.exists());
        File dir = fileManager.getTemplateLocation(info);
        assertTrue(dir.getPath().endsWith("cite"));
        templateFile.delete();
    }

    @Test
    public void testSaveInFeatureTypeInfo() {
        TemplateFileManager fileManager = TemplateFileManager.get();
        TemplateInfo info = new TemplateInfo();
        info.setTemplateName("template_name_json");
        info.setWorkspace(MockData.CDF_PREFIX);
        info.setFeatureType(MockData.FIFTEEN.getLocalPart());
        info.setExtension("json");
        String voidTemplate = "{}";
        fileManager.saveTemplateFile(info, voidTemplate);
        File templateFile = fileManager.getTemplateResource(info).file();
        assertTrue(templateFile.exists());
        File dir = fileManager.getTemplateLocation(info);
        assertTrue(dir.getPath().endsWith(MockData.FIFTEEN.getLocalPart()));
        templateFile.delete();
    }
}
