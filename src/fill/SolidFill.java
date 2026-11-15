package fill;

public class SolidFill implements PatternFill {

    private final int color;

    public SolidFill(int color) {
        this.color = color;
    }

    @Override
    public int getColor(int x, int y) {
        return color;
    }
}
