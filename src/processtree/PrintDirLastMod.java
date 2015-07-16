/*
 * Program to print a directory tree
 * Created on Nov 3, 2005
 * By Kenneth Evans, Jr.
 */

package processtree;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.ListIterator;
import java.util.TreeSet;

/**
 * PrintDirLastMod processes the list of directories on the command line and
 * finds the latest time one of the files was modified in each of its top-level
 * directories. It does not check modification times for directories. This can
 * be uses to check for directories that have not been modified recently.
 * 
 * @author Kenneth Evans, Jr.
 */
public class PrintDirLastMod extends ProcessTree
{
    private static final boolean showProgress = false;
    protected static final int MAX_DEPTH = 2;
    private TreeSet<Data> results = new TreeSet<Data>();
    private long start, prev, cur;
    private int rootAbsLength = 0;
    private int rootCanonicalLength = 0;

    /**
     * Data Internal class to hold a File and the lastMod
     * 
     * @author Kenneth Evans, Jr.
     */
    class Data implements Comparable<Data>
    {
        private File file = null;
        long lastMod = 0;

        Data(File file, long size) {
            this.file = file;
            this.lastMod = size;
        }

        /**
         * @return The value of file.
         */
        public File getFile() {
            return file;
        }

        /**
         * @return The value of lastMod.
         */
        public long getLastMod() {
            return lastMod;
        }

        public int compareTo(Data data) {
            if(lastMod == data.lastMod)
                return 0;
            else if(lastMod > data.lastMod)
                return -1;
            else
                return 1;
        }

    }

