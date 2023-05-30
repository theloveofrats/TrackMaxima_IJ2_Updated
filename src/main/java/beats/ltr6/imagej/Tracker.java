package beats.ltr6.imagej;

import ij.IJ;
import ij.ImageJ;
import ij.ImagePlus;
import ij.gui.Line;
import ij.gui.Overlay;
import ij.gui.PointRoi;
import ij.gui.Roi;
import ij.plugin.ImageCalculator;
import ij.plugin.ZProjector;
import ij.plugin.filter.Analyzer;
import ij.plugin.filter.MaximumFinder;
import ij.plugin.frame.RoiManager;
import ij.plugin.filter.ImageMath;
import ij.process.*;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.apache.commons.lang3.mutable.MutableInt;

import java.awt.*;
import java.util.*;
import java.util.List;

public class Tracker<T extends RealType> implements Runnable{

    public boolean preview    = true;
    public boolean dark       = false;
    public double  threshold  = 16.0;
    public double  blur       = 4.0;
    public boolean dog        = false;
    public int     maxMissing = 2;
    public double  maxSpeed   = 200.0;
    public double  minSpeed   = 0.5;
    public int     minFrames  = 8;
    public boolean med_sub    = false;
    public double minPointSep = 5.0;
    public double speedfraction = 0.4;

    public boolean currentlyTracking = false;
    private ImageCalculator calc = new ImageCalculator();

    private ImagePlus image;
    private ImagePlus ghost;
    private ImageProcessor median;
    private RoiManager rm = new RoiManager(true);
    private MaximumFinder mf = new MaximumFinder();
    Point[] framePoints;
    private List<Track> currentTracks = new ArrayList<>();
    private ArrayList<Track> finalTracks = new ArrayList<>();
    private Map<Point, List<PointClash>> clashes = new LinkedHashMap<Point, List<PointClash>>();
    private HungarianAssigner hungarian = new HungarianAssigner(this);

    private int totalTracks = 0;

    private TrackMaxima tm;
    private int dot_algorithm = 0;


    public Tracker(ImagePlus image, TrackMaxima tm){
        this.image = image;
        this.tm = tm;
        UpdatePreview();
    }

    public ArrayList<Track> GetTracks(){
        finalTracks.sort((t1,t2)->Double.compare(t1.GetID(),t2.GetID()));
        return finalTracks;
    }
    public int NTracks(){
        return finalTracks.size();
    }
    public void SetTracks(List<Track> tracks){

        if(finalTracks==null) finalTracks = new ArrayList<>();
        else finalTracks.clear();

        for(Track track : tracks){
            CommitTrack(track);
        }
        finalTracks.sort((t1,t2)->Integer.compare(t1.GetID(),t2.GetID()));
    }


    public void SetTrackingParameters(boolean dark, double threshold, double blur, boolean dog, boolean med_sub, int maxMissing, double maxSpeed, double minSpeed, double speedfraction, int minFrames, double minSep, int dot_algorithm, boolean preview){
        this.preview = preview;
        this.dark = dark;
        this.threshold = threshold;
        this.blur = blur;
        this.dog = dog;
        this.maxMissing = maxMissing;
        this.maxSpeed = maxSpeed;
        this.minSpeed = minSpeed;
        this.minFrames = minFrames;
        this.med_sub = med_sub;
        this.minPointSep = minSep;
        this.dot_algorithm = dot_algorithm;
        this.speedfraction = speedfraction;

        UpdatePreview();
    }

    private void GhostImage(ImageProcessor ip){

        if(med_sub){
            double upShift = ip.maxValue()-ip.getMax();
            double dnShift = ip.getMin()-ip.minValue();

            double val;

            for(int i=0; i<ip.getWidth(); i++){
                for(int j=0; j<ip.getHeight(); j++){

                    val = (ip.getPixelValue(i,j)+upShift)-(median.getPixelValue(i,j)-dnShift);
                    ip.putPixelValue(i,j,val);
                }
            }
        }

        ip.blurGaussian(blur);
        if(dark) ip.invert();

        if(dog){
            double shift = 0.65*(ip.maxValue()-ip.getMax());
            ImageProcessor ip2 = this.image.getProcessor().duplicate();
            ip2.blurGaussian(1.6*blur);
            double val = 0;
            for(int i=0; i<ip.getWidth(); i++){
                for(int j=0; j<ip.getHeight(); j++){
                    val = ip.getPixelValue(i,j)-ip2.getPixelValue(i,j)+shift;
                    ip.putPixelValue(i,j,val);
                }
            }
        }
    }

