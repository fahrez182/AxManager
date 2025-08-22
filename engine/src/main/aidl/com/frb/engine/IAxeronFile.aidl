// IAxeronFile.aidl
package com.frb.engine;

// Declare any non-default types here with import statements

interface IAxeronFile {
    boolean mkdirs(String folderName);
    boolean exists(String path);
    boolean delete(String path);
    boolean isFile(String filePath);
    boolean isDirectory(String path);
    boolean renameTo(String from, String to);
    long length(String filePath);
    long lastModified(String filePath);
    boolean setLastModified(String filePath, long newLastModified);
    List<String> getDirectories(String directoryPath);
    ParcelFileDescriptor readFile(String filePath);
    ParcelFileDescriptor readFileUri(in ParcelFileDescriptor pfd);
    ParcelFileDescriptor readFileTouch(String filePath);
    ParcelFileDescriptor writeFile(String path);
}