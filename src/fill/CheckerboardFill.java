package fill;

public class CheckerboardFill implements PatternFill {

    private final int firstColor, secondColor;
    private final int size;

    public CheckerboardFill(int firstColor, int secondColor, int size) {
        this.firstColor = firstColor;
        this.secondColor = secondColor;
        this.size = size;
    }

    @Override
    public int getColor(int x, int y) {
        int i = Math.floorMod(x, size * 2) / size;
        int j = Math.floorMod(y, size * 2) / size;
        return i == j ? firstColor : secondColor;
    }
}
