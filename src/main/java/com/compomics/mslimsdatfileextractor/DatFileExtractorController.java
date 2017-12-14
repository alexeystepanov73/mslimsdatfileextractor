package com.compomics.mslimsdatfileextractor;

import java.awt.Cursor;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.List;
import javax.swing.JOptionPane;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.PropertiesConfiguration;

/**
 * The database login view controller.
 *
 * @author Niels Hulstaert
 */
public class DatFileExtractorController {

    //model
    private final PropertiesConfiguration propertiesConfiguration;
    private String dbUrl;
    private String dbUserName;
    private char[] dbPassword;
    private final String[] args;
    //view
    private DatFileExtractorDialog datFileExtractorDialog;

    /**
     * Constructor.
     *
     * @param args the command line arguments
     * @throws ConfigurationException the ConfigurationException
     * @throws IOException the IOException
     * @throws java.net.URISyntaxException
     */
    public DatFileExtractorController(String[] args) throws ConfigurationException, IOException, URISyntaxException {
        this.args = args;
        //load the properties file
        Path properties = getPropertiesFile();
        propertiesConfiguration = new PropertiesConfiguration(properties.toFile());
    }

    /**
     * Get the view of this controller.
     *
     * @return the DatabaseLoginDialog
     */
    public DatFileExtractorDialog getDatabaseLoginDialog() {
        return datFileExtractorDialog;
    }

