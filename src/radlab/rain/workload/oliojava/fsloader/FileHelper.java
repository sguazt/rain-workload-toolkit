/* The contents of this file are subject to the terms
 * of the Common Development and Distribution License
 * (the License). You may not use this file except in
 * compliance with the License.
 *
 * You can obtain a copy of the License at
 * http://www.sun.com/cddl/cddl.html or
 * install_dir/legal/LICENSE
 * See the License for the specific language governing
 * permission and limitations under the License.
 *
 * When distributing Covered Code, include this CDDL
 * Header Notice in each file and include the License file
 * at install_dir/legal/LICENSE.
 * If applicable, add the following below the CDDL Header,
 * with the fields enclosed by brackets [] replaced by
 * your own identifying information:
 * "Portions Copyrighted [year] [name of copyright owner]"
 *
 * $Id$
 *
 * Copyright 2005-2009 Sun Microsystems Inc. All Rights Reserved
 *
 * Modifications by: Marco Guazzone
 * 1) Adapted to the Java version of Olio.
 * 2) Added note to class doc about the origin of the code.
 */
//package com.sun.faban.harness.util;
package radlab.rain.workload.oliojava.fsloader;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * File utilities. A collection of static methods to deal with file operations.
 * <br/>
 * NOTE: Code based on {@code com.sun.faban.harness.util.FileHelper} class
 * and adapted for the Java version of Olio.
 */
public class FileHelper {
    
    private static final Logger logger = Logger.getLogger(FileHelper.class.getName());

