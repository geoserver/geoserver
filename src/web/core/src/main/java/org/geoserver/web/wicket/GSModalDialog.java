package org.geoserver.web.wicket;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import java.io.InputStreamReader;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalDialog;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.extensions.ajax.markup.html.modal.theme.DefaultTheme;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.OnDomReadyHeaderItem;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.geoserver.catalog.StyleInfo;

public class GSModalDialog extends ModalDialog {

    GSModalDialog _this;

    public static String jsModalDialogJS;

    static {
        try {
            jsModalDialogJS =
                    CharStreams.toString(
                            new InputStreamReader(
                                    GSModalDialog.class.getResourceAsStream(
                                            "/org/geoserver/web/wicket/gs-modal-dialog.js"),
                                    Charsets.UTF_8));

        } catch (Exception e) {

        }
    }

    public GSModalDialog(String id, String title) {
        super(id);
        _this = this;
        addTitleLabel(title);

        var dialog = (WebMarkupContainer) this.get("overlay:dialog");
        var closeform =
                new Form<StyleInfo>("GSModalDialog-closeForm") {
                    @Override
                    protected void onSubmit() {
                        super.onSubmit();
                    }
                };

        dialog.add(closeform);

        AjaxSubmitLink ajaxSubmit =
                new AjaxSubmitLink("GSModalDialog-closeButton") {
                    private static final long serialVersionUID = -695617463194724617L;

                    @Override
                    protected void onSubmit(AjaxRequestTarget target) {
                        _this.close(target);
                        onWindowClosed(target);
                    }
                };
        closeform.add(ajaxSubmit);

        DefaultTheme theme = new DefaultTheme();
        add(theme);
    }

    ModalWindow.WindowClosedCallback onWindowClosedCallback;

    public void setOnWindowClosedCallback(ModalWindow.WindowClosedCallback onWindowClosedCallback) {
        this.onWindowClosedCallback = onWindowClosedCallback;
    }

    public void onWindowClosed(AjaxRequestTarget target) {
        if (onWindowClosedCallback != null) {
            onWindowClosedCallback.onClose(target);
        }
    }

    public GSModalDialog(String id) {
        this(id, "");
    }

    protected void addTitleLabel(String title) {
        var dialog = (WebMarkupContainer) this.get("overlay:dialog");
        dialog.add(new Label("gsmodaldialog-title", title));
    }

    public void setTitle(String title) {
        var dialog = (WebMarkupContainer) this.get("overlay:dialog");
        dialog.remove("gsmodaldialog-title");
        addTitleLabel(title);
    }

    public String getContentId() {
        return ModalDialog.CONTENT_ID;
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        response.render(OnDomReadyHeaderItem.forScript(jsModalDialogJS));
    }
}
