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
    EMF and XSD Libraries, or a work derivative of such a combination, even if
    such copying, modification, propagation, or distribution would otherwise
    violate the terms of the GPL. Nothing in this exception exempts you from
    complying with the GPL in all respects for all of the code used other
    than the EMF and XSD Libraries. You may include this exception and its grant
    of permissions when you distribute GeoServer.  Inclusion of this notice
    with such a distribution constitutes a grant of such permissions.  If
    you do not wish to grant these permissions, remove this paragraph from
    your distribution. "GeoServer" means the GeoServer software licensed
    under version 2 or any later version of the GPL, or a work based on such
    software and licensed under the GPL. "EMF and XSD Libraries" means 
    Eclipse Modeling Framework Project and XML Schema Definition software
    distributed by the Eclipse Foundation, all licensed 
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

The full GPL license is available in this directory in the file GPL.txt

*************************************************************************
Additional Libraries and Code used
*************************************************************************
GeoServer uses several additional libraries and pieces of code.  We are 
including the appropriate notices in this file.  We'd like to thank all
the creators of the libraries we rely on, GeoServer would certainly not
be possible without them.  There are also several LGPL libraries that do
not require us to cite them, but we'd like to thank GeoTools - 
http://geotools.org, JTS - http://www.vividsolutions.com/jts/jtshome.htm
 WKB4J http://wkb4j.sourceforge.net iText - http://www.lowagie.com/iText/ 
and J. David Eisenberg's PNG encoder http://www.catcode.com/pngencoder/

GeoServer also thanks Anthony Dekker for the NeuQuant Neural-Net Quantization
Algorithm.  The copyright notice is intact in the source code and also here:

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


The GeoServer Project also thank J. M. G. Elliot for his improvements on 
Jef Poskanzer's GifEncoder.  Notice is included below on his Elliot's 
release to public domain and Poskanzer's original notice (which is new
BSD).  Source code is included in GeoServer source, with modifications done
by David Blasby for The Open Planning Project (now OpenPlans).  

------
Since Gif89Encoder includes significant sections of code from Jef Poskanzer's
GifEncoder.java, I'm including its notice in this distribution as requested (appended
below).

As for my part of the code, I hereby release it, on a strictly "as is" basis,
to the public domain.

J. M. G. Elliott
15-Jul-2000

--------------------- from Jef Poskanzer's GifEncoder.java ---------------------

// GifEncoder - write out an image as a GIF
//
// Transparency handling and variable bit size courtesy of Jack Palevich.
//
// Copyright (C) 1996 by Jef Poskanzer <jef@acme.com>.  All rights reserved.
//
// Redistribution and use in source and binary forms, with or without
// modification, are permitted provided that the following conditions
// are met:
// 1. Redistributions of source code must retain the above copyright
//    notice, this list of conditions and the following disclaimer.
// 2. Redistributions in binary form must reproduce the above copyright
//    notice, this list of conditions and the following disclaimer in the
//    documentation and/or other materials provided with the distribution.
//
// THIS SOFTWARE IS PROVIDED BY THE AUTHOR AND CONTRIBUTORS ``AS IS'' AND
// ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
// IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
// ARE DISCLAIMED.  IN NO EVENT SHALL THE AUTHOR OR CONTRIBUTORS BE LIABLE
// FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
// DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS
// OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
// HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT
// LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
// OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
// SUCH DAMAGE.
//
// Visit the ACME Labs Java page for up-to-date versions of this and other
// fine Java utilities: http://www.acme.com/java/
------


JAI Image-io jars from Sun are also included.  These are released under a 
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

GeoServer also includes binaries and from Jetty, the standard version can be 
found at http://www.eclipse.org/jetty/, released under an OSI-approved artistic
license.  We include the license completely, as some versions will be 
distributed without full source.

------
Jetty License
$Revision: 3.7 $
Preamble:

The intent of this document is to state the conditions under which the Jetty Package may be copied, such that the Copyright Holder maintains some semblance of control over the development of the package, while giving the users of the package the right to use, distribute and make reasonable modifications to the Package in accordance with the goals and ideals of the Open Source concept as described at http://www.opensource.org.

It is the intent of this license to allow commercial usage of the Jetty package, so long as the source code is distributed or suitable visible credit given or other arrangements made with the copyright holders.

Definitions:

    * "Jetty" refers to the collection of Java classes that are distributed as a HTTP server with servlet capabilities and associated utilities.

    * "Package" refers to the collection of files distributed by the Copyright Holder, and derivatives of that collection of files created through textual modification.

    * "Standard Version" refers to such a Package if it has not been modified, or has been modified in accordance with the wishes of the Copyright Holder.

    * "Copyright Holder" is whoever is named in the copyright or copyrights for the package.
      Mort Bay Consulting Pty. Ltd. (Australia) is the "Copyright Holder" for the Jetty package.

    * "You" is you, if you're thinking about copying or distributing this Package.

    * "Reasonable copying fee" is whatever you can justify on the basis of media cost, duplication charges, time of people involved, and so on. (You will not be required to justify it to the Copyright Holder, but only to the computing community at large as a market that must bear the fee.)

    * "Freely Available" means that no fee is charged for the item itself, though there may be fees involved in handling the item. It also means that recipients of the item may redistribute it under the same conditions they received it.

0. The Jetty Package is Copyright (c) Mort Bay Consulting Pty. Ltd. (Australia) and others. Individual files in this package may contain additional copyright notices. The javax.servlet packages are copyright Sun Microsystems Inc.

1. The Standard Version of the Jetty package is available from http://jetty.mortbay.org.

2. You may make and distribute verbatim copies of the source form of the Standard Version of this Package without restriction, provided that you include this license and all of the original copyright notices and associated disclaimers.

3. You may make and distribute verbatim copies of the compiled form of the Standard Version of this Package without restriction, provided that you include this license.

4. You may apply bug fixes, portability fixes and other modifications derived from the Public Domain or from the Copyright Holder. A Package modified in such a way shall still be considered the Standard Version.