    /**
     * Copies a file.
     * @param srcFile  - the full pathname of the source file
     * @param destFile  - the full pathname of the destination file
     * @param append - should destination file be appended with source file
     * @return Whether the copy succeeded
     */
    public static boolean copyFile(String srcFile, String destFile,
                                   boolean append) {
        try {
            FileChannel src = (new FileInputStream(srcFile)).getChannel();
            FileChannel dest = (new FileOutputStream(destFile)).getChannel();
            if (append)
                dest.position(dest.size());
            src.transferTo(0, src.size(), dest);
            dest.close();
            src.close();
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Could not copy " + srcFile + " to " +
                                     destFile, e);
            return false;
        }
        return true;
    }

    /**
     * This method opens, traverses through the file and
     * finds the token and replaces it with new value
     * This method updates only the first occurrence of
     * the token in the file to eliminate cases where it
     * changes props defined like PROP=$PROP:MYPROP.
     * @param fileName The full pathname of the file
     * @param token Token to find
     * @param replacement The replacement string
     * @param backupFileName If needed pass a backup file name
     * @return Whether there is at least one replacement
     */
    public static boolean tokenReplace(String fileName, String token, String replacement, String backupFileName) {

        String tmpFile = System.getProperty("java.io.tmpdir") + File.separator + ".FileHelper";

        //copy the file
        if(backupFileName == null)
            backupFileName = tmpFile;

        if(!FileHelper.copyFile(fileName, backupFileName, false))
            return false; // Failed to backup the file

        System.out.println("Token : " + token);
        System.out.println("Replacement : " + replacement);

        try {
            BufferedReader in = new BufferedReader(new FileReader(backupFileName));
            BufferedWriter out = new BufferedWriter(new FileWriter(fileName));
            String line;
            boolean replaced = false;
            while((line = in.readLine()) != null) {
                // replace only the first occurrence of the token
                if((!replaced) && (line.indexOf(token) != -1)) {
                    System.err.println("FileHelper.tokenReplace : replacing " + token + " with " + replacement);
                    StringBuffer sb = new StringBuffer();
                    // to escape special chars in token
                    for(int i = 0; i < token.length(); i++) {
                        if(Character.isLetterOrDigit(token.charAt(i)))
                            sb.append(token.charAt(i));
                        else
                            sb.append("\\").append(token.charAt(i));
                    }

                    // to escape special chars in replacement string
                    token = sb.toString();
                    sb = new StringBuffer();
                    for(int i = 0; i < replacement.length(); i++) {
                        if(Character.isLetterOrDigit(replacement.charAt(i)))
                            sb.append(replacement.charAt(i));
                        else
                            sb.append("\\").append(replacement.charAt(i));
                    }
                    replacement = sb.toString();

                    line = line.replaceAll(token, replacement);
                    replaced = true;
                }
                // Write the line to file
                out.write(line, 0, line.length());
                out.newLine();
            }
            in.close();
            out.close();

            // Delete the temp file
            if(tmpFile.equals(backupFileName))
                (new File(tmpFile)).delete();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed with {0}", e);
            logger.log(Level.FINE, "Exception", e);
            return false;
        }
        return true;
    }

    /**
     * This method opens, traverses through the file and
     * finds the token, it will avoid comments when searching.
     * @param fileName The full pathname of the file
     * @param token Token to search for
     * @return true if found, false otherwise
     */
    public static boolean isInFile(String fileName, String token) {
        boolean found = false;

        try {
            BufferedReader in = new BufferedReader(new FileReader(fileName));
            String line;
            while((line = in.readLine()) != null) {
                // Avoid comments
                if(line.length() > token.length() &&
                   Character.isLetterOrDigit(line.charAt(0)) && line.indexOf(token) != -1) {
                    found = true;
                    break;
                }
            }
            in.close();
        }
        catch (Exception e) {
            logger.log(Level.SEVERE, "Failed with {0}", e);
            logger.log(Level.FINE, "Exception", e);
            return false;
        }
        return found;
    }

    /**
     * This  method is used to delete a directory and 
     * recursively delete files and subdirectories within it.
     *
     * @param file The file or directory to delete
     * @return Whether the delete succeeded
     */
    public static boolean recursiveDelete(File file) {
        // Does a post-order traversal of the directory hierarchy and deletes
        // all the nodes in its path.
	
        // Logging file deletions at fine so we can turn on if there's
        // anything spooky. We use an exception to capture the stack, but
        // do not really throw the exception. After all, nothing is wrong.
        Exception te = new Exception("Deleting all files in " + file.getName());
        logger.log(Level.FINE, te.getMessage(), te);

        boolean success = true;
 
	    try {
            if (file.isDirectory()) {
                File[] list = file.listFiles();
                for (int i = 0; i < list.length; i++)
                    if (!recursiveDelete(list[i])) {
                        logger.log(Level.SEVERE, "Delete failed for file {0}", file.getPath());
                        success = false;
                    }
            }
            if (!file.delete()) {
                logger.log(Level.WARNING, "Delete failed for file {0}", file.getPath());
                success = false;
            }
        } catch(Exception e) {
            logger.log(Level.SEVERE, "Delete failed", e);
            success = false;
        }
        return success;
    }

    /**
     * Deletes all files matched by the filter in a certain directory.
     * @param dir The directory to look for files to delete
     * @param filter The file name filter
     * @return True if all deletes succeeded, false otherwise
     */
    public static boolean delete(File dir, FileFilter filter) {
        boolean success = true;

        try {
            if (!dir.isDirectory())
                return false;
            File[] list = dir.listFiles(filter);
            for (File file : list)
                if (!recursiveDelete(file)) {
                    logger.log(Level.SEVERE, "Delete failed for file {0}", file.getPath());
                    success = false;
                }
        } catch (Exception e) {
            logger.log(Level.SEVERE, "Delete failed", e);
            success = false;
        }
        return success;
    }

    /**
     * This method is used to delete a directory and
     * recursively delete files and subdirectories within it.
     *
     * @param parentDir The file object corresponding to the parent directory
     * @param name Name of the directory to be deleted
     * @return Whether the delete succeeded
     */
    public static boolean recursiveDelete(File parentDir, String name) {
        return recursiveDelete(new File(parentDir, name));
    }

    /**
     * Copies a file from source to dest. If src is a directory, the whole
     * directory tree is copied.
     * @param src The source file
     * @param dest The dest file, must not exist before calling method
     * @return true if copy succeeded, false afterwise
     */
    public static boolean recursiveCopy(File src, File dest) {
        if (src.isDirectory()) {
            if (!dest.exists() && !dest.mkdir())
                return false;
            File[] files = src.listFiles();
            for (File s : files) {
                File d = new File(dest, s.getName());
                if (!recursiveCopy(s, d))
                    return false;
            }
        } else {
            return copyFile(src.getAbsolutePath(), dest.getAbsolutePath(),
                            false);
        }
        return true;
    }

    private static void addEntry(JarOutputStream out, String baseDir,
                                 String name, byte[] buffer)
            throws IOException {
        File input = new File(baseDir + '/' + name);
        if (input.isDirectory()) {
            JarEntry jarEntry = new JarEntry(name + '/');
            jarEntry.setTime(input.lastModified());
            out.putNextEntry(jarEntry);
            out.closeEntry();
            String[] entries = input.list();
            for (String entry : entries)
                addEntry(out, baseDir, name + '/' + entry, buffer);
        } else {
            JarEntry jarEntry = new JarEntry(name);
            jarEntry.setTime(input.lastModified());
            FileInputStream in = new FileInputStream(input);
            out.putNextEntry(jarEntry);
            for (;;) {
                int len = in.read(buffer);
                if (len == -1)
                    break;
                out.write(buffer, 0, len);
            }
            out.closeEntry();
            in.close();
        }
    }

    /**
     * Unjars a jar file into an output directory.
     * @param jarPath The path to the jar file
     * @param outputDir The output directory
     * @throws IOException If there is an error running unjar
     */
    public static void unjar(String jarPath, String outputDir)
            throws IOException {

        logger.log(Level.FINE, "Unjar''ing {0} to {1}{2}", new Object[]{jarPath, outputDir, '.'});

        File target;
        FileOutputStream out;

        JarInputStream in =
                new JarInputStream(new FileInputStream(jarPath));

        Manifest manifest = in.getManifest();

        if (manifest != null) {
            target = new File(outputDir + File.separator + "META-INF");
            target.mkdirs();
            out = new FileOutputStream(new File(target, "MANIFEST.MF"));
            manifest.write(out);
            out.close();
        }

        byte[] buffer = new byte[8192];
        for (;;) {
            JarEntry entry = in.getNextJarEntry();
            if (entry == null)
                break;
            target = new File(outputDir + File.separator + entry.getName());
            if (entry.isDirectory()) {
                target.mkdirs();
            } else {
                File parent = target.getParentFile();
                if (!parent.isDirectory())
                    parent.mkdirs();
                out = new FileOutputStream(target);
                for (;;) {
                    int size = in.read(buffer);
                    if (size == -1)
                        break;
                    out.write(buffer, 0, size);
                }
                target.setLastModified(entry.getTime());
                out.flush();
                out.close();
            }
            in.closeEntry();
        }
        in.close();
    }


    /**
     * Unjars a temporary jar file xxxx.jar under the directory
     * xxxx in the same path.
     * @param tmpJarFile The temporary jar file
     * @return The file reference to the resulting directory
     * @throws IOException If there is an error unjaring
     */
    public static File unjarTmp(File tmpJarFile) throws IOException {
        logger.log(Level.INFO, "Preparing run from {0}{1}", new Object[]{tmpJarFile.getAbsolutePath(), '.'});

        String dirName = tmpJarFile.getName();
        int dotPos = dirName.lastIndexOf('.');
        dirName = dirName.substring(0, dotPos);
        File unjarDir = new File(tmpJarFile.getParent(), dirName);
        unjarDir.mkdir();

        unjar(tmpJarFile.getAbsolutePath(), unjarDir.getAbsolutePath());
        return unjarDir;
    }

    /**
     * Writes a string to a file. Replaces the file if it already exists.
     * @param string The string to be written
     * @param file The target file
     * @throws IOException If the write fails
     */
    public static void writeStringToFile(String string, File file)
            throws IOException {
        file.delete();
        file.createNewFile();
        FileOutputStream out = new FileOutputStream(file);
        out.write(string.getBytes());
        out.flush();
        out.close();
    }

    /**
     * Reads a whole file and obtains the contents as a string.
     * @param file The file to be read
     * @return The string representing the whole content of the file
     * @throws IOException If the read fails
     */
    public static String readStringFromFile(File file) throws IOException {
        String content = null;
        if (file.isFile())
            content = new String(getContent(file.getAbsolutePath()));
        return content;
    }

    /**
     * Writes the entire content to file. Replaces the file if it already exists.
     * @param string
     * @param file
     * @throws java.io.IOException
     */
    public static void writeContentToFile(String string, File file)
            throws IOException {
        //StringTokenizer t = new StringTokenizer(string," \n,\t");
        file.delete();
        file.createNewFile();
        RandomAccessFile rf = new RandomAccessFile(file, "rwd");
        long size = rf.length();
        byte[] buffer = new byte[(int) size];
        rf.readFully(buffer);
        rf.seek(rf.length());
        rf.writeBytes(string);
        //while (t.hasMoreTokens()) {
               // rf.writeBytes(t.nextToken().trim() + "\n");
        //}
        rf.close();
    }

    /**
     * Reads a whole file and obtains the contents as a formatted string with
     * "\n" seperated.
     * @param file
     * @return string
     * @throws java.io.IOException
     */
    public static String readContentFromFile(File file) throws IOException {
        String content = null;
        StringBuilder formattedTags = new StringBuilder();
        if (file.isFile()) {
            content = new String(getContent(file.getAbsolutePath()));
        StringTokenizer t = new StringTokenizer(content,"\n");
            while (t.hasMoreTokens()) {
                formattedTags.append(t.nextToken().trim()).append(" ");
            }
        }
        return formattedTags.toString();
    }

    /**
     * Obtains the content of a file as a strin array.
     * @param file
     * @return string array
     * @throws java.io.IOException
     */
    public static String[] readArrayContentFromFile(File file)
            throws IOException {
        String content = null;
        ArrayList<String> contentList = new ArrayList<String>();
        if (file.isFile()) {
            content = new String(getContent(file.getAbsolutePath()));
            StringTokenizer t = new StringTokenizer(content, "\n");
            while (t.hasMoreTokens()) {
                contentList.add(t.nextToken().trim());
            }
        }
        return contentList.toArray(new String[contentList.size()]);
    }

    /**
     * Obtains the whole content of a local file in a byte array.
     * @param file The file name
     * @return The byte content of the file
     * @throws IOException If the file cannot be read.
     */
    public static byte[] getContent(String file) throws IOException {
        return getContent(new File(file));
    }

    /**
     * Obtains the whole content of a local file in a byte array.
     * @param file The file name
     * @return The byte content of the file
     * @throws IOException If the file cannot be read.
     */
    public static byte[] getContent(File file) throws IOException {
        long size = file.length();
        if (size == 0)
            throw new IOException("Cannot determine file size.");
        if (size >= Integer.MAX_VALUE)
            throw new IOException("Cannot handle file size >= 2GB.");
        byte[] content = new byte[(int) size];
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            int readLength = 0;
            while (readLength < size) {
                int bytes = in.read(content, readLength,
                            (int) size - readLength);
                if (bytes == -1) // EOS
                    break;
                readLength += bytes;
            }
        } finally {
            if (in != null)
                in.close();
        }
        return content;
    }

    /**
     * Checks a file whether it contains the given string.
     * @param file The file
     * @param string The string
     * @return True if the string is found in the file, false otherwise
     * @throws IOException Problem reading the file
     */
    public static boolean hasString(File file, String string)
            throws IOException {
        boolean retVal = false;
        BufferedReader bufR = new BufferedReader(new FileReader(file));
        String s;
        while ((s = bufR.readLine()) != null) {
            if (s.indexOf(string) != -1) {
                retVal = true;
                break;
            }
        }
        bufR.close();
        return retVal;
    }
    /**
     * Unit test the functionality.
     * @param args The command line arguments
     */
    public static void main(String[] args) {
        if (args.length < 3) {
//            System.out.println("Usage: java FileHelper File Key Value");
            System.out.println("Usage: java FileHelper File token replacement");
            return;
        }
        Properties prop = new Properties();
        prop.setProperty(args[1], args[2]);

        //FileHelper.editPropFile(args[0], prop, null);
        // FileHelper.tokenReplace(args[0], args[1], args[2], null);
        //FileHelper.tokenReplace(System.getProperty("java.io.tmpdir") +
        //      "/t", "\"$JAVA_HOME\"/bin/java",
        // "profcmd= \n\\$profcmd \"\\$JAVA_HOME\"/bin/java", null);
        FileHelper.tokenReplace(System.getProperty("java.io.tmpdir") + "/t",
                "\"$JAVA_HOME\"/bin/java",
                "profcmd= \n$profcmd \"$JAVA_HOME\"/bin/java", null);
    }
}
/*
// This code will replace properties which is specified anywhere in the line
// But will messup cases where there are blanks in it.
                        if((line.indexOf(propName)) != -1) {
                            StringBuffer sb = new StringBuffer();
                            StringTokenizer st = new StringTokenizer(line);
                            while (st.hasMoreTokens()) {
                                String tk = st.nextToken();
                                if(tk.indexOf(propName) != -1)
                                    sb.append(propName).append("=")
                                      .append(props.getProperty(propName)).append(" ");
                                else
                                    sb.append(tk).append(" ");
                            }
                            line = sb.toString();
                        }

*/
