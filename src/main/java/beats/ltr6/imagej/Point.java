package beats.ltr6.imagej;

public class Point {

    public int x;
    public int y;
    public int t;

    public Track track;

    public Point(int x, int y, int t){
        this.x = x;
        this.y = y;
        this.t = t;
    }

    public double SquareSpeedFromPoint(Point other){

        if(other.t==this.t) {
            if (other == this) return 0;
            else return Double.MAX_VALUE;
        }

        double td = Math.abs(this.t-other.t);

        return SquareDistanceFromPoint(other)/(td*td);
    }


    public double SquareDistanceFromPoint(Point other){
        return 1.0*((this.x-other.x)*(this.x-other.x) + (this.y-other.y)*(this.y-other.y));
    }

    public Point Next(){
        int idx = this.track.points.indexOf(this);
        if(track.points.size()>idx+1){
            return track.points.get(idx+1);
        }
        return  null;
    }
    public Point Prev(){
        int idx = this.track.points.indexOf(this);
        if(idx>0){
            return track.points.get(idx-1);
        }
        return  null;
    }
}
