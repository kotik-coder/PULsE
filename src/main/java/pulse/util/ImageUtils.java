package pulse.util;

import static java.awt.Image.SCALE_SMOOTH;

import java.awt.Color;

import javax.swing.ImageIcon;

import pulse.ui.Launcher;

public class ImageUtils {

	private ImageUtils() {
		//intentionally blank
	}
	
	public static ImageIcon loadIcon(String path, int iconSize) {
		var imageIcon = new ImageIcon(Launcher.class.getResource("/images/" + path)); // load the image to a
																						// imageIcon
		var image = imageIcon.getImage(); // transform it
		var newimg = image.getScaledInstance(iconSize, iconSize, SCALE_SMOOTH); // scale it the smooth way
		return new ImageIcon(newimg); // transform it back
	}
	
	public static Color blend( final Color c1, final Color c2, float ratio ) {
	    if ( ratio > 1f ) ratio = 1f;
	    else if ( ratio < 0f ) ratio = 0f;
	    float iRatio = 1.0f - ratio;

	    int i1 = c1.getRGB();
	    int i2 = c2.getRGB();

	    int a1 = (i1 >> 24 & 0xff);
	    int r1 = ((i1 & 0xff0000) >> 16);
	    int g1 = ((i1 & 0xff00) >> 8);
	    int b1 = (i1 & 0xff);

	    int a2 = (i2 >> 24 & 0xff);
	    int r2 = ((i2 & 0xff0000) >> 16);
	    int g2 = ((i2 & 0xff00) >> 8);
	    int b2 = (i2 & 0xff);

	    int a = (int)((a1 * iRatio) + (a2 * ratio));
	    int r = (int)((r1 * iRatio) + (r2 * ratio));
	    int g = (int)((g1 * iRatio) + (g2 * ratio));
	    int b = (int)((b1 * iRatio) + (b2 * ratio));

	    return new Color( a << 24 | r << 16 | g << 8 | b );
	}
	
}