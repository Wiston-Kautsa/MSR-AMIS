package com.mycompany.msr.amis;

import java.io.File;
import javafx.stage.FileChooser;

public final class FileLocationHelper {

    private FileLocationHelper() {
    }

    public static File getDownloadsDirectory() {
        File downloads = new File(System.getProperty("user.home"), "Downloads");
        if (!downloads.exists()) {
            downloads.mkdirs();
        }
        return downloads;
    }

    public static void useDownloadsDirectory(FileChooser chooser) {
        File downloads = getDownloadsDirectory();
        if (downloads.exists() && downloads.isDirectory()) {
            chooser.setInitialDirectory(downloads);
        }
    }

    public static File fileInDownloads(String fileName) {
        return new File(getDownloadsDirectory(), fileName);
    }
}
