package malgnsoft.util;

import java.io.File;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;
import javax.imageio.ImageIO;


public class Resizer {

	public Resizer(String srcImgName, String dstImgName, float scaleX, float scaleY, String imgOutputFormat) {
		BufferedImage srcImg = null, dstImg = null;

		try {
			srcImg = ImageIO.read(new File(srcImgName));
			dstImg = resizeImage(srcImg, scaleX, scaleY);
			ImageIO.write(dstImg, imgOutputFormat, new File(dstImgName));
		}
		catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	public BufferedImage resizeImage(BufferedImage srcImg, float scaleX, float scaleY) {
		// get image dimensions
		int srcW = srcImg.getWidth();
		int srcH = srcImg.getHeight();
		int dstW = (int) (srcW * scaleX);
		int dstH = (int) (srcH * scaleY);

		// Get data structures
		BufferedImage dstImg = new BufferedImage(dstW, dstH, srcImg.getType());
		Raster srcRaster = srcImg.getRaster();
		WritableRaster dstRaster = dstImg.getRaster();
		double[] tmpPix = {0, 0, 0};

		// resize image
		for (int y=0; y<dstH; y++) {
			for (int x=0; x<dstW; x++) {
				int xPos = (int) (x * (1/scaleX)); // (find corresponding src x pos)
				int yPos = (int) (y * (1/scaleY)); // (find corresponding src y pos)
				tmpPix = srcRaster.getPixel(xPos, yPos, tmpPix);
				dstRaster.setPixel(x, y, tmpPix);
			}
		}

		return dstImg;
	}


	/**
	* Application starting point.
	* @param argv <p>argv[0] --> the source image name</p>
	* <p>argv[1] --> the destination image name</p>
	* <p>argv[2] --> x scaling factor</p>
	* <p>argv[3] --> y scaling factor</p>
	*/
	/*
	public static void main(String[] argv) {
		if (argv.length == 5) {
			float scaleX = Float.parseFloat(argv[2]);
			float scaleY = Float.parseFloat(argv[3]);
			Resizer imageResizer = new eResizer(argv[0], argv[1],
			scaleX, scaleY,
			argv[4]);
		}
		else {
			System.err.println("usage: java Resizer " +
			"<srcImg> <dstImg> <scaleX> <scaleY> <output
			format>");
			System.exit(1);
		}

	}
	*/

}



