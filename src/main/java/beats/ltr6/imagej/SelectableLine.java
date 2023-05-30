package beats.ltr6.imagej;

import ij.gui.Line;

import java.awt.*;

public class SelectableLine extends Line {

    private Track track;
    private Color cachedColor;
    public Track Track(){
        return track;
    }

    public SelectableLine(int x1, int y1, int x2, int y2, Track track){
        super(x1,y1,x2,y2);
        this.track = track;
    }

    public void Highlight(){
        if(this.strokeColor!=Color.CYAN) this.cachedColor = this.strokeColor;
        this.setStrokeColor(Color.CYAN);
    }
    public void Unhighlight(){
        if(this.cachedColor!=null) this.setStrokeColor(cachedColor);
    }
}
