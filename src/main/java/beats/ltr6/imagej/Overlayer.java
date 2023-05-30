package beats.ltr6.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.*;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Overlayer {

    private double directionCutoff = 0.1;

    private Tracker tracker;
    private ImagePlus image;

    boolean lines = false;
    boolean circles = true;
    boolean masks = false;
    boolean direction = true;

    public double fade = 0;
    public double angle = 0;
    private double radAngle;
    public int calcWindowSize = 5;

    public boolean pointTarget = false;
    public int[]   targetPoint;

    int calcWindowType = 0;
    int overlaySize = 1;

    Color pos;
    Color neg;
    Color neu;


    public Overlayer(ImagePlus img, Tracker tracker){
        this.tracker = tracker;
        this.image = img;
    }


    public void SetOverlayerParameters(boolean lines, boolean circles,boolean masks, boolean direction, double fade, double angle, int overlaySize, int calcWindowSize, int calcWindowType, Color pos, Color neg, Color neu){

        this.lines = lines;
        this.circles = circles;
        this.masks = masks;
        this.fade = fade;
        this.angle = angle;
        this.radAngle=  Math.toRadians(angle);
        this.overlaySize = overlaySize+1;
        this.calcWindowSize = calcWindowSize;
        this.calcWindowType = calcWindowType;
        this.direction = direction;
        this.pos = pos;
        this.neg = neg;
        this.neu = neu;
    }

    private double GetCosTheta(Point start, Point end){

        if(start.x==end.x && start.y==end.y) return 0;

        double dx = end.x-start.x;
        double dy = end.y-start.y;

        double effX = 0;

        if(pointTarget && targetPoint!=null){

            double uVecX = targetPoint[0]-start.x;
            double uVecY = targetPoint[1]-start.y;
            double vecR  = Math.sqrt(uVecX*uVecX+uVecY*uVecY);
            double cellR = Math.sqrt(dx*dx+dy*dy);

            uVecX/=vecR;
            uVecY/=vecR;

            dx/=cellR;
            dy/=cellR;

            uVecX*=dx;
            uVecY*=dy;

            effX = uVecX+uVecY;
        }
        else {
            effX = dx * Math.cos(radAngle) + dy * Math.sin(radAngle);
        }
        return effX/Math.sqrt(dx*dx+dy*dy);
    }

    public void MakeOverlayFromResults(){
        
        Overlay overlay = new Overlay();
        ImageJ ij = IJ.getInstance();
        List<Track> tracks = tracker.GetTracks();

        double baseSize = 1.5*overlaySize;
        int maxSteps = (int) Math.ceil(fade==0 ? 10000 : 1.0/fade);

        if(lines){
            for (int n=0; n<tracks.size(); n++) {
                Track t = tracks.get(n);
                for(int i=t.points.size()-1; i>0; i--) {

                    Point basePoint = t.points.get(i);

                    int stepsBack = Math.min(maxSteps, i-1);

                    for(int j=0; j<stepsBack-1; j++) {
                        Point jthPoint = t.points.get(i-j);
                        Point jm1thPoint = t.points.get(i-(j+1));
                        double lineFade = fade*(basePoint.t-jthPoint.t);

                        if(lineFade>=1) break;

                        double clrscore = 0;
                        if(direction) clrscore = GetCosTheta(jm1thPoint, jthPoint);
                        else clrscore = Math.sqrt(jthPoint.SquareSpeedFromPoint(jm1thPoint))/(0.9*tracker.maxSpeed);

                        Color clr = neu;

                        if(clrscore>directionCutoff) clr = pos;
                        if(clrscore<-directionCutoff) clr = neg;

                        clr = new Color(clr.getRed(),clr.getGreen(),clr.getBlue(),neu.getAlpha()-(int) (lineFade*neu.getAlpha()));

                        Line line = new Line(jm1thPoint.x,jm1thPoint.y,jthPoint.x,jthPoint.y);
                        line.setStrokeWidth(baseSize);
                        line.setStrokeColor(clr);
                        line.setPosition(basePoint.t);
                        overlay.add(line);
                    }
                }
            }
        }

        if(circles) {
            for (int n = 0; n < tracks.size(); n++) {
                Track t = tracks.get(n);
                for (int i = 0; i < t.points.size()-1; i++) {

                    int min = Math.max(0, i - calcWindowSize);
                    int max = Math.min(t.points.size() - 2, i + calcWindowSize);

                    Color clr, clr2;
                    clr = neu;

                    double score = GetWindowedScore(t.points.subList(min, max), i);

                    if (score > directionCutoff) clr = pos;
                    if (score < -directionCutoff) clr = neg;

                    clr2 = new Color(clr.getRed(), clr.getGreen(), clr.getBlue(), (int) (clr.getAlpha()*0.24));

                    double speed = t.points.get(i+1).SquareSpeedFromPoint(t.points.get(i))/(0.81*tracker.maxSpeed*tracker.maxSpeed);
                    double dR = 0.3*baseSize * 8.0d;
                    if(!lines) dR*=Math.max(1.0,5.0*speed);

                    OvalRoi oval1 = new OvalRoi(t.points.get(i).x-dR,t.points.get(i).y-dR,2.0*dR,2.0*dR);
                    oval1.setFillColor(clr2);
                    oval1.setPosition(t.points.get(i).t);
                    overlay.add(oval1);

                    OvalRoi oval2 = new OvalRoi(t.points.get(i).x-dR,t.points.get(i).y-dR,2.0*dR,2.0*dR);
                    oval2.setStrokeWidth(baseSize);
                    oval2.setStrokeColor(clr);
                    oval2.setPosition(t.points.get(i).t);
                    overlay.add(oval2);
                }
            }
        }

        if(masks) {
            for (int n = 0; n < tracks.size(); n++) {
                Track t = tracks.get(n);
                for (int i = 0; i < t.points.size()-1; i++) {

                    int min = Math.max(0, i - calcWindowSize);
                    int max = Math.min(t.points.size() - 2, i + calcWindowSize);

                    Color clr;
                    clr = neu;

                    double score = GetWindowedScore(t.points.subList(min, max), i);

                    if (score > directionCutoff) clr = pos;
                    if (score < -directionCutoff) clr = neg;

                    IJ.doWand(this.image, t.points.get(i).x, t.points.get(i).y, tracker.threshold, "8-connected");
                    Roi roi = image.getRoi();
                    roi.setFillColor(clr);
                    roi.setPosition(t.points.get(i).t);
                    overlay.add(roi);
                }
            }
        }
        /*
                Point prev = t.points.get(i-1);
                Point curr = t.points.get(i);
                int frame = curr.t;

                Line line = new Line(prev.x,prev.y,curr.x,curr.y);
                line.setStrokeWidth(5f);
                line.setStrokeColor(Color.red);
                line.setPosition(frame);
                overlay.add(line);
            }
        } */

        image.setOverlay(overlay);
        image.updateAndDraw();
    }

    public void DrawNumbers(){

        Overlay overlay = new Overlay();

        for(int n=0; n<tracker.GetTracks().size(); n++){
            Track track = (Track) tracker.GetTracks().get(n);
            for(int i=0; i<track.points.size(); i++) {
                Point pt = track.points.get(i);
                TextRoi txt = new TextRoi(pt.x,pt.y,Integer.toString(track.GetID()));
                txt.setPosition(pt.t);
                overlay.add(txt);
            }
        }
        image.setOverlay(overlay);
    }

    private double GetWindowedScore(List<Point> points, int midpoint){

        double[] weights = new double[points.size()];
        double ttlW = 0;
        double score = 0;

        for(int i=0; i<points.size()-1; i++){
            if(calcWindowType==0) weights[i]      = 1;
            else if(calcWindowType==1) weights[i] = Math.exp(-((i-(midpoint-0.5))*(i-(midpoint-0.5)))/(0.5*calcWindowSize*calcWindowSize));
            else if(calcWindowType==2) weights[i] = Math.min(1,0.95*(1+Math.cos(Math.PI*(i-(midpoint-0.5))/calcWindowSize)));

            ttlW+= weights[i];
        }

        for(int i=0; i<points.size()-1; i++){
            if(direction)  score += (weights[i]/ttlW) * GetCosTheta(points.get(i),points.get(i+1));
            else           score += (weights[i]/ttlW) * Math.sqrt(points.get(i).SquareSpeedFromPoint(points.get(i+1)))/(0.9*tracker.maxSpeed);
        }

        return score;
    }
}
