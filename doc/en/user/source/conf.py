# -*- coding: utf-8 -*-
#
# GeoServer documentation build configuration file, created by
# sphinx-quickstart on Tue Oct 28 10:01:09 2008.
#
# This file is execfile()d with the current directory set to its containing dir.
#
# The contents of this file are pickled, so don't put values in the namespace
# that aren't pickleable (module imports are okay, they're removed automatically).
#
# All configuration values have a default value; values that are commented out
# serve to show the default value.

import sys, os, string
import xml.etree.ElementTree as ET
import re
import datetime

# If your extensions are in another directory, add it here. If the directory
# is relative to the documentation root, use os.path.abspath to make it
# absolute, like shown here.
#sys.path.append(os.path.abspath('some/directory'))

now = datetime.datetime.now()

# General configuration
# ---------------------

# Add any Sphinx extension module names here, as strings. They can be extensions
# coming with Sphinx (named 'sphinx.ext.*') or your custom ones.
extensions = ['sphinx.ext.todo', 'sphinx.ext.extlinks']

#todo_include_todos = True

# Add any paths that contain templates here, relative to this directory.
#templates_path = ['../../theme/_templates']

# The suffix of source filenames.
source_suffix = {'.rst': 'restructuredtext'}

# The master toctree document.
master_doc = 'index'

# General substitutions.
project = u'GeoServer'
manual = u'User Manual'
copyright = u'{}, Open Source Geospatial Foundation'.format(now.year)

# The default replacements for |version| and |release|, also used in various
# other places throughout the built documents.

# The full version, including alpha/beta/rc tags.
# This is looked up from the pom.xml
pompath = os.path.join(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(os.path.dirname(__file__))))), "src","pom.xml")
print("Reading version from ",pompath)
pomtree = ET.parse(pompath)

version = pomtree.getroot().find("{http://maven.apache.org/POM/4.0.0}version").text
snapshot = version.find('SNAPSHOT') != -1
if snapshot:
    print("Building snapshot docs version", version)
else:
    print("Building release docs version", version)
    


# The relacement |release| most recent release version, including alpha/beta/rc tags.
# This should be updated after each release
release = '2.28.0'
if not snapshot:
  release = version

print("Examples use release", release)

# sphinx-build -D release=${project.version} overrides release configuration
# but only after conf.py has been used...

# check environmental variable to see if ant build.xml passed in project.version
# project_version = os.getenv("project.version")
# if project_version == None: 
#  release = '3.0-SNAPSHOT'
# else:
#  release = project_version

# Used in build and documentation links
# branch = version+'.x'
branch = 'main'
series = '3.x'

# Users don't need to see the "SNAPSHOT" notation when it's there
community = '3.0-SNAPSHOT'

download_release =   'https://sourceforge.net/projects/geoserver/files/GeoServer/'+release+'/geoserver-'+release+'-%s.zip'
download_extension = 'https://sourceforge.net/projects/geoserver/files/GeoServer/'+release+'/extensions/geoserver-'+release+'-%s-plugin.zip'
download_pending = 'https://build.geoserver.org/geoserver/files/files/GeoServer/'+release+'/community/geoserver-'+release+'-%s-plugin.zip'

nightly_release =   'https://build.geoserver.org/geoserver/'+branch+'/geoserver-'+community+'-%s.zip'
nightly_extension = 'https://build.geoserver.org/geoserver/'+branch+'/extensions/geoserver-'+community+'-%s-plugin.zip'
nightly_community = 'https://build.geoserver.org/geoserver/'+branch+'/community-latest/geoserver-'+community+'-%s-plugin.zip'

print("  download_release:", download_release )
print("download_extension:", download_extension )
print("download_pending:", download_pending )
print("  nightly_release:", nightly_release )
print("nightly_extension:", nightly_extension )
print("nightly_community:", nightly_community )

# There are two options for replacing |today|: either, you set today to some
# non-false value, then it is used:
#today = ''
# Else, today_fmt is used as the format for a strftime call.
today_fmt = '%B %d, %Y'

# List of documents that shouldn't be included in the build.
#unused_docs = []

# A list of glob-style patterns [1] that should be excluded when looking for source files.
# They are matched against the source file names relative to the source directory,
# using slashes as directory separators on all platforms.
exclude_patterns = [
   '**/symbolizers/include/*.rst'
]

# List of directories, relative to source directories, that shouldn't be searched
# for source files.
exclude_trees = []

# The reST default role (used for this markup: `text`) to use for all documents.
#default_role = None

# If true, '()' will be appended to :func: etc. cross-reference text.
#add_function_parentheses = True

# If true, the current module name will be prepended to all description
# unit titles (such as .. function::).
#add_module_names = True

# If true, sectionauthor and moduleauthor directives will be shown in the
# output. They are ignored by default.
#show_authors = False

# The name of the Pygments (syntax highlighting) style to use.
pygments_style = 'sphinx'

# Options for extlinks
#
# :website:`license <License>`
# :geos:`1234`
# :wiki:`Proposals`
# -----------------------------------

