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

public class PrintDirSizes extends ProcessTree
{
  private static final boolean showProgress = true;
  protected static final int MAX_DEPTH = 2;
  private TreeSet<Data> results = new TreeSet<Data>();
  private long start, prev, cur;
  private int rootAbsLength = 0;
  private int rootCanonicalLength = 0;

  /**
   * Data Internal class to hold a File and the size
   * 
   * @author Kenneth Evans, Jr.
   */
  class Data implements Comparable<Data>
  {
    private File file = null;
    long size = 0;

    Data(File file, long size) {
      this.file = file;
      this.size = size;
    }

    /**
     * @return The value of file.
     */
    public File getFile() {
      return file;
    }

    /**
     * @return The value of size.
     */
    public long getSize() {
      return size;
    }

    public int compareTo(Data data) {
      if(size == data.size)
        return 0;
      else if(size > data.size)
        return -1;
      else
        return 1;
    }

  }

  /**
   * PrintTree constructor.
   */
  public PrintDirSizes() {
    super();
    start = prev = cur = System.currentTimeMillis();
  }

  /* (non-Javadoc)
   * @see processtree.ProcessTree#processBefore(java.lang.Object)
   */
  public void processBefore(Object obj) {
    System.out.println((String)obj);
  }

  /* (non-Javadoc)
   * @see processtree.ProcessTree#processBetween(java.lang.Object)
   */
  public void processBetween(Object obj) {
    System.out.println();
  }

  /* (non-Javadoc)
   * @see processtree.ProcessTree#processEnd(java.lang.Object)
   */
  public void processEnd(Object obj) {
    printResults();
  }

  /* (non-Javadoc)
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
        long size = 0;
        File directoryList[] = item.listFiles();
        if(level == 2) {
          if(showProgress) {
            prev = System.currentTimeMillis();
            System.out.printf("%s Processing %s...\n", timeStamp(), item
              .getName());
          }
          // Check for symbolic link
          if(isSymbolicLink(item)) {
            size = 0;
            if(showProgress) {
              try {
                File canonicalFile = item.getCanonicalFile();
                System.out.printf("  %s [link to %s]\n", item.getName(),
                  canonicalFile.getAbsolutePath());
              } catch(IOException e) {
                System.out.printf("  %s [link to %s]\n", item.getName(),
                  "<Unknown>");
              }
            }
          } else {
            size = getDirSize(item);
            if(showProgress) {
              cur = System.currentTimeMillis();
              double elapsed = (cur - prev) / (60000.);
              System.out.printf(
                "  %s %d Bytes %.2f KB %.2f MB [%.2f min %s]\n",
                item.getName(), size, size / 1024., size / (1024. * 1024.),
                elapsed, getMemoryUsage());
            }
          }
          Data data = new Data(item, size);
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
   * Recurse to get the size of all files in the directory
   * 
   * @param dir
   * @return
   */
  protected long getDirSize(File dir) {
    long size = 0;
    if(!dir.isDirectory()) return 0;
    File directoryList[] = dir.listFiles();
    List<File> list = Arrays.asList(directoryList);
    ListIterator<File> iter = list.listIterator();
    while(iter.hasNext()) {
      File file1 = (File)iter.next();
      if(isSymbolicLink(file1)) {
        return 0;
      }
      if(file1.isDirectory()) {
        size += getDirSize(file1);
      } else {
        size += file1.length();
      }
    }
    return size;
  }

  /**
   * Prints out the results
   */
  public void printResults() {
    double total = 0;
    String format = "  %-40s %10.3f MB %6.2f%%\n";
    for(Data data : results) {
      total += data.getSize();
    }
    System.out.printf(format, "TOTAL", total / (1024. * 1024.),
      (total == 0) ? 0. : 100.);

    for(Data data : results) {
      System.out.printf(format, data.getFile().getName(), data.getSize()
        / (1024. * 1024.), (total == 0) ? 0. : 100. * data.getSize() / total);
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
    String info = String.format("%.1f MB(%.0f%%)", usedMem, usedMem / totalMem);
    return info;
  }

  /**
   * Determines if a file is a symbolc link by checking if its absolute path is
   * the same as its canonical path.
   * 
   * @param file
   * @return
   */
  public  boolean isSymbolicLink(File file) {
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
    System.out.println("\nUsage: java " + this.getClass().getName()
      + " [Options] directory-list\n"
      + "  PrintTree: Print directory sizes for the given list \n"
      + "             of directories\n"
      + "             Use \"d:\\.\" for the root\n" + "\n" + "  Options:\n"
      + "    -h        Help (This message)\n" + "");
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    PrintDirSizes printTree = new PrintDirSizes();
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
