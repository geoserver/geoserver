.. _install_latex:

Installing LaTeX
================

In order to build the PDF Documentation, you will need to install `LaTeX <http://www.latex-project.org/>`_, and a number of LaTeX extensions. If you just want to build the HTML documentation, LaTeX is not necessary.

Windows
-------

1. Install `MiKTeX <https://miktex.org/howto/install-miktex>`_.

2. In the Settings section of the installer, set "Install missing packages on-the-fly" to "Always" or "Ask me first". This will let MikTex download the various extension packages required to build the GeoServer docs as they are needed.

Ubuntu
------ 

#. Install the following TeX Live packages::

      sudo apt-get install -y texlive-base texlive-latex-recommended \
        texlive-science texlive-latex-extra texlive-extra-utils


2. As an alternative to (1), you can install the standard `TeX Live <http://tug.org/texlive/acquire-netinstall.html>`_ distribution, then install `texliveonfly <http://www.ctan.org/tex-archive/support/texliveonfly>`_ to install any missing packages as they are needed.

CentOS
------

1. Install the following TeX Live packages::

    sudo yum install texlive-pdftex texlive-latex-bin texlive-texconfig* texlive-latex* texlive-metafont* texlive-cmap* texlive-ec texlive-fncychap* texlive-pdftex-def texlive-fancyhdr* texlive-titlesec* texlive-multirow texlive-framed* texlive-wrapfig* texlive-parskip* texlive-caption texlive-ifluatex* texlive-collection-fontsrecommended texlive-collection-latexrecommended

2. As an alternative to (1), you can install the standard `TeX Live <http://tug.org/texlive/acquire-netinstall.html>`_ distribution, then install `texliveonfly <http://www.ctan.org/tex-archive/support/texliveonfly>`_ to install any missing packages as they are needed.

3. The CentOS distribution of TeX Live is missing some required extensions, which you will need to `install manually <https://en.wikibooks.org/wiki/LaTeX/Installing_Extra_Packages#Installing_a_package>`_:

   * `tabulary <https://www.ctan.org/pkg/tabulary>`_
   * `upquote <https://www.ctan.org/pkg/upquote>`_
   * `capt-of <https://www.ctan.org/pkg/capt-of>`_
   * `needspace <https://www.ctan.org/pkg/needspace>`_
