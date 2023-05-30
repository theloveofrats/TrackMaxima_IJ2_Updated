package beats.ltr6.imagej;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.gui.*;
import ij.measure.Calibration;
import ij.measure.CurveFitter;
import ij.measure.ResultsTable;
import ij.process.ByteProcessor;
import ij.process.FloatProcessor;
import ij.process.ImageProcessor;
import ij.text.TextWindow;

import java.awt.*;
import java.awt.event.WindowEvent;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

public class Analyser {


    // Summary statistics
    public boolean cos_theta      = true;
    public boolean speed          = true;
    public boolean ci             = true;
    public boolean directedness   = true;
    public boolean projectedSpeed = true;

    public boolean transitionMatrix = true;
    public boolean msd = true;
    public boolean warningsDone = false;

    private TrackMaxima tm;

    public Analyser(TrackMaxima tm){
        this.tm = tm;
    }

    public void UpdateFields(boolean cos_theta,boolean speed, boolean ci, boolean directedness, boolean projectedSpeed, boolean transitionMatrix, boolean msd){
        this.cos_theta = cos_theta;
        this.speed = speed;
        this.ci = ci;
        this.directedness = directedness;
        this.projectedSpeed = projectedSpeed;
        this.msd = msd;
        this.transitionMatrix = transitionMatrix;
    }

    private double[] ApplyWindowFilter(double[] input){

        double[] output = new double[input.length];
        int type = tm.overlayer.calcWindowType;
        int size = tm.overlayer.calcWindowSize;

        for(int i=0; i<input.length; i++){

            int min = Math.max(0,i-size);
            int max = Math.min(input.length-1, i+size);

            double[] weights = new double[max+1-min];
            double ttl = 0;

            for(int j=min; j<=max; j++){

                int r = (j-i);
                if(type==0) weights[j-min]      = 1;
                else if(type==1) weights[j-min] = Math.exp(-(r*r)/(0.5*size*size));
                else if(type==2) weights[j-min] = Math.min(1,0.95*(1+Math.cos(Math.PI*r/size)));
                ttl+= weights[j-min];
            }
            for(int j=0; j<weights.length; j++){
                weights[j]/=ttl;
            }
            for(int j=min; j<=max; j++){
                output[i]+=weights[j-min]*input[j];
            }
        }
        return output;
    }

    private ResultsTable GetSummaryTable() {
        ResultsTable summary;
        Frame frame = WindowManager.getFrame("Track Summary");
        if(frame!=null && frame instanceof TextWindow) {
            TextWindow tw = (TextWindow) frame;
            if(tw.getTextPanel().getResultsTable()!=null){
                return tw.getTextPanel().getResultsTable();
            }
        }

        summary = new ResultsTable();
        return summary;
    }

    public double[] MeanStd(double[] input){

        double mu = 0;
        double std = 0;
        for(int i=0; i<input.length; i++){
            mu+=input[i];
            std+=input[i]*input[i];
        }
        mu/=input.length;
        std/=input.length;
        std = Math.sqrt(std-(mu*mu));

        return new double[]{mu,std};
    }

    public void DoSpider(List<Track> tracks){

        Plot plot = new Plot("Spider plot", "x ("+tm.image.getCalibration().getUnit()+")", "y ("+tm.image.getCalibration().getUnit()+")",new double[]{},new double[]{});
        plot.setLineWidth(1.5f);
        plot.setColor(Color.black);

        double unit = tm.image.getCalibration().getX(1);
        double maxDisplacement = 0;

        for (Track track : tracks) {
            Point origin = track.points.get(0);
            for(Point pt : track.points){
                if(Math.abs(pt.x-origin.x)*unit>maxDisplacement) maxDisplacement = (pt.x-origin.x)*unit;
                if(Math.abs(pt.y-origin.y)*unit>maxDisplacement) maxDisplacement = (pt.y-origin.y)*unit;
            }

            double[] xVals = new double[track.points.size()];
            double[] yVals = new double[track.points.size()];
            for(int i=0; i<xVals.length; i++){
                xVals[i] = (track.points.get(i).x-origin.x)*unit;
                yVals[i] = (track.points.get(i).y-origin.y)*unit;
            }
            plot.addPoints(xVals,yVals,Plot.LINE);
        }

        maxDisplacement*=1.05;

        plot.setColor(Color.red);
        double[] xVals = new double[tracks.size()];
        double[] yVals = new double[tracks.size()];
        for(int i=0; i<tracks.size(); i++){
            Point first = tracks.get(i).points.get(0);
            Point last = tracks.get(i).LastPoint();
            xVals[i] = (last.x-first.x)*unit;
            yVals[i] = (last.y-first.y)*unit;
        }
        plot.addPoints(xVals,yVals,Plot.CIRCLE);
        plot.setLimits(-maxDisplacement, maxDisplacement, -maxDisplacement, maxDisplacement);
        plot.setColor(Color.black);
        plot.setSize(600,600);
        PlotWindow win = plot.show();
        //win.setSize(600,640);
        //win.drawPlot(plot);
    }

