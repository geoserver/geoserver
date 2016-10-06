.. _sld_language:

i18N in SLD
================

This section describes how to specify metadata (titles and abstracts) in different languages in SLD documents.

Metadata in different languages
-------------------------------

GeoServer extends Title and Abstract sections, so that text in different languages can be included.

This is an example of the syntax to use:

.. code-block:: xml

          <Title>This is the default title
            <Localized lang="en">English title</Localized>
            <Localized lang="it">Titolo in italiano</Localized>
          </Title>
          
A default text (``This is the default title`` in the example) and a set of Localized sections, one for each language that you want to support.

Each ``Localized`` section specifies the language (using a two letter abbreviation in the ``lang`` attribute) and the related text.

Currently, GeoServer supports localized text in SLD in WMS GetLegendGraphic requests (legends that contain labels are rendered using the
requested language, if a ``LANGUAGE`` parameter is added to the request, e.g. ``LANGUAGE=it``).

