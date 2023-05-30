package beats.ltr6.imagej;


import java.io.File;

import ij.ImagePlus;
import net.imagej.Dataset;
import net.imagej.ImageJ;
import net.imagej.ImgPlus;
import net.imagej.ops.OpService;
import net.imglib2.RandomAccessibleInterval;
import net.imglib2.img.Img;
import net.imglib2.img.display.imagej.ImageJFunctions;
import net.imglib2.type.numeric.RealType;
import org.scijava.command.Command;
import org.scijava.command.CommandService;
import org.scijava.plugin.Parameter;
import org.scijava.plugin.Plugin;
import org.scijava.ui.UIService;

/**
 *
 *
 * Author: Luke Tweedy
 *
 */
@Plugin(type = Command.class, menuPath = "Plugins>Beatson>Track Maxima (IJ2)")
public class TrackMaxima_IJ2<T extends RealType<T>> implements Command {
    //
    // Feel free to add more parameters here...
    //

    @Parameter
    private ImagePlus image;

    @Override
    public void run() {
        //Img<T> img = (Img<T>)currentData.getImgPlus();
        //ImagePlus image = ImageJFunctions.wrap(img, "Working");
        new TrackMaxima(image);
    }

    /**
     * This main function serves for development purposes. It allows you to run the plugin immediately out of 
     * your integrated development environment 
     *
     * @param args whatever, it's ignored
     * @throws Exception
     */
    public static void main(final String... args) throws Exception {
        // create the ImageJ application context with all available services
        final ImageJ ij = new ImageJ();
        ij.ui().showUI();

        String testPath = "/Users/luke/Desktop/Cell Tracker Tests/Dicty_UA_20x_18s_2.tif";

        // load the dataset
        final Dataset dataset = ij.scifio().datasetIO().open(testPath);

        // Show the image
        ij.ui().show(dataset);

        // invoke the plugins run-function for testing...
        ij.command().run(TrackMaxima_IJ2.class, true);
    }
}