    public void GraphMoments(){

        Plot plot = new Plot("Moments", "Time (frames)", "Moment value",new double[]{},new double[]{});


        Color[] clrs = new Color[]{Color.blue,Color.red,Color.orange,Color.magenta,Color.black};

        int numGraphs=0;
        plot.setLineWidth(1.8f);
        String legend = "";

        double yMin = 0;
        double yMax = 0;

        if(cos_theta){

            double[][] moment = GetMoment(tm.tracker.GetTracks(), this::CosTheta);
            moment[1] = ApplyWindowFilter(moment[1]);
            moment[2] = ApplyWindowFilter(moment[2]);

            for(int i=0; i<moment[1].length; i++){
                if(moment[1][i]>yMax) yMax = moment[1][i];
                if(moment[1][i]<yMin) yMin = moment[1][i];
            }

            plot.setColor(clrs[numGraphs]);
            plot.addPoints(moment[0],moment[1],moment[2],Plot.LINE);
            legend+="Cos(th)\n";
            //plot.addLabel(0.05,0.05+0.075*numGraphs,"Cos(th)");
            numGraphs++;
        }

        if(projectedSpeed){

            double[][] moment = GetMoment(tm.tracker.GetTracks(), this::ProjectedSpeed);
            moment[1] = ApplyWindowFilter(moment[1]);
            moment[2] = ApplyWindowFilter(moment[2]);

            for(int i=0; i<moment[1].length; i++){
                if(moment[1][i]>yMax) yMax = moment[1][i];
                if(moment[1][i]<yMin) yMin = moment[1][i];
            }

            plot.setColor(clrs[numGraphs]);
            plot.addPoints(moment[0],moment[1],moment[2],Plot.LINE);
            legend+="Projected speed\n";
            //plot.addLabel(0.05,0.05+0.075*numGraphs,"Projected speed");
            numGraphs++;
        }

        if(speed){
            double[][] moment = GetMoment(tm.tracker.GetTracks(), this::Speed);
            moment[1] = ApplyWindowFilter(moment[1]);
            moment[2] = ApplyWindowFilter(moment[2]);

            for(int i=0; i<moment[1].length; i++){
                if(moment[1][i]>yMax) yMax = moment[1][i];
                if(moment[1][i]<yMin) yMin = moment[1][i];
            }
            
            plot.setColor(clrs[numGraphs]);
            plot.addPoints(moment[0],moment[1],moment[2],Plot.LINE);
            legend+="Speed\n";
            //plot.addLabel(0.05,0.05+0.075*numGraphs,"Speed");
            numGraphs++;
        }
        plot.setColor(Color.black);
        plot.addLegend(legend);
        plot.setLimits(0,tm.image.getNFrames(), yMin,yMax);
        plot.show();
    }

    public double[][] GetMoment(List<Track> tracks, Function<Point[],Double> scoreFunc){

        List<Point> points = new ArrayList<>();
        List<Double> xVals = new ArrayList<>();
        List<Double> yVals = new ArrayList<>();
        List<Double> yErrs = new ArrayList<>();

        double angle = Math.toRadians(tm.overlayer.angle);

        for(int t=0; t<tm.image.getNFrames()-1; t++){

            points.clear();

            for (Track track : tracks) {
                int track_tp = t-track.points.get(0).t;
                if(track_tp<0) continue;
                if(track.points.size()>track_tp+1){
                    points.add(track.points.get(track_tp));
                }
            }

            if(points.size()<10) continue;

            double[] pointMoments = new double[points.size()];
            double mu = 0;
            for(int i=0; i<points.size(); i++){
                Point pt = points.get(i);
                pointMoments[i] = pt.x*Math.cos(angle)+pt.y*Math.sin(angle);
                mu+=pointMoments[i];
            }
            mu/=points.size();


            double[] pointScores  = new double[points.size()];
            double sMax = Double.MIN_VALUE;
            double sMin = Double.MAX_VALUE;

            for(int i=0; i<points.size(); i++){
                Point p0 = points.get(i);
                Point p1 = p0.Next();
                pointScores[i]=scoreFunc.apply(new Point[]{p0,p1});
                if(pointScores[i]>sMax) sMax = pointScores[i];
                if(pointScores[i]<sMin) sMin = pointScores[i];
            }
            for(int i=0; i<pointScores.length; i++){
                pointScores[i] = (pointScores[i]-sMin)/(sMax-sMin);
            }

            double mean = 0;
            double std  = 0;

            for(int i=0; i<points.size(); i++){
                Point p0 = points.get(i);
                Point p1 = p0.Next();
                pointMoments[i] = ((pointMoments[i]-mu)/mu)*pointScores[i];
                mean+=pointMoments[i];
                std+=pointMoments[i]*pointMoments[i];
            }
            mean/=points.size();
            std/=points.size();
            std-=(mean*mean);
            std/=Math.sqrt(pointMoments.length);
            xVals.add(new Double(t));
            yVals.add(mean);
            yErrs.add(std);
        }

        double[][] xo = new double[3][xVals.size()];

        for(int i=0; i<xVals.size(); i++){
            xo[0][i] = xVals.get(i);
            xo[1][i] = yVals.get(i);
            xo[2][i] = yErrs.get(i);
        }
        return xo;
    }

