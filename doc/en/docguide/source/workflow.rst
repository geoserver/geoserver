.. _workflow:

Workflow
========

GeoServer documentation aims to mirror the development process of the software itself.  The process for writing/editing documentation is as follows:

* **Step 1**: Check out source
* **Step 2**: Make changes
* **Step 3**: Build and test locally
* **Step 4**: Commit changes
   
Check out source
----------------

Repository
``````````

This documentation source code exists in the same repository as the GeoServer source code::

   https://svn.codehaus.org/geoserver/

Within this path are the various branches and tags associated with releases, and the documentation is always inside a :file:`doc` path.  Inside this path, the repository contains directories corresponding to different translations.  The languages are referred to by a two letter code, with ``en`` (English) being the default.

For example, the path to check out the English docs from 1.7.x is::

   https://svn.codehaus.org/geoserver/branches/1.7.x/doc/en/

Inside this directory, there are four directories::

   user/
   developer/
   docguide/
   theme/

.. list-table::
   :widths: 20 80

   * - **Directory**
     - **Description**
   * - :file:`user`
     - User Manual source files
   * - :file:`developer`
     - Developer Manual source files
   * - :file:`docguide`
     - Documentation Guide source files (this is what you are reading now)
   * - :file:`theme`
     - GeoServer Sphinx theme (common to all three projects)

Software
````````

You must use a version control software to retrieve files.  Most people use `Subversion <http://subversion.tigris.org/>`_ (aka :command:`svn`), a command line utility for managing version control systems.  There also exists a shell-integrated version of Subversion for Windows called `TortoiseSVN <http://tortoisesvn.tigris.org/>`_.

For example, to check out the entire English documentation source tree for 1.7.x, run the following command::

   svn checkout https://svn.codehaus.org/geoserver/branches/1.7.x/doc/en/

This will create a directory locally with the same name as the final directory of the checkout (in this case, ``en``).  To create with a different directory name, append the desired name to the end of the command::

   svn checkout https://svn.codehaus.org/geoserver/branches/1.7.x/doc/en/ gs-17x-docs

This will check out the source into a directory called :file:`gs-17x-docs`.

Make changes
------------

Documentation in Sphinx is written in `reStructuredText <http://docutils.sourceforge.net/rst.htm>`_, a lightweight markup syntax.  For suggestions on writing reStructuredText for use with Sphinx, please see the section on :ref:`sphinx`.  For suggestions about writing style, please see the :ref:`style_guidelines`.


Build and test locally
----------------------

You should install Sphinx on your local system to build the documentation locally and view any changes made.  Sphinx builds the reStructuredText files into HTML pages and PDF files.

HTML
````

#. On a terminal, navigate to your GeoServer source checkout and change to the :file:`doc/en/user` directory (or whichever project you wish to build).

#. Run the following command::

      make html

   The resulting HTML pages will be contained in :file:`doc/en/user/build/html`.

#. Watch the output of the above command for any errors and warnings.  These could be indicative of problems with your markup.  Please fix any errors and warnings before continuing.

PDF
```

#. On a terminal, navigate to your GeoServer source checkout and change to the :file:`doc/en/user` directory (or whichever project you wish to build).

#. Run the following command::

      make latex

   The resulting LaTeX pages will be contained in :file:`doc/en/user/build/latex`.

#. Change to the :file:`doc/en/user/build/latex` directory.

#. Run the following command::

      pdflatex [GeoServerProject].tex

   This will create a PDF file called :file:`{GeoServerProject}.pdf` in the same directory

   .. note:: The exact name of :file:`{GeoServerProject}` depends on which project is being built.  However, there will only be one file with the extension ``.tex`` in the :file:`doc/en/user/build/latex` directory, so there should hopefully be little confusion.

   .. warning:: This command requires `LaTeX <http://www.latex-project.org/>`_ to be installed, and :command:`pdflatex` to be added to your Path.

#. Watch the output of the above command for any errors and warnings.  These could be indicative of problems with your markup.  Please fix any errors and warnings before continuing.


Commit changes
--------------

.. warning:: If you have any errors or warnings in your project, please fix them before committing!

The final step is to commit the changes to the repository.  If you are using Subversion, the command to use is::

   svn commit [path/file(s)]
   
where :file:`{path/file(s)}` is the path and file(s) you wish to commit to the repository.

.. note:: You must have commit rights to do this.  Please see the section on how to :ref:`commit_rights` for details.

