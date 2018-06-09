/* (c) 2016 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geogig.geoserver.config.WhitelistRule;
import org.geogig.geoserver.web.security.SecurityLogsPanel;
import org.geogig.geoserver.web.security.WhitelistRuleEditor;
import org.geogig.geoserver.web.security.WhitelistRulePanel;
import org.geoserver.web.GeoServerSecuredPage;

/** */
public class RemotesPage extends GeoServerSecuredPage {

    private ModalWindow window;

    public RemotesPage() {
        add(new SecurityLogsPanel("securityLogsPanel"));

        window = new ModalWindow("popup");
        add(window);
        final WhitelistRulePanel whitelistRulePanel =
                new WhitelistRulePanel("whitelist.rules", window);
        whitelistRulePanel.setOutputMarkupId(true);
        add(whitelistRulePanel);

        add(
                new AjaxLink<Void>("whitelist.add") {

                    private static final long serialVersionUID = 5869313981483016964L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        //                window.setInitialHeight(300);
                        //                window.setInitialWidth(300);
                        window.setTitle(new Model<String>("Edit whitelist rule"));
                        IModel<WhitelistRule> model =
                                new Model<>(new WhitelistRule(null, null, false));
                        window.setContent(
                                new WhitelistRuleEditor(
                                        window.getContentId(),
                                        model,
                                        window,
                                        whitelistRulePanel,
                                        true));
                        window.show(target);
                    };
                });
    }
}