    public void MakeSpatialAveragesGraph(){

        Plot plot = new Plot("Tracking values over space", "Position", "value",new double[]{},new double[]{});
        Color[] clrs = new Color[]{Color.blue,Color.red,Color.orange,Color.magenta,Color.black};
        plot.setLineWidth(1.8f);
        int numGraphs = 0;
        String legend = "";

        double xMin = 0;
        double xMax = Double.MIN_VALUE;
        double yMin = 0;
        double yMax = 1;

        List<Track> tracks = tm.tracker.GetTracks();

        if(cos_theta){

            double[][] values = GetSpatialAverages(tracks, this::CosTheta);

            for(int i=0; i<values.length; i++){
                //if(values[i]==null) IJ.log("null at "+i);
            }
            for(int i=0; i<values.length; i++){
                double val1 = values[i][1]+values[i][2];
                double val2 = values[i][1]-values[i][2];
                if(val1>yMax) yMax = val1;
                if(val2<yMin) yMin = val2;

                if(values[i][0]>xMax) xMax = values[i][0];
                if(values[i][0]<xMin) xMin = values[i][0];
            }

            double[] xVals = new double[values.length];
            double[] yVals = new double[values.length];
            double[] yErrs = new double[values.length];

            for(int i=0; i<values.length; i++){
                xVals[i] = values[i][0];
                yVals[i] = values[i][1];
                yErrs[i] = values[i][2];
            }

            plot.setColor(clrs[numGraphs]);
            plot.addPoints(xVals,yVals,yErrs,Plot.LINE);
            legend+="Cos(th)\n";
            //plot.addLabel(0.05,0.05+0.075*numGraphs,"Cos(th)");
            numGraphs++;
        }

        if(projectedSpeed){

            double[][] values = GetSpatialAverages(tracks, this::ProjectedSpeed);
            double meanValue = MeanScore(tracks, this::Speed);

            double[] xVals = new double[values.length];
            double[] yVals = new double[values.length];
            double[] yErrs = new double[values.length];

            for(int i=0; i<values.length; i++){
                double val1 = (values[i][1]+values[i][2])/meanValue;
                double val2 = (values[i][1]-values[i][2])/meanValue;
                if(val1>yMax) yMax = val1;
                if(val2<yMin) yMin = val2;
                if(values[i][0]>xMax) xMax = values[i][0];
                if(values[i][0]<xMin) xMin = values[i][0];
            }


            for(int i=0; i<values.length; i++){
                xVals[i] = values[i][0];
                yVals[i] = values[i][1]/meanValue;
                yErrs[i] = values[i][2]/meanValue;
            }

            plot.setColor(clrs[numGraphs]);
            plot.addPoints(xVals,yVals,yErrs,Plot.LINE);
            legend+="Projected speed/ mean\n";
            //plot.addLabel(0.05,0.05+0.075*numGraphs,"Cos(th)");
            numGraphs++;
        }

        if(speed){

            double[][] values = GetSpatialAverages(tracks, this::Speed);

            double meanValue = MeanScore(tracks, this::Speed);
            double[] xVals = new double[values.length];
            double[] yVals = new double[values.length];
            double[] yErrs = new double[values.length];


            for(int i=0; i<values.length; i++){
                double val1 = (values[i][1]+values[i][2])/meanValue;
                double val2 = (values[i][1]-values[i][2])/meanValue;
                if(val1>yMax) yMax = val1;
                if(val2<yMin) yMin = val2;
                if(values[i][0]>xMax) xMax = values[i][0];
                if(values[i][0]<xMin) xMin = values[i][0];
            }

            for(int i=0; i<values.length; i++){
                xVals[i] = values[i][0];
                yVals[i] = values[i][1]/meanValue;
                yErrs[i] = values[i][2]/meanValue;
            }

            plot.setColor(clrs[numGraphs]);
            plot.addPoints(xVals,yVals,yErrs,Plot.LINE);
            legend+="Speed/mean\n";
            //plot.addLabel(0.05,0.05+0.075*numGraphs,"Cos(th)");
            numGraphs++;
        }
        plot.setColor(Color.black);
        plot.addLegend(legend);
        plot.setLimits(xMin,xMax,yMin,yMax);
        plot.show();
    }

