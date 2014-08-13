/* Copyright (c) 2001 - 2007 TOPP - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package gmx.iderc.geoserver.tjs.web.dataset;

import gmx.iderc.geoserver.tjs.TJSExtension;
import gmx.iderc.geoserver.tjs.catalog.*;
import gmx.iderc.geoserver.tjs.data.TJSDataStore;
import gmx.iderc.geoserver.tjs.data.TJSDatasource;
import gmx.iderc.geoserver.tjs.web.TJSBasePage;
import gmx.iderc.geoserver.tjs.web.columns.ColumnEditPage;
import gmx.iderc.geoserver.tjs.web.columns.ColumnsProvider;
import org.apache.wicket.Component;
import org.apache.wicket.PageParameters;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.markup.html.form.DateTextField;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.*;
import org.apache.wicket.model.CompoundPropertyModel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.protocol.http.servlet.ServletWebRequest;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.validator.StringValidator;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;
import org.geoserver.web.wicket.XMLNameValidator;
import org.geotools.util.NullProgressListener;

import java.io.*;
import java.net.*;
import java.util.*;

import static gmx.iderc.geoserver.tjs.web.columns.ColumnsProvider.*;

/**
 * A page listing the resources contained in a store, and whose links will bring
 * the user to a new resource configuration page
 *
 * @author Andrea Aime - OpenGeo
 */
@SuppressWarnings("serial")
public class DatasetEditPage extends TJSBasePage {

    Form form;
    GeoServerTablePanel<ColumnInfo> columns;

    public DatasetEditPage(PageParameters parameters) {
        String dataStoreId = parameters.getString("dataStoreId");
        String datasetName = parameters.getString("datasetName");
//    public DatasetEditPage(String dataStoreId, String datasetName) {

        DatasetInfo datasetInfo = getTJSCatalog().getDataset(dataStoreId, datasetName);
        final ColumnsProvider provider = new ColumnsProvider(datasetInfo);

        final IModel model = new CompoundPropertyModel(datasetInfo);
        form = new Form("form", model) {

            @Override
            protected void onSubmit() {
                TJSCatalog catalog = getTJSCatalog();
                DatasetInfo dsi = (DatasetInfo) form.getModelObject();
                catalog.save(dsi);
                setResponsePage(DatasetPage.class);

                if (dsi.getAutoJoin()) {
                    joinMap(dsi);
                }

            }
        };
        add(form);

        TextField<String> nameTextField = new TextField<String>("name");
        nameTextField.setRequired(true);
        nameTextField.add(new XMLNameValidator());
        form.add(nameTextField.setRequired(true));

        DataStoreInfo dataStoreInfo = datasetInfo.getDataStore();
        TJSDataStore store = dataStoreInfo.getTJSDataStore(new NullProgressListener());
        TJSDatasource tJSDatasource = store.getDatasource(datasetInfo.getDatasetName(), dataStoreInfo.getConnectionParameters());
        String[] fields = tJSDatasource.getFields();
        List<String> fieldList = Arrays.asList(fields);

        DropDownChoice<String> geoKeyfield = new DropDownChoice<String>("geoKeyField", fieldList);
        form.add(geoKeyfield);

        DropDownChoice<String> framework = new DropDownChoice<String>("framework", new FrameworkListModel(), new FrameworkListRenderer());
        form.add(framework);

        TextField<String> descriptionTextField = new TextField<String>("description");
        form.add(descriptionTextField.setRequired(false));

        TextField<String> organizationTextField = new TextField<String>("organization");
        form.add(organizationTextField.setRequired(false));

        DateTextField referenceDateTextField = new DateTextField("referenceDate", "dd-MM-yyyy");
        referenceDateTextField.setConvertEmptyInputStringToNull(true);
        form.add(referenceDateTextField.setRequired(false));

        TextField<String> versionTextField = new TextField<String>("version");
        form.add(versionTextField.setRequired(false));

        TextField<String> documentationTextField = new TextField<String>("documentation");
        documentationTextField.add(new StringValidator() {

            @Override
            protected void onValidate(IValidatable<String> paramIValidatable) {
                try {
                    URI uri = new URI(paramIValidatable.getValue());
                } catch (URISyntaxException ex) {
                    error(paramIValidatable, "badUriError");
                }
            }
        });
        form.add(documentationTextField.setRequired(false));

        //selector para estilos por defecto del dataset, Alvaro Javier
        DropDownChoice defaultStyleChoice = new DropDownChoice("defaultStyle",
                                                                      new LoadableDetachableModel() {
                                                                          @Override
                                                                          protected Object load() {
                                                                              List<StyleInfo> styles = getCatalog().getStyles();
                                                                              List<String> stylesNames = new ArrayList<String>(styles.size());
                                                                              for (StyleInfo si : styles) {
                                                                                  stylesNames.add(si.getName());
                                                                              }
                                                                              Collections.sort(stylesNames, new Comparator<String>() {
                                                                                  @Override
                                                                                  public int compare(String s1, String s2) {
                                                                                      return s1.compareToIgnoreCase(s2);
                                                                                  }
                                                                              });
                                                                              return stylesNames;
                                                                          }
                                                                      }
        );
        form.add(defaultStyleChoice);

        CheckBox autoJoinChk = new CheckBox("autoJoin", new PropertyModel(model, "autoJoin"));
        form.add(autoJoinChk);

        CheckBox enabledChk = new CheckBox("enabled", new PropertyModel(model, "enabled"));
        form.add(enabledChk);

        columns = new GeoServerTablePanel<ColumnInfo>("columns", provider, true) {

            @Override
            protected Component getComponentForProperty(String id, IModel itemModel, Property<ColumnInfo> property) {
                if (property == NAME) {
                    return columnLink(id, itemModel);
                } else if (property == TITLE) {
                    return new Label(id, TITLE.getModel(itemModel));
                } else if (property == TYPE) {
                    return new Label(id, TYPE.getModel(itemModel));
                }
                throw new IllegalArgumentException("Don't know a property named " + property.getName());
            }

        };
        add(columns);

        SubmitLink submitLink = new SubmitLink("save", form);
        form.add(submitLink);
        form.setDefaultButton(submitLink);

        AjaxLink cancelLink = new AjaxLink("cancel") {

            @Override
            public void onClick(AjaxRequestTarget target) {
                setResponsePage(DatasetPage.class);
            }
        };
        form.add(cancelLink);
    }

