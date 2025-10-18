.. _style_guidelines:

Style Guidelines
================

This document provides a set of guidelines to use for all GeoServer project documentation. These guidelines will help to ensure that all project documentation remains clear, consistent, and easy to read.

Content conventions
-------------------

This section concerns the tone to use when writing text for pages. 

Be concise
``````````

Documentation should be concise and not just a brain dump. Reference material should contain short pages and be easy to refer to without having to scroll through a large volume of text.  Tutorials can be longer, depending on scope.  If the point of the document is to share your thoughts and insights, it belongs in a blog post.  This documentation is a manual, not a wiki.

Avoid marketing
```````````````

If the point of the document is to showcase a new feature it does not belong in the documentation. Write an article or a blog post about it. If it is necessary to point out a technical benefit of a feature then do so from a technical standpoint.

Bad
   Super-overlays are a great way to publish super cool datasets awesomely in Google Earth!
Good
   Super-overlays allow you to efficiently publish data via Google Earth.

Be professional
```````````````

Avoid the use of slang or other "colorful" language. The point of a technical document is to be informative, not to keep the reader amused.  Avoiding slang helps keep the document accessible to as large an audience as possible.


Bad
   Next, fire up whatever tool you use to browse the web and point it in the direction of ...
Good
   Next, start your web browser and navigate to ...

Use direct commands
```````````````````

When providing step-by-step instructions, use direct commands or requests. Avoid the use of "we" and "let's".

Bad
   Now let's add a shapefile by ...
Good
   Add a shapefile by ...


Naming conventions
------------------

.. note:: Many of the guidelines in this section are taken from the `Wikipedia naming conventions <http://en.wikipedia.org/wiki/Wikipedia:Naming_conventions>`_.

Capitalization of page names
````````````````````````````

Each word in the page name should be capitalized except for articles (such as "the", "a", "an") and conjunctions (such as "and", "but", "or"). A page name should never start with an article.

Bad
   Adding a shapefile or postgis table
Good
   Adding a Shapefile or PostGIS Table

Bad
   The Shapefile Tutorial
Good
   Shapefile Tutorial

Capitalization of section names
```````````````````````````````

Do not capitalize second and subsequent words unless the title is almost always capitalized in English (like proper names). Thus, capitalize John Wayne and Art Nouveau, but not Video Games.

Bad
   Creating a New Datastore
Good
   Creating a new datastore

Verb usage
``````````

It is recommended that the gerund (the -ing form in English) be used unless there is a more common noun form. For example, an article on swimming is better than one on swim.

Bad
   Create a new datastore
Good
   Creating a new datastore

Avoid plurals
`````````````

Create page titles that are in the singular.  Exceptions to this are nouns that are always plural (scissors, trousers), a small class that requires a plural (polar coordinates, Bantu languages, The Beatles).

Bad
   Templates tutorial
Good
   Template tutorial

Formatting
----------

Code and command line
`````````````````````

Any code or command line snippets should be formatted as code::

   This is a code block.

When lines are longer than 77 characters, extend multiple lines in a format appropriate for the language in use.  If possible, snippets should be functional when pasted directly into the appropriate target.  

For example, Java and XML make no distinction between a single space and multiple spaces, so the following snippets are fine::

   org.geoserver.package.Object someVeryLongIdentifier =
      org.geoserver.package.Object.factoryMethod();

::

   <namespace:tagname attributename="attributevalue" attribute2="attributevalue"
      nextattribute="this is on another line"/>

For shell scripts, new lines can be escaped with a backslash character (\\). It is also recommended to use a simple ``$`` prompt to save space. For example::

   $ /org/jdk1.5.0*/bin/java \
      -cp /home/user/.m2/repository/org/geoserver/*/*.jar \
      org.geoserver.GeoServer -DGEOSERVER_DATA_DIR=/var/lib/geoserver_data/release
