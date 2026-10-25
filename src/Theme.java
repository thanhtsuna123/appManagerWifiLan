import java.awt.Color;
import java.awt.Font;

public class Theme {

    public static final int THEME_LIGHT = 0;
    public static final int THEME_DARK = 1;
    public static final int THEME_BLUE = 2;
    public static final int THEME_GREEN = 3;
    public static final int THEME_PURPLE = 4;

    public static int currentTheme = THEME_LIGHT;

    public static Color getNen() {
        switch (currentTheme) {
            case THEME_DARK:   return new Color(30, 30, 30);
            case THEME_BLUE:   return new Color(220, 235, 252);
            case THEME_GREEN:  return new Color(225, 245, 230);
            case THEME_PURPLE: return new Color(240, 230, 250);
            case THEME_LIGHT:
            default:           return new Color(245, 247, 250);
        }
    }

    public static Color getPanel() {
        switch (currentTheme) {
            case THEME_DARK:   return new Color(45, 45, 48);
            case THEME_BLUE:   return new Color(200, 220, 245);
            case THEME_GREEN:  return new Color(205, 235, 215);
            case THEME_PURPLE: return new Color(225, 215, 240);
            case THEME_LIGHT:
            default:           return Color.WHITE;
        }
    }

    public static Color getChu() {
        if (currentTheme == THEME_DARK) {
            return Color.WHITE;
        }
        return new Color(30, 30, 30);
    }

    // Cac mau nut bam
    public static final Color MAU_XANH_LA  = new Color(46, 125, 50);
    public static final Color MAU_DO       = new Color(211, 47, 47);
    public static final Color MAU_XANH_DUONG = new Color(33, 150, 243);
    public static final Color MAU_CAM      = new Color(245, 124, 0);

    public static final Font FONT_TIEU_DE = new Font("Arial", Font.BOLD, 18);
    public static final Font FONT_CHU     = new Font("Arial", Font.PLAIN, 13);
    public static final Font FONT_NUT     = new Font("Arial", Font.BOLD, 12);
}