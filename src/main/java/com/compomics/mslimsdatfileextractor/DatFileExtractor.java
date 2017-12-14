package com.compomics.mslimsdatfileextractor;

import org.apache.commons.io.FileUtils;

import javax.swing.*;
import java.io.*;
import java.sql.*;
import java.util.zip.GZIPInputStream;

public class DatFileExtractor {

    /**
     * Extract the dat files for the given mslims projects.
     *
     * @param connection the database connection
     * @param projectNumbers the list of project numbers
     * @throws SQLException
     * @throws IOException
     * @throws ClassNotFoundException
     * @throws InstantiationException
     * @throws IllegalAccessException
     */
    public void extract(Connection connection, java.util.List<String> projectNumbers) throws SQLException, IOException, ClassNotFoundException, InstantiationException, IllegalAccessException {
        File datFileOutputFolder = new File(System.getProperty("user.home") + File.separator + "datfileoutput");
        if (projectNumbers.isEmpty()) {
            String projectNumber = JOptionPane.showInputDialog("Specify the project number");
            if (!datFileOutputFolder.exists()) {
                datFileOutputFolder.mkdir();
            }
            PreparedStatement stmt = connection.prepareStatement("select d.filename,d.file from datfile as d join (select distinct l_datfileid as result from identification as i, spectrum as s where s.spectrumid = i.l_spectrumid and s.l_projectid = " + projectNumber + ") as r on r.result = d.datfileid");
            File toFile = new File(datFileOutputFolder + File.separator + projectNumber);
            if (!toFile.exists()) {
                toFile.mkdir();
            }
            ResultSet rs = stmt.executeQuery();
            extractForProject(toFile, rs);
        } else {
            extractForProjects(connection, datFileOutputFolder, projectNumbers);
        }        
    }

    private void extractForProjects(Connection connection, File datFileOutputFolder, java.util.List<String> projectNumbers) throws SQLException, IOException {
        for (String projectNumber : projectNumbers) {
            PreparedStatement stmt = connection.prepareStatement("select d.filename,d.file from datfile as d join (select distinct l_datfileid as result from identification as i, spectrum as s where s.spectrumid = i.l_spectrumid and s.l_projectid = " + projectNumber + ") as r on r.result = d.datfileid");
            File outputDirectory = new File(datFileOutputFolder + File.separator + projectNumber);
            if (!outputDirectory.exists()) {
                outputDirectory.mkdir();
            }

            ResultSet rs = stmt.executeQuery();

            extractForProject(outputDirectory, rs);
        }
    }

    /**
     * Extract the dat files from the result set to the given output directory.
     *
     * @param outputDirectory the output directory
     * @param resultSet the result set
     * @throws SQLException
     * @throws IOException
     */
    private static void extractForProject(File outputDirectory, ResultSet resultSet) throws SQLException, IOException {
        while (resultSet.next()) {
            File datFile = new File(outputDirectory + File.separator + resultSet.getString(1));

            System.out.println(datFile.getAbsolutePath());

            unzipAndWrite(resultSet.getBytes(2), datFile);
        }
    }

    /**
     * Write the byte array to file.
     *
     * @param bytes the byte array
     * @param file the file to be written to
     * @throws java.io.IOException exception thrown in case of an IO problem
     */
    private static void write(final byte[] bytes, final File file) throws IOException {
        FileUtils.writeByteArrayToFile(file, bytes);
    }

    /**
     * Unzip and write to a byte array.
     *
     * @param bytes the byte array
     * @return the unzipped byte array
     * @throws java.io.IOException exception thrown in case of an IO problem
     */
    private static byte[] unzip(final byte[] bytes) throws IOException {
        byte[] unzippedBytes;

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
                ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
                GZIPInputStream gzipis = new GZIPInputStream(bais)) {
            //unzip
            //this method uses a buffer internally
            org.apache.commons.io.IOUtils.copy(gzipis, baos);

            unzippedBytes = baos.toByteArray();
        }

        return unzippedBytes;
    }

    /**
     * Unzip and write the byte array to file.
     *
     * @param bytes the byte array
     * @param file the file to be written to
     * @throws java.io.IOException exception thrown in case of an IO problem
     */
    private static void unzipAndWrite(final byte[] bytes, final File file) throws IOException {
        //first unzip byte array
        byte[] unzippedBytes = unzip(bytes);

        //then write to file
        write(unzippedBytes, file);
    }
}
