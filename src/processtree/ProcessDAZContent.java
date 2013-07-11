package processtree;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.Hashtable;
import java.util.List;
import java.util.ListIterator;

/**
 * ProcessDAZContent
 * 
 * @author Kenneth Evans, Jr.<br>
 * <br>
 *         This class compares a given directory of DAZ content to a base
 *         directory and removes files and directories from the given directory
 *         which also exist in the base directory. It is expected to be used to
 *         leave only content that is not found in the base directory.<br>
 * <br>
 * 
 */
public class ProcessDAZContent extends ProcessTree
{
    public static final String LS = System.getProperty("line.separator");

    // If no options are given, these flags can be set here, which is more
    // convenient during development
    /** Whether to actually process or not */
    private static final boolean DRY_RUN = true;
    /** Whether to print the names of the retained directories. */
    private static final boolean PRINT_DIRS_RETAINED = false;
    /** Whether to print the names of the retained files. */
    private static final boolean PRINT_FILES_RETAINED = false;
    /** Whether to print the names of the deleted directories. */
    private static final boolean PRINT_DIRS_DELETED = true;
    /** Whether to print the names of the deleted files. */
    private static final boolean PRINT_FILES_DELETED = false;
    /** Maximum number of deleted items (files and directories) to print. */
    private static final int MAX_PRINT_ITEMS = Integer.MAX_VALUE;
    /** Number of deleted items (files and directories) printed. */
    private int nItemsPrinted = 0;

    private boolean dryRun = DRY_RUN;
    private boolean printDirsRetained = PRINT_DIRS_RETAINED;
    private boolean printFilesRetained = PRINT_FILES_RETAINED;
    private boolean printDirsDeleted = PRINT_DIRS_DELETED;
    private boolean printFilesDeleted = PRINT_FILES_DELETED;

    /** The directory to compare against. */
    private String baseDir = null;
    /** Whether the base directory is specified. */
    protected boolean baseDirSpecified = false;
    /** The length of the directory name. */
    private int dirNameLength = Integer.MAX_VALUE;
    /** Hashtable to hold the number of items deleted for each directory */
    private Hashtable<String, Integer> deleteTable = null;
    /** Comparator to use for sorting files */
    private Comparator<File> fileComparator = null;

    /** Number of directories processed */
    private int nDirsProcessed = 0;
    /** Number of directories deleted */
    private int nFilesDeleted = 0;
    /** Number of files processed */
    private int nFilesProcessed = 0;
    /** Number of files deleted */
    private int nDirsDeleted = 0;

