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

Using the language function
---------------------------

GeoServer provides a ``language`` function that can be used to get the ``LANGUAGE`` requested in ``GetMap`` or ``GetFeatureInfo`` request. The function can be used to generate maps whose symbology is language dependent.

Here is an example providing **labels in multiple languages**, integrating the ``language`` function with ``Recode`` e.g:

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


The empty ``<ogc:Literal/>`` elements acts as the default language, matching a value with a missing language parameter. If there is no default value,the  default language will be returned. See :ref:`internationalization` for details on ``Default Language``.

It is also possible to use the ``language`` function in a rule filter, **filtering rules
for both rendering and legend production** purposes. This one shows how to refer to different symbols
based on the current language:

.. code-block:: xml

        <Rule>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:Function name="language"/>
              <ogc:Literal>it</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:type="simple" xlink:href="it_symbol.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>32</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>
        <Rule>
          <ogc:Filter>
            <ogc:PropertyIsEqualTo>
              <ogc:Function name="language"/>
              <ogc:Literal>de</ogc:Literal>
            </ogc:PropertyIsEqualTo>
          </ogc:Filter>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:type="simple" xlink:href="de_symbol.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>32</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>

Specifically for the external graphics, if the external symbols are all co-located, and follow
a naming convention including the language identifier, then it's also possible to **embed the
language in the symbol URL**:

.. code-block:: xml

        <Rule>
          <PointSymbolizer>
            <Graphic>
              <ExternalGraphic>
                <OnlineResource xlink:type="simple" xlink:href="${language()}_symbol.png"/>
                <Format>image/png</Format>
              </ExternalGraphic>
              <Size>32</Size>
            </Graphic>
          </PointSymbolizer>
        </Rule>