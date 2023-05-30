package beats.ltr6.imagej;

import ij.IJ;
import ij.io.OpenDialog;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.exception.NotANumberException;
import org.scijava.display.Display;
import org.json.*;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileSystem;
import java.util.EventListener;
import java.util.List;

public class TrackerForm {
    private JTabbedPane Tabs;
    private JPanel masterPanel;
    private JPanel autoTrackingPanel;
    private JPanel overlayPanel;
    @Preference(PreferenceType.COMBO_BOX)
    private JComboBox LightDarkBox;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField ThresholdField;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField blurField;
    @Preference(PreferenceType.CHECK_BOX)
    private JCheckBox useDoGCheckBox;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField framesField;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField MaxSpeedField;
    private JButton trackButton;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField MinSpeedField;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField MinFramesField;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton trackingLinesCheck;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField trackFadeField;
    private JButton drawOverlayButton;
    @Preference(PreferenceType.COMBO_BOX)
    private JComboBox overlaySizeChooser;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton trackingCircleCheck;
    @Preference(PreferenceType.COLOUR_PANEL)
    private JPanel positiveColourPanel;
    @Preference(PreferenceType.COLOUR_PANEL)
    private JPanel negativeColourPanel;
    @Preference(PreferenceType.COLOUR_PANEL)
    private JPanel neutralColourPanel;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField calculationWindowField;
    @Preference(PreferenceType.COMBO_BOX)
    private JComboBox calcWindowTypeChooser;
    private JLabel cellCountLabel;
    @Preference(PreferenceType.TEXT_FIELD)
    private JTextField angleField;
    @Preference(PreferenceType.COMBO_BOX)
    private JComboBox directionSpeedDD;
    private JButton importTrackButton;
    private JButton displayTrackTableButton;
    private JButton displaySummaryButton;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton speedRadioButton;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton cosThetaRadioButton;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton directednessRadioButton;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton chemotacticIndexRadioButton;
    @Preference(PreferenceType.CHECK_BOX)
    private JCheckBox medianSubtraction;
    private JButton msdButton;
    private JButton selectTargetPointButton;
    private JButton clearTargetPointButton;
    private JButton graphMomentsButton;
    private JButton drawNumbersButton;
    @Preference(PreferenceType.COMBO_BOX)
    private JComboBox dotAlgorithmComboBox;
    private JTextField minPointSepField;
    private JTextField speedFractionField;
    @Preference(PreferenceType.RADIO_BUTTON)
    private JRadioButton projectedSpeedRadioButton;
    private JButton deleteTracksButton;
    private JButton mergeTracksButton;
    private JButton trackingDoneButton;
    private JButton newTrackButton;
    private JButton cutAfterButton;
    private JButton cutBeforeButton;
    private JPanel handTrackingPanel;
    private JPanel analysisPanel;
    private JPanel advancedPanel;
    private JLabel calibrationWarning;
    private JButton calibrationOKButton;
    private JCheckBox previewCheckBox;
    private JButton meanSquaredDisplacementButton;
    private JButton spiderPlotButton;
    private JButton transitionMatrixButton;
    private JButton overwriteDefaultSettingsButton;
    private JButton projectedSpeedHeatmapButton;
    private JRadioButton maskOverlayButton;
    private JCheckBox saveMasksCheckBox;

    private static String warningTxt = "<html>Image not calibrated! <br>Fix under Image>Properties";

    public TrackMaxima tm;


    public TrackerForm(TrackMaxima tm){
        this.tm = tm;
        DisplayUI();
        SetupColourPickers();
        AddActionListeners();

        try{
            this.LoadPrefs();
        }
        catch (Exception e){

        }
        UpdateTrackInformation();
        UpdateFieldParameters(true,true,true);
    }

    private void SetupColourPickers() {
        // Set up colour pickers.
        positiveColourPanel.addMouseListener(new ColourPanelAdapter(positiveColourPanel));
        negativeColourPanel.addMouseListener(new ColourPanelAdapter(negativeColourPanel));
        neutralColourPanel.addMouseListener(new ColourPanelAdapter(neutralColourPanel));
    }

