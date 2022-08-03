# GeoServer Notice

GeoServer is distributed under the GNU General Public License Version 2.0 license:

    GeoServer, open geospatial information server
    Copyright (C) 2014-2020 Open Source Geospatial Foundation.
    Copyright (C) 2001-2014 OpenPlans
    
    This program is free software; you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation; either version 2 of the License, or
    (at your option) any later version (collectively, "GPL").
    
    As an exception to the terms of the GPL, you may copy, modify,
    propagate, and distribute a work formed by combining GeoServer with the
    EMF, XSD and OSHI Libraries, or a work derivative of such a combination, even if
    such copying, modification, propagation, or distribution would otherwise
    violate the terms of the GPL. Nothing in this exception exempts you from
    complying with the GPL in all respects for all of the code used other
    than the EMF, XSD and OSHI Libraries. You may include this exception and its grant
    of permissions when you distribute GeoServer.  Inclusion of this notice
    with such a distribution constitutes a grant of such permissions.  If
    you do not wish to grant these permissions, remove this paragraph from
    your distribution. "GeoServer" means the GeoServer software licensed
    under version 2 or any later version of the GPL, or a work based on such
    software and licensed under the GPL. "EMF, XSD and OSHI Libraries" means 
    Eclipse Modeling Framework Project and XML Schema Definition software
    distributed by the Eclipse Foundation and the OSHI library, all licensed 
    under the Eclipse Public License Version 1.0 ("EPL"), or a work based on 
    such software and licensed under the EPL.
    
    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with this program; if not, write to the Free Software
    Foundation, Inc., 51 Franklin Street, Suite 500, Boston, MA 02110-1335  USA

For latest contact information of Open Source Geospatial Foundation see the website at
http://www.osgeo.org.  Current email is info@osgeo.org and address is OSGeo, 14525 SW Millikan #42523, Beaverton, Oregon, United States, 97005-2343.

The full GPL license is available in this directory in the file [GPL.md](GPL.md)

## Additional Libraries and Code used

GeoServer uses several additional libraries and pieces of code.  We are 
including the appropriate notices in this file.  We'd like to thank all
the creators of the libraries we rely on, GeoServer would certainly not
be possible without them.  There are also several LGPL libraries that do
not require us to cite them, but we'd like to thank GeoTools - 
http://geotools.org, JTS - http://www.vividsolutions.com/jts/jtshome.htm
 WKB4J http://wkb4j.sourceforge.net, OpenPDF - https://github.com/LibrePDF/OpenPDF,
and J. David Eisenberg's PNG encoder http://www.catcode.com/pngencoder/

### NeuQuant Neural-Net Quantization Algorithm

GeoServer also thanks Anthony Dekker for the NeuQuant Neural-Net Quantization
Algorithm.  The copyright notice is intact in the source code and also here:

-----

    /* NeuQuant Neural-Net Quantization Algorithm
     * ------------------------------------------
     *
     * Copyright (c) 1994 Anthony Dekker
     *
     * NEUQUANT Neural-Net quantization algorithm by Anthony Dekker, 1994.
     * See "Kohonen neural networks for optimal colour quantization"
     * in "Network: Computation in Neural Systems" Vol. 5 (1994) pp 351-367.
     * for a discussion of the algorithm.
     *
     * Any party obtaining a copy of these files from the author, directly or
     * indirectly, is granted, free of charge, a full and unrestricted irrevocable,
     * world-wide, paid up, royalty-free, nonexclusive right and license to deal
     * in this software and documentation files (the "Software"), including without
     * limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
     * and/or sell copies of the Software, and to permit persons who receive
     * copies from any such party to do so, with the only requirement being
     * that this copyright notice remain intact.
     */

-----

### GifEncoder

The GeoServer Project also thanks J. M. G. Elliot for his improvements on 
Jef Poskanzer's GifEncoder.  Notice is included below on his Elliot's 
release to public domain and Poskanzer's original notice (which is new
BSD).  Source code is included in GeoServer source, with modifications done
by David Blasby for The Open Planning Project (now OpenPlans).  

------

