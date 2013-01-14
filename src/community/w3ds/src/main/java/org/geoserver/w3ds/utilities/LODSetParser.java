/* This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 * 
 * @author Jorge Gustavo Rocha / Universidade do Minho
 * @author Nuno Carvalho Oliveira / Universidade do Minho 
 */

package org.geoserver.w3ds.utilities;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.geoserver.w3ds.types.LOD;
import org.geoserver.w3ds.types.LODSet;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class LODSetParser {

	private List<LODSet> lodSets;
	
	public LODSetParser() {
		lodSets = new ArrayList<LODSet>();
	}

	public LODSetParser(File dir, Logger LOGGER)
			throws ParserConfigurationException, SAXException {
		lodSets = new ArrayList<LODSet>();
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isFile()) {
				SAXParser parser;
				parser = SAXParserFactory.newInstance().newSAXParser();
				DefaultHandler lodSetHandler = new LODSetHandler();
				try {
					parser.parse(file, lodSetHandler);
					lodSets.add(((LODSetHandler) lodSetHandler).getLodSet());
				} catch (SAXException e) {
					LOGGER.warning("Fail parsing the file '" + file.getName()
							+ "': " + e.toString());
				} catch (IOException e) {
					LOGGER.warning("Fail parsing the file '" + file.getName()
							+ "': " + e.toString());
				}
			}
		}
	}

	public List<LODSet> getLodSets() {
		return lodSets;
	}

	public List<String> getLodSetsNames() {
		List<String> names = new ArrayList<String>();
		for (LODSet l : this.lodSets) {
			names.add(l.getIdentifier());
		}
		return names;
	}
	
	public LODSet getLODSet(String identifier) {
		for (LODSet l : this.lodSets) {
			if(l.getIdentifier().equalsIgnoreCase(identifier)) {
				return l;
			}
		}
		return null;
	}

	private class LODSetHandler extends DefaultHandler {

		private LODSet lodSet;
		private LOD lod;
		private boolean inIdentifierLODSet, inLOD, inTitle, inAbstract,
				inIdentifier, inLODValue, inDefaultRange;
		private boolean finIdentifierLODSet, finLOD, finTitle, finAbstract,
				finIdentifier, finLODValue, finDefaultRange;
		private int n_lodSets;

		public LODSet getLodSet() {
			return lodSet;
		}

		public LODSetHandler() {
			super();
			inIdentifierLODSet = false;
			inLOD = false;
			inTitle = false;
			inAbstract = false;
			inIdentifier = false;
			inLODValue = false;
			inDefaultRange = false;
			setFind(false);
			n_lodSets = 0;
			lodSet = new LODSet();
		}

		private void setFind(boolean value) {
			finTitle = value;
			finAbstract = value;
			finIdentifier = value;
			finLODValue = value;
			finDefaultRange = value;
			finLOD = value;
			finIdentifierLODSet = value;
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("LODSet")) {
				n_lodSets++;
			} else if (qName.equalsIgnoreCase("Identifier") & !finLOD) {
				if (this.finIdentifierLODSet) {
					throw new SAXException("Tag " + qName
							+ " defined multiple times.");
				}
				this.inIdentifierLODSet = true;
				this.finIdentifierLODSet = true;
			} else if (qName.equalsIgnoreCase("LOD")) {
				if (this.finLOD) {
					throw new SAXException("Tag " + qName
							+ " defined multiple times.");
				}
				this.lod = new LOD();
				this.inLOD = true;
				this.finLOD = true;
			} else if (inLOD) {
				if (qName.equalsIgnoreCase("Title")) {
					if (this.finTitle) {
						throw new SAXException("Tag " + qName
								+ " defined multiple times.");
					}
					this.inTitle = true;
					this.finTitle = true;
				} else if (qName.equalsIgnoreCase("Abstract")) {
					if (this.finAbstract) {
						throw new SAXException("Tag " + qName
								+ " defined multiple times.");
					}
					this.inAbstract = true;
					this.finAbstract = true;
				} else if (qName.equalsIgnoreCase("Identifier")) {
					if (this.finIdentifier) {
						throw new SAXException("Tag " + qName
								+ " defined multiple times.");
					}
					this.inIdentifier = true;
					this.finIdentifier = true;
				} else if (qName.equalsIgnoreCase("LODValue")) {
					if (this.finLODValue) {
						throw new SAXException("Tag " + qName
								+ " defined multiple times.");
					}
					this.inLODValue = true;
					this.finLODValue = true;
				} else if (qName.equalsIgnoreCase("DefaultRange")) {
					if (this.finDefaultRange) {
						throw new SAXException("Tag " + qName
								+ " defined multiple times.");
					}
					this.inDefaultRange = true;
					this.finDefaultRange = true;
				}
			} else {
				throw new SAXException("Tag " + qName
						+ " no reconized or not in the right place.");
			}
		}

		public void endElement(String uri, String localName, String qName)
				throws SAXException {
			if (qName.equalsIgnoreCase("LOD")) {
				inLOD = false;
				if (!finIdentifier) {
					throw new SAXException(
							"Don't fin a 'Identifier' definition.");
				}
				if (!finLODValue) {
					throw new SAXException("Don't fin a 'LODValue' definition.");
				}
				if (!finDefaultRange) {
					throw new SAXException(
							"Don't fin a 'DefaultRange' definition.");
				}
				lodSet.addLOD(lod);
				setFind(false);
			}
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {
			if (inIdentifierLODSet) {
				inIdentifierLODSet = false;
				lodSet.setIdentifier(new String(ch, start, length));
			}
			if (inTitle) {
				inTitle = false;
				lod.setTitle(new String(ch, start, length));
			}
			if (inAbstract) {
				inAbstract = false;
				lod.setAbstractTxt(new String(ch, start, length));
			}
			if (inIdentifier) {
				inIdentifier = false;
				lod.setIdentifier(new String(ch, start, length));
			}
			if (inLODValue) {
				inLODValue = false;
				try {
					int lodValue = Integer
							.valueOf(new String(ch, start, length));
					lod.setLodValue(lodValue);
				} catch (Exception e) {
					throw new SAXException(
							"Error parsing the 'LODValue' element "
									+ new String(ch, start, length) + ".");
				}
			}
			if (inDefaultRange) {
				inDefaultRange = false;
				try {
					float defaultRange = Float.valueOf(new String(ch, start,
							length));
					lod.setDefaultRange(defaultRange);
				} catch (Exception e) {
					throw new SAXException(
							"Error parsing the 'DefaultRange' element "
									+ new String(ch, start, length) + ".");
				}
			}
		}

		public void endDocument() throws SAXException {
			if (n_lodSets == 0) {
				throw new SAXException("Don't find any 'LODSet' definition.");
			}
			if (n_lodSets > 1) {
				throw new SAXException("Find multiple 'LODSet' definitions.");
			}
		}

	}

}
