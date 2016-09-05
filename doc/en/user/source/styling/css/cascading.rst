.. _css_cascading:

Understanding Cascading in CSS 
==============================

Cascading Style Sheets are the styling language of the web, use a simple syntax, but sometimes their
simplicity can be deceitful if the writer is not aware of how the "Cascading" part of it works.
The confusion might become greater by looking at the translated SLD, and wondering how all the SLD
rules came to be from a much smaller set of CSS rules.

This document tries to clarify how cascading works, how it can be controlled in SLD translation,
and for those that would prefer simpler, if more verbose, styles, shows how to turn cascading off for good.  

CSS rules application
---------------------

Given a certain feature, how are CSS rules applied to it? This is roughly the algorithm:

* Locate all rules whose selector matches the current feature
* Sort them by specificity, less specific to more specific
* Have more specific rules add to and override properties set in less specific rules

As you can see, depending on the feature attributes a new rule is built by the above algorithm, mixing all
the applicable rules for that feature.

The core of the algorithm allows to prepare rather succinct style sheets for otherwise very complex rule sets,
by setting the common bits in less specific rules, and override them specifying the exceptions to the norm
in more specific rules. 

Understanding specificity
-------------------------

In web pages CSS `specificity <http://www.w3.org/TR/CSS21/cascade.html#specificity>`_ is setup as a tuple of four numbers called a,b,c,d:

* ``a``: set to 1 if the style is local to an element, that is, defined in the element ``style`` attribute
* ``b``: counts the number of ID attributes in the selector
* ``c``: count the number of other attributes and pseudo classes in the selector
* ``d``: count the number of element names or pseudo elements in the selector

``a`` is more important than ``b``, which is more important than ``c``, and so on, so for example, if one rule has ``a=1`` and then second has ``a=0``, the first
is more specific, regardless of what values have ``b``, ``c`` and ``d``.

Here are some examples from the CSS specification, from less specific to more specific:

.. code-block:: css

     *             {}  /* a=0 b=0 c=0 d=0 -> specificity = 0,0,0,0 */
     li            {}  /* a=0 b=0 c=0 d=1 -> specificity = 0,0,0,1 */
     li:first-line {}  /* a=0 b=0 c=0 d=2 -> specificity = 0,0,0,2 */
     ul li         {}  /* a=0 b=0 c=0 d=2 -> specificity = 0,0,0,2 */
     ul ol+li      {}  /* a=0 b=0 c=0 d=3 -> specificity = 0,0,0,3 */
     h1 + *[rel=up]{}  /* a=0 b=0 c=1 d=1 -> specificity = 0,0,1,1 */
     ul ol li.red  {}  /* a=0 b=0 c=1 d=3 -> specificity = 0,0,1,3 */
     li.red.level  {}  /* a=0 b=0 c=2 d=1 -> specificity = 0,0,2,1 */
     #x34y         {}  /* a=0 b=1 c=0 d=0 -> specificity = 0,1,0,0 */
     style="..."       /* a=1 b=0 c=0 d=0 -> specificity = 1,0,0,0 */

In cartographic CSS there are no HTML elements that could have a local style, so ``a`` is always zero. 
The others are calculated as follows:

* ``b``: number of feature ids in the rule 
* ``c``: number of attributes in CQL filters and pseudo-classes (e.g., ``:mark``) used in the selector 
* ``d``: 1 if a typename is specified, 0 otherwise

Here are some examples, from less to more specific:
 
.. code-block:: css

     *                  {}  /* a=0 b=0 c=0 d=0 -> specificity = 0,0,0,0 */
     topp:states        {}  /* a=0 b=0 c=0 d=1 -> specificity = 0,0,0,1 */
     :mark              {}  /* a=0 b=0 c=1 d=0 -> specificity = 0,0,1,0 */
     [a = 1 and b > 10] {}  /* a=0 b=0 c=1 d=0 -> specificity = 0,0,2,0 */
     #states.1          {}  /* a=0 b=1 c=0 d=0 -> specificity = 0,1,0,0 */
     
In case two rules have the same specificity, the last one in the document wins.

Understanding CSS to SLD translation in cascading mode
------------------------------------------------------

As discussed above, CSS rule application can potentially generate a different rule for each
feature, depending on its attributes and how they get matched by the various CSS selectors.

SLD on the other hand starts from the rules, and applies all of them, in turn, to each feature,
painting each matching rule. The two evaluation modes are quite different, in order to turn
CSS into SLD the translator has to generate every possible CSS rule combination, while making
sure the generated SLD rules are mutually exclusive (CSS generated a single rule for a given
feature in the end).

The combination of all rules is called a `power set <https://en.wikipedia.org/wiki/Power_set>`_, and the exclusivity is guaranteed by
negating the filters of all previously generated SLD rules and adding to the current one.
As one might imagine, this would result in a lot of rules, with very complex filters.

The translator addresses the above concerns by applying a few basic strategies:

* The generated filters are evaluated in memory, if the filter is found to be "impossible", that is, something that
  could never match an exiting feature, the associated rule is not emitted (e.g., ``a = 1 and a = 2`` or ``a = 1 and not(a = 1)``)
