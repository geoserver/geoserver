package org.geoserver.wms.worldwind.etc1;

public class ETCConstants {

	public static final int D3DFMT_ETC1 = makeFourCC('E', 'T', 'C', '1');
	
    /**
	* Size in bytes of an encoded block.
	*/
	public static final int ENCODED_BLOCK_SIZE = 8;

	/**
	* Size in bytes of a decoded block.
	*/
	public static final int DECODED_BLOCK_SIZE = 48;

	/**
	* Accepted by the internalformat parameter of glCompressedTexImage2D.
	*/
	public static final int ETC1_RGB8_OES = 0x8D64;
	
	public static int makeFourCC(char ch0, char ch1, char ch2, char ch3)
    {
        return (((int) ch0))
               | (((int) ch1) << 8)
               | (((int) ch2) << 16)
               | (((int) ch3) << 24);
    }
}
