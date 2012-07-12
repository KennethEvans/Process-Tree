/*
 * Program to check for installed Java extensions, such as J3D and ImageIO
 * Created on Aug 27, 2006
 * By Kenneth Evans, Jr.
 */

package processtree;

import java.io.File;
import java.util.Enumeration;
import java.util.Vector;
/**
 * CheckJavaExtensions checks Java extension directories for J3D and ImageIO
 * files.
 * 
 * @author Kenneth Evans, Jr.
 */
public class CheckJavaExtensions
{
  private static final String[] DEFAULT_DIRS = {
  // "c:/j2sdk1.4.2_06",
  // "c:/Program Files/Java",
  "c:/",};
  private static final String[] J3D_FILES = {"j3daudio.jar", "j3dcore.jar",
    "j3dutils.jar", "vecmath.jar",};
  private static final String[] IMAGEIO_FILES = {"jai_codec.jar",
    "jai_core.jar", "mlibwrapper_jai.jar", "jai_imageio.jar",};
  private static final String JAVA_EXE = "bin" + File.separator + "java.exe";
  private static final String JRE_DIR = "jre";
  private static final String EXT_DIR = "lib" + File.separator + "ext";
  private static int MAX_DEPTH = 2;
  public static final String LS = System.getProperty("line.separator");
  protected Vector<String> dirList = new Vector<String>();
  private boolean dirSpecified = false;
  int level = 0;
  int maxDepth = MAX_DEPTH;
  int dirsChecked = 0;
  int jreChecked = 0;

  public CheckJavaExtensions() {
    super();
  }

  public void check() {
    level = 0;
    dirsChecked = 0;
    jreChecked = 0;
    System.out.println("Checking JRE directories:");
    System.out.println("Depth=" + maxDepth);
    if(!dirSpecified) {
      for(int i = 0; i < DEFAULT_DIRS.length; i++) {
        dirList.add(DEFAULT_DIRS[i]);
      }
      dirSpecified = true;
    }
    Enumeration<?> e = dirList.elements();
    while(e.hasMoreElements()) {
      String name = (String)e.nextElement();
      try {
        File dir = new File(name);
        if(false) {
          // Debug
          System.out.println(dir.getAbsolutePath() + LS);
        }
        if(dir == null) {
          System.err.println("Invalid directory: " + name);
          System.exit(1);
        }
        if(!dir.isDirectory()) {
          System.err.println("Not a directory: " + name);
          System.exit(1);
        }
        checkDirectory(dir);
      } catch(Exception ex) {
        ex.printStackTrace();
        System.exit(1);
      }
    }
  }

  public void checkDirectory(File dir) {
    File file = null;
    try {
      if(dir == null || !dir.isDirectory()) {
        return;
      }
      if(false) {
        // Debug
        System.out.println(dir.getAbsolutePath());
      }
      level++;
      if(level >= maxDepth) return; // Will do finally first
      dirsChecked++;
      file = dir;
      File dirList[] = dir.listFiles();
      for(int i = 0; i < dirList.length; i++) {
        file = dirList[i];
        if(isJRE(file)) {
          processJRE(file);
        }
        checkDirectory(file);
      }
    } catch(Exception ex) {
      System.out.println();
      System.out.println("While processing " + file.getAbsolutePath());
      System.out.println("Processing error: " + ex + LS + ex.getMessage());
    } finally {
      level--;
    }
  }

  /**
   * Checks if a directory has bin/java.exe.
   * 
   * @param dir
   * @return
   */
  public boolean isJRE(File dir) {
    String checkName = dir.getAbsolutePath() + File.separator + JAVA_EXE;
    File checkFile = new File(checkName);
    if(false) {
      // Debug
      System.out.println(checkName);
    }
    if(!checkFile.exists()) return false;
    return true;
  }

  /**
   * Checks if a directory has bin/java.exe and a jre directory.
   * 
   * @param dir
   * @return
   */
  public boolean isJDK(File dir) {
    // First it must have a jre
    if(!isJRE(dir)) return false;
    String checkName = dir.getAbsolutePath() + File.separator + JRE_DIR;
    File checkFile = new File(checkName);
    if(!checkFile.exists()) return false;
    return true;
  }

  /**
   * Process a JRE directory, looking for files.
   * 
   * @param dir
   */
  private void processJRE(File dir) {
    jreChecked++;
    System.out.println();
    System.out.println(dir.getAbsolutePath());
    String checkName = dir.getAbsolutePath() + File.separator + JRE_DIR;
    File checkFile = new File(checkName);
    if(checkFile.exists()) {
      System.out.println("  Has " + JRE_DIR);
    }
    checkName = dir.getAbsolutePath() + File.separator + EXT_DIR;
    checkFile = new File(checkName);
    if(!checkFile.exists()) {
      System.out.println("  No " + EXT_DIR);
      return;
    }
    int missing = 0;
    missing += checkFiles(checkName, J3D_FILES);
    missing += checkFiles(checkName, IMAGEIO_FILES);
    if(missing == 0) {
      System.out.println("  OK");
    } else {
      System.out.println("  Missing " + missing + " file(s) in " + EXT_DIR);
    }
  }

  /**
   * Checks an array of files for existence.
   * 
   * @param dirName parent directory path.
   * @param files array of files.
   * @return
   */
  public int checkFiles(String dirName, String[] files) {
    int missing = 0;
    for(int i = 0; i < files.length; i++) {
      String checkName1 = dirName + File.separator + files[i];
      missing += checkFile(checkName1);
    }
    return missing;
  }

  /**
   * Checks a single file for existence.
   * 
   * @param path full path to file.
   * @return
   */
  public int checkFile(String path) {
    int missing = 0;
    File file = new File(path);
    if(file.exists()) {
    } else {
      System.out.println("  Not found: " + path);
      missing++;
    }
    return missing;
  }

  /**
   * @return the number of directories checked.
   */
  public int getDirsChecked() {
    return dirsChecked;
  }

  /**
   * @return the jreChecked
   */
  public int getJreChecked() {
    return jreChecked;
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    CheckJavaExtensions app = new CheckJavaExtensions();
    if(!app.parseCommand(args)) {
      System.exit(1);
    }
    app.check();
    System.out.println();
    System.out.println("JREs processed: " + app.getJreChecked());
    System.out.println("Directories checked: " + app.getDirsChecked());
    System.out.println("All done");
  }

  private boolean parseCommand(String[] args) {
    int i;

    for(i = 0; i < args.length; i++) {
      if(args[i].startsWith("-")) {
        switch(args[i].charAt(1)) {
        case 'd':
          maxDepth = Integer.parseInt(args[++i]);
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
        dirList.add(args[i]);
        dirSpecified = true;
      }
    }
    return true;
  }

  /**
   * Prints usage
   */
  private void usage() {
    System.out.println("\nUsage: java " + this.getClass().getName()
      + " [Options] directory-list\n"
      + "    CheckJavaExtensions: Check Java extension directories\n"
      + "    for J3D and ImageIO files \n"
      + " Use \"d:\\.\" for the root\n" + "\n" + "  Options:\n"
      + "    -h        Help (This message)\n"
      + "    -l  int   Maximum depth to check (Default is " + MAX_DEPTH + ")\n"
      + "");
  }
}
