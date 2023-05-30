package beats.ltr6.imagej;

import java.util.ArrayList;

public class Track {

    private int id;
    public int ID(){
        return  id;
    }
    ArrayList<Point> points = new ArrayList<Point>();

    public void AddPoint(Point point){
        point.track = this;
        this.points.add(point);
    }
    public void AddPoint(int x, int y, int t){

        Point point = new Point(x,y,t);
        point.track = this;
        points.add(point);
    }


    public Track(Point p, int id){
        p.track = this;
        this.points.add(p);
        this.id = id;
    }

    public int GetID(){
        return this.id;
    }

    public Point LastPoint(){
        return points.get(points.size()-1);
    }

    public int LastFrame(){
        return points.get(points.size()-1).t;
    }

    public double AverageSpeed(){
        double ttl = 0;
        for(int i=1; i<points.size(); i++){
            ttl+=Math.sqrt(points.get(i).SquareSpeedFromPoint(points.get(i-1)));
        }
        return ttl/(1.0*(points.size()-1));
    }

    public void Sort(){
        points.sort((p1,p2)->Double.compare(p1.t, p2.t));
    }

    public void InterpolateMissing(){
        Sort();
        ArrayList<Point> newPoints = new ArrayList<>();

        for(int i=1; i<points.size(); i++){

            Point pm1 = points.get(i-1);
            Point p = points.get(i);

            newPoints.add(pm1);
            int iNumNew = (p.t-pm1.t)-1;

            if(iNumNew<=0) continue;

            int pdx = (p.x-pm1.x)/(iNumNew+1);
            int pdy = (p.y-pm1.y)/(iNumNew+1);

            for(int j=1; j<=iNumNew; j++){
                Point newPoint = new Point(pm1.x+j*pdx, pm1.y+j*pdy, pm1.t+j);
                newPoint.track = this;
                newPoints.add(newPoint);
            }
        }
        newPoints.add(this.LastPoint());
        this.points = newPoints;
    }
}
