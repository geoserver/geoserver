.. _install_sphinx:

Installing Sphinx
=================

In order to work with Sphinx (>= 1.7.0) and generate the HTML/PDF documentation you'll need the following:

* `Python <http://www.python.org/download/>`_
* `pip <https://pypi.org/project/pip/>`_ (Package Installer for Python, included with Python >= 2.7.9 and Python >= 3.4)
* `LaTeX <http://www.latex-project.org/>`_ installation with full extensions (in order to build PDF documentation). For more details, see :ref:`install_latex`.

Windows
-------

#. Download and install Python. Though there are various distributions and versions, the `official versions <https://www.python.org/downloads/>`_ have been tested and work as expected.

#. Put :command:`python` in your Path.  To do so, go to :menuselection:`Control Panel --> System --> Advanced --> Environment Variables`.  Look for ``PATH`` among the system variables, and add the installation locations to the end of the string.  For example, if :command:`python` is installed in :file:`C:\\Python` add the following to the end of the string::
   
   ...;C:\Python
   
#. Open a command line window and run::
   
      pip install sphinx

#. To test for a successful installation, in a command line window, navigate to your GeoServer source checkout, change to the :file:`doc\\en` directory, and run::
  
      ant user
  
   This should generate HTML pages in the :file:`doc\\en\\target\\user\\html` directory.

Ubuntu
------

.. note:: These instructions may work on other Linux distributions as well, but have not been tested.

#. Open a terminal and type the following command::
  
      sudo apt-get install python-dev build-essential
  
   Depending on your system this may trigger the installation of other packages.

#. Install Sphinx using :command:`pip`::
  
      pip install --user sphinx
  
#. To test for a successful installation, navigate to your GeoServer source checkout, go into the :file:`doc/en` directory and run::
  
      ant user
  
   This should generate HTML pages in the :file:`doc/en/target/user/html` directory.
   
Mac OS X
--------

Installing Sphinx on Mac OS X is nearly identical to installing Sphinx on a Linux system. 

Confirm availability of python::
   
   python --version

Use ``pip`` to install :command:`sphinx`::

   pip install sphinx

.. tip::

   Users of `homebrew <https://brew.sh>`__ package manager can install with::

       brew install python

   Then use ``pip`` to install :command:`sphinx`::

       pip3 install sphinx
   
   Home brew installs python into :file:`/usr/local` and does not require `sudo` privileges.


Confirm availability with::
   
   sphinx-build --version

::

   sphinx-build 1.8.4

To test for a successful installation, navigate to your GeoServer source checkout, go into the :file:`doc/en` directory and run::

   ant user
