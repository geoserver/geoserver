Wicket Development In GeoServer
===============================

This page explains the steps to follow in creating a page for the Wicket-based configuration interface in GeoServer 2.  For more information on Wicket, check out the project site at http://wicket.apache.org/.

Adding a Page
-------------

In Wicket, each page has one corresponding Java class.  To add a page, you need to create a new class that extends ``org.geoserver.web.GeoServerBasePage``.  You will also want to create a link somewhere that brings the user to your page.  (In general, Wicket pages do not have reliable URLs, so you must explicitly create a link in an existing page and let Wicket generate the proper URL.)  In the case where your class does not require arguments to its constructor, you can insert a link using Spring.  Creating links in Spring requires that your page link text be internationalizable.  We'll discuss internationalization in more depth later.  The simplest possible Wicket extension for GeoServer involves 3 files.  There is a Java class definition (this would be in :file:`src/main/java/org/geoserver/web/example/MyPage.java`).

.. code-block:: java

    package org.geoserver.web.example;

    import org.geoserver.web.GeoServerBasePage;

    public class MyPage extends GeoServerBasePage {
        // We will fill in the rest later, for now the page can just be blank
    }

There would also need to be a Spring application context document (:file:`src/main/java/applicationContext.xml`):

.. code-block:: xml

     <bean class="org.geoserver.web.MenuPageInfo" id="myPageLink">
         <!-- An internal identifier for the link component -->
         <property name="id" value="mypage"/> 
         <!-- The i18n key for the link title -->
         <property name="titleKey" value="org.geoserver.web.example.MyPage.page.title"/>
         <!-- The i18n key for a longer description of the page -->
         <property name="descriptionKey" value="org.geoserver.web.example.MyPage.page.description"/>
         <!-- The fully qualified name of the page class -->
         <property name="componentClass" value="org.geoserver.web.example.MyPage"/>
         <!-- Optional, an icon to display alongside the link. -->
         <property name="icon" value="imgs/some-image.png"/>
         <!-- Optional, the category in which the link should be grouped. -->
         <property name="category" ref="someCategory"/>
         <!-- Optional, a key used to order the links in the menu. -->
         <property name="order" value="100"/>
     </bean>

The third necessary file is the default dictionary for internationalized strings, at :file:`src/main/resources/GeoServerApplication.properties`: 

.. code-block:: ini

    org.geoserver.web.example.MyPage.page.title=My Example Page
    org.geoserver.web.example.MyPage.page.description=An example page for developers trying to extend the GeoServer UI.

If you create a jar with these three files and add it to the GeoServer classpath, you should see the new link in the left-hand menu.

Adding to a Page
----------------

At this point you've added a page to the UI, but it's not very interesting. In Wicket, pages provide their content with an HTML file stored with the same name as the Java code except for the extension. There are a few details about these files that differ from standard HTML; for one thing, they must be valid XML for Wicket's parser to work. In addition, Wicket uses a few "special" elements to specify where the Java code should hook into the HTML. The following are used quite extensively in the GeoServer administrative console.

.. list-table::
   :widths: 35 65

   * - **Wicket Element**  
     - **Purpose**
   * - ``<foo wicket:id="bar"></foo>``
     - A wicket:id attribute tells Wicket the name to be used for an element when attaching Wicket Components
   * - ``<wicket:child/>``
     - Requires no contents, but specifies that classes extending this page can insert content at this position.
   * - ``<wicket:extend></wicket:extend>``
     - Specifies that the enclosed content will be inserted into a parent page at the point indicated by <wicket:child/>
   * - ``<wicket:panel></wicket:panel>``
     - Similar to ``wicket:extend``, but used in creating custom components rather than extending pages.
   * - ``<wicket:head></wicket:head>``
     - Indicates a section (like a CSS or JavaScript include) that should be added to the header of pages that include this markup (can be used for pages or panels).
   * - ``<wicket:link></wicket:link>``
     - Encloses sections in which Wicket will rewrite links to pages, CSS files, and other resources that it manages.  (This lets you refer to resources using paths relative to the Java source and not the rendered HTML.)
   * - ``<wicket:message key="i18nKey"> Default Text </wicket:message>``
     - Tells Wicket to look up a string in the internationalization database and replace the provided text if one is found.


Wicket provides quite a few components, of which several can be seen in the `Wicket Component Reference <http://wicketstuff.org/wicket13/compref/>`_\ .  In general, Wicket components require a Model object which handles the getting, setting, and conversion to/from String of the value associated with a component.  For the purposes of this example, we will focus on one of the simplest, the Label, which simply replaces the contents of the element it is bound to with a value provided at runtime.  Continuing the example from above, we can pass a String to the Label's constructor and it is transparently converted to a Model: 

.. code-block:: java
    
    package org.geoserver.web.example;

    import org.geoserver.web.GeoServerBasePage;
    import org.apache.wicket.markup.html.basic.Label;

    public class MyPage extends GeoServerBasePage{
        public MyPage(){
            add(new Label("label", "Hello World"));
        }
    }

The corresponding HTML source would live at :file:`src/main/java/org/geoserver/web/example/MyPage.html`: 

.. code-block:: html

    <html>
    <head></head>
    <body>
        <wicket:extend>
        Greetings, GeoServer User! My message for you is <span wicket:id="label"> thanks for using GeoServer </span>.
        </wicket:extend>
    </body>
    </html>

Of course, there are much more complicated (and useful) things we can do with Wicket, but this example demonstrates the most common usage; just adding some behavior to an HTML element.

Adding a Link Outside the Navigation Menu
-----------------------------------------

