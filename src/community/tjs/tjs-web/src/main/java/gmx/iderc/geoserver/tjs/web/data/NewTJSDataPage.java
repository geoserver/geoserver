/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.data;

import gmx.iderc.geoserver.tjs.data.TJSDataAccessFactory;
import gmx.iderc.geoserver.tjs.data.TJSDataAccessFinder;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.SubmitLink;
import org.apache.wicket.markup.html.list.ListItem;
import org.apache.wicket.markup.html.list.ListView;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.web.CatalogIconFactory;

import java.util.*;

/**
 * Page that presents a list of vector and raster store types available in the classpath in order to
 * choose what kind of data source to create, as well as which workspace to create the store in.
 * <p>
 * Meant to be called by {@link DataPage} when about to add a new datastore or coverage.
 * </p>
 *
 * @author Gabriel Roldan
 */
@SuppressWarnings("serial")
public class NewTJSDataPage extends TJSBasePage {

    // do not access directly, it is transient and the instance can be the de-serialized version
    private transient Map<String, TJSDataAccessFactory> dataStores = getAvailableDataStores();


    @SuppressWarnings("serial")
    public NewTJSDataPage() {

        final boolean thereAreWorkspaces = !getTJSCatalog().getFrameworks().isEmpty();

        if (!thereAreWorkspaces) {
            super.error((String) new ResourceModel("NewTJSDataPage.noFrameworksErrorMessage")
                                         .getObject());
        }

        final Form storeForm = new Form("tjsStoreForm");
        add(storeForm);

        final ArrayList<String> sortedDsNames = new ArrayList<String>(getAvailableDataStores()
                                                                              .keySet());
        Collections.sort(sortedDsNames);

        final CatalogIconFactory icons = CatalogIconFactory.get();
        final ListView dataStoreLinks = new ListView("tjsResources", sortedDsNames) {
            @Override
            protected void populateItem(ListItem item) {
                final String dataStoreFactoryName = item.getDefaultModelObjectAsString();
                final TJSDataAccessFactory factory = getAvailableDataStores()
                                                             .get(dataStoreFactoryName);
                final String description = factory.getDescription();
                SubmitLink link;
                link = new SubmitLink("resourcelink") {
                    @Override
                    public void onSubmit() {
                        setResponsePage(new DataStoreNewPage(dataStoreFactoryName));
                    }
                };
                link.setEnabled(thereAreWorkspaces);
                link.add(new Label("resourcelabel", dataStoreFactoryName));
                item.add(link);
                item.add(new Label("resourceDescription", description));
//                Image icon = new Image("tjsStoreIcon", icons.getStoreIcon(factory.getClass()));
//                // TODO: icons could provide a description too to be used in alt=...
//                icon.add(new AttributeModifier("alt", true, new Model("")));
//                item.add(icon);
            }
        };

        storeForm.add(dataStoreLinks);
    }

    /**
     * @return the name/description set of available datastore factories
     */
    private Map<String, TJSDataAccessFactory> getAvailableDataStores() {
        // dataStores is transient, a back button may get us to the serialized version so check for
        // it
        if (dataStores == null) {
            final Iterator<TJSDataAccessFactory> availableDataStores;

            availableDataStores = TJSDataAccessFinder.getAvailableDataStores();

            Map<String, TJSDataAccessFactory> storeNames = new HashMap<String, TJSDataAccessFactory>();

            while (availableDataStores.hasNext()) {
                TJSDataAccessFactory factory = availableDataStores.next();
                if (factory.getDisplayName() != null) {
                    storeNames.put(factory.getDisplayName(), factory);
                }
            }
            dataStores = storeNames;
        }
        return dataStores;
    }

}
