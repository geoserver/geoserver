Manipulating the Catalog
========================
This tutorial discusses how to manipulate the GeoServer catalog programmatically
through scriptlet.  In particular, we will ensure that the layer titles in the
catalog all use title casing.

As mentioned in :doc:`hello`, the scriptlet environment includes a reference to
the GeoServer catalog.  We can pretty simply iterate over the layers in the
catalog::

    var iter = catalog.getLayers().iterator();
    var report = "Updated: ";

    while (iter.hasNext()) {
        var layer = iter.next();
        var resource = layer.getResource();
        var title = resource.getTitle();
        report += "\n " + title;
        title = toTitleCase(title);
        resource.setTitle(title);
        catalog.save(resource);
        report += " -> " + title;
    }

Here we fetch each layer, find the resource it represents, fetch the title,
convert it to title case with some toTitleCase function, and save it back to the
catalog.  We also maintain a log of all the titles we changed in a 'report'
string so that we can report it back to the user at the end::

    response.setEntity(new StringRepresentation(
        report, MediaType.TEXT_PLAIN
    ));

And, to tie up that last loose end, here's how that 'toTitleCase' function is
defined::

    function toTitleCase(str) {
        return (""+str).replace(/(:?^|\s)\w/g, function(x) {
            return x.toUpperCase();
        });
    }