    public double[][] GetSpatialAverages(List<Track> tracks, Function<Point[],Double> scoreFunc){

        double xMax = Double.MIN_VALUE;
        double xMin = Double.MAX_VALUE;
        int bins = 20;

        List<List<double[]>> vals = new ArrayList<>();

        double angle = Math.toRadians(tm.overlayer.angle);

        for(Track t : tracks){
            for(int i=0; i<t.points.size(); i++){
                Point p = t.points.get(i);

                double px = p.x*Math.cos(angle) + p.y*Math.sin(angle);

                if(px<xMin) xMin = px;
                if(px>xMax) xMax = px;
            }
        }

        double binWidth = (xMax-xMin)/(bins-0.001);
        for(int i=0; i<bins; i++){
            vals.add(new ArrayList<double[]>());
        }


        double px, py;

        for(Track t : tracks){
            for(int i=1; i<t.points.size(); i++){
                Point p0 = t.points.get(i-1);
                Point p1 = t.points.get(i);

                px = p0.x*Math.cos(angle) + p0.y*Math.sin(angle);

                int bin = (int) ((px-xMin)/binWidth);

                px = tm.image.getCalibration().getX(px);
                py = scoreFunc.apply(new Point[]{p0,p1});

                vals.get(bin).add(new double[]{px,py});
            }
        }



        int count = 0;
        for(int i=0; i<vals.size(); i++) {
            if (vals.get(i).size() < 3) continue;
            count++;
        }

        double[][] profile = new double[count][];

        for(int i=0; i<vals.size(); i++){

            List<double[]> bin = vals.get(i);
            if(bin.size()<3) continue;

            double muX = 0;
            double muY = 0;
            double sdY = 0;

            for(double[] val : bin){
                muX+=val[0];
                muY+=val[1];
                sdY+=val[1]*val[1];
            }
            muX/=bin.size();
            muY/=bin.size();
            sdY/=bin.size();
            sdY-=muY*muY;

            sdY/=Math.sqrt(bin.size());

            profile[i] = new double[]{muX,muY, sdY};
        }

        for(int i=0; i<profile.length; i++){
            if(profile[i]==null) IJ.log("Profile["+i+"] is null.");
            //else IJ.log("Profile["+i+"].x :: "+profile[i][0]);
        }
        return profile;
    }

    private void DoTrackSummary(){

        ResultsTable summary = new ResultsTable();
        List<Track> tracks = tm.tracker.GetTracks();
        Calibration cal = tm.image.getCalibration();


        for(Track t : tracks){
            summary.incrementCounter();

            summary.addValue("Track ID", t.ID());
            summary.addValue("# points", t.points.size());
            if(cos_theta)       summary.addValue("Cos(th)", MeanScore(t,this::CosTheta));
            if(speed)           summary.addValue("Speed ("+cal.getUnit()+"/"+cal.getTimeUnit()+")", cal.getX(MeanScore(t,this::Speed))/cal.frameInterval);
            if(projectedSpeed)  summary.addValue("Projected Speed ("+cal.getUnit()+"/"+cal.getTimeUnit()+")", cal.getX(MeanScore(t,this::ProjectedSpeed))/cal.frameInterval);
            if(ci)              summary.addValue("CEI", CI(t));
            if(directedness)    summary.addValue("Directedness", Directedness(t));
        }

        summary.show("Track summaries for "+tm.image.getTitle());
    }

    public void DisplaySummary(){

        DoTrackSummary();

        ResultsTable summary = GetSummaryTable();
        List<Track> tracks = tm.tracker.GetTracks();
        Calibration cal = tm.image.getCalibration();

        summary.incrementCounter();
        summary.addValue("Image", tm.image.getTitle());

        double[] scores = new double[tracks.size()];
        double[] mn_std = new double[2];

        // AVERAGED PER STEP
        if(cos_theta) {
            for (int i = 0; i < tracks.size(); i++) {
                scores[i]=MeanScore(tracks.get(i), this::CosTheta);
            }
            mn_std = MeanStd(scores);

            summary.addValue("Cos (th)", mn_std[0]);
            summary.addValue("std[Cos (th)]", mn_std[1]);
        }
        if(speed) {
            for (int i = 0; i < tracks.size(); i++) {
                scores[i]=(cal.getX(1)/cal.frameInterval)*MeanScore(tracks.get(i), this::Speed);
            }
            mn_std = MeanStd(scores);

            String lbl = "Speed";
            String er_lbl = "std["+lbl+"]";

            lbl+=" ("+cal.getUnit()+"/"+cal.getTimeUnit()+")";

            summary.addValue(lbl, mn_std[0]);
            summary.addValue(er_lbl, mn_std[1]);
        }
        if(projectedSpeed) {
            for (int i = 0; i < tracks.size(); i++) {
                scores[i]=(cal.getX(1)/cal.frameInterval)*MeanScore(tracks.get(i), this::ProjectedSpeed);
            }
            mn_std = MeanStd(scores);

            String lbl = "Proj. Speed";
            String er_lbl = "std["+lbl+"]";


            lbl+=" ("+cal.getXUnit()+"/"+cal.getTimeUnit()+")";


            summary.addValue(lbl, mn_std[0]);
            summary.addValue(er_lbl, mn_std[1]);
        }


        // ONLY MAKE SENSE AT WHOLE TRACK LEVEL
        if(ci) {
            for (int i = 0; i < tracks.size(); i++) {
                scores[i]=CI(tracks.get(i));
            }
            mn_std = MeanStd(scores);

            summary.addValue("CEI", mn_std[0]);
            summary.addValue("std[CEI]", mn_std[1]);
        }
        if(directedness) {
            for (int i = 0; i < tracks.size(); i++) {
                scores[i]=Directedness(tracks.get(i));
            }
            mn_std = MeanStd(scores);

            summary.addValue("Directedness", mn_std[0]);
            summary.addValue("std[Directedness]", mn_std[1]);
        }


        summary.show("Track Summary");
    }

