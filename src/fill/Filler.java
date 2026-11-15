package fill;

import raster.Raster;

public abstract class Filler {

    protected final Raster raster;
    protected PatternFill pattern;

    public Filler(Raster raster, PatternFill pattern) {
        this.raster = raster;
        this.pattern = pattern;
    }

    public void setPattern(PatternFill pattern) {
        this.pattern = pattern;
    }
}