5. You may otherwise modify your copy of this Package in any way, provided that you insert a prominent notice in each changed file stating how and when you changed that file, and provided that you do at least ONE of the following:

    a) Place your modifications in the Public Domain or otherwise make them Freely Available, such as by posting said modifications to Usenet or an equivalent medium, or placing the modifications on a major archive site such as ftp.uu.net, or by allowing the Copyright Holder to include your modifications in the Standard Version of the Package.

    b) Use the modified Package only within your corporation or organization.

    c) Rename any non-standard classes so the names do not conflict with standard classes, which must also be provided, and provide a separate manual page for each non-standard class that clearly documents how it differs from the Standard Version.

    d) Make other arrangements with the Copyright Holder.

6. You may distribute modifications or subsets of this Package in source code or compiled form, provided that you do at least ONE of the following:

    a) Distribute this license and all original copyright messages, together with instructions (in the about dialog, manual page or equivalent) on where to get the complete Standard Version.

    b) Accompany the distribution with the machine-readable source of the Package with your modifications. The modified package must include this license and all of the original copyright notices and associated disclaimers, together with instructions on where to get the complete Standard Version.

    c) Make other arrangements with the Copyright Holder.

7. You may charge a reasonable copying fee for any distribution of this Package. You may charge any fee you choose for support of this Package. You may not charge a fee for this Package itself. However, you may distribute this Package in aggregate with other (possibly commercial) programs as part of a larger (possibly commercial) software distribution provided that you meet the other distribution requirements of this license.

8. Input to or the output produced from the programs of this Package do not automatically fall under the copyright of this Package, but belong to whomever generated them, and may be sold commercially, and may be aggregated with this Package.

9. Any program subroutines supplied by you and linked into this Package shall not be considered part of this Package.

10. The name of the Copyright Holder may not be used to endorse or promote products derived from this software without specific prior written permission.

11. This license may change with each release of a Standard Version of the Package. You may choose to use the license associated with version you are using or the license of the latest Standard Version.

12. THIS PACKAGE IS PROVIDED "AS IS" AND WITHOUT ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, WITHOUT LIMITATION, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE.

13. If any superior law implies a warranty, the sole remedy under such shall be , at the Copyright Holders option either a) return of any price paid or b) use or reasonable endeavours to repair or replace the software.

14. This license shall be read under the laws of Australia. 
-------


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

GeoServer uses a number of libraries licensed under the Apache License, 
Version 2.0.  These include Spring - http://www.springsource.org/,
a number of Apache commons libraries - http://jakarta.apache.org/commons/
whose jars we distribute and include in our source tree under lib/.  Also 
included as libraries are log4 http://logging.apache.org/log4j/docs/index.htmlj, 
batik http://xmlgraphics.apache.org/batik/, and xerces http://xerces.apache.org/xerces-j/.
Note there is some disagreement as to 
whether GPL and Apache 2.0 are compatible see 
http://www.apache.org/licenses/GPL-compatibility.html for more information.  We
hope that something will work out, as GeoServer would not be possible without
apache libraries.  Notice for apache license is included below:
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

GeoServer is build using a number of eclipse libraries including emf and xsd made available under the Eclipse Public License.

The notice for EPL license is included below:

 Copyright (c) 2002-2006 IBM Corporation and others.
 All rights reserved.   This program and the accompanying materials
 are made available under the terms of the Eclipse Public License v1.0
 which accompanies this distribution, and is available at
 http://www.eclipse.org/legal/epl-v10.html