    private double Speed(Point[] pt){

        return Math.sqrt(pt[1].SquareSpeedFromPoint(pt[0]));
    }

    private double ProjectedSpeed(Point[] pt){


        double value, dx, dy, tx = 0;
        double angle = Math.toRadians(tm.overlayer.angle);
        double dt = pt[1].t-pt[0].t;

        if(pt[0].x==pt[1].x&&pt[0].y==pt[1].y) return 0;

        dx = pt[1].x - pt[0].x;
        dy = pt[1].y - pt[0].y;
        tx = dx * Math.cos(angle) + dy * Math.sin(angle);

        return tx/dt;
    }

    private double CosTheta(Point[] pt){
        double value, dx, dy, tx, ty = 0;
        double forward = tm.overlayer.angle;
        double angle = Math.toRadians(tm.overlayer.angle);

        if(pt[0].x==pt[1].x&&pt[0].y==pt[1].y) return 0;

        dx = pt[1].x - pt[0].x;
        dy = pt[1].y - pt[0].y;

        tx = dx * Math.cos(angle) + dy * Math.sin(angle);

        return tx/Math.sqrt(dx*dx+dy*dy);
    }

    private double MeanScore(Track track, Function<Point[], Double> func){

        double score = 0;

        for(int i=0; i<track.points.size()-1; i++) {

            Point p0 = track.points.get(i);
            Point p1 = track.points.get(i+1);

            score += func.apply(new Point[]{p0,p1});
        }

        score/=track.points.size()-1;

        return score;
    }

    private double MeanScore(List<Track> tracks, Function<Point[], Double> func){

        double score = 0;
        double count = 0;
        for(Track track : tracks) {
            for (int i = 0; i < track.points.size() - 1; i++) {

                Point p0 = track.points.get(i);
                Point p1 = track.points.get(i + 1);

                count+=1;
                score += func.apply(new Point[]{p0, p1});
            }
        }
        score/=count;

        return score;
    }

    private double CI(Track track){

        Point last  = track.LastPoint();
        Point first = track.points.get(0);

        double dx, dy, ddg, ttl;

        double angle = Math.toRadians(tm.overlayer.angle);

        dx = last.x-first.x;
        dy = last.y-first.y;
        ddg = dx*Math.cos(angle) + dy*Math.sin(angle);

        ttl = 0;
        for(int i=0; i<track.points.size()-1; i++){
            ttl+= Math.sqrt(track.points.get(i+1).SquareDistanceFromPoint(track.points.get(i)));
        }

        return ddg/ttl;
    }

    private double Directedness(Track track){

        Point last  = track.LastPoint();
        Point first = track.points.get(0);

        double dx, dy, ddg, ttl;

        double angle = Math.toRadians(tm.overlayer.angle);

        dx = last.x-first.x;
        dy = last.y-first.y;
        ddg = Math.sqrt(dx*dx+dy*dy);

        ttl = 0;
        for(int i=0; i<track.points.size()-1; i++){
            ttl+= Math.sqrt(track.points.get(i+1).SquareDistanceFromPoint(track.points.get(i)));
        }

        return ddg/ttl;
    }


    public void SaveTracksToFile(String filePath){

        ResultsTable outMTJTable = new ResultsTable();
        Calibration cal = tm.image.getCalibration();

        List<Track> tracks = tm.tracker.GetTracks();
        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(4);

        int line = 0;

        for(int i=0; i<tracks.size(); i++){
            Track track = tracks.get(i);
            for(int j=0; j< track.points.size(); j++){
                Point p = track.points.get(j);

                outMTJTable.incrementCounter();
                line++;

                outMTJTable.addValue("Nr", line);
                outMTJTable.addValue("TID", track.ID());
                outMTJTable.addValue("PID", j+1);
                outMTJTable.addValue("x ("+cal.getUnit()+")", cal.getX(p.x));
                outMTJTable.addValue("y ("+cal.getUnit()+")", cal.getX(p.y));
                outMTJTable.addValue("t ("+cal.getTimeUnit()+")", p.t*cal.frameInterval);
            }
        }

        outMTJTable.show("Full Results for "+tm.image.getTitle());
    }

