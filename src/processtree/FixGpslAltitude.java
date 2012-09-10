package processtree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

/**
 * ProcessLicense
 * 
 * @author Kenneth Evans, Jr.<br>
 * <br>
 *         This class fixes altitudes in GPSL files that were incorrectly
 *         written as meters. Note that the file creation time is not changed.
 *         It could be a template for fixing all files in some directory, easily
 *         extended to a list of directories.<br>
 * <br>
 * 
 */
public class FixGpslAltitude extends ProcessTree
{
    // /////////////////////////////////////////////////////////////////////////////
    // Important!
    // The original run of this program tokenized the date and time as two
    // tokens.
    // This caused a nasty error in GPSLink.
    // It has been fixed to define tokens by TAB only, and has been tested.
    // But check before using!
    // /////////////////////////////////////////////////////////////////////////////

    // Whether to actually process or not
    private static final boolean DRY_RUN = true;
    private static final String SRC_DIR = "C:\\Documents and Settings\\evans\\My Documents\\GPSLINK\\Altitude Corrected\\";

    private static final int N_PRINT_LINES = 8;

    // Max altitude to convert. Used to avoid converting cabin altitudes that
    // are
    // already OK. Use Integer.MAX_VALUE to avoid this check.
    private static final int MAX_ALT = 1400;

    // A convenience definition. Leave as is.
    public static final String LS = System.getProperty("line.separator");
    // The separator around preview lines when there is a prompt
    private static String SEPARATOR = "-----------------------------------------"
        + LS;
    public static final double METERS_TO_FEET = 3.280839895;

    private Comparator<File> fileComparator = null;
    private boolean abort = false;

    private int nProcessed = 0;

    InputStreamReader stdinStreamReader = null;
    BufferedReader stdinReader = null;

    // 
    public static enum Mode {
        NONE, WAYPOINT, TRACK,
    };

