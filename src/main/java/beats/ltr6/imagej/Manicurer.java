package beats.ltr6.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.gui.*;

import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.List;

public class Manicurer implements MouseListener {

    private ImagePlus image;
    private TrackMaxima tm;

    private Track newHandTrack;
    private Point mergeFirstPoint;
    private Point mergeSecondPoint;
    private Point cutPoint;
    private List<Roi> newTrackROI = new ArrayList<Roi>();

    public enum ControlState {NONE, DELETE, MERGE, TRACK,CUT_BEFORE,CUT_AFTER};

    public ControlState currentState = ControlState.NONE;

    public Manicurer(TrackMaxima tm, ImagePlus img){
        this.image = img;
        this.tm = tm;
    }

    private Point IdentifyPointFromClick(int x, int y, int frame){

        List<Track> tracks = tm.tracker.GetTracks();
        double currentMinimum = 40d;
        Point currentSelected = null;

        for(Track track : tracks){
            for(int i=0; i<=frame; i++){
                if(track.points.size()<=i) break;
                Point pt = track.points.get(i);
                if(pt.t==frame){
                    double squid = (pt.x-x)*(pt.x-x) + (pt.y-y)*(pt.y-y);
                    if(squid<currentMinimum*currentMinimum){
                        currentMinimum = Math.sqrt(squid);
                        currentSelected = pt;
                    }
                    break;
                }
            }
        }

        return  currentSelected;
    }

    private boolean HasThisAsMouseListner(){
        MouseListener[] mls = image.getWindow().getCanvas().getMouseListeners();
        for(int i=0; i<mls.length; i++){
            if(mls[i]==this) return true;
        }
        return false;
    }

    public void SetListening(boolean state){

        ImageCanvas canvas = image.getWindow().getCanvas();

        if(state){
            canvas.removeMouseListener(canvas);
            image.killRoi();
            IJ.setTool(Toolbar.HAND);
            image.getWindow().requestFocus();
            if(!HasThisAsMouseListner()) image.getWindow().getCanvas().addMouseListener(this);
        }
        else{
            canvas.addMouseListener(canvas);
            currentState = ControlState.NONE;
            if(HasThisAsMouseListner()) image.getWindow().getCanvas().removeMouseListener(this);
        }
    }

    public void DeleteTrack(int x, int y, int frame){
        Point pt = IdentifyPointFromClick(x,y,frame);
        if(pt==null) return;
        IJ.log("Deleting track "+pt.track.GetID());
        tm.tracker.GetTracks().remove(pt.track);
        tm.overlayer.MakeOverlayFromResults();
    }

    private void MergePoints(){
        Track track1 = mergeFirstPoint.track;
        Track track2 = mergeSecondPoint.track;

        track1.points.removeIf((p)->p.t>mergeFirstPoint.t);
        track2.points.removeIf((p)->p.t<mergeSecondPoint.t);

        track1.points.addAll(track2.points);

        IJ.log("Total points now "+track1.points.size());

        track2.points.clear();
        tm.tracker.GetTracks().remove(track2);
        track1.InterpolateMissing();

        SetState(ControlState.NONE);
    }

    private void CutBefore(int x, int y, int frame){

        cutPoint = IdentifyPointFromClick(x,y,frame);
        if(cutPoint==null) return;

        Track track = cutPoint.track;

        track.points.removeIf((p)->p.t<cutPoint.t);

        if(track.points.size()<tm.tracker.minFrames || track.AverageSpeed()<tm.tracker.minSpeed) tm.tracker.GetTracks().remove(track);
        tm.overlayer.MakeOverlayFromResults();
    }
    private void CutAfter(int x, int y, int frame){

        cutPoint = IdentifyPointFromClick(x,y,frame);
        if(cutPoint==null) return;

        Track track = cutPoint.track;

        track.points.removeIf((p)->p.t>cutPoint.t);

        if(track.points.size()<tm.tracker.minFrames || track.AverageSpeed()<tm.tracker.minSpeed) tm.tracker.GetTracks().remove(track);
        tm.overlayer.MakeOverlayFromResults();
    }

    private void IdentifyTracksToMerge(int x, int y, int frame){
        Point pt = IdentifyPointFromClick(x,y,frame);
        if(pt==null) return;

        if(mergeFirstPoint==null) {
            mergeFirstPoint = pt;
            OvalRoi merge1 = new OvalRoi(pt.x-2, pt.y-2, 4,4);
            merge1.setFillColor(Color.yellow);
            newTrackROI.add(merge1);
            image.getOverlay().add(merge1);
            image.repaintWindow();
            return;
        }
        if(mergeSecondPoint==null){
            if(pt.t<=mergeFirstPoint.t){
                IJ.log("Error- second point must be in a later frame than first point");
                return;
            }
            else if(pt.track == mergeFirstPoint.track){
                IJ.log("Tried to merge track with itself. ignoring.");
                return;
            }
            mergeSecondPoint = pt;
            MergePoints();
        }
    }

