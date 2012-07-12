/*
 * Program to print a directory tree
 * Created on Nov 3, 2005
 * By Kenneth Evans, Jr.
 */

package processtree;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;

/**
 * SvnReplace
 * 
 * This class replaces a directory under SVN control with another under SVN
 * control. All the files not in .svn directories in the first directory are
 * deleted on the first pass. In the second pass the files not in .svn
 * directories in the second directory are then copied to the corresponding
 * location in the first directory. The .svn directories are not changed. The
 * net result is to replace everything but the .svn directories in the first
 * directory with the the files in the second directory. If only one directory
 * is specified, then only the first pass is done.
 * 
 * @author Kenneth Evans, Jr.
 */
public class SvnReplace extends ProcessTree
{
  private static final boolean dryRun = false;
  protected static final int MAX_DEPTH = 1000;
  protected static final String INDENT = "| ";
  private boolean directoriesFirst = false;
  private boolean filesFirst = false;
  private Comparator<File> fileComparator = null;
  private int maxDepth = MAX_DEPTH;
  private String dir1 = null;
  private String dir2 = null;

  private int nPass = 0;

  /**
   * PrintTree constructor.
   */
  public SvnReplace() {
    super();
    fileComparator = new Comparator<File>() {
      public int compare(File fa, File fb) {
        if(directoriesFirst) {
          if(fa.isDirectory() && !fb.isDirectory()) return -1;
          if(fb.isDirectory() && !fa.isDirectory()) return 1;
        } else if(filesFirst) {
          if(fa.isDirectory() && !fb.isDirectory()) return 1;
          if(fb.isDirectory() && !fa.isDirectory()) return -1;
        }
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
    nPass++;
    // System.out.println("-- " + nProc + " --------------------------------");
    System.out.println((String)obj);
  }

  /*
   * (non-Javadoc)
   * 
   * @see processtree.ProcessTree#processBetween(java.lang.Object)
   */
  public void processBetween(Object obj) {
    System.out.println();
  }

  /*
   * (non-Javadoc)
   * 
   * @see processtree.ProcessTree#processEnd(java.lang.Object)
   */
  public void processEnd(Object obj) {
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
      if(level > maxDepth) return; // Will do finally first
      if(item.isDirectory()) {
        if(!item.getName().equals(".svn")) {
          File dirList[] = item.listFiles();
          System.out.println(tabs() + item.getName());
          // Convert it to a list so we can sort it
          List<File> list = Arrays.asList(dirList);
          Collections.sort(list, fileComparator);
          ListIterator<File> iter = list.listIterator();
          while(iter.hasNext()) {
            File file1 = (File)iter.next();
            process((Object)file1);
          }
        }
      } else {
        if(nPass == 1) {
          System.out.println(tabs() + "Deleting: " + item.getPath());
          if(!dryRun) {
            boolean res = item.delete();
            if(!res) {
              System.err.println("Failed to delete " + item.getPath());
            }
          }
        } else if(nPass == 2) {
          String destName = dir1 + item.getPath().substring(dir2.length());
          File dest = new File(destName);
          System.out.println(tabs() + "Copying: " + item.getPath() + " to "
            + dest.getPath());
          if(!dryRun) {
            try {
              // Create the file, which should not exist
              if(!dest.exists()) {
                boolean res = dest.createNewFile();
                if(!res) {
                  System.err.println("Failed to create: " + dest.getPath());
                } else {
                  copy(item, dest);
                }
              }
              copy(item, dest);
            } catch(IOException ex) {
              System.err.println("Copy error: " + ex);
            }
          }
        }
      }
    } catch(Exception ex) {
      System.out.println(tabs() + ex.getMessage());
    } finally {
      level--;
    }
  }

  /**
   * Copies the file src to the file dest.
   * 
   * @param src
   * @param dest
   * @throws IOException
   */
  public static void copy(File src, File dest) throws IOException {
    InputStream in = new FileInputStream(src);
    OutputStream out = new FileOutputStream(dest);

    // Transfer bytes from in to out
    byte[] buf = new byte[1024];
    int len;
    while((len = in.read(buf)) > 0) {
      out.write(buf, 0, len);
    }
    in.close();
    out.close();
  }

  /**
   * Create a String with the appropriate number of tabs for the current level.
   * 
   * @return
   */
  private String tabs() {
    String tabs = "";
    for(int i = 1; i < level; i++) {
      tabs += INDENT;
    }
    return tabs;
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
        case 'd':
          directoriesFirst = true;
          break;
        case 'f':
          filesFirst = true;
          break;
        case 'l':
          maxDepth = Integer.parseInt(args[++i]);
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
    if(dirList.size() == 0 || dirList.size() > 2) {
      System.err
        .println("\n\nThere must be either one or two directories specified");
      usage();
      return false;
    }

    // Get the directory names
    Enumeration<String> e = dirList.elements();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      if(dir1 == null) {
        dir1 = name;
        File file = new File(dir1);
        if(!file.exists()) {
          System.err.println("\n\n" + dir1 + " does not exist");
          return false;
        }
      } else if(dir2 == null) {
        dir2 = name;
        if(dir1.equals(dir2)) {
          System.err.println("\n\nThe directories are the same");
          usage();
          return false;
        }
        File file = new File(dir2);
        if(!file.exists()) {
          System.err.println("\n\n" + dir2 + " does not exist");
          return false;
        }
      }
    }

    System.out.println("Deleting files in " + dir1);
    if(dir2 != null) {
      System.out.println("Copying files from " + dir2);
    }
    System.out.println();

    // Ask if OK?
    System.out.print("Is this OK [Y|n]? ");
    try {
      java.io.BufferedReader stdin = new java.io.BufferedReader(
        new java.io.InputStreamReader(System.in));
      String line = stdin.readLine();
      if(line.length() > 0 && !line.startsWith("Y") && !line.startsWith("y")) {
        System.out.println("Aborting");
        return false;
      }
    } catch(Exception ex) {
      System.out.println("Aborting");
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
      .println("\nUsage: java " + this.getClass().getName()
        + " [Options] dir1 [dir2]\n"
        + "  SvnReplace: Delete non .svn files in dir1\n"
        + "              Copy non .svn files in dir2 to dir1\n"
        + "              Use \"d:\\.\" for the root\n" + "\n" + "  Options:\n"
        + "    -h        Help (This message)\n"
        + "    -l  int   Maximum depth to check (Default is " + MAX_DEPTH
        + ")\n");
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    SvnReplace printTree = new SvnReplace();
    if(!printTree.parseCommand(args)) {
      System.exit(1);
    }
    printTree.processDirectoryList();
    System.out.println();
    System.out.println("All done");
  }

}