    public void UpdateTrackInformation(){

        if(tm.image.getCalibration().frameInterval==0 || tm.image.getCalibration().getUnit().equals("pixel")){
            calibrationWarning.setText(warningTxt);
            calibrationOKButton.setVisible(true);
        }
        else{
            calibrationWarning.setText("");
            calibrationOKButton.setVisible(false);
        }

        if(tm.overlayer.targetPoint==null){
            clearTargetPointButton.setEnabled(false);
            angleField.setEnabled(true);
        }
        else{
            clearTargetPointButton.setEnabled(true);
            angleField.setEnabled(false);
        }

        if(tm.tracker.GetTracks()!=null && tm.tracker.GetTracks().size()>0) {
            displayTrackTableButton.setEnabled(true);
        }
        else displayTrackTableButton.setEnabled(false);

        if(tm.manicurer.currentState== Manicurer.ControlState.NONE){
            trackingDoneButton.setEnabled(false);
            deleteTracksButton.setEnabled(true);
            mergeTracksButton.setEnabled(true);
            trackButton.setEnabled(true);
            newTrackButton.setEnabled(true);
            cutBeforeButton.setEnabled(true);
            cutAfterButton.setEnabled(true);

            Tabs.setEnabledAt(0,true);
            Tabs.setEnabledAt(1,true);
            Tabs.setEnabledAt(3,true);

            if(tm.tracker.GetTracks()!=null &&tm.tracker.GetTracks().size()>0)  Tabs.setEnabledAt(4,true);
            else{
                Tabs.setEnabledAt(4,false);
            }

            /*analysisPanel.setEnabled(true);
            overlayPanel.setEnabled(true);
            autoTrackingPanel.setEnabled(true);
            advancedPanel.setEnabled(true); */
        }
        else{
            trackingDoneButton.setEnabled(true);
            deleteTracksButton.setEnabled(false);
            mergeTracksButton.setEnabled(false);
            trackButton.setEnabled(false);
            newTrackButton.setEnabled(false);
            cutBeforeButton.setEnabled(false);
            cutAfterButton.setEnabled(false);


            Tabs.setEnabledAt(0,false);
            Tabs.setEnabledAt(1,false);
            Tabs.setEnabledAt(3,false);
            Tabs.setEnabledAt(4,false);

            /*analysisPanel.setEnabled(false);
            overlayPanel.setEnabled(false);
            autoTrackingPanel.setEnabled(false);
            advancedPanel.setEnabled(false);*/
        }
        this.cellCountLabel.setText("Tracks: "+tm.tracker.GetTracks().size());
    }

    public void Hide(){
        masterPanel.setVisible(false);
    }

    private void UpdateFieldParameters(boolean tracking, boolean overlayer,boolean analyser){
        if(tracking){

            double min = 0;
            double max = Double.MAX_VALUE;
            if(tm.image.getBitDepth()==8) max = 255;
            if(tm.image.getBitDepth()==16) max = 65535;

            double threshold = TryParseAsDouble(ThresholdField, tm.tracker.threshold, min, max);
            double blur      = TryParseAsDouble(blurField, tm.tracker.blur);
            double maxSpeed  = TryParseAsDouble(MaxSpeedField, tm.tracker.maxSpeed);
            double minSpeed  = TryParseAsDouble(MinSpeedField, tm.tracker.minSpeed);
            double minSep  = TryParseAsDouble(minPointSepField, tm.tracker.minPointSep);
            double speedfraction  = TryParseAsDouble(speedFractionField, tm.tracker.speedfraction);

            int minFrames    = TryParseAsInt(MinFramesField, tm.tracker.minFrames);
            int maxMissing   = TryParseAsInt(framesField, tm.tracker.maxMissing);
            int dotAl        = dotAlgorithmComboBox.getSelectedIndex();

            boolean dog      = useDoGCheckBox.isSelected();
            boolean dark     = LightDarkBox.getSelectedIndex()==1;
            boolean med_sub  = medianSubtraction.isSelected();
            boolean preview  = previewCheckBox.isSelected();

            tm.tracker.SetTrackingParameters(dark,threshold,blur,dog,med_sub,maxMissing,maxSpeed,minSpeed,speedfraction, minFrames, minSep, dotAl, preview);
        }
        if(overlayer){
            boolean lines   = trackingLinesCheck.isSelected();
            boolean circles = trackingCircleCheck.isSelected();
            boolean masks = maskOverlayButton.isSelected();

            double    fade     = TryParseAsDouble(trackFadeField,tm.overlayer.fade);
            double    angle    = TryParseAsDouble(angleField,    tm.overlayer.angle);
            int overlaySize    = overlaySizeChooser.getSelectedIndex();
            int calcWindowSize = TryParseAsInt(calculationWindowField, tm.overlayer.calcWindowSize);
            int calcWindowType = calcWindowTypeChooser.getSelectedIndex();

            boolean direction = directionSpeedDD.getSelectedIndex()==0;

            Color pos = positiveColourPanel.getBackground();
            Color neg = negativeColourPanel.getBackground();
            Color neu = neutralColourPanel.getBackground();

            tm.overlayer.SetOverlayerParameters(lines,  circles, masks, direction, fade, angle,  overlaySize,  calcWindowSize,  calcWindowType, pos, neg, neu);
        }
        if(analyser){
            tm.analyser.cos_theta = this.cosThetaRadioButton.isSelected();
            tm.analyser.speed     = this.speedRadioButton.isSelected();
            tm.analyser.directedness = this.directednessRadioButton.isSelected();
            tm.analyser.ci = this.chemotacticIndexRadioButton.isSelected();
            tm.analyser.projectedSpeed = this.projectedSpeedRadioButton.isSelected();
        }
    }

