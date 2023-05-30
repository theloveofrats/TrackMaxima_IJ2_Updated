package beats.ltr6.imagej;

import ij.plugin.frame.ColorPicker;
import org.jhotdraw.color.ColorWheelChooser;

import javax.swing.*;
import javax.swing.colorchooser.AbstractColorChooserPanel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;

public class ColourPanelAdapter implements MouseListener {

    JPanel panel;
    JFrame chooserFrame = new JFrame("Select Colour");
    Color  color;
    JColorChooser chooser = new JColorChooser();
    //ColorWheelChooser wheelChooser = new ColorWheelChooser();
    

    public ColourPanelAdapter(JPanel panel){
        this.panel = panel;
        this.color = panel.getBackground();

        this.chooser.setChooserPanels(new AbstractColorChooserPanel[]{chooser.getChooserPanels()[2]});
        this.chooser.setPreviewPanel(new JPanel());
        this.chooser.getSelectionModel().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                color = chooser.getSelectionModel().getSelectedColor();
                panel.setBackground(color);
            }
        });

        this.chooserFrame.setContentPane(chooser);
        this.chooserFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
        this.chooserFrame.pack();
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        this.chooser.setColor(panel.getBackground());
        this.chooserFrame.setVisible(true);
    }

    @Override
    public void mousePressed(MouseEvent e){

    }

    @Override
    public void mouseReleased(MouseEvent e){

    }

    @Override
    public void mouseEntered(MouseEvent e){

    }

    @Override
    public void mouseExited(MouseEvent e){

    }
}