* The generated SLD has a vendor option ``<sld:VendorOption name="ruleEvaluation">first</sld:VendorOption>`` which forces
  the renderer to give up evaluating further rules once one of them actually matched a feature
  
The above is nice and sufficient in theory, while in practice it can break down with very complex CSS styles
having a number of orthogonal selectors (e.g., 10 rules controlling the fill on the values of attribute ``a`` and
10 rules controlling the stroke on values of attribute ``b``, and another 10 rules controlling the opacity of fill and stroke based on attribute ``c``, 
resulting in 1000 possible combinations).

For this reason by default the translator will try to generated simplified and fully exclusive
rules only if the set of rules is "small", and will instead generate the full power set
otherwise, to avoid incurring in a CSS to SLD translation time of minutes if not hours.

The translation modes are controlled by the ``@mode`` directive, with the following values:

* ``'Exclusive'``: translate the style sheet in a minimum set of SLD rules with simplified selectors, taking whatever time and memory required
* ``'Simple'``: just generated the power set without trying to build a minimum style sheet, ensuring the translation is fast, even if the resulting SLD might look very complex
* ``'Auto'``: this is the default value, it will perform the power set expansion, and then will proceed in ``Exclusive`` mode if the power set contains less than 100 derived rules, or in ``Simple`` mode otherwise. The rule count threshold can be manually controlled by using the ``@autoThreshold`` directive.  

The Flat translation mode
-------------------------

The ``@mode`` directive has one last possible value, ``Flat``, which enables a flat translation mode in
which specificity and cascading are not applied.

In this mode the CSS will be translated almost 1:1 into a corresponding SLD, each CSS rule producing and equivalent SLD rule,
with the exception of the rules with pseudo-classes specifying how to stroke/fill marks and symbols in general.

Care should be taken when writing rules with pseudo classes, they will be taken into consideration only
if their selector matches the one of the preceding rule. Consider this example:

.. code-block:: css
  
  @mode "Flat";
  
  [type = 'Capital'] { 
    mark: symbol(circle);
  }
  
  [type = 'Capital'] :mark {
    fill: white;
    size: 6px;
  }
  
  :mark {
    stroke: black;
    stroke-width: 2px;
  }
    
In the above example, the first rule with the ``:mark`` pseudo class will be taken into consideration and
merged with the capital one, the second one instead will be ignored. The resulting SLD will thus not
contain any stroke specification for the 'circle' mark:

.. code-block:: xml

  <?xml version="1.0" encoding="UTF-8"?><sld:StyledLayerDescriptor xmlns="http://www.opengis.net/sld" 
        xmlns:sld="http://www.opengis.net/sld" xmlns:ogc="http://www.opengis.net/ogc" 
        xmlns:gml="http://www.opengis.net/gml" version="1.0.0">
    <sld:NamedLayer>
      <sld:Name/>
      <sld:UserStyle>
        <sld:Name>Default Styler</sld:Name>
        <sld:FeatureTypeStyle>
          <sld:Rule>
            <ogc:Filter>
              <ogc:PropertyIsEqualTo>
                <ogc:PropertyName>type</ogc:PropertyName>
                <ogc:Literal>Capital</ogc:Literal>
              </ogc:PropertyIsEqualTo>
            </ogc:Filter>
            <sld:PointSymbolizer>
              <sld:Graphic>
                <sld:Mark>
                  <sld:WellKnownName>circle</sld:WellKnownName>
                  <sld:Fill>
                    <sld:CssParameter name="fill">#ffffff</sld:CssParameter>
                  </sld:Fill>
                </sld:Mark>
                <sld:Size>6</sld:Size>
              </sld:Graphic>
            </sld:PointSymbolizer>
          </sld:Rule>
        </sld:FeatureTypeStyle>
      </sld:UserStyle>
    </sld:NamedLayer>
  </sld:StyledLayerDescriptor>

The advantages of flat mode are:

* Easy to understand, the rules are applied in the order they are written
* Legend control, the generated legend contains no surprises as rules are not mixed together and are not reordered

The main disadvantage is that there is no more a way to share common styling bits in general rules, all common bits have to be
repeated in all rules.

.. note:: In the future we hope to add the ability to nest rules, which is going to address some of the limitations of flat mode without introducing the most complex bits of the standard cascading mode

Comparing cascading vs flat modes, an example
---------------------------------------------

Consider the following CSS:

.. code-block:: css
  
  * { stroke: black; stroke-width: 10 }
  
  [cat = 'important'] { stroke: yellow; }

If the above style is translated in cascading mode, it will generate two mutually exclusive SLD rules:

* One applying a 10px wide yellow stroke on all features whose cat attribute is 'important'
* One applying a 10px wide black stroke on all feature whose cat attribute is not 'important'

Thus, each feature will be painted by a single line, either  yellow or black.

If instead the style contains a ``@mode 'Flat'`` directive at the top, it will generated two non mutually exclusive SLD rules:

* One applying a 10px wide black stroke on all features
* One applying a 1px wide yewllow stroke on all feature whose cat attribute is 'important'

Thus, all features will at least be painted 10px black, but the 'important' ones will also have a second 1px yellow line *on top of the first one*