    private void AddActionListeners(){

        // Things that update the preview
        ActionListener trackerUpdate = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(true,false,false);
            }
        };
        FocusListener focusTrackerUpdate = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                UpdateFieldParameters(true,false,false);
            }
        };
        ActionListener overlayerUpdate = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                if(tm.image.getOverlay()!=null && tm.image.getOverlay().size()>0) {
                    tm.overlayer.MakeOverlayFromResults();
                }
            }
        };
        FocusListener focusOverlayerUpdate = new FocusAdapter() {
            @Override
            public void focusLost(FocusEvent e) {
                UpdateFieldParameters(false,true,true);
                if(tm.image.getOverlay()!=null && tm.image.getOverlay().size()>0) {
                    tm.overlayer.MakeOverlayFromResults();
                }
            }
        };

        useDoGCheckBox.addActionListener(trackerUpdate);
        previewCheckBox.addActionListener(trackerUpdate);
        LightDarkBox.addActionListener(trackerUpdate);
        blurField.addActionListener(trackerUpdate);
        ThresholdField.addActionListener(trackerUpdate);
        medianSubtraction.addActionListener(trackerUpdate);

        ThresholdField.addFocusListener(focusTrackerUpdate);
        blurField.addFocusListener(focusTrackerUpdate);

        angleField.addActionListener(overlayerUpdate);
        angleField.addFocusListener(focusOverlayerUpdate);
        trackFadeField.addActionListener(overlayerUpdate);
        trackFadeField.addFocusListener(focusOverlayerUpdate);

        overwriteDefaultSettingsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                try{
                    SavePrefs();
                    IJ.log("Settings written!");
                }
                catch (Exception ex){}
            }
        });

        importTrackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                OpenDialog dia = new OpenDialog("Select track file");
                if(dia.getPath()==null) return;

                try{
                    tm.tracker.SetTracks(TrackIO.ReadTracks(new File(dia.getPath()), tm.image.getCalibration()));
                    tm.image.killRoi();
                }
                catch(IOException error){
                    error.printStackTrace();
                    return;
                }
                UpdateTrackInformation();
                tm.overlayer.MakeOverlayFromResults();
            }
        });

        displayTrackTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.SaveTracksToFile("DOESN\'T WORK YET");
            }
        });
        msdButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.DoAC(tm.tracker.GetTracks());
            }
        });
        meanSquaredDisplacementButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.DoMSD(tm.tracker.GetTracks());
            }
        });
        transitionMatrixButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.MakeTransitionMatrix();
            }
        });
        projectedSpeedHeatmapButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.DoTSHeatMap(tm.tracker.GetTracks());
            }
        });
        spiderPlotButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.DoSpider(tm.tracker.GetTracks());
            }
        });

        drawNumbersButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                tm.overlayer.DrawNumbers();
            }
        });

        graphMomentsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,true);
                tm.analyser.GraphMoments();
                tm.analyser.MakeSpatialAveragesGraph();

            }
        });

        displaySummaryButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                UpdateFieldParameters(false,true,true);
                tm.analyser.DisplaySummary();
            }
        });

        trackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                UpdateFieldParameters(true,true,false);
                Thread thread = new Thread(tm.tracker);
                thread.start();
            }
        });

        calibrationOKButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

                UpdateTrackInformation();
            }
        });
        Tabs.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if(previewCheckBox.isSelected()){
                    //IJ.log("Changed tab!");
                    tm.image.deleteRoi();
                }
                if(Tabs.getSelectedIndex()==3){
                    if(!tm.analyser.warningsDone) {
                        if (!tm.tracker.currentlyTracking) {
                            if(tm.tracker.NTracks()>1){
                                tm.analyser.DoWarnings();
                            }
                        }
                    }
                }
            }
        });

        ActionListener overlayListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                UpdateFieldParameters(false,true,false);
                tm.overlayer.MakeOverlayFromResults();
            }
        };

        newTrackButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.TRACK);
            }
        });

        deleteTracksButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.DELETE);
            }
        });

        mergeTracksButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.MERGE);
            }
        });

        cutAfterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.CUT_AFTER);
            }
        });

        cutBeforeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.CUT_BEFORE);
            }
        });

        mergeTracksButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.MERGE);
            }
        });

        trackingDoneButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                tm.manicurer.SetState(Manicurer.ControlState.NONE);
            }
        });

        selectTargetPointButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                GetCoordinateClick();
            }
        });

        clearTargetPointButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e){
                ClearCoordinates();
            }
        });
        
        drawOverlayButton.addActionListener(overlayListener);
    }

    private void DisplayUI(){
        JFrame frame = new JFrame("Tracker Window");
        frame.setContentPane(masterPanel);
        frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        frame.pack();
        int posX = tm.image.getWindow().getX()+tm.image.getWindow().getWidth();
        int posY = tm.image.getWindow().getY();

        frame.setLocation(posX,posY);
        frame.setVisible(true);
    }

    private double TryParseAsDouble(JTextField field, double dflt){
        return TryParseAsDouble(field,dflt,0,Double.MAX_VALUE);
    }
    private double TryParseAsDouble(JTextField field, double dflt, double min, double max){
        double dbl = 0;
        try {
            dbl = Double.parseDouble(field.getText());
            if(dbl<min) dbl = 0;
            if(dbl>max) dbl = max;
            return dbl;
        }
        catch (NumberFormatException e){
            field.setText(Double.toString(dflt));
            return  dflt;
        }
    }

    private int TryParseAsInt(JTextField field, int dflt){
        int dbl = 0;
        try {
            dbl = Integer.parseInt(field.getText());
            return dbl;
        }
        catch (NumberFormatException e){
            field.setText(Integer.toString(dflt));
            return  dflt;
        }
    }

    private void SavePrefs() throws Exception{

        String home = System.getProperty("user.home");
        File file = new File(home+File.separator+"ijmaxtracker_prefs.txt");
        //IJ.log(file.getPath());
        if(!file.exists()){
            //file.mkdirs();
            if(file.createNewFile()){

            }
            else{
                //IJ.log("Preferences file creation failed");
                return;
            }
        }

        JSONObject obj = new JSONObject();
        Field[] fields = this.getClass().getDeclaredFields();
        //IJ.log("# Fields: "+fields.length);
        for(Field field : fields){
            if(Modifier.isPrivate(field.getModifiers())) field.setAccessible(true);
            //IJ.log(field.getName());
            if(field.getAnnotation(Preference.class)!=null){
                //IJ.log("Found pref annotation");
                Preference preference = field.getAnnotation(Preference.class);
                PreferenceType prefType = preference.value();
                String key = field.getName();

                switch (prefType){
                    case CHECK_BOX:
                        JCheckBox box = (JCheckBox) field.get(this);
                        obj.put(key, box.isSelected());
                        break;
                    case RADIO_BUTTON:
                        JRadioButton btn = (JRadioButton) field.get(this);
                        obj.put(key, btn.isSelected());
                        break;
                    case COMBO_BOX:
                        JComboBox combo = (JComboBox) field.get(this);
                        obj.put(key, combo.getSelectedIndex());
                        break;
                    case COLOUR_PANEL:
                        JPanel panel = (JPanel) field.get(this);

                        Color clr = panel.getBackground();
                        obj.put(key+"_r", clr.getRed());
                        obj.put(key+"_g", clr.getGreen());
                        obj.put(key+"_b", clr.getBlue());
                        obj.put(key+"_a", clr.getAlpha());
                        break;
                    case TEXT_FIELD:
                        JTextField fld = (JTextField) field.get(this);
                        obj.put(key, fld.getText());
                        break;
                }
                //IJ.log(obj.toString());
            }
        }


        FileWriter fw = new FileWriter(file);
        BufferedWriter writer = new BufferedWriter(fw);
        obj.write(writer);
        writer.flush();
        writer.close();
    }

    private void LoadPrefs() throws Exception{

        String home = System.getProperty("user.home");
        File file = new File(home+File.separator+"ijmaxtracker_prefs.txt");
        if(!file.exists()) return;

        String content = FileUtils.readFileToString(file,"utf-8");

        JSONObject obj = new JSONObject(content);

        Field[] fields = this.getClass().getDeclaredFields();
        for(Field field : fields){
            if(Modifier.isPrivate(field.getModifiers())) field.setAccessible(true);
            if(field.getAnnotation(Preference.class)!=null){

                Preference preference = field.getAnnotation(Preference.class);
                PreferenceType prefType = preference.value();
                String key = field.getName();
                //IJ.log("Working on "+key);
                switch (prefType){
                    case CHECK_BOX:
                        JCheckBox box = (JCheckBox) field.get(this);
                        boolean bValue = obj.getBoolean(key);
                        box.setSelected(bValue);
                        //IJ.log("Set "+key+" to "+bValue);
                        break;
                    case RADIO_BUTTON:
                        JRadioButton btn = (JRadioButton) field.get(this);
                        bValue = obj.getBoolean(key);
                        btn.setSelected(bValue);
                        //IJ.log("Set "+key+" to "+bValue);
                        break;
                    case COMBO_BOX:
                        JComboBox combo = (JComboBox) field.get(this);
                        int iValue = obj.getInt(key);
                        combo.setSelectedIndex(iValue);
                        //IJ.log("Set "+key+" to "+iValue);
                        break;
                    case COLOUR_PANEL:
                        int r = obj.getInt(key+"_r");
                        int g = obj.getInt(key+"_g");
                        int b = obj.getInt(key+"_b");
                        int a = obj.getInt(key+"_a");
                        JPanel panel = (JPanel) field.get(this);
                        panel.setBackground(new Color(r,g,b,a));
                        break;
                    case TEXT_FIELD:
                        JTextField fld = (JTextField) field.get(this);
                        String sValue = obj.getString(key);
                        fld.setText(sValue);
                        //IJ.log("Set "+key+" to "+sValue);
                        break;
                }

            }
        }
    }

    private void ClearCoordinates(){
        tm.overlayer.targetPoint = null;
        tm.overlayer.pointTarget = false;
        UpdateTrackInformation();
        tm.overlayer.MakeOverlayFromResults();
    }


    private void GetCoordinateClick(){

        tm.image.killRoi();
        String sTool = IJ.getToolName();
        IJ.setTool("multipoint");

        MouseListener ada = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {

                if (e.getButton() == MouseEvent.BUTTON1) {
                    tm.overlayer.pointTarget = true;
                    IJ.wait(10);
                    int x = tm.image.getRoi().getPolygon().xpoints[0];
                    int y = tm.image.getRoi().getPolygon().ypoints[0];
                    tm.image.killRoi();

                    tm.overlayer.targetPoint = new int[]{x,y};
                    UpdateTrackInformation();
                    tm.overlayer.MakeOverlayFromResults();
                }
                else{
                    tm.overlayer.pointTarget = false;
                }
                IJ.setTool(sTool);
                tm.image.getCanvas().removeMouseListener(this);
            }
        };

        tm.image.getCanvas().addMouseListener(ada);
    }

    private void createUIComponents() {
        // TODO: place custom component creation code here
    }
}