    /**
     * Constructor.
     */
    public FixGpslAltitude() {
        super();
        dirList.add(SRC_DIR);
        fileComparator = new Comparator<File>() {
            public int compare(File fa, File fb) {
                if(fa.isDirectory() && !fb.isDirectory()) return -1;
                if(fb.isDirectory() && !fa.isDirectory()) return 1;
                return (fa.getName().compareTo(fb.getName()));
            }
        };
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#processBefore(java.lang.Object)
     */
    public void processBefore(Object obj) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#processBetween(java.lang.Object)
     */
    public void processBetween(Object obj) {
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#processEnd(java.lang.Object)
     */
    public void processEnd(Object obj) {
        System.out.printf("\nProcessed: %d\n", nProcessed);
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#process(java.lang.Object)
     */
    public void process(Object obj) {
        File item = (File)obj;
        try {
            if(item == null) {
                return;
            }
            level++;
            if(item.isDirectory()) {
                File dirList[] = item.listFiles();
                // Convert it to a list so we can sort it
                List<File> list = Arrays.asList(dirList);
                Collections.sort(list, fileComparator);
                ListIterator<File> iter = list.listIterator();
                while(iter.hasNext()) {
                    File file1 = (File)iter.next();
                    process((Object)file1);
                }
            } else {
                processFile(item);
            }
        } catch(Exception ex) {
            System.err.println("Processing error:" + LS + ex + LS
                + ex.getMessage());
            System.err.println();
        } finally {
            level--;
        }
    }

    /**
     * Method to process a file.
     * 
     * @param file
     */
    void processFile(File file) {
        fixFileAltitude(file);
        if(abort) {
            System.out.println(LS + "Aborted");
            System.exit(1);
        }
        nProcessed++;
    }

    boolean renameFile(File src, File dst) {
        boolean result = true;
        if(DRY_RUN) {
            System.out.print("Simulated: ");
        } else {
            if(dst.exists()) {
                System.err.println("Destination exists: " + dst);
                System.err.println("Aborting rename");
                System.err.flush();
                return false;
            } else {
                result = src.renameTo(dst);
            }
        }
        System.out.println(src);
        System.out.println(" > " + dst);
        System.out.flush();
        if(!result) {
            System.err.println("Error renaming " + src);
            System.err.flush();
        }
        return result;
    }

    /**
     * Method to fix the altitude in a file.
     * 
     * @param file
     */
    void fixFileAltitude(File file) {
        if(file == null) return;

        System.out.println(file.getPath());
        replaceAltitude(file, true);
    }

    /**
     * Returns the contents of a file as a byte array.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public static byte[] loadFileBytes(File file) throws IOException {
        if(file == null) return null;
        if(!file.exists()) {
            throw new IOException("File does not exist");
        }
        int len = (int)file.length();
        // if(len == 0) {
        // throw new IOException("File is empty");
        // }
        byte bytes[] = new byte[len]; // Has to be int here
        int nRead = 0;
        FileInputStream in = null;
        try {
            in = new FileInputStream(file);
            nRead = in.read(bytes);
            if(nRead != len) {
                throw new IOException("Only read " + nRead + " of " + len
                    + " bytes");
            }
            return bytes;
        } finally {
            if(in != null) in.close();
        }
    }

    /**
     * Returns the contents of a file as a String.
     * 
     * @param file
     * @return
     * @throws IOException
     */
    public static String loadFileString(File file) throws IOException {
        byte[] bytes = loadFileBytes(file);
        if(bytes == null) return null;
        return new String(bytes);
    }

    /**
     * Prints the first nLines lines of a file.
     * 
     * @param file
     * @param nLines
     */
    public static boolean printFirstLines(File file, int nLines) {
        if(!file.exists()) {
            System.out.println(file.getName() + " does not exist");
            return false;
        }
        BufferedReader in = null;
        try {
            in = new BufferedReader(new FileReader(file));
            String line;
            int lineNum = 0;
            while((line = in.readLine()) != null && lineNum < nLines) {
                lineNum++;
                System.out.println(line);
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(in != null) in.close();
            } catch(IOException ex) {
                ex.printStackTrace();
                return false;
            }
        }
        return true;
    }

    /**
     * Method that fixes the altitude in a .gpsl file. The old file is renamed
     * to a backup, which is deleted on success.
     * 
     * @param file
     * @param replace Whether to replace the old license or to just add the new
     *            license.
     * @param prompt Whether to prompt for changing the license. Typically used
     *            when replace is false for security.
     */
    public void replaceAltitude(File file, boolean prompt) {
        if(DRY_RUN) return;
        if(prompt) {
            System.out.print(SEPARATOR);
            printFirstLines(file, N_PRINT_LINES);
            System.out.print(SEPARATOR);
            try {
                System.out.print("Continue [Y/n]? ");
                if(stdinReader == null) {
                    // Create stdin reader
                    stdinStreamReader = new InputStreamReader(System.in);
                    stdinReader = new BufferedReader(stdinStreamReader);
                }
                String input = stdinReader.readLine();
                input = input.toLowerCase();
                if(input.length() > 0 && !input.startsWith("y")) {
                    System.out.println("  Skipped");
                    return;
                }
            } catch(IOException ex) {
                System.out.println("Prompt error: " + LS + ex + LS
                    + ex.getMessage());
                System.out.println("  Skipped");
                return;
            }
        }
        String inName = file.getPath();
        String bkpName = inName + ".BKP";
        File bkpFile = new File(bkpName);
        Boolean result;
        if(bkpFile.exists()) {
            result = bkpFile.delete();
            if(!result) {
                System.err.println("Failed to delete existing backup file:"
                    + LS + bkpFile.getPath());
                abort = true;
                return;
            }
        }
        result = file.renameTo(bkpFile);
        if(!result) {
            System.err.println("Failed to rename original file:" + LS
                + bkpFile.getPath());
            abort = true;
            return;
        }
        // Do it
        BufferedReader in = null;
        PrintWriter out = null;
        String[] tokens;
        int nTokens;
        double val;
        try {
            in = new BufferedReader(new FileReader(bkpFile));
            out = new PrintWriter(new FileWriter(file));
            String line;
//            int lineNum = 0;
            Mode mode = Mode.NONE;
            while((line = in.readLine()) != null) {
//                lineNum++;
                // Note: "\\s+" matches one or more white spaces
                if(false) {
                    tokens = line.trim().split("\\s+");
                } else {
                    // We want to break on TABs
                    tokens = line.trim().split("\t");
                }
                nTokens = tokens.length;
                if(nTokens == 1) {
                    if(tokens[0].equals("Waypoints")) {
                        // Switch to doing waypoints
                        mode = Mode.TRACK;
                    } else if(tokens[0].equals("Tracks")) {
                        // Switch to doing tracks
                        mode = Mode.TRACK;
                    }
                } else if(nTokens == 0) {
                    // Switch back
                    mode = Mode.NONE;
                }
                if(mode == Mode.NONE) {
                    out.println(line);
                    continue;
                }
                // This is where the conversion is done
                if(nTokens >= 5
                    && (tokens[0].equals("W") || tokens[0].equals("T"))) {
                    val = Integer.parseInt(tokens[4]);
                    if(val < MAX_ALT) {
                        val *= METERS_TO_FEET;
                        tokens[4] = Long.toString(Math.round(val));
                    }
                    for(int i = 0; i < nTokens; i++) {
                        out.print(tokens[i]);
                        if(i < nTokens - 1) {
                            out.print("\t");
                        } else {
                            out.println();
                        }
                    }
                } else {
                    out.println(line);
                }
            }
            // Cleanup
            in.close();
            out.close();
            in = null;
            out = null;
            if(file.exists()) {
                result = bkpFile.delete();
                if(!result) {
                    System.err.println("Failed to delete backup file:" + LS
                        + bkpFile.getPath());
                    abort = true;
                    return;
                }
            }
            System.out.println("  Done");
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(in != null) in.close();
                if(out != null) out.close();
            } catch(IOException ex) {
                ex.printStackTrace();
                abort = true;
                return;
            }
        }
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#parseCommand(java.lang.String[])
     */
    protected boolean parseCommand(String[] args) {
        int i;

        for(i = 0; i < args.length; i++) {
            if(args[i].startsWith("-")) {
                switch(args[i].charAt(1)) {
                case 'h':
                    usage();
                    System.exit(0);
                default:
                    System.err.println("\n\nInvalid option: " + args[i]);
                    usage();
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * Generates a timestamp.
     * 
     * @return String timestamp
     */
    public static String timeStamp(long longDate) {
        Date date = new Date(longDate);
        final SimpleDateFormat defaultFormatter = new SimpleDateFormat(
            "M-dd-yyyy HH:mm:ss");
        return defaultFormatter.format(date);
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#usage()
     */
    protected void usage() {
        System.out.println("\nUsage: java " + this.getClass().getName()
            + " [Options]\n"
            + "  FixGpsAltitude: Fix altitude in .gpsl files\n"
            + "    -h        Help (This message)\n" + "");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        System.out.println("FixGpslAltitude" + LS + SRC_DIR + LS);
        FixGpslAltitude app = new FixGpslAltitude();
        if(!app.parseCommand(args)) {
            System.exit(1);
        }
        app.processDirectoryList();
        System.out.println();
        System.out.println("All done");
    }

}