    /**
     * Constructor.
     */
    public ProcessDAZContent() {
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
        System.out.println("\nDirectories: Processed=" + nDirsProcessed
            + " Deleted=" + nDirsDeleted + " Retained="
            + (nDirsProcessed - nDirsDeleted));
        System.out.println("Files: Processed=" + nFilesProcessed + " Deleted="
            + nFilesDeleted + " Retained=" + (nFilesProcessed - nFilesDeleted));
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
                // Add this directory to the deleteTable
                deleteTable.put(item.getPath(), dirList.length);
                // DEBUG
                // System.out.println("dirList.length=" + dirList.length +
                // " for "
                // + item.getPath());
                // Convert it to a list so we can sort it
                List<File> list = Arrays.asList(dirList);
                Collections.sort(list, fileComparator);
                ListIterator<File> iter = list.listIterator();
                while(iter.hasNext()) {
                    File file1 = (File)iter.next();
                    process((Object)file1);
                }
                processDir(item);
                // Remove this item from the deleteList
                deleteTable.remove(item.getPath());
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
            File baseFile = new File(baseDir, fileName.substring(dirNameLength,
                fileName.length()));
            if(baseFile.exists()) {
                boolean deleted = false;
                if(dryRun) {
                    deleted = true;
                } else {
                    deleted = file.delete();
                }
                if(!deleted) {
                    System.out.println("Failed to delete " + file.getPath());
                } else {
                    nFilesDeleted++;
                    String parentPath = file.getParentFile().getPath();
                    int nDeleted = deleteTable.get(parentPath);
                    deleteTable.put(parentPath, nDeleted - 1);
                    // DEBUG
                    // System.out.println("nDeleted=" + nDeleted + " for "
                    // + parentPath);
                    if(printFilesDeleted && nItemsPrinted < MAX_PRINT_ITEMS) {
                        System.out.println("Deleted F " + file.getPath());
                    }
                }
            } else if(printFilesRetained) {
                System.out.println("Retained F " + file.getPath());
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
        try {
            String dirName = dir.getPath();
            File dirList[] = dir.listFiles();
            int nFilesLeft = 0;
            if(dryRun) {
                nFilesLeft = deleteTable.get(dirName);
                // DEBUG
                // if(nFilesLeft < 0) {
                // System.out.println("*** Got unexpected nFilesLeft = "
                // + nFilesLeft + " for " + dirName);
                // } else {
                // System.out.println("nFilesLeft=" + nFilesLeft + " for "
                // + dirName);
                // }
            } else {
                nFilesLeft = dirList.length;
            }
            if(nFilesLeft == 0) {
                File baseFile = new File(baseDir, dirName.substring(
                    dirNameLength, dirName.length()));
                if(baseFile.exists()) {
                    boolean deleted = false;
                    if(dryRun) {
                        deleted = true;
                    } else {
                        deleted = dir.delete();
                    }
                    if(!deleted) {
                        System.out.println("Failed to delete " + dir.getPath());
                    } else {
                        nDirsDeleted++;
                        String parentPath = dir.getParentFile().getPath();
                        int nDeleted = deleteTable.get(parentPath);
                        deleteTable.put(parentPath, nDeleted - 1);
                        // DEBUG
                        // System.out.println("nDeleted=" + nDeleted + " for "
                        // + parentPath);
                        if(printDirsDeleted && nItemsPrinted < MAX_PRINT_ITEMS) {
                            System.out.println("Deleted D " + dir.getPath());
                        }
                    }
                } else if(printDirsRetained) {
                    System.out.println("Retained D " + dir.getPath() + " ["
                        + nFilesLeft + "]");
                }
            } else if(printDirsRetained) {
                System.out.println("Retained D " + dir.getPath() + " ["
                    + nFilesLeft + "]");
            }
        } catch(Exception ex) {
            excMsg("Error deleting " + dir.getPath(), ex);
        }
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
                case 'd':
                    dryRun = false;
                    break;
                case 'h':
                    usage();
                    System.exit(0);
                case 'p':
                    if(args[i].equals("-pfr")) {
                        printFilesRetained = false;
                    } else if(args[i].equals("-pfd")) {
                        printFilesDeleted = false;
                    } else if(args[i].equals("-pdr")) {
                        printDirsRetained = false;
                    } else if(args[i].equals("-pdd")) {
                        printDirsDeleted = false;
                    }
                    break;
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
                case 'p':
                    if(args[i].equals("+pfr")) {
                        printFilesRetained = true;
                    } else if(args[i].equals("+pfd")) {
                        printFilesDeleted = true;
                    } else if(args[i].equals("+pdr")) {
                        printDirsRetained = true;
                    } else if(args[i].equals("+pdd")) {
                        printDirsDeleted = true;
                    }
                    break;
                default:
                    System.err.println("\n\nInvalid option: " + args[i]);
                    usage();
                    return false;
                }
            } else {
                if(!dirSpecified) {
                    dirList.add(args[i]);
                    dirNameLength = args[i].length();
                    dirSpecified = true;
                } else if(!baseDirSpecified) {
                    baseDir = args[i];
                    baseDirSpecified = true;
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

        System.out.println("ProcessDAZContent");
        if(dryRun) {
            System.out.println("Dry Run");
        }
        System.out.println("Base directory: " + baseDir);
        System.out.println("Directory to Process: " + dirList.firstElement());
        System.out.println();

        // Create a Hashtable to hold the number of items deleted for each
        // directory
        deleteTable = new Hashtable<String, Integer>();

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
        System.out
            .println("\nProcessDAZContent: Compares a given directory of DAZ content\n"
                + " to a base directory and removes files and directories from\n"
                + " the given directory which also exist in the base directory.\n"
                + "Usage: ProcessDAZContent [Options] processDir baseDir\n"
                + "  Removes items in processDir that exist in baseDir\n"
                + "    -h      Help (This message)\n\n"
                + "    +d      Dry run (No items deleted)\n"
                + "    -d      Not a dry run (Items will be deleted)\n\n"
                + "    +pfr    Print files retained\n"
                + "    +pdr    Print directories retained\n"
                + "    +pfd    Print files deleted\n"
                + "    +pdd    Print directories deleted\n\n"
                + "    -pfr    Do not printfiles retained\n"
                + "    -pdr    Do not printdirectories retained\n"
                + "    -pfd    Do not printfiles deleted\n"
                + "    -pdd    Do not printdirectories deleted\n" + "");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        ProcessDAZContent app = new ProcessDAZContent();
        if(!app.parseCommand(args)) {
            System.exit(1);
        }
        app.processDirectoryList();
        System.out.println();
        System.out.println("All done");
    }

}
