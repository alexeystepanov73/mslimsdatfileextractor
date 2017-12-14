/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.compomics.mslimsdatfileextractor;

import java.awt.Color;
import java.awt.Dimension;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.Painter;
import javax.swing.UIManager;

/**
 *
 * @author niels
 */
public class MainClass {

    /**
     * The startup error message.
     */
    private static final String ERROR_MESSAGE = "An error occured during startup, please try again.";            

    /**
     * Private constructor.
     */
    private MainClass(String[] args) {
        launch(args);
    }

    /**
     * Main method.
     *
     * @param args the main method arguments
     */
    public static void main(final String[] args) {
        /*
         * Set the Nimbus look and feel
         */
        //<editor-fold defaultstate="collapsed" desc=" Look and feel setting code (optional) ">
        /*
         * If Nimbus (introduced in Java SE 6) is not available, stay with the
         * default look and feel. For details see
         * http://download.oracle.com/javase/tutorial/uiswing/lookandfeel/plaf.html
         */
        try {
            for (UIManager.LookAndFeelInfo info : UIManager.getInstalledLookAndFeels()) {
                if ("Nimbus".equals(info.getName())) {
                    UIManager.setLookAndFeel(info.getClassName());
                    break;
                }
            }
        } catch (ClassNotFoundException | InstantiationException | IllegalAccessException | javax.swing.UnsupportedLookAndFeelException ex) {
            System.out.println(ex.getMessage());
        }
        //</editor-fold>

        //set background color for JOptionPane and JPanel instances
        UIManager.getLookAndFeelDefaults().put("OptionPane.background", Color.WHITE);
        UIManager.getLookAndFeelDefaults().put("Panel.background", Color.WHITE);
        UIManager.getLookAndFeelDefaults().put("FileChooser.background", Color.WHITE);
        //set background color for JFileChooser instances
        UIManager.getLookAndFeelDefaults().put("FileChooser[Enabled].backgroundPainter",
                (Painter<JFileChooser>) (g, object, width, height) -> {
                    g.setColor(Color.WHITE);
                    g.draw(object.getBounds());
                });

        MainClass mainClass = new MainClass(args);
    }

    /**
     * Launch the client.
     *
     * @param contextPaths the spring context paths
     */
    private void launch(String[] args) {
        try {
            //init and show database login dialog for database login credentials
            DatFileExtractorController databaseLoginController = new DatFileExtractorController(args);
            databaseLoginController.init();
            databaseLoginController.run();
        } catch (Exception ex) {
            if (args.length == 0) {
                //add message to JTextArea
                JTextArea textArea = new JTextArea(ERROR_MESSAGE + System.lineSeparator() + System.lineSeparator() + ex.getMessage());
                //put JTextArea in JScrollPane
                JScrollPane scrollPane = new JScrollPane(textArea);
                scrollPane.setPreferredSize(new Dimension(600, 200));
                textArea.setEditable(false);
                textArea.setLineWrap(true);
                textArea.setWrapStyleWord(true);

                JOptionPane.showMessageDialog(null, scrollPane, "Dat file extractor startup error", JOptionPane.ERROR_MESSAGE);
            } else {
                System.out.println(ex.getMessage());
            }
            System.exit(0);
        }
    }

}
