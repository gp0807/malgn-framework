package malgnsoft.util;

import java.awt.Container;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.BufferedOutputStream;
import java.io.FileOutputStream;

import com.sun.image.codec.jpeg.JPEGCodec;
import com.sun.image.codec.jpeg.JPEGEncodeParam;
import com.sun.image.codec.jpeg.JPEGImageEncoder;

import java.io.*;
import java.util.Hashtable;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.awt.Image;
import javax.swing.ImageIcon;

public class Thumbnail {

	public Thumbnail(String imgFilePath, String thumbPath, int thumbWidth) throws Exception {
		resize(imgFilePath, thumbPath, thumbWidth, 10000);
	}
	public Thumbnail(String imgFilePath, String thumbPath, int thumbWidth, int thumbHeight) throws Exception {
		resize(imgFilePath, thumbPath, thumbWidth, thumbHeight);
	}
	public void resize(String imgFilePath, String thumbPath, int thumbWidth, int thumbHeight) throws Exception {
		Image image = Toolkit.getDefaultToolkit().getImage(imgFilePath);
		MediaTracker mediaTracker = new MediaTracker(new Container());
		mediaTracker.addImage(image, 0);
		mediaTracker.waitForID(0);
		int imageWidth = image.getWidth(null);
		int imageHeight = image.getHeight(null);

		if(imageWidth <= thumbWidth) thumbWidth = imageWidth;

		double thumbRatio = (double)thumbWidth / (double)thumbHeight;
		double imageRatio = (double)imageWidth / (double)imageHeight;
		if (thumbRatio < imageRatio) {
			thumbHeight = (int)(thumbWidth / imageRatio);
		} else {
			thumbWidth = (int)(thumbHeight * imageRatio);
		}

		BufferedImage thumbImage = new BufferedImage(thumbWidth, thumbHeight, BufferedImage.TYPE_INT_RGB);
		Graphics2D graphics2D = thumbImage.createGraphics();
		graphics2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
		graphics2D.drawImage(image, 0, 0, thumbWidth, thumbHeight, null);
		BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(thumbPath));
		JPEGImageEncoder encoder = JPEGCodec.createJPEGEncoder(out);
		JPEGEncodeParam param = encoder.getDefaultJPEGEncodeParam(thumbImage);
		int quality = 100;
		param.setQuality((float)quality / 100.0f, false);
		encoder.setJPEGEncodeParam(param);
		encoder.encode(thumbImage);
		out.close(); 
	}

	public Hashtable<String, Integer> getImageSize(String path, int maxWidth, int maxHeight) {
		Hashtable<String, Integer> imgsize = new Hashtable<String, Integer>();
		try {
			File file = new File(path);
			BufferedImage bi = ImageIO.read(file);
			int width = bi.getWidth(); 
			int height = bi.getHeight();
			int resizeWidth = width;
			int resizeHeight = height;
			if(width > maxWidth || height > maxHeight) {
				if(width > height) {
					resizeWidth = maxWidth;
					resizeHeight = (int)Math.round((height * resizeWidth * 1.0) / width);
				} else {
					resizeHeight = maxHeight;
					resizeWidth = (int)Math.round((width * resizeHeight * 1.0) / height);
				}
			} else {
				resizeWidth = width;
				resizeHeight = height;
			}
			imgsize.put("width", new Integer(width));
			imgsize.put("height", new Integer(height));
			imgsize.put("resize_width", new Integer(resizeWidth));
			imgsize.put("resize_height", new Integer(resizeHeight));
		} catch(Exception e) {
			imgsize.put("width", new Integer(maxWidth));
			imgsize.put("height", new Integer(maxHeight));
			imgsize.put("resize_width", new Integer(maxWidth));
			imgsize.put("resize_height", new Integer(maxHeight));
		}
		return imgsize;
	}

}
