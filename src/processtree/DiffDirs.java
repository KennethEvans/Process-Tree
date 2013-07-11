package processtree;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;

import runutils.Run;

/**
 * DiffDirs
 * 
 * @author Kenneth Evans, Jr.<br>
 * <br>
 *         This class compares files in a given directory to those in another
 *         given directory.<br>
 * <br>
 * 
 */
public class DiffDirs extends ProcessTree
{
    public static final String LS = System.getProperty("line.separator");

    // If no options are given, these flags can be set here, which is more
    // convenient during development
    /** Whether to actually process or not */
    private static final boolean DRY_RUN = false;
    private boolean dryRun = DRY_RUN;
    private static final boolean VERBOSE = true;
    private boolean verbose = VERBOSE;

    /** The directory to compare against. */
    private String baseDir = null;
    /** The directory to be compared. */
    private String processDir = null;
    /** Whether the base directory is specified. */
    protected boolean baseDirSpecified = false;
    /** The length of the directory name. */
    private int dirNameLength = Integer.MAX_VALUE;
    /** Comparator to use for sorting files */
    private Comparator<File> fileComparator = null;

    /** Number of directories processed */
    private int nDirsProcessed = 0;
    /** Number of directories deleted */
    private int nFilesFailed = 0;
    /** Number of files processed */
    private int nFilesProcessed = 0;

    /** The time the application was started [milliseconds] */
    private long start;

    /**
     * Constructor.
     */
    public DiffDirs() {
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
        System.out.println("\nDirectories processed=" + nDirsProcessed);
        System.out.println("Files: Processed=" + nFilesProcessed + " Failed="
            + nFilesFailed);
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
                List<File> list = Arrays.asList(dirList);
                Collections.sort(list, fileComparator);
                ListIterator<File> iter = list.listIterator();
                while(iter.hasNext()) {
                    File file1 = (File)iter.next();
                    process((Object)file1);
                }
                processDir(item);
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
        nFilesProcessed++;
        try {
            String fileName = file.getPath();
            File processFile = new File(processDir, fileName.substring(
                dirNameLength, fileName.length()));
            if(processFile.exists()) {
                boolean ok = diff(file, processFile);
                if(!ok) {
                    nFilesFailed++;
                }
            } else {
                System.out.println("Does not exist: " + processFile);
                nFilesFailed++;
            }
        } catch(Exception ex) {
            excMsg("Error deleting " + file.getPath(), ex);
        }
    }

    /**
     * Method to process a directory.
     * 
     * @param dir
     */
    void processDir(File dir) {
        nDirsProcessed++;
    }

    /**
     * Diff two files.
     * 
     * @param file1 Should be the base file
     * @param file2 Should be the process file.
     */
    public boolean diff(File file1, File file2) {
        boolean res = true;
        String cmd = null;
        if(!dryRun) {
            cmd = "diff \"" + file1.getPath() + "\" \"" + file2.getPath()
                + "\"";
            if(verbose) {
                System.out.println(cmd);
            }
        } else {
            cmd = "ls \"" + file2.getPath() + "\"";
            System.out.println("Simulating " + cmd);
        }
        Run run = new Run();
        run.setLineTerminator(LS);
        int retCode = run.exec(cmd);
        if(retCode != 0) {
            res = false;
            System.out.println("Failed: " + file1.getPath());
            // Print output
            String output = run.getOutput();
            if(output != null) {
                System.out.println(output);
            }
            // Print error output
            String errOutput = run.getErrOutput();
            if(errOutput != null && errOutput.length() > 0) {
                System.out.println("Error output:");
                System.out.println(errOutput);
            }
        } else {
            if(verbose) {
                System.out.println("Succeeded");
            }
        }
        return res;
    }

    /**
     * Exception message dialog. Displays message plus the exception and
     * exception message.
     * 
     * @param msg
     * @param ex
     */
    public static void excMsg(String msg, Exception ex) {
        final String fullMsg = msg += LS + "Exception: " + ex + LS
            + ex.getMessage();
        System.out.println(fullMsg);
    }

    /**
     * Gets the elapsed time;
     * 
     * @return
     */
    public double getElapsedTime() {
        long now = System.currentTimeMillis();
        return (now - start) / 1000.;
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#parseCommand(java.lang.String[])
     */
    protected boolean parseCommand(String[] args) {
        start = System.currentTimeMillis();
        int i;
        for(i = 0; i < args.length; i++) {
            if(args[i].startsWith("-")) {
                switch(args[i].charAt(1)) {
                case 'd':
                    dryRun = false;
                    break;
                case 'h':
                    usage();
                    System.exit(0);
                default:
                    System.err.println("\n\nInvalid option: " + args[i]);
                    usage();
                    return false;
                }
            } else if(args[i].startsWith("+")) {
                switch(args[i].charAt(1)) {
                case 'd':
                    dryRun = true;
                    break;
                case 'h':
                    usage();
                    System.exit(0);
                default:
                    System.err.println("\n\nInvalid option: " + args[i]);
                    usage();
                    return false;
                }
            } else {
                if(!dirSpecified) {
                    // processDir is just a variable
                    processDir = args[i];
                    dirSpecified = true;
                } else if(!baseDirSpecified) {
                    // baseDir is in the dirList and will be processed
                    baseDir = args[i];
                    baseDirSpecified = true;
                    dirNameLength = baseDir.length();
                    dirList.add(baseDir);
                }
            }
        }
        if(!baseDirSpecified) {
            System.out.println("No base directory specified");
            usage();
            return false;
        }
        if(!dirSpecified) {
            System.out.println("No directory specified");
            usage();
            return false;
        }

        System.out.println("DiffDir");
        if(dryRun) {
            System.out.println("Dry Run");
        }
        System.out.println(timeStamp(start));
        System.out.println("Directory to Check: " + processDir);
        System.out.println("Base directory: " + dirList.firstElement());
        System.out.println();

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
            "MMMMM dd, yyyy HH:mm:ss");
        return defaultFormatter.format(date);
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#usage()
     */
    protected void usage() {
        System.out.println("\nDiffDirs: Compares a given directory of files\n"
            + " to a base directory.\n"
            + "Usage: DiffDirs [Options] processDir baseDir\n"
            + "  Compares items in processDir to those in baseDir\n"
            + "    -h      Help (This message)\n\n"
            + "    +d      Dry run (No items deleted)\n"
            + "    -d      Not a dry run (Items will be deleted)\n\n" + "");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        DiffDirs app = new DiffDirs();
        if(!app.parseCommand(args)) {
            System.exit(1);
        }
        app.processDirectoryList();
        System.out.println();
        double elapsed = app.getElapsedTime();
        System.out.printf("Elapsed time: %.1f min = %.1f hr" + LS,
            elapsed / 60., elapsed / 3600.);
        System.out.println();
        System.out.println("All done");
    }

}
