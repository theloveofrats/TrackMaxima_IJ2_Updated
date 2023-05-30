package beats.ltr6.imagej;

import com.opencsv.CSVReader;
import ij.measure.Calibration;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class TrackIO {

    public static List<Track> ReadTracks(File file, Calibration calibration) throws IOException {
        Reader reader = Files.newBufferedReader(file.toPath());
        List<Track> tracksRead = new ArrayList<>();
        String[] line;

        Track currentTrack = null;
        int tracknum = -1;

        int pX,pY,pT,newTracknum;
        CSVReader csvIn = new CSVReader(reader);
        csvIn.readNext(); // Skip header

        while((line=csvIn.readNext())!=null){

            newTracknum = Integer.parseInt(line[1]);
            pX = (int) Math.round(calibration.getRawX(Double.parseDouble(line[3])));
            pY = (int) Math.round(calibration.getRawY(Double.parseDouble(line[4])));
            pT = (int) Math.round(Double.parseDouble(line[5])/calibration.frameInterval);

            Point pt = new Point(pX,pY,pT);

            if(newTracknum!=tracknum){
                if(currentTrack!=null) tracksRead.add(currentTrack);

                tracknum = newTracknum;
                currentTrack = new Track(pt,tracknum);
                pt.track = currentTrack;
            }
            else{
                currentTrack.points.add(pt);
            }
        }

        if(currentTrack!=null) tracksRead.add(currentTrack);
        csvIn.close();
        return tracksRead;
    }
}
