package gmx.iderc.geoserver.tjs.web;

import gmx.iderc.geoserver.tjs.catalog.TJSCatalogObject;
import org.apache.wicket.Component;
import org.geoserver.web.ComponentBuilder;
import org.geoserver.web.FormTestPage;

public class TJSConfirmRemovalPanelTest extends TJSWicketTestSupport {

    void setupPanel(final TJSCatalogObject... roots) {
        tester.startPage(new FormTestPage(new ComponentBuilder() {

            public Component buildComponent(String id) {
                return new TJSConfirmRemovalPanel(id, roots);
            }
        }));
    }

    public void testRemoveFramework() {
        setupPanel(getTjsCatalog().getFrameworkByName("Provincias"));

        print(tester.getLastRenderedPage(), true, true);

        tester.assertRenderedPage(FormTestPage.class);
        tester.assertNoErrorMessage();

//        tester.assertLabel("form:panel:removedObjects:storesRemoved:stores", "cite");

//        String layers = tester.getComponentFromLastRenderedPage("form:panel:removedObjects:layersRemoved:layers").getDefaultModelObjectAsString();
//        String[] layerArray = layers.split(", ");
//        DataStoreInfo citeStore = getCatalog().getStoreByName("cite", DataStoreInfo.class);
//        List<FeatureTypeInfo> typeInfos = getCatalog().getResourcesByStore(citeStore, FeatureTypeInfo.class);
//        assertEquals(typeInfos.size(), layerArray.length);
    }

//    public void testRemoveDataset() {
//        FrameworkInfo finfo = getTjsCatalog().getFrameworkByName("DefaultFrameworkInfo");
//        setupPanel(getTjsCatalog().getDatasetByName(finfo.getId(),"DefaultDatasetInfo"));
//
//        print(tester.getLastRenderedPage(), true, true);
//
//        tester.assertRenderedPage(FormTestPage.class);
//        tester.assertNoErrorMessage();
//    }

}
