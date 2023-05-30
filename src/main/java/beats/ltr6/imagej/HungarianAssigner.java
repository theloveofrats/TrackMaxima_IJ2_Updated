package beats.ltr6.imagej;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class HungarianAssigner {

    private double[][] matrix;
    private double dummyCost = 1E10;
    private Tracker tracker;
    double[] minColWeight, minRowWeight;
    int[] squareInRow, squareInCol;
    int[] starredZeroesInRow, starredZeroesInCol;
    boolean[] rowCovered, colCovered;

    public HungarianAssigner(Tracker tracker){
        this.tracker = tracker;
    }

    public void DoAssignment(List<Track> tracks, Point[] newPoints){

        int rows         = newPoints.length;
        int cols         = tracks.size();
        int rcDifference = rows - cols;
        int matrixSize   = Math.max(rows,cols);

        minColWeight = new double[matrixSize];
        minRowWeight = new double[matrixSize];
        squareInRow = new int[matrixSize];
        squareInCol = new int[matrixSize];
        Arrays.fill(squareInRow, -1);
        Arrays.fill(squareInCol, -1);
        starredZeroesInRow = new int[matrixSize];
        starredZeroesInCol = new int[matrixSize];
        Arrays.fill(starredZeroesInRow, -1);
        Arrays.fill(starredZeroesInCol, -1);
        rowCovered = new boolean[matrixSize];
        colCovered = new boolean[matrixSize];

        for(int i=0; i<matrixSize; i++){
            minRowWeight[i] = dummyCost;
            minColWeight[i] = dummyCost;
        }

        int dummyTracks = 0;
        int dummyPoints = 0;

        if(rcDifference>0)      dummyTracks = rcDifference;
        else if(rcDifference<0) dummyPoints = -rcDifference;

        matrix = new double[matrixSize][matrixSize];

        boolean allAssigned = false;
        double wht = 0;


        // Calc initial weights & ad dummies
        if(dummyPoints>0){
            for(int i=rows; i<matrixSize; i++){
                for(int j=0; j<matrixSize; j++){
                    matrix[i][j] = dummyCost;
                }
            }
        }
        if(dummyTracks>0){
            for(int j=cols; j<matrixSize; j++){
                for(int i=0; i<matrixSize; i++){
                    matrix[i][j] = dummyCost;
                }
            }
        }

        for(int i=0; i<rows; i++){
            for(int j=0; j<cols; j++){
                wht = tracker.GetMatchWeight(newPoints[i], tracks.get(j).LastPoint());
                matrix[i][j] = wht;
                if(wht<minRowWeight[i]) minRowWeight[i] = wht;
                if(wht<minColWeight[j]) minColWeight[j] = wht;
            }
        }

        // Reduce matrix
        SubRows();
        SubCols();

        // Mark row-col zeroes
        MarkZeros();
        CoverColumns();

        while(!allColsCovered()){
            int[] mZ = GetMainZeroes();
            while (mZ==null){
                ReduceByMinUncoveredValue();
                mZ = GetMainZeroes();
            }

            if(squareInRow[mZ[0]]==-1){

                ProcessLinkedZeroChain(mZ);
                CoverColumns();
            }
            else{
                rowCovered[mZ[0]] = true;
                colCovered[squareInRow[mZ[0]]] = false;
                ReduceByMinUncoveredValue();
            }
        }

        // Assign points
        for(int i=0; i<tracks.size(); i++){

            Track track = tracks.get(i);
            int pointIndex = squareInCol[i];

            if(pointIndex>=newPoints.length) continue;
            if(newPoints[pointIndex].SquareSpeedFromPoint(track.LastPoint())>tracker.maxSpeed*tracker.maxSpeed) continue;

            newPoints[pointIndex].track = track;
        }
    }

    private void ProcessLinkedZeroChain(int[] mZ){
        int i = mZ[0];
        int j = mZ[1];

        Set<int[]> K = new LinkedHashSet<>();
        K.add(mZ);
        boolean found = false;

        do{
            if(squareInCol[j]!=-1){
                K.add(new int[]{squareInCol[j],j});
                found = true;
            }
            else found = false;
            if(!found) break;

            i = squareInCol[j];
            j = starredZeroesInRow[i];

            if(j!=-1){
                K.add(new int[]{i,j});
                found = true;
            }
            else found = false;

        } while(found);


        for(int[] zero : K){

            if(squareInCol[zero[1]] == zero[0]){
                squareInCol[zero[1]] = -1;
                squareInRow[zero[0]] = -1;
            }

            if(starredZeroesInRow[zero[0]]==zero[1]){
                squareInRow[zero[0]] = zero[1];
                squareInCol[zero[1]] = zero[0];
            }
        }

        Arrays.fill(starredZeroesInRow, -1);
        Arrays.fill(rowCovered, false);
        Arrays.fill(colCovered, false);
    }

    private void ReduceByMinUncoveredValue(){

        double minUncVal = Double.MAX_VALUE;

        for(int i=0; i<matrix.length; i++){
            if(rowCovered[i]) continue;
            for(int j=0; j<matrix.length; j++){
                if(colCovered[j]) continue;
                if(matrix[i][j]<minUncVal) minUncVal = matrix[i][j];
            }
        }

        if(minUncVal>0){
            for(int i=0; i<matrix.length; i++){
                for(int j=0; j<matrix[i].length; j++){
                    if(rowCovered[i]&&colCovered[j])            matrix[i][j]+=minUncVal;
                    else if(!rowCovered[i] && !rowCovered[j])   matrix[i][j]-=minUncVal;
                }
            }
        }
    }

    private int[] GetMainZeroes(){
        for(int i=0; i<matrix.length; i++){
            if(!rowCovered[i]) {
                for (int j = 0; j<matrix[i].length; j++){
                    if(matrix[i][j]==0 && !colCovered[j]){
                        starredZeroesInRow[i] = j;
                        return new int[]{i,j};
                    }
                }
            }
        }
        return null;
    }

    private boolean allColsCovered(){
        for(int i=0; i<colCovered.length; i++){
            if(!colCovered[i]) return false;
        }
        return true;
    }

    private void SubRows(){
        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[i].length; j++){
                if(matrix[i][j]==minColWeight[j]) minColWeight[j]-=minRowWeight[i];
                matrix[i][j]-=minRowWeight[i];
            }
            minRowWeight[i] = 0;
        }
    }

    private void SubCols(){
        for(int j=0; j<matrix[0].length; j++){
            for(int i=0; i<matrix.length; i++){
                if(matrix[i][j]==minRowWeight[i]) minRowWeight[i]-=minColWeight[j];
                matrix[i][j]-=minColWeight[j];
            }
            minColWeight[j] = 0;
        }
    }

    private void CoverColumns(){
        for(int i=0; i<squareInCol.length; i++){
            if(squareInCol[i]!=-1) colCovered[i] = true;
            else colCovered[i] = false;
        }
    }


    private void MarkZeros(){

        boolean[] rowSquare = new boolean[matrix.length];
        boolean[] colSquare = new boolean[matrix[0].length];

        for(int i=0; i<matrix.length; i++){
            for(int j=0; j<matrix[i].length; j++){
                if(matrix[i][j]==0 && !rowSquare[i] && !colSquare[j]){
                    rowSquare[i] = true;
                    colSquare[j] = true;
                    squareInRow[i] = j;
                    squareInCol[j] = i;

                    continue;
                }
            }
        }
    }
}