Of course, we can't have everything in the sidebar menu; for one thing, it defines only a static set of links while GeoServer is bound to contain lots of resources that vary from configuration to configuration.  For another, some pages need to have arguments to their constructors.  If you want to add a custom link to some page, you can use a Wicket Link component and customize the ``onClick`` behavior to call the appropriate constructor.  (You can use ``setResponsePage`` in other methods that handle user input as well, such as on form submits.  Check the Wicket documentation for more information.)  An example: 

.. code-block:: java 

    //...
    import org.apache.wicket.markup.html.link.Link;
    //...

    add(new Link("link"){
        public void onClick(){
            setResponsePage(new MyPage());
        }
    });

The corresponding HTML would look like: 

.. code-block:: html

    Follow this lovely <a href="#" wicket:id="link">link</a>.

Making it Internationalizable
-----------------------------

In the GeoServer UI, we use a customized resource lookup utility within Wicket to allow any module to provide resource strings.  All you need to do is include your :abbr:`i18n (Internationalization)` values in a Java Properties file named ``GeoServerApplication.properties`` in the ``resources`` directory of your ``src`` directory (ie, :file:`{project}/src/main/resources/GeoServerApplication.properties`).

The ``<wicket:message>`` element makes it quite easy to make text internationalizable, but in the event that you need to insert a value into a sentence at a position that changes dependent on the language, you'll need to use something more complicated.

In Wicket, :abbr:`i18n (Internationalization)` value strings can define parameters which provide the ability to place dynamic values into internationalized strings.

.. seealso:: http://wicket.apache.org/docs/wicket-1.3.2/wicket/apidocs/org/apache/wicket/model/StringResourceModel.html for details.

Adding Resources
----------------
Often in HTML, you will need to include assets such as CSS files, JavaScript libraries, or images to include in your page.  Wicket allows you to specify URLs to these relative to your Java source file, using relative paths enclosed in ``<wicket:link>`` tags.  Wicket will rewrite these links at runtime to use the correct path.  However, such resources are not inherited from parent classes, so if you need to include a resource in multiple packages you will need to extract the functionality that uses it to a new class that can be shared between the two.  See the ``XMLEditor`` component in the core module of GeoServer's UI for an example of a component that does this.
                                                                          
UI Design Guidelines
--------------------
A brief listing of UI design guidelines for Wicket pages in GeoServer follows.

    Forms
        In forms, group each field as a ``<div>`` with a label and a form field, try to avoid using lists for the layout, as they are only intended for listing items. For radio buttons and checkboxes, the label should come after the field; for all others the label should precede the field.  For example:
        
        .. code-block:: html

            <div>
              <label for="foo"><wicket:message key="foo"> Foo </wicket:message></label>
              <input wicket:id="foo" type="text"></input>
            </div>

        Similar fields can be grouped within a ``<fieldset>``, the title of this group can be added with a ``<legend>``

        .. code-block:: html

            <fieldset>
               <legend>
                 <span>
                   <wicket:message key="foo"> Foo </wicket:message>
                 </span>
               </legend>
               ...
            </fieldset>

    Spacing
        Spacing elements in GeoServer is done with the Bootstrap Utilities (https://getbootstrap.com/docs/5.2/utilities/spacing/). For both ``padding`` and ``margin`` a special notation is used as described in the Bootstrap documentation.

        For example extra padding at the top of a ``form`` can be achieved as follows:

        .. code-block:: html

            <form wicket:id="form" class="pt-3">
                ...
            </form>

    Sizing
        As with Spacing, for Sizing Bootstrap Utilities are used (https://getbootstrap.com/docs/5.2/utilities/sizing/). Besides the available Bootstrap classes, GeoServer styling needs more classes, however in most cases these extra classes are not needed.

        The extra GeoServer sizing classes are:

        .. code-block:: css

            /* widths */
            .w-10-em {
              width: 10em !important;
            }
            .w-15-em {
              width: 15em !important;
            }
            .w-25-em {
              width: 25em !important;
            }
            .w-30-em {
              width: 30em !important;
            }
            .w-50-em {
              width: 50em !important;
            }
            .w-100-em {
              width: 100em !important;
            }
            .w-20-ex {
              width: 20ex !important;
            }
            .w-60-ex {
              width: 60ex !important;
            }
            /* add to Bootstrap utilities */
            .w-10 {
              width: 10% !important;
            }
            .w-95 {
              width: 95% !important;
            }
            /* heights */
            .h-5-em {
              height: 5em !important;
            }
            .h-10-em {
              height: 10em !important;
            }
            .h-20-em {
              height: 20em !important;
            }
            .h-25-em {
              height: 25em !important;
            }
            .h-50-em {
              height: 50em !important;
            }
            .h-100-em {
              height: 100em !important;
            }

        The suffix in the class name corresponds with the sizing unit used.

        Bootstrap uses mostly ``em`` and ``rem`` as sizing units, and the ``3`` suffix (like ``.px-3``) is roughly the same as 15px. When extra spacing is needed it is advised to use this.

        Some other sizing used to set the width of an element and its pixel equivalent:

        .. list-table::

           * - **Class**
             - **Size in em**
             - **Size in pixels** (approximately)
           * - ``*-5-em``
             - 5em
             - 100px
           * - ``*-20-em``
             - 20em
             - 325px
           * - ``*-25-em``
             - 25em
             - 400px
           * - ``*-30-em``
             - 30em
             - 600px


    Avoid requiring special knowledge from the user.
        For example, where a list of values is required, provide a widget that allows manipulating the list one element at a time rather than expecting a comma-separated list of values.

    Custom Components
        We recommend creating a reusable Wicket component for any complex values that might need to be edited by users, such as a bounding box or a list of free strings.  By extracting this into a component, it is much simpler to provide consistent, rich editing for users.