    /**
     * PrintTree constructor.
     */
    public PrintDirLastMod() {
        super();
        start = prev = cur = System.currentTimeMillis();
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#processBefore(java.lang.Object)
     */
    public void processBefore(Object obj) {
        System.out.println((String)obj);
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#processBetween(java.lang.Object)
     */
    public void processBetween(Object obj) {
        printResults();
        System.out.println();
        results = new TreeSet<Data>();
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#processEnd(java.lang.Object)
     */
    public void processEnd(Object obj) {
        printResults();
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
            // Save the canonical and absolute path lengths;
            if(level == 1) {
                try {
                    File canonicalFile = item.getCanonicalFile();
                    String canonicalName = canonicalFile.getAbsolutePath();
                    String absoluteName = item.getAbsolutePath();
                    rootAbsLength = absoluteName.length();
                    rootCanonicalLength = canonicalName.length();
                } catch(IOException e) {
                    rootAbsLength = 0;
                    rootCanonicalLength = 0;
                }
            }
            if(level > 2) return; // Will do finally first
            if(item.isDirectory()) {
                long lastMod = 0;
                File directoryList[] = item.listFiles();
                if(level == 2) {
                    if(showProgress) {
                        prev = System.currentTimeMillis();
                        System.out.printf("%s Processing %s...\n", timeStamp(),
                            item.getName());
                    }
                    // Check for symbolic link
                    if(isSymbolicLink(item)) {
                        lastMod = 0;
                        if(showProgress) {
                            try {
                                File canonicalFile = item.getCanonicalFile();
                                System.out.printf("  %s [link to %s]\n",
                                    item.getName(),
                                    canonicalFile.getAbsolutePath());
                            } catch(IOException e) {
                                System.out.printf("  %s [link to %s]\n",
                                    item.getName(), "<Unknown>");
                            }
                        }
                    } else {
                        lastMod = getDirLastMod(item, lastMod);
                        if(showProgress) {
                            cur = System.currentTimeMillis();
                            double elapsed = (cur - prev) / (60000.);
                            System.out
                                .printf(
                                    "  %s %d Bytes %.2f KB %.2f MB [%.2f min %s]\n",
                                    item.getName(), lastMod, lastMod / 1024.,
                                    lastMod / (1024. * 1024.), elapsed,
                                    getMemoryUsage());
                        }
                    }
                    Data data = new Data(item, lastMod);
                    results.add(data);
                }
                // Convert it to a list so we can sort it
                List<File> list = Arrays.asList(directoryList);
                ListIterator<File> iter = list.listIterator();
                while(iter.hasNext()) {
                    File file1 = (File)iter.next();
                    process((Object)file1);
                }
            } else {
                return;
            }
        } catch(Exception ex) {
            System.out.println(ex.getMessage());
        } finally {
            level--;
        }
    }

    /**
     * Recurse to get the lastMod of all files in the directory
     * 
     * @param dir
     * @return
     */
    protected long getDirLastMod(File dir, long prevLastMod) {
        long lastMod = prevLastMod;
        if(!dir.isDirectory()) {
            System.out.println("Is not a directory: " + dir.getPath());
            return prevLastMod;
        }
        File directoryList[] = dir.listFiles();
        List<File> list = Arrays.asList(directoryList);
        ListIterator<File> iter = list.listIterator();
        long lastMod1;
        while(iter.hasNext()) {
            File file1 = (File)iter.next();
            if(isSymbolicLink(file1)) {
                System.out.println("Is symbolic link: " + dir.getPath());
                return prevLastMod;
            }
            if(file1.isDirectory()) {
                lastMod1 = getDirLastMod(file1, lastMod);
            } else {
                lastMod1 = file1.lastModified();
            }
            if(lastMod1 > lastMod) {
                lastMod = lastMod1;
            }
        }
        return (lastMod > prevLastMod) ? lastMod : prevLastMod;
    }

    /**
     * Prints out the results
     */
    public void printResults() {
        String format = "  %-40s %s\n";
        long overallLastMod = 0;
        long lastMod;
        for(Data data : results) {
            lastMod = data.getLastMod();
            if(lastMod > overallLastMod) {
                overallLastMod = lastMod;
            }
        }
        Date date = new Date(overallLastMod);
        System.out.printf(format, "OVERALL", overallLastMod > 0 ? date
            : "Unknown");
        for(Data data : results) {
            lastMod = data.getLastMod();
            date = new Date(data.getLastMod());
            System.out.printf(format, data.getFile().getName(),
                lastMod > 0 ? date : "Unknown");
        }
    }

    /**
     * Generates a timestamp.
     * 
     * @return String timestamp with the current time
     */
    public static String timeStamp() {
        Date now = new Date();
        final SimpleDateFormat defaultFormatter = new SimpleDateFormat(
            "MMM dd, yyyy HH:mm:ss.SSS");
        return defaultFormatter.format(now);
    }

    public double getElapsedTime() {
        long now = System.currentTimeMillis();
        return (now - start) / 1000.;
    }

    public static String getMemoryUsage() {
        double scale = 1 / (1024. * 1024.);
        double totalMem = scale * Runtime.getRuntime().totalMemory();
        double freeMem = scale * Runtime.getRuntime().freeMemory();
        double maxMem = scale * Runtime.getRuntime().maxMemory();
        double usedMem = maxMem - freeMem;
        String info = String.format("%.1f MB(%.0f%%)", usedMem, usedMem
            / totalMem);
        return info;
    }

    /**
     * Determines if a file is a symbolc link by checking if its absolute path
     * is the same as its canonical path.
     * 
     * @param file
     * @return
     */
    public boolean isSymbolicLink(File file) {
        // Get the canonical path - that is, follow the link to the file it is
        // actually linked to
        File canonicalFile = null;
        try {
            canonicalFile = file.getCanonicalFile();
        } catch(IOException e) {
            return false;
        }

        // Just use the substring in case the top-level directory is a link
        String canonicalName = canonicalFile.getAbsolutePath().substring(
            rootCanonicalLength);
        String absoluteName = file.getAbsolutePath().substring(rootAbsLength);
        if(canonicalName.equals(absoluteName)) {
            return false;
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
        return true;
    }

    /*
     * (non-Javadoc)
     * 
     * @see processtree.ProcessTree#usage()
     */
    protected void usage() {
        System.out
            .println("\nUsage: java "
                + this.getClass().getName()
                + " [Options] directory-list\n"
                + "  PrintTree: Print directory last modification time for the given list \n"
                + "             of directories\n"
                + "             Use \"d:\\.\" for the root\n" + "\n"
                + "  Options:\n" + "    -h        Help (This message)\n" + "");
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        PrintDirLastMod printTree = new PrintDirLastMod();
        if(!printTree.parseCommand(args)) {
            System.exit(1);
        }
        printTree.processDirectoryList();
        System.out.println();
        System.out.printf("Elapsed time: %.1f min\n",
            printTree.getElapsedTime() / 60.);
        System.out.println("All done");
    }

}
