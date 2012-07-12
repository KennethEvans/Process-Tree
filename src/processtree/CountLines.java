package processtree;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
 *         This class counts lines in all files with the given extension in the
 *         given drectories.<br>
 * <br>
 * 
 */
public class CountLines extends ProcessTree
{
    // Whether to actually process or not
    // private static final boolean DRY_RUN = true;
    private static final boolean PRINT_NAMES = false;
    // A convenience definition. Leave as is.
    public static final String LS = System.getProperty("line.separator");

    private Comparator<File> fileComparator = null;
    private boolean abort = false;

    private String[] patterns;;
    private int[] patternCount;
    private int[] fileCount;
    private int nProcessed = 0;
    private int nLinesTotal = 0;

    InputStreamReader stdinStreamReader = null;
    BufferedReader stdinReader = null;

    /**
     * Constructor.
     */
    public CountLines() {
        super();
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
        for(int i = 0; i < patternCount.length; i++) {
            patternCount[i] = 0;
            fileCount[i] = 0;
        }
        System.out.println("Processing " + (String)obj);
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
        System.out.println();
        for(int i = 0; i < patterns.length; i++) {
            System.out.printf("Found %d lines in %d files matching %s\n",
                patternCount[i], fileCount[i], patterns[i]);
        }
        System.out.println("\nTotal lines found: " + nLinesTotal
            + " Total files found: " + nProcessed);
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
        String fileName = file.getName();
        for(int i = 0; i < patterns.length; i++) {
            int newLines = 0;
            if(fileName.matches(patterns[i])) {
                nProcessed++;
                fileCount[i]++;
                newLines = countLines(patterns[i], file);
                nLinesTotal += newLines;
                patternCount[i] += newLines;
            }
            if(abort) {
                System.out.println(LS + "Aborted");
                System.exit(1);
            }
        }
    }

    /**
     * Method to count the lines in a file.
     * 
     * @param file
     */
    int countLines(String pattern, File file) {
        if(file == null) return 0;
        if(PRINT_NAMES) {
            System.out.println(file.getPath());
        }
        BufferedReader in = null;
        int nLines = 0;
        try {
            in = new BufferedReader(new FileReader(file));
            // String line;
            // int lineNum = 0;
            // while((line = in.readLine()) != null) {
            while((in.readLine()) != null) {
                // lineNum++;
                nLines++;
            }
        } catch(Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if(in != null) in.close();
            } catch(IOException ex) {
                ex.printStackTrace();
                abort = true;
            }
        }
        return nLines;
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

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#parseCommand(java.lang.String[])
     */
    protected boolean parseCommand(String[] args) {
        int i;
        ArrayList<String> patternList = new ArrayList<String>();

        for(i = 0; i < args.length; i++) {
            if(args[i].startsWith("-")) {
                switch(args[i].charAt(1)) {
                case 'h':
                    usage();
                    System.exit(0);
                case 'p':
                    patternList.add(args[++i]);
                    break;
                default:
                    System.err.println("\n\nInvalid option: " + args[i]);
                    usage();
                    return false;
                }
            } else {
                dirList.add(args[i]);
                dirSpecified = true;
            }
        }
        if(!dirSpecified) {
            System.out.println("No directory specified");
            usage();
            return false;
        }
        if(patternList.isEmpty()) {
            // If no patterns were given, use this one
            patternList.add(".*");
        }
        patterns = new String[patternList.size()];
        patternCount = new int[patternList.size()];
        fileCount = new int[patternList.size()];
        patternList.toArray(patterns);
        for(i = 0; i < patternCount.length; i++) {
            patternCount[i] = 0;
            fileCount[i] = 0;
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
        System.out.println("\nCountLines: Count lines in files\n"
            + "Usage: CountLines [Options] directory-list\n"
            + "    -h    Help (This message)\n"
            + "    -p    Wildcard pattern for file name (e.g. *.java)\n"
            + "            May have multiple -p options\n" + "");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        CountLines app = new CountLines();
        if(!app.parseCommand(args)) {
            System.exit(1);
        }
        app.processDirectoryList();
        System.out.println();
        System.out.println("All done");
    }

}
