/* (c) 2014 - 2016 Open Source Geospatial Foundation - all rights reserved
 * (c) 2001 - 2013 OpenPlans
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.wms.web.data;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.logging.Level;
import org.apache.wicket.Component;
import org.apache.wicket.WicketRuntimeException;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.behavior.Behavior;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnLoadHeaderItem;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.StringResourceModel;
import org.apache.wicket.request.mapper.parameter.PageParameters;
import org.geoserver.catalog.SLDHandler;
import org.geoserver.catalog.StyleInfo;
import org.geoserver.catalog.Styles;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geotools.util.Version;

/** Style edit page */
public class StyleEditPage extends AbstractStylePage {

    private static final long serialVersionUID = 5478083954402101191L;

    public static final String NAME = "name";
    public static final String WORKSPACE = "workspace";

    public StyleEditPage(PageParameters parameters) {
        String name = parameters.get(NAME).toString();
        String workspace = parameters.get(WORKSPACE).toOptionalString();

        StyleInfo si =
                workspace != null
                        ? getCatalog().getStyleByName(workspace, name)
                        : getCatalog().getStyleByName(name);

        if (si == null) {
            error(new ParamResourceModel("StyleEditPage.notFound", this, name).getString());
            doReturn(StylePage.class);
            return;
        }

        recoverCssStyle(si);
        initPreviewLayer(si);
        initUI(si);

        if (!isAuthenticatedAsAdmin()) {
            // global styles only editable by full admin
            if (si.getWorkspace() == null) {
                styleForm.setEnabled(false);

                editor.add(new AttributeAppender("class", new Model<String>("disabled"), " "));
                get("validate")
                        .add(
                                new AttributeAppender(
                                        "style", new Model<String>("display:none;"), " "));
                add(
                        new Behavior() {

                            private static final long serialVersionUID = -4336130086161028141L;

                            @Override
                            public void renderHead(Component component, IHeaderResponse response) {
                                super.renderHead(component, response);
                                response.render(
                                        OnLoadHeaderItem.forScript(
                                                "document.getElementById('mainFormSubmit').style.display = 'none';"));
                                response.render(
                                        OnLoadHeaderItem.forScript(
                                                "document.getElementById('uploadFormSubmit').style.display = 'none';"));
                            }
                        });
                info(new StringResourceModel("globalStyleReadOnly", this, null).getString());
            }
        }
    }

    public StyleEditPage(StyleInfo style) {
        super(style);
    }

    @Override
    protected String getTitle() {
        StyleInfo style = styleModel.getObject();
        String styleName = "";
        if (style != null) {
            styleName =
                    (style.getWorkspace() == null ? "" : style.getWorkspace().getName() + ":")
                            + style.getName();
        }

        return new ParamResourceModel("title", this, styleName).getString();
    }

    @Override
    protected void onStyleFormSubmit() {
        // write out the file and save name modifications
        try {
            StyleInfo style = getStyleInfo();
            String format = style.getFormat();
            style.setFormat(format);
            Version version = Styles.handler(format).version(rawStyle);
            style.setFormatVersion(version);
            // make sure the legend is null if there is no URL
            if (null == style.getLegend()
                    || null == style.getLegend().getOnlineResource()
                    || style.getLegend().getOnlineResource().isEmpty()) {
                style.setLegend(null);
            }
            // write out the SLD, we try to use the old style so the same path is used
            StyleInfo stylePath = getCatalog().getStyle(style.getId());
            if (stylePath == null) {
                // the old style is no available anymore, so use the new path
                stylePath = style;
            }
            // ask the catalog to write the style
            try {
                getCatalog()
                        .getResourcePool()
                        .writeStyle(stylePath, new ByteArrayInputStream(rawStyle.getBytes()));
            } catch (IOException e) {
                throw new WicketRuntimeException(e);
            }
            // update the catalog
            getCatalog().save(style);
            // provide feedback to the user
            styleForm.info("Style saved");
            // retrieve sld style for non-sld formatted styles on update
            if ((!SLDHandler.FORMAT.equals(format))) {
                getCatalog().getResourcePool().getStyle(stylePath);
            }
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error occurred saving the style", e);
            styleForm.error(e);
        }
    }
}