    public void MakeTransitionMatrix(){

        List<Track> tracks = tm.tracker.GetTracks();
        double fwdAngle = tm.overlayer.angle;
        int half_width = tm.overlayer.calcWindowSize;

        double dx1, dy1, dx2, dy2;
        double angle1, angle2;
        int a1x, a2y;

        ImageProcessor matrix = new FloatProcessor(72,72);

        for(int i=0; i<tracks.size(); i++){
            for(int j=0; j<tracks.get(i).points.size()-2*half_width; j+=0.5*half_width){

                Point p0 = tracks.get(i).points.get(j);
                Point p1 = tracks.get(i).points.get(j+half_width);
                Point p2 = tracks.get(i).points.get(j+2*half_width);

                dx1 = p1.x-p0.x;
                dy1 = p1.y-p0.y;

                dx2 = p2.x-p1.x;
                dy2 = p2.y-p1.y;

                angle1 = 180.0*Math.atan2(dy1,dx1)/Math.PI;
                angle2 = 180.0*Math.atan2(dy2,dx2)/Math.PI;

                //IJ.log(Double.toString(angle1)+ " : " + Double.toString(angle2));

                a1x = (int) (36.5 + Math.round(0.2*angle1));
                a2y = (int) (36.5 + Math.round(0.2*angle2));

                matrix.putPixelValue(a1x,a2y,matrix.getPixelValue(a1x,a2y)+1);
            }
        }
        matrix.blurGaussian(0.75);
        ImagePlus mPlus = new ImagePlus("Transition Matrix",matrix);
        IJ.run(mPlus, "Size...", "width=360 height=360 depth=1 constrain average interpolation=None");
        IJ.run(mPlus, "mpl-viridis", "");
        IJ.run(mPlus, "Enhance Contrast", "saturated=0.25");
        IJ.run(mPlus, "Flip Horizontally", "");
        mPlus.show();
    }

    public boolean HasPossibleSamplingBias(List<Track> tracks){

        int nFrames = tm.image.getNFrames();
        int width = tm.image.getWidth();
        if(nFrames<10) return false;

        double muX0 = 0;
        int nSamples0 = 0;
        double muX1 = 0;
        int nSamples1 = 0;

        double angle = Math.toRadians(tm.overlayer.angle);

        for(Track t : tracks) {
            for(Point p : t.points){
                if(p.t<10 && p.t<nFrames/10){
                    muX0+=p.x*Math.cos(angle)+p.y*Math.sin(angle);
                    nSamples0+=1;
                }
                else if(p.t>nFrames/2){
                    muX1+=p.x*Math.cos(angle)+p.y*Math.sin(angle);
                    nSamples1+=1;
                }
            }
        }


        muX0/=nSamples0;
        muX1/=nSamples1;

        if(muX0<muX1/2 && muX0<0.25*width) return true;
        if(muX0>2*muX1 && muX0>0.75*width) return true;
        return false;
    }

    public void DoTSHeatMap(List<Track> tracks) {

        ArrayList<ArrayList<ArrayList<Point>>> bins = new ArrayList<>();

        int binsX = 20;
        int binsT = (int) Math.round(0.2*tm.image.getNFrames());

        for (int i = 0; i < binsT; i++) {
            ArrayList<ArrayList<Point>> spaceBins = new ArrayList<>();
            for (int j = 0; j < binsX; j++) {
                spaceBins.add(new ArrayList<>());
            }
            bins.add(spaceBins);
        }

        double angle = Math.toRadians(tm.overlayer.angle);
        double xMin = Double.MAX_VALUE;
        double xMax = Double.MIN_VALUE;
        int tMin = Integer.MAX_VALUE;
        int tMax = Integer.MIN_VALUE;

        double mu = 0;
        int ttl = 0;

        for (Track t : tracks) {
            for (Point p : t.points) {
                if (p.t > tMax) tMax = p.t;
                if (p.t < tMin) tMin = p.t;

                double psn = p.x * Math.cos(angle) + p.y * Math.sin(angle);

                if (psn > xMax) xMax = psn;
                if (psn < xMin) xMin = psn;

                if (p.Prev() != null) {
                    mu += CosTheta(new Point[]{p.Prev(), p});
                    ttl += 1;
                }
            }
        }

        mu /= ttl;

        double xW = (xMax - xMin) * 1.00001d/binsX;
        double tW = (tMax - tMin) * 1.00001d/binsT;

        for(Track t : tracks){
            for(Point p : t.points) {
                if(p.Prev()==null) continue;

                int tBin = (int) Math.floor((p.t-tMin)/tW);
                int xBin = (int) Math.floor((p.x*Math.cos(angle) + p.y*Math.sin(angle) - xMin)/xW);
                bins.get(tBin).get(xBin).add(p);
            }
        }

        ImageProcessor heatMap = new FloatProcessor(bins.size(),bins.get(0).size());

        for(int i=0; i<bins.size(); i++){
            for(int j=0; j<bins.get(i).size(); j++){
                ArrayList<Point> bin = bins.get(i).get(j);

                double binMean = 0;
                double binSD   = 0;
                double binSEM = 0;

                for(Point p : bin){
                    double value = ProjectedSpeed(new Point[]{p.Prev(),p});
                    binMean+=value;
                    binSD+=value*value;
                }

                binMean/=bin.size();

                heatMap.putPixelValue(i,j,binMean);
            }
        }
        heatMap.blurGaussian(0.5);
        ImagePlus mPlus = new ImagePlus("Projected Speed Map",heatMap);

        IJ.run(mPlus, "Rotate 90 Degrees Left", "");
        int iScale = 1;
        for(iScale = 1; iScale<12; iScale++) {
            if(iScale*heatMap.getWidth()>200) break;
            if(iScale*heatMap.getHeight()>200) break;
        }
        if(iScale>1){
            IJ.run(mPlus, "Size...", "width="+Integer.toString(iScale*heatMap.getWidth())+" height="+Integer.toString(iScale*heatMap.getHeight())+" depth=1 constrain average interpolation=Bilinear");
        }
        IJ.run(mPlus, "mpl-viridis", "");
        IJ.run(mPlus, "Enhance Contrast", "saturated=0.2");

        //IJ.run(mPlus, "Flip Horizontally", "");
        mPlus.show();
    }

