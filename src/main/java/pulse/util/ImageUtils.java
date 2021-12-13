package pulse.util;

import static java.awt.Image.SCALE_SMOOTH;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;

import pulse.ui.Launcher;

public class ImageUtils {

    private ImageUtils() {
        // intentionally blank
    }

    public static ImageIcon loadIcon(String path, int iconSize) {
        var imageIcon = new ImageIcon(Launcher.class.getResource("/images/" + path)); // load the image to a
        // imageIcon
        var image = imageIcon.getImage(); // transform it
        var newimg = image.getScaledInstance(iconSize, iconSize, SCALE_SMOOTH); // scale it the smooth way
        return new ImageIcon(newimg); // transform it back
    }

    public static ImageIcon loadIcon(String path, int iconSize, Color clr) {
        var icon = loadIcon(path, iconSize);
        return dye(icon, clr);
    }

    /**
     * Credit to Marco13
     * (https://stackoverflow.com/questions/21382966/colorize-a-picture-in-java)
     */
    public static BufferedImage dye(BufferedImage image, Color color) {
        int w = image.getWidth();
        int h = image.getHeight();
        BufferedImage dyed = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = dyed.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setComposite(AlphaComposite.SrcAtop);
        g.setColor(color);
        g.fillRect(0, 0, w, h);
        g.dispose();
        return dyed;
    }

    /**
     * Credit to Werner Kvalem Vesterås
     * (https://stackoverflow.com/questions/15053214/converting-an-imageicon-to-a-bufferedimage)
     */
    public static ImageIcon dye(ImageIcon icon, Color color) {
        var bi = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics g = bi.createGraphics();
        // paint the Icon to the BufferedImage.
        icon.paintIcon(null, g, 0, 0);
        g.dispose();
        var dyedImage = dye(bi, color);
        return new ImageIcon(dyedImage);
    }

    /**
     * Credit to bmauter
     * (https://stackoverflow.com/questions/19398238/how-to-mix-two-int-colors-correctly)
     */
    public static Color blend(final Color c1, final Color c2, float ratio) {
        if (ratio > 1f) {
            ratio = 1f;
        } else if (ratio < 0f) {
            ratio = 0f;
        }
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

        int a = (int) ((a1 * iRatio) + (a2 * ratio));
        int r = (int) ((r1 * iRatio) + (r2 * ratio));
        int g = (int) ((g1 * iRatio) + (g2 * ratio));
        int b = (int) ((b1 * iRatio) + (b2 * ratio));

        return new Color(a << 24 | r << 16 | g << 8 | b);
    }

}
