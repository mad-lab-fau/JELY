package de.fau.lme.ecglib;

public class Ecglib {

    public Ecglib() {
	// TODO Auto-generated constructor stub
    }
    
    private static boolean debugMode = false;

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static void setDebugMode(boolean debugMode) {
        Ecglib.debugMode = debugMode;
    }
    

}