> Since Gif89Encoder includes significant sections of code from Jef Poskanzer's
> GifEncoder.java, I'm including its notice in this distribution as requested (appended
> below).
> 
> As for my part of the code, I hereby release it, on a strictly "as is" basis,
> to the public domain.
> 
> J. M. G. Elliott
> 15-Jul-2000
> 
> ---- from Jef Poskanzer's GifEncoder.java ----
> 
>     // GifEncoder - write out an image as a GIF
>     //
>     // Transparency handling and variable bit size courtesy of Jack Palevich.
>     //
>     // Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
>     //
>     // Redistribution and use in source and binary forms, with or without
>     // modification, are permitted provided that the following conditions
>     // are met:
>     // 1. Redistributions of source code must retain the above copyright
>     //    notice, this list of conditions and the following disclaimer.
>     // 2. Redistributions in binary form must reproduce the above copyright
>     //    notice, this list of conditions and the following disclaimer in the
>     //    documentation and/or other materials provided with the distribution.
>     //
>     // THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
>     // ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
>     // IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
>     // ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
>     // FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
>     // DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
>     // OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
>     // HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
>     // LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
>     // OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
>     // SUCH DAMAGE.
>     //
>     // Visit the ACME Labs Java page for up-to-date versions of this and other
>     // fine Java utilities: http://www.acme.com/java/

------

### JAI ImageIO

JAI ImageIO jars from Sun are also included.  These are released under a 
BSD license (new).  Notice is below:

-------

    Initial sources

    Copyright (c) 2005 Sun Microsystems, Inc. All  Rights Reserved.

    Redistribution and use in source and binary forms, with or without
    modification, are permitted provided that the following conditions
    are met: 

    - Redistribution of source code must retain the above copyright 
      notice, this  list of conditions and the following disclaimer.

    - Redistribution in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in 
      the documentation and/or other materials provided with the
      distribution.

    Neither the name of Sun Microsystems, Inc. or the names of 
    contributors may be used to endorse or promote products derived 
    from this software without specific prior written permission.

    This software is provided "AS IS," without a warranty of any 
    kind. ALL EXPRESS OR IMPLIED CONDITIONS, REPRESENTATIONS AND 
    WARRANTIES, INCLUDING ANY IMPLIED WARRANTY OF MERCHANTABILITY, 
    FITNESS FOR A PARTICULAR PURPOSE OR NON-INFRINGEMENT, ARE HEREBY
    EXCLUDED. SUN MIDROSYSTEMS, INC. ("SUN") AND ITS LICENSORS SHALL 
    NOT BE LIABLE FOR ANY DAMAGES SUFFERED BY LICENSEE AS A RESULT OF 
    USING, MODIFYING OR DISTRIBUTING THIS SOFTWARE OR ITS
    DERIVATIVES. IN NO EVENT WILL SUN OR ITS LICENSORS BE LIABLE FOR 
    ANY LOST REVENUE, PROFIT OR DATA, OR FOR DIRECT, INDIRECT, SPECIAL,
    CONSEQUENTIAL, INCIDENTAL OR PUNITIVE DAMAGES, HOWEVER CAUSED AND
    REGARDLESS OF THE THEORY OF LIABILITY, ARISING OUT OF THE USE OF OR
    INABILITY TO USE THIS SOFTWARE, EVEN IF SUN HAS BEEN ADVISED OF THE
    POSSIBILITY OF SUCH DAMAGES. 

    You acknowledge that this software is not designed or intended for 
    use in the design, construction, operation or maintenance of any 
    nuclear facility. 

-------

### Jetty

GeoServer also includes binaries and from Jetty, the standard version can be 
found at http://www.eclipse.org/jetty/, released under an OSI-approved artistic
license.  We include the license completely, as some versions will be 
distributed without full source.

------

