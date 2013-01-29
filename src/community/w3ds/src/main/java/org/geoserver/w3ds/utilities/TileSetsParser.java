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

import org.geoserver.w3ds.types.TileSet;
import org.geotools.referencing.CRS;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class TileSetsParser {

	private List<TileSet> tileSets;
	
	public TileSetsParser() {
		tileSets = new ArrayList<TileSet>();
	}

	public TileSetsParser(File dir, Logger LOGGER)
			throws ParserConfigurationException, SAXException {
		tileSets = new ArrayList<TileSet>();
		File[] files = dir.listFiles();
		for (File file : files) {
			if (file.isFile()) {
				SAXParser parser;
				parser = SAXParserFactory.newInstance().newSAXParser();
				DefaultHandler tileSetHandler = new TileSetHandler();
				try {
					parser.parse(file, tileSetHandler);
				} catch (SAXException e) {
					LOGGER.warning("Fail parsing the file '" + file.getName()
							+ "': " + e.toString());
				} catch (IOException e) {
					LOGGER.warning("Fail parsing the file '" + file.getName()
							+ "': " + e.toString());
				}
				tileSets.add(((TileSetHandler) tileSetHandler).getTileSet());
			}
		}
	}

	public List<TileSet> getTileSets() {
		return tileSets;
	}
	
	public List<String> getTileSetsNames() {
		List<String> names = new ArrayList<String>();
		for(TileSet t : this.tileSets) {
			names.add(t.getIdentifier());
		}
		return names;
	}
	
	public TileSet getTileSet(String identifier) {
		for(TileSet t : this.tileSets) {
			if(t.getIdentifier().equalsIgnoreCase(identifier)) {
				return t;
			}
		}
		return null;
	}

	private class TileSetHandler extends DefaultHandler {

		private TileSet tileSet;
		private boolean inIdentifier, inCRS, inTileSizes, inLowerCorner;
		private boolean finIdentifier, finCRS, finTileSizes, finLowerCorner;
		private int n_tileSets;

		public TileSet getTileSet() {
			return tileSet;
		}

		public TileSetHandler() {
			super();
			inIdentifier = false;
			inCRS = false;
			inTileSizes = false;
			inLowerCorner = false;
			finIdentifier = false;
			finCRS = false;
			finTileSizes = false;
			finLowerCorner = false;
			n_tileSets = 0;
		}

		public void startElement(String uri, String localName, String qName,
				Attributes attributes) throws SAXException {
			if (qName.equalsIgnoreCase("TileSet")) {
				this.tileSet = new TileSet();
				n_tileSets++;
			} else if (qName.equalsIgnoreCase("Identifier")) {
				if (this.finIdentifier) {
					throw new SAXException("Tag " + qName
							+ " defined multiple times.");
				}
				this.inIdentifier = true;
				this.finIdentifier = true;
			} else if (qName.equalsIgnoreCase("CRS")) {
				if (this.finCRS) {
					throw new SAXException("Tag " + qName
							+ " defined multiple times.");
				}
				this.inCRS = true;
				this.finCRS = true;
			} else if (qName.equalsIgnoreCase("TileSizes")) {
				if (this.finTileSizes) {
					throw new SAXException("Tag " + qName
							+ " defined multiple times.");
				}
				this.inTileSizes = true;
				this.finTileSizes = true;
			} else if (qName.equalsIgnoreCase("LowerCorner")) {
				if (this.finLowerCorner) {
					throw new SAXException("Tag " + qName
							+ " defined multiple times.");
				}
				this.inLowerCorner = true;
				this.finLowerCorner = true;
			} else {
				throw new SAXException("Tag " + qName + " no reconized.");
			}
		}

		public void characters(char ch[], int start, int length)
				throws SAXException {
			if (inIdentifier) {
				inIdentifier = false;
				tileSet.setIdentifier(new String(ch, start, length));
			} else if (inCRS) {
				CoordinateReferenceSystem crs = null;
				try {
					inCRS = false;
					crs = CRS.decode(new String(ch, start, length));
					tileSet.setCrs(crs);
				} catch (NoSuchAuthorityCodeException e) {
					throw new SAXException("CRS "
							+ new String(ch, start, length) + " not reconized.");
				} catch (Exception e) {
					throw new SAXException("Error parsing the CRS "
							+ new String(ch, start, length) + ".");
				}
			} else if (inTileSizes) {
				inTileSizes = false;
				String[] tilesSizes_str = W3DSUtils.parseStrArray(new String(
						ch, start, length), "\\s+");
				if (!(tilesSizes_str.length > 0)) {
					throw new SAXException("Error parsing the 'TileSizes' tag "
							+ new String(ch, start, length) + ".");
				}
				for (String s : tilesSizes_str) {
					try {
						float size = Float.valueOf(s);
						tileSet.addTileSize(size);
					} catch (Exception e) {
						throw new SAXException("Error parsing the 'size' " + s
								+ ".");
					}
				}
			} else if (inLowerCorner) {
				inLowerCorner = false;
				String[] lowerCorner_str = W3DSUtils.parseStrArray(new String(
						ch, start, length), "\\s+");
				if (lowerCorner_str.length != 2) {
					throw new SAXException(
							"Error parsing the 'LowerCorner' tag "
									+ new String(ch, start, length) + ".");
				}
				try {
					tileSet.setLowerCornerX(Double.valueOf(lowerCorner_str[0]));
				} catch (Exception e) {
					throw new SAXException("Error parsing the 'X LowerCorner' "
							+ lowerCorner_str[0] + ".");
				}
				try {
					tileSet.setLowerCornerY(Double.valueOf(lowerCorner_str[1]));
				} catch (Exception e) {
					throw new SAXException("Error parsing the 'Y LowerCorner' "
							+ lowerCorner_str[1] + ".");
				}
			}
		}

		public void endDocument() throws SAXException {
			if (n_tileSets == 0) {
				throw new SAXException("Don't find any 'TileSet' definition.");
			}
			if (n_tileSets > 1) {
				throw new SAXException("Find multiple 'TileSet' definitions.");
			}
			if (!finIdentifier) {
				throw new SAXException("Don't fin a 'Identifier' definition.");
			}
			if (!finCRS) {
				throw new SAXException("Don't fin a 'CRS' definition.");
			}
			if (!finTileSizes) {
				throw new SAXException("Don't fin a 'TileSizes' definition.");
			}
			if (!finLowerCorner) {
				throw new SAXException("Don't fin a 'LowerCorner' definition.");
			}
		}

	}

}