-------
GeoServer includes the Java Service Wrapper from Tanuki Software (http://wrapper.tanukisoftware.com/). Tanuki Software allows this sofwtare to be used with GPL v2-compatible projects according to the following license agreement (found at http://wrapper.tanukisoftware.com/doc/english/licenseCommunity.html):

----------------------------------------------------------------------
-----------------                                    -----------------
                         Tanuki Software, Ltd.
                 Community Software License Agreement
                             Version 1.3

IMPORTANT-READ CAREFULLY: This license agreement is a legal agreement
between you ("Licensee") and Tanuki Software, Ltd. ("TSI"), which
includes computer software, associated media, printed materials, and
may include online or electronic documentation ( Software ).  PLEASE
READ THIS AGREEMENT CAREFULLY BEFORE YOU INSTALL, COPY, DOWNLOAD OR
USE THE SOFTWARE ACCOMPANYING THIS PACKAGE.

Section 1 - Grant of License

Community editions of the Software are made available on the GNU
General Public License, Version 2 ("GPLv2") or Version 3 ("GPLv3"),
included in Sections 4 and 5 of this license document.  All sections
of the Community Software License Agreement must be complied with in
addition to those of either the GPLv2 or GPLv3.  This license allows
the Software Program to be used with Products that are released under
either GPLv2 or GPLv3.


Section 2 - Definitions

2.1. "Community Edition" shall mean versions of the Software Program
distributed in source form under this license agreement, and all new
releases, corrections, enhancements and updates to the Software
Program, which TSI makes generally available under this agreement.

2.2. "Documentation" shall mean the contents of the website
describing the functionality and use of the Software Program, located
at http://wrapper.tanukisoftware.org

2.3. "Product" shall mean the computer programs, that are provided by
Licensee to Licensee customers or potential customers, and that
contain both the Software Program as a component of the Product, and a
component or components (other than the Software Program) that provide
the material functionality of the Product.  If the Product is released
in source form, the Software Program or any of its components may only
be included in executable form.

2.4. "Software Program" shall mean the computer software and license
file provided by TSI under this Agreement, including all new releases,
corrections, enhancements and updates to such computer software, which
TSI makes generally available and which Licensee receive pursuant to
Licensee subscription to TSIMS. Some specific features or platforms
may not be enabled if they do not fall under the feature set(s)
covered by the specific license fees paid.

2.5 "End User" shall mean the customers of the Licensee or any
recipient of the Product whether or not any payment is made to use
the Product.


Section 3 - Licensee Obligations

A copy of this license must be distributed in full with the Product
in a location that is obvious to any End User.

In accordance with Section 4, the full source code of all components
of the Product must be made available to any and all End Users.

Licensee may extend and/or modify the Software Program and distribute
under the terms of this agreement provided that the copyright notice
and license information displayed in the console and log files are
not obfuscated or obstructed in any way.


Section 4 - GPLv2 License Agreement

                        GNU GENERAL PUBLIC LICENSE
                           Version 2, June 1991

         Copyright (C) 1989, 1991 Free Software Foundation, Inc.
       51 Franklin Street, Fifth Floor, Boston, MA 02110-1301, USA

    Everyone is permitted to copy and distribute verbatim copies of
    this license document, but changing it is not allowed.

    Preamble

    The licenses for most software are designed to take away your
    freedom to share and change it. By contrast, the GNU General
    Public License is intended to guarantee your freedom to share and
    change free software--to make sure the software is free for all
    its users. This General Public License applies to most of the Free
    Software Foundation's software and to any other program whose
    authors commit to using it.  (Some other Free Software Foundation
    software is covered by the GNU Library General Public License
    instead.) You can apply it to your programs, too.

    When we speak of free software, we are referring to freedom, not
    price. Our General Public Licenses are designed to make sure that
    you have the freedom to distribute copies of free software (and
    charge for this service if you wish), that you receive source code
    or can get it if you want it, that you can change the software or
    use pieces of it in new free programs; and that you know you can
    do these things.

    To protect your rights, we need to make restrictions that forbid
    anyone to deny you these rights or to ask you to surrender the
    rights. These restrictions translate to certain responsibilities
    for you if you distribute copies of the software, or if you modify
    it.

    For example, if you distribute copies of such a program, whether
    gratis or for a fee, you must give the recipients all the rights
    that you have. You must make sure that they, too, receive or can
    get the source code. And you must show them these terms so they
    know their rights.

    We protect your rights with two steps:

    (1) copyright the software, and
    (2) offer you this license which gives you legal permission to
    copy, distribute and/or modify the software.

    Also, for each author's protection and ours, we want to make
    certain that everyone understands that there is no warranty for
    this free software. If the software is modified by someone else
    and passed on, we want its recipients to know that what they have
    is not the original, so that any problems introduced by others
    will not reflect on the original authors' reputations.

    Finally, any free program is threatened constantly by software
    patents. We wish to avoid the danger that redistributors of a free
    program will individually obtain patent licenses, in effect making
    the program proprietary. To prevent this, we have made it clear
    that any patent must be licensed for everyone's free use or not
    licensed at all.

    The precise terms and conditions for copying, distribution and
    modification follow.

    GNU GENERAL PUBLIC LICENSE
    TERMS AND CONDITIONS FOR COPYING, DISTRIBUTION AND MODIFICATION

    0. This License applies to any program or other work which
    contains a notice placed by the copyright holder saying it may be
    distributed under the terms of this General Public License. The
    "Program", below, refers to any such program or work, and a "work
    based on the Program" means either the Program or any derivative
    work under copyright law: that is to say, a work containing the
    Program or a portion of it, either verbatim or with modifications
    and/or translated into another language. (Hereinafter, translation
    is included without limitation in the term "modification".) Each
    licensee is addressed as "you".

    Activities other than copying, distribution and modification are
    not covered by this License; they are outside its scope. The act
    of running the Program is not restricted, and the output from the
    Program is covered only if its contents constitute a work based on
    the Program (independent of having been made by running the
    Program). Whether that is true depends on what the Program does.

    1. You may copy and distribute verbatim copies of the Program's
    source code as you receive it, in any medium, provided that you
    conspicuously and appropriately publish on each copy an
    appropriate copyright notice and disclaimer of warranty; keep
    intact all the notices that refer to this License and to the
    absence of any warranty; and give any other recipients of the
    Program a copy of this License along with the Program.

    You may charge a fee for the physical act of transferring a copy,
    and you may at your option offer warranty protection in exchange
    for a fee.

    2. You may modify your copy or copies of the Program or any
    portion of it, thus forming a work based on the Program, and copy
    and distribute such modifications or work under the terms of
    Section 1 above, provided that you also meet all of these
    conditions:

    a) You must cause the modified files to carry prominent notices
    stating that you changed the files and the date of any change.

    b) You must cause any work that you distribute or publish, that in
    whole or in part contains or is derived from the Program or any
    part thereof, to be licensed as a whole at no charge to all third
    parties under the terms of this License.

    c) If the modified program normally reads commands interactively
    when run, you must cause it, when started running for such
    interactive use in the most ordinary way, to print or display an
    announcement including an appropriate copyright notice and a
    notice that there is no warranty (or else, saying that you provide
    a warranty) and that users may redistribute the program under
    these conditions, and telling the user how to view a copy of this
    License. (Exception: if the Program itself is interactive but does
    not normally print such an announcement, your work based on the
    Program is not required to print an announcement.)

    These requirements apply to the modified work as a whole. If
    identifiable sections of that work are not derived from the
    Program, and can be reasonably considered independent and separate
    works in themselves, then this License, and its terms, do not
    apply to those sections when you distribute them as separate works.
    But when you distribute the same sections as part of a whole which
    is a work based on the Program, the distribution of the whole must
    be on the terms of this License, whose permissions for other
    licensees extend to the entire whole, and thus to each and every
    part regardless of who wrote it.

    Thus, it is not the intent of this section to claim rights or
    contest your rights to work written entirely by you; rather, the
    intent is to exercise the right to control the distribution of
    derivative or collective works based on the Program.

    In addition, mere aggregation of another work not based on the
    Program with the Program (or with a work based on the Program) on
    a volume of a storage or distribution medium does not bring the
    other work under the scope of this License.

    3. You may copy and distribute the Program (or a work based on it,
    under Section 2) in object code or executable form under the terms
    of Sections 1 and 2 above provided that you also do one of the
    following:

    a) Accompany it with the complete corresponding machine-readable
    source code, which must be distributed under the terms of Sections
    1 and 2 above on a medium customarily used for software
    interchange; or,

    b) Accompany it with a written offer, valid for at least three
    years, to give any third party, for a charge no more than your
    cost of physically performing source distribution, a complete
    machine-readable copy of the corresponding source code, to be
    distributed under the terms of Sections 1 and 2 above on a medium
    customarily used for software interchange; or,

    c) Accompany it with the information you received as to the offer
    to distribute corresponding source code. (This alternative is
    allowed only for noncommercial distribution and only if you
    received the program in object code or executable form with such
    an offer, in accord with Subsection b above.)

    The source code for a work means the preferred form of the work
    for making modifications to it. For an executable work, complete
    source code means all the source code for all modules it contains,
    plus any associated interface definition files, plus the scripts
    used to control compilation and installation of the executable.
    However, as a special exception, the source code distributed need
    not include anything that is normally distributed (in either
    source or binary form) with the major components (compiler,
    kernel, and so on) of the operating system on which the executable
    runs, unless that component itself accompanies the executable.

    If distribution of executable or object code is made by offering
    access to copy from a designated place, then offering equivalent
    access to copy the source code from the same place counts as
    distribution of the source code, even though third parties are not
    compelled to copy the source along with the object code.

    4. You may not copy, modify, sublicense, or distribute the Program
    except as expressly provided under this License. Any attempt
    otherwise to copy, modify, sublicense or distribute the Program is
    void, and will automatically terminate your rights under this
    License. However, parties who have received copies, or rights,
    from you under this License will not have their licenses
    terminated so long as such parties remain in full compliance.

    5. You are not required to accept this License, since you have not
    signed it. However, nothing else grants you permission to modify
    or distribute the Program or its derivative works. These actions
    are prohibited by law if you do not accept this License.
    Therefore, by modifying or distributing the Program (or any work
    based on the Program), you indicate your acceptance of this
    License to do so, and all its terms and conditions for copying,
    distributing or modifying the Program or works based on it.

    6. Each time you redistribute the Program (or any work based on
    the Program), the recipient automatically receives a license from
    the original licensor to copy, distribute or modify the Program
    subject to these terms and conditions. You may not impose any
    further restrictions on the recipients' exercise of the rights
    granted herein. You are not responsible for enforcing compliance
    by third parties to this License.

    7. If, as a consequence of a court judgment or allegation of
    patent infringement or for any other reason (not limited to
    patent issues), conditions are imposed on you (whether by court
    order, agreement or otherwise) that contradict the conditions of
    this License, they do not excuse you from the conditions of this
    License. If you cannot distribute so as to satisfy simultaneously
    your obligations under this License and any other pertinent
    obligations, then as a consequence you may not distribute the
    Program at all. For example, if a patent license would not permit
    royalty-free redistribution of the Program by all those who
    receive copies directly or indirectly through you, then the only
    way you could satisfy both it and this License would be to refrain
    entirely from distribution of the Program.

    If any portion of this section is held invalid or unenforceable
    under any particular circumstance, the balance of the section is
    intended to apply and the section as a whole is intended to apply
    in other circumstances.

    It is not the purpose of this section to induce you to infringe
    any patents or other property right claims or to contest validity
    of any such claims; this section has the sole purpose of
    protecting the integrity of the free software distribution system,
    which is implemented by public license practices. Many people have
    made generous contributions to the wide range of software
    distributed through that system in reliance on consistent
    application of that system; it is up to the author/donor to decide
    if he or she is willing to distribute software through any other
    system and a licensee cannot impose that choice.

    This section is intended to make thoroughly clear what is believed
    to be a consequence of the rest of this License.

    8. If the distribution and/or use of the Program is restricted in
    certain countries either by patents or by copyrighted interfaces,
    the original copyright holder who places the Program under this
    License may add an explicit geographical distribution limitation
    excluding those countries, so that distribution is permitted only
    in or among countries not thus excluded. In such case, this
    License incorporates the limitation as if written in the body of
    this License.

    9. The Free Software Foundation may publish revised and/or new
    versions of the General Public License from time to time. Such new
    versions will be similar in spirit to the present version, but may
    differ in detail to address new problems or concerns.

    Each version is given a distinguishing version number. If the
    Program specifies a version number of this License which applies
    to it and "any later version", you have the option of following
    the terms and conditions either of that version or of any later
    version published by the Free Software Foundation. If the Program
    does not specify a version number of this License, you may choose
    any version ever published by the Free Software Foundation.

    10. If you wish to incorporate parts of the Program into other
    free programs whose distribution conditions are different, write
    to the author to ask for permission. For software which is
    copyrighted by the Free Software Foundation, write to the Free
    Software Foundation; we sometimes make exceptions for this. Our
    decision will be guided by the two goals of preserving the free
    status of all derivatives of our free software and of promoting
    the sharing and reuse of software generally.

    NO WARRANTY

    11. BECAUSE THE PROGRAM IS LICENSED FREE OF CHARGE, THERE IS NO
    WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY APPLICABLE
    LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE COPYRIGHT HOLDERS
    AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS" WITHOUT WARRANTY
    OF ANY KIND, EITHER EXPRESSED OR IMPLIED, INCLUDING, BUT NOT
    LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
    FOR A PARTICULAR PURPOSE. THE ENTIRE RISK AS TO THE QUALITY AND
    PERFORMANCE OF THE PROGRAM IS WITH YOU. SHOULD THE PROGRAM PROVE
    DEFECTIVE, YOU ASSUME THE COST OF ALL NECESSARY SERVICING, REPAIR
    OR CORRECTION.

    12. IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
    WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MAY
    MODIFY AND/OR REDISTRIBUTE THE PROGRAM AS PERMITTED ABOVE, BE
    LIABLE TO YOU FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL,
    INCIDENTAL OR CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR
    INABILITY TO USE THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF
    DATA OR DATA BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU
    OR THIRD PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY
    OTHER PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN
    ADVISED OF THE POSSIBILITY OF SUCH DAMAGES.

    END OF TERMS AND CONDITIONS


