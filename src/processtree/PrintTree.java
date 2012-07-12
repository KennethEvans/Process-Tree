/*
 * Program to print a directory tree
 * Created on Nov 3, 2005
 * By Kenneth Evans, Jr.
 */

package processtree;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.ListIterator;

public class PrintTree extends ProcessTree
{
  protected static final int MAX_DEPTH = 1000;
  protected static final String INDENT = "| ";
  private boolean directoriesFirst = false;
  private boolean filesFirst = false;
  private boolean doSizes = false;
  private boolean directoriesOnly = false;
  private Comparator<File> fileComparator = null;
  private int maxDepth = MAX_DEPTH;
 
  /**
   * PrintTree constructor.
   */
  public PrintTree()
  {
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
        return(fa.getName().compareTo(fb.getName()));
      }
    };
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
      if(level > maxDepth) return; // Will do finally first
      if(item.isDirectory()) {
        File dirList[] = item.listFiles();
        if(doSizes) {
          System.out.println(tabs() + item.getName() + " [" + dirList.length
            + " items]");
        } else {
          System.out.println(tabs() + item.getName());
        }
        // Convert it to a list so we can sort it
        List<File> list = Arrays.asList(dirList);
        Collections.sort(list, fileComparator);
        ListIterator<File> iter = list.listIterator();
        while(iter.hasNext()) {
          File file1 = (File)iter.next();
          process((Object)file1);
        }
      } else {
        if(!directoriesOnly) {
          if(doSizes) {
            System.out.println(tabs() + item.getName() + " [" + item.length()
              + "]");
          } else {
            System.out.println(tabs() + item.getName());
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
   * Create a String with the appropriate number of tabs for the current level.
   * @return
   */
  private String tabs() {
    String tabs = "";
    for(int i = 1; i < level; i++) {
      tabs += INDENT;
    }
    return tabs;
  }

  /* (non-Javadoc)
   * @see processtree.ProcessTree#parseCommand(java.lang.String[])
   */
  protected boolean parseCommand(String[] args) {
    int i;
    
    for(i=0; i < args.length; i++) {
      if (args[i].startsWith("-")) {
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
        case 'o':
          directoriesOnly = true;
          break;
        case 's':
          doSizes = true;
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
    return true;
  }

  /* (non-Javadoc)
   * @see processtree.ProcessTree#usage()
   */
  protected void usage() {
    System.out.println("\nUsage: java " +
      this.getClass().getName() + " [Options] directory-list\n" +
      "  PrintTree: Print directory trees for the given list \n" + 
      "             of directories\n" +
      "             Use \"d:\\.\" for the root\n" +
      "\n" +
      "  Options:\n" +
      "    -h        Help (This message)\n" +
      "    -d        Directories before files\n" +
      "    -f        Files before directories\n" +
      "              (Default is alphabetical)\n" +
      "    -l  int   Maximum depth to check (Default is " + MAX_DEPTH + ")\n" +
      "    -o        Directories only\n" +
      "    -s        Print sizes\n" +
      ""
    );
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    PrintTree printTree = new PrintTree();
    if(!printTree.parseCommand(args)) {
      System.exit(1);
    }
    printTree.processDirectoryList();
    System.out.println();
    System.out.println("All done");
  }

}