    public boolean HasPossibleTSVariation(List<Track> tracks){

        ArrayList<ArrayList<ArrayList<Point>>> bins = new ArrayList<>();

        for(int i=0; i<5; i++){
            ArrayList<ArrayList<Point>> spaceBins = new ArrayList<>();
            for(int j=0; j<5; j++){
                spaceBins.add(new ArrayList<>());
            }
            bins.add(spaceBins);
        }

        double angle = Math.toRadians(tm.overlayer.angle);
        double xMin = Double.MAX_VALUE;
        double xMax = Double.MIN_VALUE;
        int tMin = Integer.MAX_VALUE;
        int tMax = Integer.MIN_VALUE;

        double mu = 0;
        int ttl = 0;

        for(Track t : tracks){
            for(Point p : t.points){
                if(p.t>tMax) tMax = p.t;
                if(p.t<tMin) tMin = p.t;

                double psn = p.x*Math.cos(angle) + p.y*Math.sin(angle);

                if(psn>xMax) xMax = psn;
                if(psn<xMin) xMin = psn;

                if(p.Prev()!=null){
                    mu+=CosTheta(new Point[]{p.Prev(),p});
                    ttl+=1;
                }
            }
        }

        mu/=ttl;

        double xW = (xMax-xMin)*0.20000000001;
        double tW = (tMax-tMin)*0.20000000001;

        for(Track t : tracks){
            for(Point p : t.points) {
                if(p.Prev()==null) continue;

                int tBin = (int) Math.floor((p.t-tMin)/tW);
                int xBin = (int) Math.floor((p.x*Math.cos(angle) + p.y*Math.sin(angle) - xMin)/xW);
                bins.get(tBin).get(xBin).add(p);
            }
        }

        for(int i=0; i<5; i++){
            for(int j=0; j<5; j++){
                ArrayList<Point> bin = bins.get(i).get(j);

                double binMean = 0;
                double binSD   = 0;
                double binSEM = 0;

                for(Point p : bin){
                    double value = ProjectedSpeed(new Point[]{p.Prev(),p});
                    binMean+=value;
                    binSD+=value*value;
                }

                binMean/=bin.size();
                binSD/=bin.size();
                binSD-=binMean*binMean;
                binSD = Math.sqrt(binSD);
                binSEM = binSD/Math.sqrt(bin.size());

                double test = (mu-binMean);

                //IJ.log("test : SD : SEM  ->  "+test*test+" : "+binSD+" : "+binSEM);

                if(test*test>156*binSEM*binSEM) return true;
            }
        }
        return false;
    }

    public void DoWarnings(){
        warningsDone = true;
        List<Track> tracks = tm.tracker.GetTracks();
        String msg = "";
        if(HasPossibleTSVariation(tracks)){
            msg+="There are parts of the movie where cell behaviour differs strongly from the ensemble mean.\nConsider looking at spatial distributions of your tracking statistics, rather than reporting their average values.\n";
        }
        if(HasPossibleSamplingBias(tracks)){
            msg+="\nThe initial positions of cells are strongly biased along the chemotactic axis. \nConsider the possibility that any directionality is due to persistence rather than bias.";
        }
        if(!msg.equals("")){
            IJ.log(msg);
        }
    }


    public void DoMSD(List<Track> tracks){

        // First get MSD and quartiles
        int len = tm.image.getStackSize();

        double[] mu  = new double[len];
        List<Double> pts = new ArrayList<Double>();
        double[] q1  = new double[len];
        double[] q2  = new double[len];
        double[] num = new double[len];
        double val;

        Calibration cal = tm.image.getCalibration();

        int count = len;

        for(int n=1; n<len; n++){

            pts.clear();

            for(int i=0; i<tracks.size(); i++){
                Track track = tracks.get(i);

                if(track.points.size()>n){
                    num[n]++;
                    val = (cal.getX(1)*cal.getX(1)) * track.points.get(n).SquareDistanceFromPoint(track.points.get(0));
                    mu[n]+= val;
                    pts.add(val);
                }
            }
            if(num[n]<16) {
                count = n-1;
                break;
            }
            pts.sort((a,b)->a.compareTo(b));
            q1[n] = pts.get((int) (1*num[n]/4));
            q2[n] = pts.get((int) (3*num[n]/4));
        }

        double[] mu2  = new double[count];
        double[] q12  = new double[count];
        double[] q22  = new double[count];

        double[] x = new double[count];

        for(int i=0; i<count; i++){
            x[i] = i*cal.frameInterval;
            mu2[i]  =  mu[i]/num[i];

            q12[i] = q1[i];
            q22[i] = q2[i];
        }

        // Next, use the curve fitter to keep track of the way the MSD power changes over time

        CurveFitter fitter = new CurveFitter(x,mu2);
        fitter.setInitialParameters(new double[]{5.0, 1.5});
        fitter.doFit(CurveFitter.POWER);
        double[] params = fitter.getParams();

        double[] fit = new double[count];
        for(int i=0; i<fit.length; i++){
            fit[i] = params[0]*Math.pow(x[i],params[1]);
        }

        //IJ.log("Params: "+params[0]+" :: "+params[1]);

        Plot plot = new Plot("MSD", "Time (frames)","Squared distance ("+cal.getUnit()+"^2/"+cal.getTimeUnit()+")",x,mu2);
        plot.changeFont(new Font("Arial", Font.PLAIN, 18));

        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(4);

        plot.addLabel(0.1,0.1,"Speed: "+format.format(Math.sqrt(params[0])));
        plot.addLabel(0.1,0.18,"Exponent: "+format.format(params[1]));


        plot.setLineWidth(3f);
        plot.setColor(Color.red);
        plot.addPoints(x,fit, PlotWindow.LINE);

        plot.setLineWidth(1f);
        plot.setColor(Color.gray);
        plot.addPoints(x,q12, PlotWindow.LINE);
        plot.addPoints(x,q22, PlotWindow.LINE);

        plot.setLineWidth(2f);
        plot.setColor(Color.blue);

        
        plot.show();
    }