    public void init() {
        //init view
        datFileExtractorDialog = new DatFileExtractorDialog(null, true);

        //set db url and user name from client properties
        datFileExtractorDialog.getDbUrlTextField().setText(propertiesConfiguration.getString("db.url"));
        datFileExtractorDialog.getDbUserNameTextField().setText(propertiesConfiguration.getString("db.username"));

        datFileExtractorDialog.getDbPasswordTextField().requestFocus();

        datFileExtractorDialog.getRunButton().addActionListener(e -> {
            dbUrl = datFileExtractorDialog.getDbUrlTextField().getText();
            dbUserName = datFileExtractorDialog.getDbUserNameTextField().getText();
            dbPassword = datFileExtractorDialog.getDbPasswordTextField().getPassword();
            String projectsString = datFileExtractorDialog.getProjectsTextField().getText();

            if (!dbUrl.isEmpty() && !dbUserName.isEmpty() && dbPassword.length != 0 && !projectsString.isEmpty()) {
                onLogin(projectsString);
            } else {
                JOptionPane.showMessageDialog(datFileExtractorDialog, "Please provide a database url, user name, password and projects.", "dat file extractor validation", JOptionPane.WARNING_MESSAGE);
            }
        });

        datFileExtractorDialog.getCloseButton().addActionListener(e -> {
            datFileExtractorDialog.dispose();
            System.exit(0);
        });

        datFileExtractorDialog.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(final WindowEvent we) {
                System.exit(0);
            }
        });
    }

    /**
     * Run the extractor in GUI or command line mode.
     */
    public void run() {
        if (args.length == 0) { //show GUI
            datFileExtractorDialog.setLocationRelativeTo(null);
            datFileExtractorDialog.setVisible(true);
        } else { //get the connection paramaters from the command line
            dbUserName = args[0];
            dbPassword = args[1].toCharArray();
            List<String> projects = Arrays.asList(args[2].split(","));

            Connection connection = getConnection(propertiesConfiguration.getString("db.driver"), propertiesConfiguration.getString("db.url"), dbUserName, String.valueOf(dbPassword));
            if (connection != null && !projects.isEmpty()) {
                try {
                    System.out.println("Started extracting...");

                    DatFileExtractor datFileExtractor = new DatFileExtractor();
                    datFileExtractor.extract(connection, projects);

                    System.out.println("Done extracting!");
                } catch (SQLException | IOException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                    System.out.println(ex.getMessage());

                    System.exit(0);
                }
            } else {
                System.out.println("Please provide a username, password, and one or more projects (comma separated).");
            }
        }
    }

    /**
     * Get the properties file.
     *
     * @return the properties file
     */
    private Path getPropertiesFile() throws URISyntaxException {
        Path jarPath = new File(DatFileExtractorController.class.getProtectionDomain().getCodeSource().getLocation().toURI().getPath()).getParentFile().toPath();
        Path propertiesPath = Paths.get(jarPath.toString(), "config/datfileextractor.properties");

        //if the file could not be found, fall back to the one in the jar
        if (!Files.exists(propertiesPath)) {
            propertiesPath = Paths.get(ClassLoader.getSystemResource("config/datfileextractor.properties").toURI().getPath());
        }

        return propertiesPath;
    }

    /**
     * Connect to the database and show a message dialog if unsuccessful.
     *
     * @param projectsString the comma separated string of projects
     */
    private void onLogin(String projectsString) {
        Connection connection = getConnection(propertiesConfiguration.getString("db.driver"), dbUrl, dbUserName, String.valueOf(dbPassword));
        if (connection != null) {
            checkForPropertiesUpdates();

            List<String> projects = Arrays.asList(projectsString.split(","));
            DatFileExtractor datFileExtractor = new DatFileExtractor();
            try {
                datFileExtractorDialog.setCursor(new Cursor(Cursor.WAIT_CURSOR));

                datFileExtractor.extract(connection, projects);

                datFileExtractorDialog.setCursor(new Cursor(Cursor.DEFAULT_CURSOR));

                JOptionPane.showMessageDialog(datFileExtractorDialog, "Done extracting!", "dat file extractor", JOptionPane.INFORMATION_MESSAGE);

                datFileExtractorDialog.dispose();
                System.exit(0);
            } catch (SQLException | IOException | ClassNotFoundException | InstantiationException | IllegalAccessException ex) {
                JOptionPane.showMessageDialog(null, "Something went wrong"
                        + System.lineSeparator() + ex.getMessage(), "extraction error", JOptionPane.WARNING_MESSAGE);
            }

        } else {
            JOptionPane.showMessageDialog(datFileExtractorDialog, "The database login attempt failed."
                    + System.lineSeparator() + "Please verify your credentials and connectivity and try again.", "Database login unsuccessful", JOptionPane.WARNING_MESSAGE);
            Arrays.fill(dbPassword, '0');
            datFileExtractorDialog.getDbPasswordTextField().selectAll();
            datFileExtractorDialog.getDbPasswordTextField().requestFocusInWindow();
        }
    }

    /**
     * Test the connection to the database. Return true if successful.
     *
     * @param driverClassName the db driver class name
     * @param url the db url
     * @param userName the db user name
     * @param password the db password
     * @return is the connection successful
     */
    private Connection getConnection(final String driverClassName, final String url, final String userName, final String password) {
        Connection connection = null;
        try {
            Class.forName(driverClassName);
            connection = DriverManager.getConnection(url, userName, password);
        } catch (ClassNotFoundException | SQLException ex) {
            System.out.println(ex.getMessage());
        }

        return connection;
    }

    /**
     * Check if the database properties in the client properties file have been
     * overwritten by the user. If so, ask the user whether or not to store the
     * changes to the client properties file.
     *
     */
    private void checkForPropertiesUpdates() {
        boolean urlChanged = false;
        boolean userNameChanged = false;

        //check for changes
        if (!propertiesConfiguration.getString("db.url").equals(dbUrl)) {
            urlChanged = true;
        }
        if (!propertiesConfiguration.getString("db.username").equals(dbUserName)) {
            userNameChanged = true;
        }

        //show dialog if necessary
        if (urlChanged || userNameChanged) {
            int result = JOptionPane.showConfirmDialog(datFileExtractorDialog, "The database url and/or user name differ from the ones stored"
                    + System.lineSeparator() + "in the client properties file (config/datfileextractor.properties)."
                    + System.lineSeparator() + "Do you want to save the current changes?", "Store database property", JOptionPane.YES_NO_OPTION);
            if (result == JOptionPane.YES_OPTION) {
                //store the changes
                if (urlChanged) {
                    propertiesConfiguration.setProperty("db.url", dbUrl);
                }
                if (userNameChanged) {
                    propertiesConfiguration.setProperty("db.username", dbUserName);
                }
                try {
                    propertiesConfiguration.save();
                } catch (ConfigurationException ex) {
                    System.out.println(ex.getMessage());
                }
            }
        }
    }

}
