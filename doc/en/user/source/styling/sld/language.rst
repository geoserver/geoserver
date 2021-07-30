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

Labels in different languages
-----------------------------

GeoServer provides a ``language`` function that can be used to get the ``LANGUAGE`` requested in ``GetMap`` or ``GetFeatureInfo`` request. The function can be used to generate maps whose symbology is language dependent.
Here is an example providing labels in multiple languages, integrating the ``language`` function with ``Recode`` e.g:

.. code-block:: xml

          <TextSymbolizer>
                 <Label>
                   <ogc:Function name="Recode">
                     <ogc:Function name="language"/>
                     <ogc:Literal/>
                     <ogc:PropertyName>name_default</ogc:PropertyName>
                     <ogc:Literal>en</ogc:Literal>
                     <ogc:PropertyName>name_en</ogc:PropertyName>
                     <ogc:Literal>it</ogc:Literal>
                     <ogc:PropertyName>name_it</ogc:PropertyName>
                     <ogc:Literal>fr</ogc:Literal>
                     <ogc:PropertyName>name_fr</ogc:PropertyName>
                   </ogc:Function>
                 </Label>
                 <Fill>
                   <CssParameter name="fill">#000000</CssParameter>
                 </Fill>
         </TextSymbolizer>


From the example it is possible to see that by putting an empty property name it is possible to match a value with a notExisting/* language parameter. If there is no default value, default language will be returned. See :ref:`internationalization` for details on ``Default Language``.