    private void GetFrameResults(ImageProcessor ip, int frameNumber){

        ghost.setProcessor(ip);
        //ghost.show();
        IJ.run(this.ghost, "Find Maxima...", "prominence="+Double.toString(threshold)+" strict dark output=[Point Selection]");

        PointRoi points = (PointRoi) ghost.getRoi();
        java.awt.Point[] prept = points.getContainedPoints();
        Point[] frame = new Point[prept.length];

        //IJ.log("Num points in frame:: "+points.getSize()+" ::: "+prept.length);

        for(int i=0; i<frame.length; i++){
            frame[i] = new Point(prept[i].x,prept[i].y,frameNumber);
        }
        if(minPointSep>=2) frame = RemoveClosePoints(frame);

        framePoints = frame;
    }

    private Point[] RemoveClosePoints(Point[] points){

        clashes.clear();

        for(int i=1; i<points.length; i++){
            for(int j=0; j<i; j++){

                if(points[j].SquareDistanceFromPoint(points[i])<minPointSep*minPointSep){

                    // Add for i
                    PointClash clash = new PointClash(points[i],points[j]);
                    List<PointClash> clashList = clashes.get(points[i]);
                    if(clashList==null) {
                        clashList = new ArrayList<PointClash>();
                        clashes.put(points[i], clashList);
                    }
                    clashList.add(clash);

                    // Add for j
                    clashList = clashes.get(points[j]);
                    if(clashList==null) {
                        clashList = new ArrayList<PointClash>();
                        clashes.put(points[j], clashList);
                    }
                    clashList.add(clash);
                }
            }
        }
        if(clashes.size()==0) return points;

        Set<Point> pointSet = new HashSet<Point>();
        for(int i=0; i<points.length; i++) pointSet.add(points[i]);

        if(clashes.size()<3) {
            pointSet.remove(clashes.keySet().toArray()[0]);
        }
        else {
            Set<Point> removeSet = new HashSet<>();
            LinkedList<Map.Entry<Point, List<PointClash>>> sorter = new LinkedList(clashes.entrySet());
            sorter.sort((a, b) -> Integer.compare(a.getValue().size(), b.getValue().size()));
            for(int i=0; i<sorter.size(); i++){
                Point topClasher = sorter.get(i).getKey();
                List<PointClash> topClasherClashes = clashes.get(topClasher);

                boolean added = false;
                for(int j=0; j<topClasherClashes.size()-1; j++){
                    if(!added && !topClasherClashes.get(j).resolved){
                        added = true;
                        removeSet.add(topClasher);
                    }
                    topClasherClashes.get(j).resolved = true;
                }
            }

            pointSet.removeAll(removeSet);
        }

        return pointSet.toArray(new Point[pointSet.size()]);
    }

    public void CommitTrack(Track track){
        if(track.AverageSpeed()<minSpeed) return;
        if(finalTracks.contains(track)) return;
        track.Sort();
        track.InterpolateMissing();
        for(Point pt : track.points){
            pt.track = track;
        }
        if(track.points.size()<minFrames) return;
        finalTracks.add(track);
    }

    private void Reset(){
        image.killRoi();
        image.setOverlay(new Overlay());
        tm.analyser.warningsDone = false;
        this.finalTracks.clear();
        this.currentTracks.clear();
        this.totalTracks = 0;
    }

    public void run(){
        DoTracking();
    }

    private void DoTracking(){

        if(this.currentlyTracking) return;
        this.currentlyTracking = true;
        Reset();
        this.image.lock();
        // Record all point sets

        ImageJ ij = IJ.getInstance();

        for(int i=1; i<=image.getStackSize(); i++) {
            image.setPosition(i);
            ij.getProgressBar().show(i,image.getStackSize());
            ImageProcessor ip = this.image.getProcessor().duplicate();
            GhostImage(ip);
            GetFrameResults(ip, i);
            JoinTheDots(i);
        }

        //Finalise.
        for (Track t : currentTracks) {
            CommitTrack(t);
        }
        currentTracks.clear();

        /*IJ.log("Total tracks:: "+finalTracks.size());
        for(int i=0; i<finalTracks.size(); i++){

            IJ.log("track "+i+":: "+(finalTracks.get(i).points.size()));
        }*/

        this.tm.uiForm.UpdateTrackInformation();
        this.image.unlock();
        this.currentlyTracking = false;
        tm.overlayer.MakeOverlayFromResults();
    }

