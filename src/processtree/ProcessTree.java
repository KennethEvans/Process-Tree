/*
 * Program to print a directory tree
 * Created on Nov 3, 2005
 * By Kenneth Evans, Jr.
 */

package processtree;

import java.io.File;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.List;
import java.util.ListIterator;
import java.util.Vector;

public class ProcessTree
{
  protected Vector<String> dirList = new Vector<String>();
  protected int level = 0;
  protected boolean dirSpecified = false;

  /**
   * ProcessTree constructor.
   */
  public ProcessTree() {
    super();
  }

  /**
   * Main loop to process each name in the directory list. Calls processBefore,
   * process, processBetween, and processEnd. Not expected to be overridden.
   * 
   */
  public void processDirectoryList() {
    Enumeration<String> e = dirList.elements();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      try {
        processBefore(name);
        File dir = new File(name);
        if(dir == null) {
          System.err.println("Invalid directory");
          System.exit(1);
        }
        if(!dir.isDirectory()) {
          System.err.println("Not a directory: " + dir.getPath());
          System.exit(1);
        }
        level = 0;
        process((Object)dir);
        if(e.hasMoreElements()) {
          processBetween(name);
        } else {
          processEnd(null);
        }
      } catch(Exception ex) {
        ex.printStackTrace();
        System.exit(1);
      }
    }
  }

  /**
   * Method run before processing each item in the list. Expected to be
   * overridden.
   * 
   * @param obj
   */
  public void processBefore(Object obj) {
  }

  /**
   * Method run between processing each item in the list. Is not run after the
   * last item is processed. Expected to be overridden.
   * 
   * @param obj
   */
  public void processBetween(Object obj) {
  }

  /**
   * Method run after processing the last item in the list. Expected to be
   * overridden.
   * 
   * @param obj
   */
  public void processEnd(Object obj) {
  }

  /**
   * Method to run to process each item in the list. May call itself
   * recursively. Expected to be overridden.
   * 
   * @param obj
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
        List<?> list = Arrays.asList(dirList);
        ListIterator<?> iter = list.listIterator();
        while(iter.hasNext()) {
          File file1 = (File)iter.next();
          process((Object)file1);
        }
      } else {
      }
    } catch(Exception ex) {
      System.out.println(ex.getMessage());
    } finally {
      level--;
    }
  }

  /**
   * Parses the arguments on the command line.
   * 
   * @param args
   * @return
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

  /**
   * Prints usage
   */
  protected void usage() {
    System.out.println("\nUsage: java " + this.getClass().getName()
      + " [Options] directory-list\n"
      + "  PrintTree: Print directory trees for the given list \n"
      + "             of directories\n"
      + "             Use \"d:\\.\" for the root\n" + "\n" + "  Options:\n"
      + "    -h        Help (This message)\n" + "");
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    ProcessTree processTree = new ProcessTree();
    if(!processTree.parseCommand(args)) {
      System.exit(1);
    }
    processTree.processDirectoryList();
    System.out.println();
    System.out.println("All done");
  }

}
