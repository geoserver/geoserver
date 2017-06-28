.. _install_sphinx:

Installing Sphinx
=================

In order to work with Sphinx and generate the HTML/PDF documentation you'll need the following:

* `Python <http://www.python.org/download/>`_
* :command:`easy_install` (included with Python `setuptools <http://pypi.python.org/pypi/setuptools>`_)
* `LaTeX <http://www.latex-project.org/>`_ installation with full extensions (in order to build PDF documentation). For more details, see :ref:`install_latex`.

Windows
-------

GeoServer documentation projects all include a :file:`make.bat` which provides much of the same functionality as the :command:`make` command.  If you wish to install :command:`make` anyway, it is available as part of the `MSYS <http://www.mingw.org/wiki/msys>`_ package.

#. Download and install Python. Though there are various distributions and versions, the `official versions <https://www.python.org/downloads/>`_ have been tested and work as expected.

#. Download and install `setuptools for Python <http://pypi.python.org/pypi/setuptools#downloads>`_

#. Put :command:`python` and :command:`setuptools` (and :command:`make` if you installed it) in your Path.  To do so, go to :menuselection:`Control Panel --> System --> Advanced --> Environment Variables`.  Look for ``PATH`` among the system variables, and add the installation locations to the end of the string.  For example, if :command:`python` is installed in :file:`C:\\Python` and :command:`setuptools` is installed in :file:`C:\\Python\\scripts`, add the following to the end of the string::
   
   ...;C:\Python;C:\Python\scripts
   
#. Open a command line window and run::
   
      easy_install sphinx

#. To test for a successful installation, in a command line window, navigate to your GeoServer source checkout, change to the :file:`doc\\user` directory, and run::
  
      make html
  
   This should generate HTML pages in the :file:`doc\\user\\build\\html` directory.

Ubuntu
------

.. note:: These instructions may work on other Linux distributions as well, but have not been tested.

#. Open a terminal and type the following command::
  
      sudo apt-get install python-dev build-essential python-setuptools 
  
   Depending on your system this may trigger the installation of other packages.

#. Install Sphinx using :command:`easy_install`::
  
      sudo easy_install sphinx
  
#. To test for a successful installation, navigate to your GeoServer source checkout, go into the :file:`doc/user` directory and run::
  
      make html
  
   This should generate HTML pages in the :file:`doc/user/build/html` directory.
   
#. If you want to generate PDF files this command should get you the necessary tools::
  
      sudo apt-get install texlive texlive-latex-extra

Mac OS X
--------

Installing Sphinx on Mac OS X is nearly identical to installing Sphinx on a 
Linux system. 

Easy install
^^^^^^^^^^^^

If the XCode extensions are installed on the system 
:command:`easy_install` should already be available. 

To install Sphinx open a terminal window and execute the command::

  sudo easy_install sphinx

Depending on the configuration of the system, problems installing Sphinx with 
:command:`easy_install` have been known to occur. If this is the case Sphinx can
be installed manually.

Manual install
^^^^^^^^^^^^^^

When installing Sphinx manually the templating library it depends on, Jinja2, 
must also be installed manually. To install Jinja:

#. Go to http://pypi.python.org/pypi/Jinja2
#. Download the source tarball :file:`Jinja2-2.8.tar.gz`
#. Unpack the source tarball and install Jinja::

       tar xzvf Jinja2-2.8.tar.gz
       cd Jinja2-2.8
       python setup.py install

After Jinja is installed follow a similar process to install Sphinx:

#. Go to http://pypi.python.org/pypi/Sphinx
#. Download the source tarbell :file:`Sphinx-1.3.1.tar.gz`
#. Unpack the source tarball and install Sphinx::

       tar xzvf Sphinx-1.3.1.tar.gz
       cd Sphinx-1.3.1
       python setup.py install
