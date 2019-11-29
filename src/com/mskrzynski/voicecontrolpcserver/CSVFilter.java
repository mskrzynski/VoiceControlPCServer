package com.mskrzynski.voicecontrolpcserver;

import javax.swing.filechooser.FileFilter;
import java.io.File;

public class CSVFilter extends FileFilter {

    @Override
    public boolean accept(File plik) {
         if (plik.isDirectory()) {
                return true;
         }

         String rozszerzenie = Utils.getExtension(plik);
         if (rozszerzenie != null) {
             return rozszerzenie.equals(Utils.csv);
         }
         return false;
    }

    @Override
    public String getDescription() {
        return "Plik CSV";
    }
}

class Utils {
    final static String csv = "csv";

    static String getExtension(File plik) {
        String ext = null;
        String s = plik.getName();
        int i = s.lastIndexOf('.');

        if (i > 0 &&  i < s.length() - 1) {
            ext = s.substring(i+1).toLowerCase();
        }
        return ext;
    }
}

