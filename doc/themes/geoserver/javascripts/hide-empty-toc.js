/**
 * Hide "Page Contents" heading when table of contents is empty
 * 
 * Material for MkDocs generates a TOC sidebar even when there are no headings.
 * This script hides the entire TOC sidebar when the nav element has no children.
 */
document.addEventListener('DOMContentLoaded', function() {
  // Find all TOC navigation elements
  document.querySelectorAll('.md-nav--secondary').forEach(function(nav) {
    // Check if the nav has no child elements (empty TOC)
    if (nav.children.length === 0) {
      // Hide the entire sidebar inner container (includes ::before "PAGE CONTENTS" text)
      var sidebarInner = nav.closest('.md-sidebar__inner');
      if (sidebarInner) {
        sidebarInner.style.display = 'none';
      }
    }
  });
});