> Jetty License
> $Revision: 3.7 $
> Preamble:
> 
> The intent of this document is to state the conditions under which the Jetty Package may be copied, such that the Copyright Holder maintains some semblance of control over the development of the package, while giving the users of the package the right to use, distribute and make reasonable modifications to the Package in accordance with the goals and ideals of the Open Source concept as described at http://www.opensource.org.
> 
> It is the intent of this license to allow commercial usage of the Jetty package, so long as the source code is distributed or suitable visible credit given or other arrangements made with the copyright holders.
> 
> Definitions:
> 
> * "Jetty" refers to the collection of Java classes that are distributed as a HTTP server with servlet capabilities and associated utilities.
> 
> * "Package" refers to the collection of files distributed by the Copyright Holder, and derivatives of that collection of files created through textual modification.
> 
> * "Standard Version" refers to such a Package if it has not been modified, or has been modified in accordance with the wishes of the Copyright Holder.
> 
> * "Copyright Holder" is whoever is named in the copyright or copyrights for the package.
>   Mort Bay Consulting Pty. Ltd. (Australia) is the "Copyright Holder" for the Jetty package.
> 
> * "You" is you, if you're thinking about copying or distributing this Package.
> 
> * "Reasonable copying fee" is whatever you can justify on the basis of media cost, duplication charges, time of people involved, and so on. (You will not be required to justify it to the Copyright Holder, but only to the computing community at large as a market that must bear the fee.)
> 
> * "Freely Available" means that no fee is charged for the item itself, though there may be fees involved in handling the item. It also means that recipients of the item may redistribute it under the same conditions they received it.
> 
> 0. The Jetty Package is Copyright (c) Mort Bay Consulting Pty. Ltd. (Australia) and others. Individual files in this package may contain additional copyright notices. The javax.servlet packages are copyright Sun Microsystems Inc.
> 
> 1. The Standard Version of the Jetty package is available from http://jetty.mortbay.org.
> 
> 2. You may make and distribute verbatim copies of the source form of the Standard Version of this Package without restriction, provided that you include this license and all of the original copyright notices and associated disclaimers.
> 
> 3. You may make and distribute verbatim copies of the compiled form of the Standard Version of this Package without restriction, provided that you include this license.
> 
> 4. You may apply bug fixes, portability fixes and other modifications derived from the Public Domain or from the Copyright Holder. A Package modified in such a way shall still be considered the Standard Version.
> 
> 5. You may otherwise modify your copy of this Package in any way, provided that you insert a prominent notice in each changed file stating how and when you changed that file, and provided that you do at least ONE of the following:
> 
>     a) Place your modifications in the Public Domain or otherwise make them Freely Available, such as by posting said modifications to Usenet or an equivalent medium, or placing the modifications on a major archive site such as ftp.uu.net, or by allowing the Copyright Holder to include your modifications in the Standard Version of the Package.
> 
>     b) Use the modified Package only within your corporation or organization.
> 
>     c) Rename any non-standard classes so the names do not conflict with standard classes, which must also be provided, and provide a separate manual page for each non-standard class that clearly documents how it differs from the Standard Version.
> 
>     d) Make other arrangements with the Copyright Holder.
> 
> 6. You may distribute modifications or subsets of this Package in source code or compiled form, provided that you do at least ONE of the following:
> 
>     a) Distribute this license and all original copyright messages, together with instructions (in the about dialog, manual page or equivalent) on where to get the complete Standard Version.
> 
>     b) Accompany the distribution with the machine-readable source of the Package with your modifications. The modified package must include this license and all of the original copyright notices and associated disclaimers, together with instructions on where to get the complete Standard Version.
> 
>     c) Make other arrangements with the Copyright Holder.
> 
> 7. You may charge a reasonable copying fee for any distribution of this Package. You may charge any fee you choose for support of this Package. You may not charge a fee for this Package itself. However, you may distribute this Package in aggregate with other (possibly commercial) programs as part of a larger (possibly commercial) software distribution provided that you meet the other distribution requirements of this license.
> 
> 8. Input to or the output produced from the programs of this Package do not automatically fall under the copyright of this Package, but belong to whomever generated them, and may be sold commercially, and may be aggregated with this Package.
> 
> 9. Any program subroutines supplied by you and linked into this Package shall not be considered part of this Package.
> 
> 10. The name of the Copyright Holder may not be used to endorse or promote products derived from this software without specific prior written permission.
> 
> 11. This license may change with each release of a Standard Version of the Package. You may choose to use the license associated with version you are using or the license of the latest Standard Version.
> 
> 12. THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.
> 
> 13. If any superior law implies a warranty, the sole remedy under such shall be , at the Copyright Holders option either a) return of any price paid or b) use or reasonable endeavours to repair or replace the software.
> 
> 14. This license shall be read under the laws of Australia.
   
-------

### Prototype library (MIT License)

GeoServer includes a few snippets from the Prototype library (www.prototypejs.org), 
under a MIT license:

-------

    Copyright (c) 2005-2007 Sam Stephenson

    Permission is hereby granted, free of charge, to any person obtaining a copy
    of this software and associated documentation files (the "Software"), to deal
    in the Software without restriction, including without limitation the rights
    to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
    copies of the Software, and to permit persons to whom the Software is
    furnished to do so, subject to the following conditions:

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
    IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
    FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
    AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
    LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
    OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
    SOFTWARE.

-------

### Apache License