    public int GetNewID(){
        return ++totalTracks;
    }

    public void CheckTracks(){
          List<Track> removed = new ArrayList<>();

          for(Track track : finalTracks){
              if(track.AverageSpeed()<minSpeed) removed.add(track);
              track.Sort();
              track.InterpolateMissing();
              for(Point pt : track.points){
                  pt.track = track;
              }
              if(track.points.size()<minFrames) removed.add(track);
          }

          finalTracks.removeAll(removed);
    }

    private void JoinTheDots(int frameNum){

        // Try to assign points to pre-existing tracks.
        // These assign points a track, but do not actually add them to the track.
        if(dot_algorithm==0) GreedyJoin();
        else if(dot_algorithm==1) HungarianJoin();

        // Make new cells for unassigned points!
        for(Point p : framePoints){
            if(p.track==null) {
                Track newTrack = new Track(p, ++totalTracks);
                p.track = newTrack;
                this.currentTracks.add(newTrack);
                continue;
            }
            else p.track.AddPoint(p);
        }

        // Check active cells to see if any should be finalised
        for(int j=0; j<currentTracks.size(); j++){
            Track t = currentTracks.get(j);
            if(t.LastFrame()<frameNum-maxMissing){
                currentTracks.remove(t);
                CommitTrack(t);
                j--;
            }
        }
    }

    private Point FindClosestPointToTrack(Track track, Point[] newPoints){
        image.killRoi();
        if(track.points.size()==0) return null;

        double minWeight = Double.MAX_VALUE;

        Point last = track.LastPoint();
        Point out = null;

        for(Point p : newPoints){
            if(GetMatchWeight(track.LastPoint(), p) < minWeight){
                if(p.track==null || GetMatchWeight(track.LastPoint(), p) < GetMatchWeight(p.track.LastPoint(), p)){
                    out = p;
                    minWeight = last.SquareSpeedFromPoint(p);
                }
            }
        }

        return out;
    }

    private void findUnassignedPointForTrack(Track track){
        Point pt = FindClosestPointToTrack(track, framePoints);

        if(pt!=null){
            Track oldTrack = pt.track;
            pt.track = track;
            if(oldTrack!=null){
                findUnassignedPointForTrack(oldTrack);
            }
        }
    }

    private void GreedyJoin(){
        // First try to assign points to pre-existing cells
        for (Track track: currentTracks) {
            findUnassignedPointForTrack(track);
        }
        for(Point pt : framePoints){
            if(pt.track!=null){
                if(pt.SquareSpeedFromPoint(pt.track.LastPoint())>maxSpeed*maxSpeed){
                    pt.track = null;
                }
            }
        }
    }

    public double GetMatchWeight(Point trackPoint, Point newPoint){
        int x_app = trackPoint.x;
        int y_app = trackPoint.y;

        Point prevPoint = trackPoint.Prev();

        double dt = newPoint.t-trackPoint.t;

        if(speedfraction>0.01 && prevPoint!=null){
            x_app+=speedfraction*(trackPoint.x-prevPoint.x);
            y_app+=speedfraction*(trackPoint.y-prevPoint.y);
        }

        return ((newPoint.x-x_app)*(newPoint.x-x_app)+(newPoint.y-y_app)*(newPoint.y-y_app))/(dt*dt);
    }

    private void HungarianJoin(){
        this.hungarian.DoAssignment(currentTracks, framePoints);
    }


    public void UpdatePreview(){
        image.killRoi();
        if(!preview) return;
        if(currentlyTracking) return;
        
        if(med_sub){
            if(median==null || median.getWidth() != image.getWidth() || median.getHeight()!=image.getHeight()) {
                ZProjector projector = new ZProjector();

                projector.setImage(image);
                projector.setMethod(ZProjector.MEDIAN_METHOD);
                projector.doProjection();
                median = projector.getProjection().getProcessor();
            }
        }

        ImageProcessor ip = image.getProcessor().duplicate();

        GhostImage(ip);

        // Reset ghost image!
        if(this.ghost==null) this.ghost = new ImagePlus("Ghost",ip);
        else this.ghost.setProcessor(ip);
        if(rm.getCount()>0) rm.runCommand(this.ghost,"Delete");

        IJ.run(this.ghost, "Find Maxima...", "prominence="+Double.toString(threshold)+" strict dark output=[Point Selection]");
        rm.addRoi(this.ghost.getRoi());
        rm.select(this.image, 0);
        rm.select(0);
    }
}
