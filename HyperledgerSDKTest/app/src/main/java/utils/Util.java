package utils;

import java.io.File;
import java.io.FilenameFilter;

import static java.lang.String.format;



public class Util {

    private Util() {

    }



    public static File findFileSk(File directory) {

        final FilenameFilter filter = new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
                if(name.endsWith("_sk")){
                    return true;
                }else {
                    return false;
                }
            }
        };
        File[] matches = directory.listFiles(filter);

        if (null == matches) {
            throw new RuntimeException(format("Matches returned null does %s directory exist?", directory.getAbsoluteFile().getName()));
        }

        if (matches.length != 1) {
            throw new RuntimeException(format("Expected in %s only 1 sk file but found %d", directory.getAbsoluteFile().getName(), matches.length));
        }

        return matches[0];

    }


}