GeoServer uses a number of libraries licensed under the Apache License, 
Version 2.0.  These include Spring - http://www.springsource.org/,
a number of Apache commons libraries - http://jakarta.apache.org/commons/
whose jars we distribute and include in our source tree under lib/.  Also 
included as libraries are log4 http://logging.apache.org/log4j/docs/index.htmlj, 
batik http://xmlgraphics.apache.org/batik/, and xerces http://xerces.apache.org/xerces-j/.

Note there is some disagreement as to whether GPL and Apache 2.0 are compatible see 
http://www.apache.org/licenses/GPL-compatibility.html for more information.  We
hope that something will work out, as GeoServer would not be possible without
apache libraries.

Notice for apache license is included below:

-------

   Apache License
   Version 2.0, January 2004
   http://www.apache.org/licenses/

   TERMS AND CONDITIONS FOR USE, REPRODUCTION, AND DISTRIBUTION

   1. Definitions.

   "License" shall mean the terms and conditions for use, reproduction, and distribution as defined by Sections 1 through 9 of this document.

   "Licensor" shall mean the copyright owner or entity authorized by the copyright owner that is granting the License.

   "Legal Entity" shall mean the union of the acting entity and all other entities that control, are controlled by, or are under common control with that entity. For the purposes of this definition, "control" means (i) the power, direct or indirect, to cause the direction or management of such entity, whether by contract or otherwise, or (ii) ownership of fifty percent (50%) or more of the outstanding shares, or (iii) beneficial ownership of such entity.

   "You" (or "Your") shall mean an individual or Legal Entity exercising permissions granted by this License.

   "Source" form shall mean the preferred form for making modifications, including but not limited to software source code, documentation source, and configuration files.

   "Object" form shall mean any form resulting from mechanical transformation or translation of a Source form, including but not limited to compiled object code, generated documentation, and conversions to other media types.

   "Work" shall mean the work of authorship, whether in Source or Object form, made available under the License, as indicated by a copyright notice that is included in or attached to the work (an example is provided in the Appendix below).

   "Derivative Works" shall mean any work, whether in Source or Object form, that is based on (or derived from) the Work and for which the editorial revisions, annotations, elaborations, or other modifications represent, as a whole, an original work of authorship. For the purposes of this License, Derivative Works shall not include works that remain separable from, or merely link (or bind by name) to the interfaces of, the Work and Derivative Works thereof.

   "Contribution" shall mean any work of authorship, including the original version of the Work and any modifications or additions to that Work or Derivative Works thereof, that is intentionally submitted to Licensor for inclusion in the Work by the copyright owner or by an individual or Legal Entity authorized to submit on behalf of the copyright owner. For the purposes of this definition, "submitted" means any form of electronic, verbal, or written communication sent to the Licensor or its representatives, including but not limited to communication on electronic mailing lists, source code control systems, and issue tracking systems that are managed by, or on behalf of, the Licensor for the purpose of discussing and improving the Work, but excluding communication that is conspicuously marked or otherwise designated in writing by the copyright owner as "Not a Contribution."

   "Contributor" shall mean Licensor and any individual or Legal Entity on behalf of whom a Contribution has been received by Licensor and subsequently incorporated within the Work.

   2. Grant of Copyright License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable copyright license to reproduce, prepare Derivative Works of, publicly display, publicly perform, sublicense, and distribute the Work and such Derivative Works in Source or Object form.

   3. Grant of Patent License. Subject to the terms and conditions of this License, each Contributor hereby grants to You a perpetual, worldwide, non-exclusive, no-charge, royalty-free, irrevocable (except as stated in this section) patent license to make, have made, use, offer to sell, sell, import, and otherwise transfer the Work, where such license applies only to those patent claims licensable by such Contributor that are necessarily infringed by their Contribution(s) alone or by combination of their Contribution(s) with the Work to which such Contribution(s) was submitted. If You institute patent litigation against any entity (including a cross-claim or counterclaim in a lawsuit) alleging that the Work or a Contribution incorporated within the Work constitutes direct or contributory patent infringement, then any patent licenses granted to You under this License for that Work shall terminate as of the date such litigation is filed.

   4. Redistribution. You may reproduce and distribute copies of the Work or Derivative Works thereof in any medium, with or without modifications, and in Source or Object form, provided that You meet the following conditions:

      1. You must give any other recipients of the Work or Derivative Works a copy of this License; and

      2. You must cause any modified files to carry prominent notices stating that You changed the files; and

      3. You must retain, in the Source form of any Derivative Works that You distribute, all copyright, patent, trademark, and attribution notices from the Source form of the Work, excluding those notices that do not pertain to any part of the Derivative Works; and

      4. If the Work includes a "NOTICE" text file as part of its distribution, then any Derivative Works that You distribute must include a readable copy of the attribution notices contained within such NOTICE file, excluding those notices that do not pertain to any part of the Derivative Works, in at least one of the following places: within a NOTICE text file distributed as part of the Derivative Works; within the Source form or documentation, if provided along with the Derivative Works; or, within a display generated by the Derivative Works, if and wherever such third-party notices normally appear. The contents of the NOTICE file are for informational purposes only and do not modify the License. You may add Your own attribution notices within Derivative Works that You distribute, alongside or as an addendum to the NOTICE text from the Work, provided that such additional attribution notices cannot be construed as modifying the License.

   You may add Your own copyright statement to Your modifications and may provide additional or different license terms and conditions for use, reproduction, or distribution of Your modifications, or for any such Derivative Works as a whole, provided Your use, reproduction, and distribution of the Work otherwise complies with the conditions stated in this License.

   5. Submission of Contributions. Unless You explicitly state otherwise, any Contribution intentionally submitted for inclusion in the Work by You to the Licensor shall be under the terms and conditions of this License, without any additional terms or conditions. Notwithstanding the above, nothing herein shall supersede or modify the terms of any separate license agreement you may have executed with Licensor regarding such Contributions.

   6. Trademarks. This License does not grant permission to use the trade names, trademarks, service marks, or product names of the Licensor, except as required for reasonable and customary use in describing the origin of the Work and reproducing the content of the NOTICE file.

   7. Disclaimer of Warranty. Unless required by applicable law or agreed to in writing, Licensor provides the Work (and each Contributor provides its Contributions) on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied, including, without limitation, any warranties or conditions of TITLE, NON-INFRINGEMENT, MERCHANTABILITY, or FITNESS FOR A PARTICULAR PURPOSE. You are solely responsible for determining the appropriateness of using or redistributing the Work and assume any risks associated with Your exercise of permissions under this License.

   8. Limitation of Liability. In no event and under no legal theory, whether in tort (including negligence), contract, or otherwise, unless required by applicable law (such as deliberate and grossly negligent acts) or agreed to in writing, shall any Contributor be liable to You for damages, including any direct, indirect, special, incidental, or consequential damages of any character arising as a result of this License or out of the use or inability to use the Work (including but not limited to damages for loss of goodwill, work stoppage, computer failure or malfunction, or any and all other commercial damages or losses), even if such Contributor has been advised of the possibility of such damages.

   9. Accepting Warranty or Additional Liability. While redistributing the Work or Derivative Works thereof, You may choose to offer, and charge a fee for, acceptance of support, warranty, indemnity, or other liability obligations and/or rights consistent with this License. However, in accepting such obligations, You may act only on Your own behalf and on Your sole responsibility, not on behalf of any other Contributor, and only if You agree to indemnify, defend, and hold each Contributor harmless for any liability incurred by, or claims asserted against, such Contributor by reason of your accepting any such warranty or additional liability.

   END OF TERMS AND CONDITIONS
   
