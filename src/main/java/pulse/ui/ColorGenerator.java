package pulse.ui;

import java.awt.Color;
import static java.awt.Color.BLUE;
import static java.awt.Color.GREEN;
import static java.awt.Color.RED;
import java.util.ArrayList;

public class ColorGenerator {

    private Color a, b, c;

    public ColorGenerator() {
        a = RED;
        b = GREEN;
        c = BLUE;
    }

    public Color[] random(int number) {
        var list = new ArrayList<Color>();
        for (int i = 0; i < number; i++) {
            list.add(sample(i / (double) (number - 1)));
        }
        //Collections.shuffle(list);
        return list.toArray(new Color[list.size()]);
    }

    public Color sample(double seed) {
        return seed < 0.5
                ? mix(a, b, (float) (seed * 2))
                : mix(b, c, (float) ((seed - 0.5) * 2));
    }

    private static Color mix(Color a, Color b, float ratio) {
        float[] aRgb = a.getRGBComponents(null);
        float[] bRgb = b.getRGBComponents(null);
        float[] cRgb = new float[3];
        for (int i = 0; i < cRgb.length; i++) {
            cRgb[i] = aRgb[i] * (1.0f - ratio) + bRgb[i] * ratio;
        }
        return new Color(cRgb[0], cRgb[1], cRgb[2]);
    }

}
