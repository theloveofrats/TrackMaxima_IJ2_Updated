package beats.ltr6.imagej;

import com.sun.org.apache.xalan.internal.xsltc.compiler.util.RealType;
import ij.IJ;
import ij.ImagePlus;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.img.Img;
import org.scijava.display.Display;
import org.scijava.display.DisplayService;
import org.scijava.ui.UIService;

import javax.swing.*;

public class TrackMaxima {

    public ImagePlus image;
    //Tracker_ClipOnWindow clipOn
    public TrackerForm uiForm;

    public Overlayer overlayer;
    public Analyser analyser;
    public Manicurer manicurer;
    public Tracker tracker;

    public TrackMaxima(ImagePlus image){
        final TrackMaxima tm = this;
        this.image = image;
        this.tracker = new Tracker(image, this);
        this.overlayer = new Overlayer(image, tracker);
        this.analyser = new Analyser(this);
        this.manicurer = new Manicurer(this, image);

        if(image.getNSlices()>1 && image.getNFrames()==1){
            image.setDimensions(image.getNChannels(), image.getNFrames(), image.getNSlices());
        }

        Thread th = new Thread(){
            public void run(){
                tm.uiForm  = new TrackerForm(tm);
            }
        };
        th.run();
    }
}