-------

### Eclipse Public License

GeoServer is build using a number of eclipse libraries including emf and xsd made available under the Eclipse Public License.

The notice for EPL license is included below:

--------

    Copyright (c) 2002-2006 IBM Corporation and others.
    All rights reserved.   This program and the accompanying materials
    are made available under the terms of the Eclipse Public License v1.0
    which accompanies this distribution, and is available at
    http://www.eclipse.org/legal/epl-v10.html

-------

### Java Service Launcher (JSL)

GeoServer uses the Java Service Launcher (JSL) by Michael Roeschter as a wrapper to install GeoServer as a Windows service.
The following license applies to this software (taken from https://roeschter.de/#license):

-------

    JAVA SERVICE LAUNCHER (JSL) is PUBLIC DOMAIN.

    This means the software is FREE. Free means you may use or reuse 
    any part of the software. You may package it with commercial software 
    and use it in commercial and business environments. 
    You may NOT claim copyright for the JSL software and it's source code 
    or any parts of the software and source. 
    Any derived work may retain a copyright or be commercialized as long 
    as the JSL parts of it are not covered by this copyright.

    You may distribute derived work in executable form and not include 
    the JSL source code if JSL constitues only a minor part of the 
    intellectual work (in other words, you can take the executable and 
    embed it in a Java application you distribute commercially), but you 
    may not charge in particular for or discriminate against the use of 
    the parts derived from JSL.

    Disclaimer: 
    This software is supplied AS IS with no claim of fitness for purpose.