Section 5 - GPLv3 License Agreement

                        GNU GENERAL PUBLIC LICENSE
                           Version 3, 29 June 2007

    Copyright c 2007 Free Software Foundation, Inc. <http://fsf.org/>

    Everyone is permitted to copy and distribute verbatim copies of
    this license document, but changing it is not allowed.

    Preamble

    The GNU General Public License is a free, copyleft license for
    software and other kinds of works.

    The licenses for most software and other practical works are
    designed to take away your freedom to share and change the works.
    By contrast, the GNU General Public License is intended to
    guarantee your freedom to share and change all versions of a
    program--to make sure it remains free software for all its users.
    We, the Free Software Foundation, use the GNU General Public
    License for most of our software; it applies also to any other
    work released this way by its authors. You can apply it to your
    programs, too.

    When we speak of free software, we are referring to freedom, not
    price. Our General Public Licenses are designed to make sure that
    you have the freedom to distribute copies of free software (and
    charge for them if you wish), that you receive source code or can
    get it if you want it, that you can change the software or use
    pieces of it in new free programs, and that you know you can do
    these things.

    To protect your rights, we need to prevent others from denying you
    these rights or asking you to surrender the rights. Therefore, you
    have certain responsibilities if you distribute copies of the
    software, or if you modify it: responsibilities to respect the
    freedom of others.

    For example, if you distribute copies of such a program, whether
    gratis or for a fee, you must pass on to the recipients the same
    freedoms that you received. You must make sure that they, too,
    receive or can get the source code. And you must show them these
    terms so they know their rights.

    Developers that use the GNU GPL protect your rights with two
    steps: (1) assert copyright on the software, and (2) offer you
    this License giving you legal permission to copy, distribute
    and/or modify it.

    For the developers' and authors' protection, the GPL clearly
    explains that there is no warranty for this free software. For
    both users' and authors' sake, the GPL requires that modified
    versions be marked as changed, so that their problems will not be
    attributed erroneously to authors of previous versions.

    Some devices are designed to deny users access to install or run
    modified versions of the software inside them, although the
    manufacturer can do so. This is fundamentally incompatible with
    the aim of protecting users' freedom to change the software. The
    systematic pattern of such abuse occurs in the area of products
    for individuals to use, which is precisely where it is most
    unacceptable. Therefore, we have designed this version of the GPL
    to prohibit the practice for those products. If such problems
    arise substantially in other domains, we stand ready to extend
    this provision to those domains in future versions of the GPL, as
    needed to protect the freedom of users.

    Finally, every program is threatened constantly by software
    patents. States should not allow patents to restrict development
    and use of software on general-purpose computers, but in those
    that do, we wish to avoid the special danger that patents applied
    to a free program could make it effectively proprietary. To
    prevent this, the GPL assures that patents cannot be used to
    render the program non-free.

    The precise terms and conditions for copying, distribution and
    modification follow.

    TERMS AND CONDITIONS

    0. Definitions.

    "This License" refers to version 3 of the GNU General Public
    License.

    "Copyright" also means copyright-like laws that apply to other
    kinds of works, such as semiconductor masks.

    "The Program" refers to any copyrightable work licensed under this
    License. Each licensee is addressed as "you". "Licensees" and
    "recipients" may be individuals or organizations.

    To "modify" a work means to copy from or adapt all or part of the
    work in a fashion requiring copyright permission, other than the
    making of an exact copy. The resulting work is called a "modified
    version" of the earlier work or a work "based on" the earlier
    work.

    A "covered work" means either the unmodified Program or a work
    based on the Program.

    To "propagate" a work means to do anything with it that, without
    permission, would make you directly or secondarily liable for
    infringement under applicable copyright law, except executing it
    on a computer or modifying a private copy. Propagation includes
    copying, distribution (with or without modification), making
    available to the public, and in some countries other activities as
    well.

    To "convey" a work means any kind of propagation that enables
    other parties to make or receive copies. Mere interaction with a
    user through a computer network, with no transfer of a copy, is
    not conveying.

    An interactive user interface displays "Appropriate Legal Notices"
    to the extent that it includes a convenient and prominently
    visible feature that (1) displays an appropriate copyright notice,
    and (2) tells the user that there is no warranty for the work
    (except to the extent that warranties are provided), that
    licensees may convey the work under this License, and how to view
    a copy of this License. If the interface presents a list of user
    commands or options, such as a menu, a prominent item in the list
    meets this criterion.

    1. Source Code.

    The "source code" for a work means the preferred form of the work
    for making modifications to it. "Object code" means any non-source
    form of a work.

    A "Standard Interface" means an interface that either is an
    official standard defined by a recognized standards body, or, in
    the case of interfaces specified for a particular programming
    language, one that is widely used among developers working in that
    language.

    The "System Libraries" of an executable work include anything,
    other than the work as a whole, that (a) is included in the normal
    form of packaging a Major Component, but which is not part of that
    Major Component, and (b) serves only to enable use of the work
    with that Major Component, or to implement a Standard Interface
    for which an implementation is available to the public in source
    code form. A "Major Component", in this context, means a major
    essential component (kernel, window system, and so on) of the
    specific operating system (if any) on which the executable work
    runs, or a compiler used to produce the work, or an object code
    interpreter used to run it.

    The "Corresponding Source" for a work in object code form means
    all the source code needed to generate, install, and (for an
    executable work) run the object code and to modify the work,
    including scripts to control those activities. However, it does
    not include the work's System Libraries, or general-purpose tools
    or generally available free programs which are used unmodified in
    performing those activities but which are not part of the work.
    For example, Corresponding Source includes interface definition
    files associated with source files for the work, and the source
    code for shared libraries and dynamically linked subprograms that
    the work is specifically designed to require, such as by intimate
    data communication or control flow between those subprograms and
    other parts of the work.

    The Corresponding Source need not include anything that users can
    regenerate automatically from other parts of the Corresponding
    Source.

    The Corresponding Source for a work in source code form is that
    same work.

    2. Basic Permissions.

    All rights granted under this License are granted for the term of
    copyright on the Program, and are irrevocable provided the stated
    conditions are met. This License explicitly affirms your unlimited
    permission to run the unmodified Program. The output from running
    a covered work is covered by this License only if the output,
    given its content, constitutes a covered work. This License
    acknowledges your rights of fair use or other equivalent, as
    provided by copyright law.

    You may make, run and propagate covered works that you do not
    convey, without conditions so long as your license otherwise
    remains in force. You may convey covered works to others for the
    sole purpose of having them make modifications exclusively for
    you, or provide you with facilities for running those works,
    provided that you comply with the terms of this License in
    conveying all material for which you do not control copyright.
    Those thus making or running the covered works for you must do
    so exclusively on your behalf, under your direction and control,
    on terms that prohibit them from making any copies of your
    copyrighted material outside their relationship with you.

    Conveying under any other circumstances is permitted solely under
    the conditions stated below. Sublicensing is not allowed; section
    10 makes it unnecessary.

    3. Protecting Users' Legal Rights From Anti-Circumvention Law.

    No covered work shall be deemed part of an effective technological
    measure under any applicable law fulfilling obligations under
    article 11 of the WIPO copyright treaty adopted on 20 December
    1996, or similar laws prohibiting or restricting circumvention of
    such measures.

    When you convey a covered work, you waive any legal power to
    forbid circumvention of technological measures to the extent such
    circumvention is effected by exercising rights under this License
    with respect to the covered work, and you disclaim any intention
    to limit operation or modification of the work as a means of
    enforcing, against the work's users, your or third parties' legal
    rights to forbid circumvention of technological measures.

    4. Conveying Verbatim Copies.

    You may convey verbatim copies of the Program's source code as you
    receive it, in any medium, provided that you conspicuously and
    appropriately publish on each copy an appropriate copyright
    notice; keep intact all notices stating that this License and any
    non-permissive terms added in accord with section 7 apply to the
    code; keep intact all notices of the absence of any warranty;
    and give all recipients a copy of this License along with the
    Program.

    You may charge any price or no price for each copy that you
    convey, and you may offer support or warranty protection for a
    fee.

    5. Conveying Modified Source Versions.

    You may convey a work based on the Program, or the modifications
    to produce it from the Program, in the form of source code under
    the terms of section 4, provided that you also meet all of these
    conditions:

    a) The work must carry prominent notices stating that you modified
    it, and giving a relevant date.

    b) The work must carry prominent notices stating that it is
    released under this License and any conditions added under section
    7. This requirement modifies the requirement in section 4 to "keep
    intact all notices".

    c) You must license the entire work, as a whole, under this
    License to anyone who comes into possession of a copy. This
    License will therefore apply, along with any applicable section 7
    additional terms, to the whole of the work, and all its parts,
    regardless of how they are packaged. This License gives no
    permission to license the work in any other way, but it does not
    invalidate such permission if you have separately received it.

    d) If the work has interactive user interfaces, each must display
    Appropriate Legal Notices; however, if the Program has interactive
    interfaces that do not display Appropriate Legal Notices, your
    work need not make them do so.

    A compilation of a covered work with other separate and
    independent works, which are not by their nature extensions of the
    covered work, and which are not combined with it such as to form a
    larger program, in or on a volume of a storage or distribution
    medium, is called an "aggregate" if the compilation and its
    resulting copyright are not used to limit the access or legal
    rights of the compilation's users beyond what the individual works
    permit. Inclusion of a covered work in an aggregate does not cause
    this License to apply to the other parts of the aggregate.

    6. Conveying Non-Source Forms.

    You may convey a covered work in object code form under the terms
    of sections 4 and 5, provided that you also convey the machine-
    readable Corresponding Source under the terms of this License, in
    one of these ways:

    a) Convey the object code in, or embodied in, a physical product
    (including a physical distribution medium), accompanied by the
    Corresponding Source fixed on a durable physical medium
    customarily used for software interchange.

    b) Convey the object code in, or embodied in, a physical product
    (including a physical distribution medium), accompanied by a
    written offer, valid for at least three years and valid for as
    long as you offer spare parts or customer support for that product
    model, to give anyone who possesses the object code either (1) a
    copy of the Corresponding Source for all the software in the
    product that is covered by this License, on a durable physical
    medium customarily used for software interchange, for a price no
    more than your reasonable cost of physically performing this
    conveying of source, or (2) access to copy the Corresponding
    Source from a network server at no charge.

    c) Convey individual copies of the object code with a copy of the
    written offer to provide the Corresponding Source. This
    alternative is allowed only occasionally and noncommercially, and
    only if you received the object code with such an offer, in accord
    with subsection 6b.

    d) Convey the object code by offering access from a designated
    place (gratis or for a charge), and offer equivalent access to the
    Corresponding Source in the same way through the same place at no
    further charge. You need not require recipients to copy the
    Corresponding Source along with the object code. If the place to
    copy the object code is a network server, the Corresponding Source
    may be on a different server (operated by you or a third party)
    that supports equivalent copying facilities, provided you maintain
    clear directions next to the object code saying where to find the
    Corresponding Source. Regardless of what server hosts the
    Corresponding Source, you remain obligated to ensure that it is
    available for as long as needed to satisfy these requirements.

    e) Convey the object code using peer-to-peer transmission,
    provided you inform other peers where the object code and
    Corresponding Source of the work are being offered to the general
    public at no charge under subsection 6d.

    A separable portion of the object code, whose source code is
    excluded from the Corresponding Source as a System Library, need
    not be included in conveying the object code work.

    A "User Product" is either (1) a "consumer product", which means
    any tangible personal property which is normally used for
    personal, family, or household purposes, or (2) anything designed
    or sold for incorporation into a dwelling. In determining whether
    a product is a consumer product, doubtful cases shall be resolved
    in favor of coverage. For a particular product received by a
    particular user, "normally used" refers to a typical or common use
    of that class of product, regardless of the status of the
    particular user or of the way in which the particular user
    actually uses, or expects or is expected to use, the product. A
    product is a consumer product regardless of whether the product
    has substantial commercial, industrial or non-consumer uses,
    unless such uses represent the only significant mode of use of the
    product.

    "Installation Information" for a User Product means any methods,
    procedures, authorization keys, or other information required to
    install and execute modified versions of a covered work in that
    User Product from a modified version of its Corresponding Source.
    The information must suffice to ensure that the continued
    functioning of the modified object code is in no case prevented or
    interfered with solely because modification has been made.

    If you convey an object code work under this section in, or with,
    or specifically for use in, a User Product, and the conveying
    occurs as part of a transaction in which the right of possession
    and use of the User Product is transferred to the recipient in
    perpetuity or for a fixed term (regardless of how the transaction
    is characterized), the Corresponding Source conveyed under this
    section must be accompanied by the Installation Information. But
    this requirement does not apply if neither you nor any third party
    retains the ability to install modified object code on the User
    Product (for example, the work has been installed in ROM).

    The requirement to provide Installation Information does not
    include a requirement to continue to provide support service,
    warranty, or updates for a work that has been modified or
    installed by the recipient, or for the User Product in which it
    has been modified or installed. Access to a network may be denied
    when the modification itself materially and adversely affects the
    operation of the network or violates the rules and protocols for
    communication across the network.

    Corresponding Source conveyed, and Installation Information
    provided, in accord with this section must be in a format that is
    publicly documented (and with an implementation available to the
    public in source code form), and must require no special password
    or key for unpacking, reading or copying.

    7. Additional Terms.

    "Additional permissions" are terms that supplement the terms of
    this License by making exceptions from one or more of its
    conditions. Additional permissions that are applicable to the
    entire Program shall be treated as though they were included in
    this License, to the extent that they are valid under applicable
    law. If additional permissions apply only to part of the Program,
    that part may be used separately under those permissions, but the
    entire Program remains governed by this License without regard to
    the additional permissions.

    When you convey a copy of a covered work, you may at your option
    remove any additional permissions from that copy, or from any part
    of it. (Additional permissions may be written to require their own
    removal in certain cases when you modify the work.) You may place
    additional permissions on material, added by you to a covered
    work, for which you have or can give appropriate copyright
    permission.

    Notwithstanding any other provision of this License, for material
    you add to a covered work, you may (if authorized by the copyright
    holders of that material) supplement the terms of this License
    with terms:

    a) Disclaiming warranty or limiting liability differently from the
    terms of sections 15 and 16 of this License; or

    b) Requiring preservation of specified reasonable legal notices or
    author attributions in that material or in the Appropriate Legal
    Notices displayed by works containing it; or

    c) Prohibiting misrepresentation of the origin of that material,
    or requiring that modified versions of such material be marked in
    reasonable ways as different from the original version; or

    d) Limiting the use for publicity purposes of names of licensors
    or authors of the material; or

    e) Declining to grant rights under trademark law for use of some
    trade names, trademarks, or service marks; or

    f) Requiring indemnification of licensors and authors of that
    material by anyone who conveys the material (or modified versions
    of it) with contractual assumptions of liability to the recipient,
    for any liability that these contractual assumptions directly
    impose on those licensors and authors.

    All other non-permissive additional terms are considered "further
    restrictions" within the meaning of section 10. If the Program as
    you received it, or any part of it, contains a notice stating that
    it is governed by this License along with a term that is a further
    restriction, you may remove that term. If a license document
    contains a further restriction but permits relicensing or
    conveying under this License, you may add to a covered work
    material governed by the terms of that license document, provided
    that the further restriction does not survive such relicensing or
    conveying.

    If you add terms to a covered work in accord with this section,
    you must place, in the relevant source files, a statement of the
    additional terms that apply to those files, or a notice indicating
    where to find the applicable terms.

    Additional terms, permissive or non-permissive, may be stated in
    the form of a separately written license, or stated as exceptions;
    the above requirements apply either way.

    8. Termination.

    You may not propagate or modify a covered work except as expressly
    provided under this License. Any attempt otherwise to propagate or
    modify it is void, and will automatically terminate your rights
    under this License (including any patent licenses granted under
    the third paragraph of section 11).

    However, if you cease all violation of this License, then your
    license from a particular copyright holder is reinstated (a)
    provisionally, unless and until the copyright holder explicitly
    and finally terminates your license, and (b) permanently, if the
    copyright holder fails to notify you of the violation by some
    reasonable means prior to 60 days after the cessation.

    Moreover, your license from a particular copyright holder is
    reinstated permanently if the copyright holder notifies you of the
    violation by some reasonable means, this is the first time you
    have received notice of violation of this License (for any work)
    from that copyright holder, and you cure the violation prior to 30
    days after your receipt of the notice.

    Termination of your rights under this section does not terminate
    the licenses of parties who have received copies or rights from
    you under this License. If your rights have been terminated and
    not permanently reinstated, you do not qualify to receive new
    licenses for the same material under section 10.

    9. Acceptance Not Required for Having Copies.

    You are not required to accept this License in order to receive or
    run a copy of the Program. Ancillary propagation of a covered work
    occurring solely as a consequence of using peer-to-peer
    transmission to receive a copy likewise does not require
    acceptance. However, nothing other than this License grants you
    permission to propagate or modify any covered work. These actions
    infringe copyright if you do not accept this License. Therefore,
    by modifying or propagating a covered work, you indicate your
    acceptance of this License to do so.

    10. Automatic Licensing of Downstream Recipients.

    Each time you convey a covered work, the recipient automatically
    receives a license from the original licensors, to run, modify and
    propagate that work, subject to this License. You are not
    responsible for enforcing compliance by third parties with this
    License.

    An "entity transaction" is a transaction transferring control of
    an organization, or substantially all assets of one, or
    subdividing an organization, or merging organizations. If
    propagation of a covered work results from an entity transaction,
    each party to that transaction who receives a copy of the work
    also receives whatever licenses to the work the party's
    predecessor in interest had or could give under the previous
    paragraph, plus a right to possession of the Corresponding Source
    of the work from the predecessor in interest, if the predecessor
    has it or can get it with reasonable efforts.

    You may not impose any further restrictions on the exercise of the
    rights granted or affirmed under this License. For example, you
    may not impose a license fee, royalty, or other charge for
    exercise of rights granted under this License, and you may not
    initiate litigation (including a cross-claim or counterclaim in a
    lawsuit) alleging that any patent claim is infringed by making,
    using, selling, offering for sale, or importing the Program or any
    portion of it.

    11. Patents.

    A "contributor" is a copyright holder who authorizes use under
    this License of the Program or a work on which the Program is
    based. The work thus licensed is called the contributor's
    "contributor version".

    A contributor's "essential patent claims" are all patent claims
    owned or controlled by the contributor, whether already acquired
    or hereafter acquired, that would be infringed by some manner,
    permitted by this License, of making, using, or selling its
    contributor version, but do not include claims that would be
    infringed only as a consequence of further modification of the
    contributor version. For purposes of this definition, "control"
    includes the right to grant patent sublicenses in a manner
    consistent with the requirements of this License.

    Each contributor grants you a non-exclusive, worldwide, royalty-
    free patent license under the contributor's essential patent
    claims, to make, use, sell, offer for sale, import and otherwise
    run, modify and propagate the contents of its contributor version.

    In the following three paragraphs, a "patent license" is any
    express agreement or commitment, however denominated, not to
    enforce a patent (such as an express permission to practice a
    patent or covenant not to sue for patent infringement). To "grant"
    such a patent license to a party means to make such an agreement
    or commitment not to enforce a patent against the party.

    If you convey a covered work, knowingly relying on a patent
    license, and the Corresponding Source of the work is not available
    for anyone to copy, free of charge and under the terms of this
    License, through a publicly available network server or other
    readily accessible means, then you must either (1) cause the
    Corresponding Source to be so available, or (2) arrange to deprive
    yourself of the benefit of the patent license for this particular
    work, or (3) arrange, in a manner consistent with the requirements
    of this License, to extend the patent license to downstream
    recipients. "Knowingly relying" means you have actual knowledge
    that, but for the patent license, your conveying the covered work
    in a country, or your recipient's use of the covered work in a
    country, would infringe one or more identifiable patents in that
    country that you have reason to believe are valid.

    If, pursuant to or in connection with a single transaction or
    arrangement, you convey, or propagate by procuring conveyance of,
    a covered work, and grant a patent license to some of the parties
    receiving the covered work authorizing them to use, propagate,
    modify or convey a specific copy of the covered work, then the
    patent license you grant is automatically extended to all
    recipients of the covered work and works based on it.

    A patent license is "discriminatory" if it does not include within
    the scope of its coverage, prohibits the exercise of, or is
    conditioned on the non-exercise of one or more of the rights that
    are specifically granted under this License. You may not convey a
    covered work if you are a party to an arrangement with a third
    party that is in the business of distributing software, under
    which you make payment to the third party based on the extent of
    your activity of conveying the work, and under which the third
    party grants, to any of the parties who would receive the covered
    work from you, a discriminatory patent license (a) in connection
    with copies of the covered work conveyed by you (or copies made
    from those copies), or (b) primarily for and in connection with
    specific products or compilations that contain the covered work,
    unless you entered into that arrangement, or that patent license
    was granted, prior to 28 March 2007.

    Nothing in this License shall be construed as excluding or
    limiting any implied license or other defenses to infringement
    that may otherwise be available to you under applicable patent
    law.

    12. No Surrender of Others' Freedom.

    If conditions are imposed on you (whether by court order,
    agreement or otherwise) that contradict the conditions of this
    License, they do not excuse you from the conditions of this
    License. If you cannot convey a covered work so as to satisfy
    simultaneously your obligations under this License and any other
    pertinent obligations, then as a consequence you may not convey it
    at all. For example, if you agree to terms that obligate you to
    collect a royalty for further conveying from those to whom you
    convey the Program, the only way you could satisfy both those
    terms and this License would be to refrain entirely from conveying
    the Program.

    13. Use with the GNU Affero General Public License.

    Notwithstanding any other provision of this License, you have
    permission to link or combine any covered work with a work
    licensed under version 3 of the GNU Affero General Public License
    into a single combined work, and to convey the resulting work. The
    terms of this License will continue to apply to the part which is
    the covered work, but the special requirements of the GNU Affero
    General Public License, section 13, concerning interaction through
    a network will apply to the combination as such.

    14. Revised Versions of this License.

    The Free Software Foundation may publish revised and/or new
    versions of the GNU General Public License from time to time. Such
    new versions will be similar in spirit to the present version, but
    may differ in detail to address new problems or concerns.

    Each version is given a distinguishing version number. If the
    Program specifies that a certain numbered version of the GNU
    General Public License "or any later version" applies to it, you
    have the option of following the terms and conditions either of
    that numbered version or of any later version published by the
    Free Software Foundation. If the Program does not specify a
    version number of the GNU General Public License, you may choose
    any version ever published by the Free Software Foundation.

    If the Program specifies that a proxy can decide which future
    versions of the GNU General Public License can be used, that
    proxy's public statement of acceptance of a version permanently
    authorizes you to choose that version for the Program.

    Later license versions may give you additional or different
    permissions. However, no additional obligations are imposed on any
    author or copyright holder as a result of your choosing to follow
    a later version.

    15. Disclaimer of Warranty.

    THERE IS NO WARRANTY FOR THE PROGRAM, TO THE EXTENT PERMITTED BY
    APPLICABLE LAW. EXCEPT WHEN OTHERWISE STATED IN WRITING THE
    COPYRIGHT HOLDERS AND/OR OTHER PARTIES PROVIDE THE PROGRAM "AS IS"
    WITHOUT WARRANTY OF ANY KIND, EITHER EXPRESSED OR IMPLIED,
    INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF
    MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE. THE ENTIRE
    RISK AS TO THE QUALITY AND PERFORMANCE OF THE PROGRAM IS WITH YOU.
    SHOULD THE PROGRAM PROVE DEFECTIVE, YOU ASSUME THE COST OF ALL
    NECESSARY SERVICING, REPAIR OR CORRECTION.

    16. Limitation of Liability.

    IN NO EVENT UNLESS REQUIRED BY APPLICABLE LAW OR AGREED TO IN
    WRITING WILL ANY COPYRIGHT HOLDER, OR ANY OTHER PARTY WHO MODIFIES
    AND/OR CONVEYS THE PROGRAM AS PERMITTED ABOVE, BE LIABLE TO YOU
    FOR DAMAGES, INCLUDING ANY GENERAL, SPECIAL, INCIDENTAL OR
    CONSEQUENTIAL DAMAGES ARISING OUT OF THE USE OR INABILITY TO USE
    THE PROGRAM (INCLUDING BUT NOT LIMITED TO LOSS OF DATA OR DATA
    BEING RENDERED INACCURATE OR LOSSES SUSTAINED BY YOU OR THIRD
    PARTIES OR A FAILURE OF THE PROGRAM TO OPERATE WITH ANY OTHER
    PROGRAMS), EVEN IF SUCH HOLDER OR OTHER PARTY HAS BEEN ADVISED OF
    THE POSSIBILITY OF SUCH DAMAGES.

    17. Interpretation of Sections 15 and 16.

    If the disclaimer of warranty and limitation of liability provided
    above cannot be given local legal effect according to their terms,
    reviewing courts shall apply local law that most closely
    approximates an absolute waiver of all civil liability in
    connection with the Program, unless a warranty or assumption of
    liability accompanies a copy of the Program in return for a fee.


Section 6 - 3rd Party Components

(1) The Software Program includes software and documentation components
developed in part by Silver Egg Technology, Inc.("SET") prior to 2001
and released under the following license.

    Copyright (c) 2001 Silver Egg Technology

    Permission is hereby granted, free of charge, to any person
    obtaining a copy of this software and associated documentation
    files (the "Software"), to deal in the Software without
    restriction, including without limitation the rights to use,
    copy, modify, merge, publish, distribute, sub-license, and/or
    sell copies of the Software, and to permit persons to whom the
    Software is furnished to do so, subject to the following
    conditions:

    The above copyright notice and this permission notice shall be
    included in all copies or substantial portions of the Software.

    THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
    EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
    OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
    NON-INFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
    HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
    WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
    FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
    OTHER DEALINGS IN THE SOFTWARE.
