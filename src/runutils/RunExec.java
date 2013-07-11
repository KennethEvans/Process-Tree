/*
 * RunExec.java
 *
 * Created on October 31, 2003, 4:15 PM
 */

package runutils;

/**
 *
 * @author  evans
 */
public class RunExec {
    
    /** Creates a new instance of RunExec */
    public RunExec() {
    }
    
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        if(args.length < 1) {
            System.out.println("Usage: java RunExec <cmd>\n");
        }
        
        Run run=new Run();
        run.setLineTerminator("\n");
        int retCode=run.exec(args[0]);
        
        if(retCode != 0) {
            System.out.println("\nThe return code was: " + retCode +
            "\n");
        }
        
        // Print output
        String output=run.getOutput();
        if(output != null) {
            System.out.println(output);
        }
        
        // Print error output
        String errOutput=run.getErrOutput();
        if(errOutput != null && errOutput.length() > 0) {
            System.out.println("\nError output:\n");
            System.out.println(errOutput);
        }
        // Print error message
        String errMsg=run.getErrMsg();
        if(errMsg != null && errMsg.length() > 0) {
            System.out.println("\nError message:\n");
            System.out.println(errMsg);
        }
        // Print error stackTrace
        String stackTrace=run.getStackTrace();
        if(stackTrace != null && stackTrace.length() > 0) {
            System.out.println("\nStack trace:\n");
            System.out.println(stackTrace);
        }
    }
}
