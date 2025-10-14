/* (c) 2022 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.system.status;

import static org.geoserver.web.system.status.ConsoleInfoUtils.getHistoMemoryDump;
import static org.geoserver.web.system.status.ConsoleInfoUtils.getThreadsInfo;

import java.io.Serial;
import org.apache.wicket.markup.head.IHeaderResponse;
import org.apache.wicket.markup.head.JavaScriptHeaderItem;
import org.apache.wicket.markup.head.OnEventHeaderItem;
import org.apache.wicket.markup.html.form.TextArea;
import org.apache.wicket.markup.html.link.Link;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.LoadableDetachableModel;

/** Panel displaying the JVM threads allocated or the heap memory objects */
public class JVMConsolePanel extends Panel {

    private final boolean lockedMonitors = true;
    private final boolean lockedSynchronizers = true;

    private String dumpLog;

    public JVMConsolePanel(String id) {
        super(id);

        add(new Link<String>("dumpThread") {
            @Serial
            private static final long serialVersionUID = 9014754243470867547L;

            @Override
            public void onClick() {
                dumpLog = getThreadsInfo(lockedMonitors, lockedSynchronizers);
            }
        });

        add(new Link<String>("dumpHeap") {
            @Serial
            private static final long serialVersionUID = 9014754243470867547L;

            @Override
            public void onClick() {
                dumpLog = getHistoMemoryDump();
            }
        });

        final TextArea<String> logs = new TextArea<>("dumpContent", new LoadableDetachableModel<>() {
            @Override
            protected String load() {
                return dumpLog;
            }
        });
        logs.setOutputMarkupId(true);
        logs.setMarkupId("dumpContent");
        add(logs);

        add(new Link<String>("downloadlink") {
            @Serial
            private static final long serialVersionUID = 9014754243470867547L;

            @Override
            public void onClick() {}
        });
    }

    @Override
    public void renderHead(IHeaderResponse response) {
        super.renderHead(response);
        response.render(JavaScriptHeaderItem.forScript(
                // reference
                // https://www.foolishdeveloper.com/2021/12/save-textarea-text-to-file.html
                """
                     function downloadFile(filename) {
                          // It works on all HTML5 Ready browsers as it uses the download attribute of the <a> element:
                          const element = document.createElement('a');
                          //A blob is a data type that can store binary data
                          // "type" is a MIME type
                          // It can have a different value, based on a file you want to save
                          const dumpContent = document.getElementById("dumpContent").value;
                          const blob = new Blob([dumpContent], { type: 'plain/text' });
                          //createObjectURL() static method creates a DOMString containing a URL representing the object given in the parameter.
                          const fileUrl = URL.createObjectURL(blob);
                          //setAttribute() Sets the value of an attribute on the specified element.
                          element.setAttribute('href', fileUrl); //file location
                          element.setAttribute('download', filename); // file name
                          element.style.display = 'none';
                          //use appendChild() method to move an element from one element to another
                          document.body.appendChild(element);
                          element.click();
                          //The removeChild() method of the Node interface removes a child node from the DOM and returns the removed node
                          document.body.removeChild(element);
                        };\
                """,
                "downloadFile"));
        response.render(OnEventHeaderItem.forMarkupId("downloadlink", "click", "downloadFile('dump.log')"));
    }
}
