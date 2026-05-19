/* (c) 2026 Open Source Geospatial Foundation - all rights reserved
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.web.demo;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import org.junit.Test;

/** Verifies markup/script hooks used by the preview formats UI behavior. */
public class PreviewFormatsUiBehaviorTest {

    @Test
    public void testFormatsLayoutRendersPillAndCopyControls() throws IOException {
        String html = panelHtml();
        assertTrue(html.contains("preview-link-items"));
        assertTrue(html.contains("preview-section-content"));
        assertTrue(html.contains("preview-link-items-separator"));
        assertTrue(html.contains("preview-copy-button"));
        assertTrue(html.contains("wicket:id=\"copyLinkButton\""));
    }

    @Test
    public void testFilterHooksAreRendered() throws IOException {
        String html = panelHtml();
        assertTrue(html.contains("preview-filter-input"));
        String js = panelJs();
        assertTrue(js.contains("data-section-title"));
        assertTrue(js.contains("data-filter-label"));
        assertTrue(js.contains("filterInput.addEventListener(\"input\""));
    }

    @Test
    public void testExpandCollapseHooksAndScriptAreRendered() throws IOException {
        String html = panelHtml();
        assertTrue(html.contains("preview-more-toggle"));
        assertTrue(html.contains("aria-expanded=\"false\""));
        assertTrue(
                html.contains(
                        "data-expand-label:PreviewHomePageContentProvider.expandSection;data-collapse-label:PreviewHomePageContentProvider.collapseSection"));
        assertTrue(html.contains("preview-section-header"));
        assertTrue(html.contains("gs-icon-bullet-arrow-right"));
        String js = panelJs();
        assertTrue(js.contains("section.classList.toggle(\"is-expanded\")"));
        assertTrue(js.contains("updateOverflowToggle(section)"));
        assertTrue(js.contains("aria-expanded"));
        assertTrue(js.contains("gs-icon-bullet-arrow-right"));
        assertTrue(js.contains("gs-icon-bullet-arrow-down"));
        assertFalse(js.contains("onchange="));
    }

    private String panelHtml() throws IOException {
        return readResource("PreviewHomePageContentProvider$PreviewPanel.html");
    }

    private String panelJs() throws IOException {
        return readResource("PreviewHomePageContentProvider.js");
    }

    private String readResource(String name) throws IOException {
        try (InputStream in = PreviewHomePageContentProvider.class.getResourceAsStream(name)) {
            if (in == null) throw new IOException("Missing resource: " + name);
            return new String(in.readAllBytes(), UTF_8);
        }
    }
}