extlinks = { 
    'wiki': ('https://github.com/geoserver/geoserver/wiki/%s', '%s'),
    'github': ('https://github.com/geoserver/geoserver/%s', 'github.com/geoserver/geoserver/%s'),
    'website': ('http://geoserver.org/%s', 'geoserver.org/%s'),
    'user': ('http://docs.geoserver.org/'+branch+'/en/user/%s', '%s'),
    'developer': ('http://docs.geoserver.org/latest/en/developer/%s', '%s'),
    'docguide': ('http://docs.geoserver.org/latest/en/docguide/%s', '%s'),
    'geos': ('https://osgeo-org.atlassian.net/browse/GEOS-%s','GEOS-%s'),
    'geot': ('https://osgeo-org.atlassian.net/browse/GEOT-%s','GEOT-%s'),
    'api': ('http://docs.geoserver.org/latest/en/api/#1.0.0/%s', 'REST API %s'),
    'geotools': ('https://docs.geotools.org/latest/userguide/%s', 'GeoTools %s'),
    'download_release': (download_release,'geoserver-'+release+'-%s.zip'),
    'download_extension': (download_extension,'geoserver-'+release+'-%s-plugin.zip'),
    'download_pending': (download_pending,'geoserver-'+release+'-%s-plugin.zip'),
    'nightly_release': (nightly_release,'geoserver-'+community+'-%s.zip'),
    'nightly_extension': (nightly_extension,'geoserver-'+community+'-%s-plugin.zip'),
    'nightly_community': (nightly_community,'geoserver-'+community+'-%s-plugin.zip')
}

# Common substitutions

rst_epilog = "\n" \
 ".. |branch| replace:: "+branch+"\n" \
 ".. |series| replace:: "+series

print(rst_epilog)
print(version)
print(release)

# Options for HTML output
# -----------------------
html_theme = 'geoserver'
html_theme_path = ['../../themes']

if os.environ.get('HTML_THEME_PATH'):
  html_theme_path.append(os.environ.get('HTML_THEME_PATH'))

# The style sheet to use for HTML and HTML Help pages. A file of that name
# must exist either in Sphinx' static/ path, or in one of the custom paths
# given in html_static_path.
#html_style = 'default.css'

# The name for this set of Sphinx documents.  If None, it defaults to
# "<project> v<release> documentation".
html_title = project + " " + release + " " + manual

# A shorter title for the navigation bar.  Default is the same as html_title.
#html_short_title = None

# The name of an image file (relative to this directory) to place at the top
# of the sidebar.
#html_logo = None

# The name of an image file (within the static path) to use as favicon of the
# docs.  This file should be a Windows icon file (.ico) being 16x16 or 32x32
# pixels large.
html_favicon = '../../themes/geoserver/static/geoserver.ico'

# Add any paths that contain custom static files (such as style sheets) here,
# relative to this directory. They are copied after the builtin static files,
# so a file named "default.css" will overwrite the builtin "default.css".
#html_static_path = ['../../theme/_static']

# If not '', a 'Last updated on:' timestamp is inserted at every page bottom,
# using the given strftime format.
html_last_updated_fmt = '%b %d, %Y'

# If true, SmartyPants will be used to convert quotes and dashes to
# typographically correct entities.
#html_use_smartypants = True

# Custom sidebar templates, maps document names to template names.
#html_sidebars = {}

# Additional templates that should be rendered to pages, maps page names to
# template names.
#html_additional_pages = {}

# If false, no module index is generated.
html_domain_indices = False

# If false, no index is generated.
html_use_index = False

# If true, the index is split into individual pages for each letter.
#html_split_index = False

# If true, the sphinx sources are included in the HTML build as _sources/<name>.
html_copy_source = False

# If true, links to the page source are added to each page.
html_show_sourcelink = False

# If true, an OpenSearch description file will be output, and all pages will
# contain a <link> tag referring to it.  The value of this option must be the
# base URL from which the finished HTML is served.
#html_use_opensearch = ''

# If nonempty, this is the file name suffix for HTML files (e.g. ".xhtml").
#html_file_suffix = ''

# Output file base name for HTML help builder.
htmlhelp_basename = 'GeoServerUserManual'

html_context = {
  'display_github': True,
  'github_user': 'geoserver',
  'github_repo': 'geoserver',
  'github_version': 'main',
  'conf_py_path': 'doc/en/user/source',
  'manual': manual,
}



# Options for LaTeX output
# ------------------------

# The paper size ('letter' or 'a4').
#latex_paper_size = 'letter'

# The font size ('10pt', '11pt' or '12pt').
#latex_font_size = '10pt'

# Grouping the document tree into LaTeX files. List of tuples
# (source start file, target name, title, author, document class [howto/manual]).
latex_documents = [
  ('index', 'GeoServerUserManual.tex', u'GeoServer User Manual',
   u'GeoServer', 'manual'),
]

# The name of an image file (relative to this directory) to place at the top of
# the title page.
latex_logo = '../../themes/geoserver/static/GeoServer_500.png'

# For "manual" documents, if this is true, then toplevel headings are parts,
# not chapters.
#latex_use_parts = False

# Additional stuff for the LaTeX preamble.
latex_elements = {
  'fontpkg': '\\usepackage{palatino}',
  'fncychap': '\\usepackage[Sonny]{fncychap}',
'preamble': #"""\\usepackage[parfill]{parskip}
  """
    \\hypersetup{
    colorlinks = true,
    linkcolor = [rgb]{0,0.46,0.63},
    anchorcolor = [rgb]{0,0.46,0.63},
    citecolor = blue,
    filecolor = [rgb]{0,0.46,0.63},
    pagecolor = [rgb]{0,0.46,0.63},
    urlcolor = [rgb]{0,0.46,0.63}
    }

"""
}

# Documents to append as an appendix to all manuals.
#latex_appendices = []

# If false, no module index is generated.
#latex_use_modindex = True