    public void DoAC(List<Track> tracks){

        // First get MSD and quartiles
        int len = tm.image.getStackSize()-1;

        double dx1, dy1, dx2, dy2, dr1,dr2, dot;
        Point p0,p1,pd0,pd1;

        List<List<Double>> acs = new ArrayList<>();
        for(int dt=0; dt<len-1; dt++){
            List<Double> pointList = new ArrayList<>();
            acs.add(pointList);
            for(Track t : tracks){
                int count = 0;
                dot = 0;
                for(int i=0; i<t.points.size()-(dt+1); i++){
                    p0 = t.points.get(i);
                    p1 = t.points.get(i+1);
                    pd0 = t.points.get(i+dt);
                    pd1 = t.points.get(i+dt+1);

                    dx1 = p1.x-p0.x;
                    dy1 = p1.y-p0.y;
                    dx2 = pd1.x-pd0.x;
                    dy2 = pd1.y-pd0.y;

                    if((dx1==0 && dy1==0) || (dx2==0&&dy2==0)) continue;
                    count++;

                    dr1 = Math.sqrt(dx1*dx1+dy1*dy1);
                    dr2 = Math.sqrt(dx2*dx2+dy2*dy2);

                    dot += (dx1*dx2 + dy1*dy2)/(dr1*dr2);
                }
                if(count==0) continue;
                dot/=count;
                acs.get(dt).add(dot);
            }
            if(acs.get(dt).size()<6){
                //IJ.log("First found fewer than 6 tracks in frame "+dt);
                acs.remove(pointList);
                break;
            }
        }

        double mu, sem, val;
        double[] ac  = new double[acs.size()];
        double[] err = new double[acs.size()];
        double[] x = new double[acs.size()];

        DecimalFormat format = new DecimalFormat();
        format.setMaximumFractionDigits(4);

        for(int i=0; i<acs.size(); i++){
            mu = 0;
            sem = 0;
            x[i] = i;
            for(int j=0; j<acs.get(i).size(); j++){
                val = acs.get(i).get(j).doubleValue();
                mu += val;
                sem+=val*val;
            }

            mu/=acs.get(i).size();
            sem/=acs.get(i).size();
            sem-=mu*mu;
            sem/=Math.sqrt(acs.get(i).size());

            ac[i] = mu;
            err[i] = sem;
        }

        // Next, use the curve fitter to keep track of the way the MSD power changes over time

        CurveFitter fitter = new CurveFitter(x,ac);
        fitter.doCustomFit("y=a+(1-a)*exp(-x/b)", new double[]{0.15, 10},false);
        double[] params = fitter.getParams();

        double gamma = 0.69314718;
        double[] fit = new double[acs.size()];
        for(int i=0; i<fit.length; i++){
            fit[i] = params[0]+(1-params[0])*Math.exp(-x[i]/params[1]);
        }

        //IJ.log("Params: "+params[0]+" :: "+params[1]);

        Plot plot = new Plot("Autocorrelation fit", "Time (frames)","Autocorrelation",x,ac);
        plot.changeFont(new Font("Arial", Font.PLAIN, 18));



        plot.addLabel(0.1,0.1,"Bias: "+format.format(params[0]));
        plot.addLabel(0.1,0.18,"Halftime: "+format.format(tm.image.getCalibration().frameInterval*params[1]*gamma)+" ("+tm.image.getCalibration().getTimeUnit()+")");
        plot.addLabel(0.1,0.26,"Goodness of fit: "+format.format(fitter.getFitGoodness()));

        plot.setLineWidth(3f);
        plot.setColor(Color.red);
        plot.addPoints(x,fit, PlotWindow.LINE);

        plot.setLineWidth(2f);
        plot.setColor(Color.blue);
        plot.addErrorBars(err);
        plot.show();
    }
}