    private void joinMap(DatasetInfo dsi) {

        final ServletWebRequest servletWebRequest = (ServletWebRequest) getRequest();
        final String url = servletWebRequest.getHttpServletRequest().getRequestURL().toString();
        final String baseURL = url.substring(0, url.indexOf("/web/") + 1);//+1 para dejarle el / final

        StringBuilder attributes = new StringBuilder("");
        for (ColumnInfo ci : dsi.getColumns()) {
            attributes.append(ci.getName());
            attributes.append(",");
        }
        if (!attributes.equals("")) {
            attributes.deleteCharAt(attributes.length() - 1);//borrar la coma del final
        }

        final String getDataURL = baseURL +
                                          "ows?Service=TJS&Version=1.0&Request=GetData&FrameworkURI=" + dsi.getFramework().getUri() +
                                          "&DatasetURI=" + dsi.getDatasetUri() +
                                          "&attributes=" + attributes.toString();

        String encodedGetDataURL = "";
        try {
            encodedGetDataURL = URLEncoder.encode(getDataURL, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            ;//no dería acurrir nunca
        }

        final String request = baseURL + "ows?Service=TJS&Version=1.0&Request=JoinData"
                                       + "&FrameworkURI=" + dsi.getFramework().getUri()
                                       + "&GetDataURL=" + encodedGetDataURL;

        try {
            final URL reqURL = new URL(request);
            final InputStream inputStream = reqURL.openStream();
            LOGGER.info("Respuesta de join Data:");
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = reader.readLine()) != null)
                LOGGER.info(line);
            inputStream.close();
        } catch (MalformedURLException e) {
            ;
        } catch (IOException e) {
            ;
        }

    }

    private Component columnLink(String id, final IModel model) {
        IModel nameModel = NAME.getModel(model);

        IModel datasetModel = form.getModel();
        DatasetInfo dsi = (DatasetInfo) datasetModel.getObject();

        PageParameters params = new PageParameters();
        params.add("dataStoreId", dsi.getDataStore().getId());
        params.add("datasetName", dsi.getName());
        params.add("columnName", (String) nameModel.getObject());
        return new SimpleBookmarkableLink(id, ColumnEditPage.class, nameModel,
                                                 params);
    }

    class FrameworkListModel extends LoadableDetachableModel {

        @Override
        protected Object load() {
            TJSCatalog catalog = TJSExtension.getTJSCatalog();
            List<FrameworkInfo> frameworks = new ArrayList<FrameworkInfo>(catalog.getFrameworks());
            Collections.sort(frameworks, new FrameworkComparator());
            return frameworks;
        }

        class FrameworkComparator implements Comparator<FrameworkInfo> {

            public int compare(FrameworkInfo w1, FrameworkInfo w2) {
                return w1.getName().compareToIgnoreCase(w2.getName());
            }
        }
    }

    class FrameworkListRenderer implements IChoiceRenderer {

        public Object getDisplayValue(Object object) {
            return ((FrameworkInfo) object).getName();
        }

        public String getIdValue(Object object, int index) {
            return ((FrameworkInfo) object).getId();
        }
    }
}
