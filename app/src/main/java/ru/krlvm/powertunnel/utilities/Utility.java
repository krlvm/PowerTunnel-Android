package ru.krlvm.powertunnel.utilities;

import java.io.Closeable;
import java.io.IOException;

public class Utility {

    //From Apache Commons-IO
    public static void closeQuietly(Closeable is) {
        if(is != null) {
            try {
                is.close();
            } catch (IOException ignore) {
            }
        }
    }
}