    private int[] FindLocalMaximum(int ms_x, int ms_y){

        int RAD = 5;

        int xMin = Math.max(ms_x-RAD,0);
        int yMin = Math.max(ms_y-RAD,0);
        int xMax = Math.min(image.getWidth()-1, ms_x+RAD);
        int yMax = Math.min(image.getHeight()-1, ms_y+RAD);

        boolean dark = tm.tracker.dark;

        int x_out = -1;
        int y_out = -1;
        double max = dark ? Double.MAX_VALUE : Double.MIN_VALUE;

        for(int i=xMin; i<=xMax; i++){
            for(int j=yMin; j<=yMax; j++){
                if(dark && image.getProcessor().getPixelValue(i,j)<max){
                    max = image.getProcessor().getPixelValue(i,j);
                    x_out = i;
                    y_out = j;
                }
                else if(!dark && image.getProcessor().getPixelValue(i,j)>max){
                    max = image.getProcessor().getPixelValue(i,j);
                    x_out = i;
                    y_out = j;
                }
            }
        }
        return new int[]{x_out,y_out};
    }

    public void SetState(ControlState newState){
        currentState = newState;
        if(newState==ControlState.NONE){
            if(newHandTrack!=null && newHandTrack.AverageSpeed()>tm.tracker.minSpeed && newHandTrack.points.size()>tm.tracker.minFrames){
                tm.tracker.CommitTrack(newHandTrack);
            }
            SetListening(false);

            for(Roi roi : this.newTrackROI){
                image.getOverlay().remove(roi);
            }
            this.newTrackROI.clear();
            tm.tracker.CheckTracks();
        }
        else{
            SetListening(true);
        }
        mergeFirstPoint = null;
        mergeSecondPoint = null;
        newHandTrack = null;

        tm.uiForm.UpdateTrackInformation();
        tm.overlayer.MakeOverlayFromResults();
    }

    private void AddTrackPoint(int x, int y, int frame){

        int[] coords = FindLocalMaximum(x,y);

        Point pt = new Point(coords[0],coords[1],frame);

        if(newHandTrack==null) {
            newHandTrack = new Track(pt, tm.tracker.GetNewID());
        }
        else {
            Point replace = null;
            int idx = -1;
            for (int i = 0; i < newHandTrack.points.size(); i++) {
                Point p = newHandTrack.points.get(i);
                if (p.t == pt.t) {
                    replace = p;
                    idx = i;
                    break;
                }
            }
            if (replace != null) {
                newHandTrack.points.remove(replace);
                newHandTrack.points.add(idx, pt);
            } else {
                newHandTrack.points.add(pt);

            }
        }
        pt.track = newHandTrack;
        if (image.getFrame() != image.getNFrames()) image.setSlice(image.getFrame() + 1);
        RefreshHandTrackROI(pt);
    }

    private void RefreshHandTrackROI(Point newPoint){

        OvalRoi oval = new OvalRoi(newPoint.x-2,newPoint.y-2,4,4);
        oval.setFillColor(Color.cyan);
        this.newTrackROI.add(oval);

        if(newHandTrack.points.size()>1){
            Line line = new Line(newPoint.Prev().x,newPoint.Prev().y,newPoint.x,newPoint.y);
            line.setStrokeColor(Color.cyan);
            line.setStrokeWidth(1.5f);
            this.newTrackROI.add(line);
        }
        for(Roi roi : this.newTrackROI) {
            image.getOverlay().add(roi);
        }
    }

    @Override
    public void mouseClicked(MouseEvent e) {

        int x = image.getWindow().getCanvas().offScreenX(e.getX());
        int y = image.getWindow().getCanvas().offScreenY(e.getY());

        if(e.getButton()==MouseEvent.BUTTON1){
            if(currentState==ControlState.NONE) SetListening(false);
            if(currentState==ControlState.DELETE){
                DeleteTrack(x,y,image.getFrame());
            }
            if(currentState==ControlState.MERGE){
                IdentifyTracksToMerge(x,y,image.getFrame());
            }
            if(currentState==ControlState.CUT_BEFORE){
                CutBefore(x,y,image.getFrame());
            }
            if(currentState==ControlState.CUT_AFTER){
                CutAfter(x,y,image.getFrame());
            }
            if(currentState==ControlState.TRACK){
                AddTrackPoint(x,y,image.getFrame());
            }
        }
        else if(e.getButton()==MouseEvent.BUTTON2){
            SetState(ControlState.NONE);
        }
    }

    @Override
    public void mouseEntered(MouseEvent e) {

    }

    @Override
    public void mouseExited(MouseEvent e) {

    }

    @Override
    public void mousePressed(MouseEvent e) {

    }

    @Override
    public void mouseReleased(MouseEvent e) {

    }
}
