/*
 * Run.java
 *
 * Created on October 31, 2003, 3:38 PM
 */

package runutils;


/**
 *
 * @author  evans
 */
public class Run {
    
    private String errMsg="";
    private String errOutput="";
    private String output="";
    private String stackTrace="";
    // private String lineTerminator="<BR>\n";
    private String lineTerminator="\n";
    private String cmd="";
    
    
    /** Creates a new instance of Run */
    public Run() {
    }
    
    public void setErrMsg(String value) {
//       String oldValue = errMsg;
        errMsg = value;
    }
    
    public String getErrMsg() {
        return errMsg;
    }
    
    public String getOutput() {
        return output;
    }
    
    public String getErrOutput() {
        return errOutput;
    }
    
    public String getStackTrace() {
        return stackTrace;
    }
    
    public String getCmd() {
        return cmd;
    }
    
    public void setLineTerminator(String value) {
 //       String oldValue = lineTerminator;
        lineTerminator = value;
    }
    
    public String getLineTerminator() {
        return lineTerminator;
    }
    
    private void createStackTrace(Throwable t) {
        stackTrace="";
        if(t == null) return;
        StackTraceElement[] elements=t.getStackTrace();
        for(int i=0; i < elements.length; i++) {
            stackTrace += elements[i].toString() + lineTerminator;
        }
    }
    
    synchronized public int exec(String cmd) {
        int retCode=0;
        errMsg="";
        errOutput="";
        output="";
        stackTrace="";
        this.cmd=cmd;
        boolean useBuffer=true;
        try {
            Runtime rt = Runtime.getRuntime();
            Process proc = rt.exec(cmd);
            // Catch stderr
            StreamCatcher errorCatcher = new
            StreamCatcher(proc.getErrorStream(),useBuffer);
            errorCatcher.setLineTerminator(lineTerminator);
            errorCatcher.start();
            
            // Catch stdout
            StreamCatcher outputCatcher = new
            StreamCatcher(proc.getInputStream(),useBuffer);
            outputCatcher.setLineTerminator(lineTerminator);
            outputCatcher.start();
            
            // Wait for the threads to exit
            errorCatcher.join(10000);
            outputCatcher.join(10000);
            
            // Wait for the process to end
            retCode = proc.waitFor();
            
            // Wait for the threads to finish
/*
            if(!outputCatcher.isFinished() || !errorCatcher.isFinished()) {
                try {
                    wait(10000);
                } catch (InterruptedException e) {
                }
            }
 */
            errOutput=errorCatcher.getBuffer();
            if(!errorCatcher.isFinished()) {
                errOutput += "Incomplete..." + lineTerminator;
            }
            output=outputCatcher.getBuffer();
            if(!outputCatcher.isFinished()) {
                output += "Incomplete..." + lineTerminator;
            }
            return retCode;
        } catch (Throwable t) {
            createStackTrace(t);
            errMsg = t.toString();
            return -1;
        }
    }
    
    /** Converts special characters to valid HTML */
    public static String convertToHtml(String input) {
        StringBuffer output=new StringBuffer(input.length());
        char c;
        for(int i=0; i < input.length(); i++) {
            c=input.charAt(i);
            if(c == '<') {
                output.append("&lt;");
            } else if(c == '>') {
                output.append("&gt;");
/* Quotes come through OK in PRE, probably & also
            } else if(c == '"') {
                output.append("&quote;");
            } else if(c == '&') {
                output.append("&amp;");
*/                
            } else {
                output.append(c);
            }
        }
        return  output.toString();
    }
    
    public String runWithOutput(String cmd) {
        String out="";
        int retCode=0;
        this.cmd=cmd;
        
        try {
            exec(cmd);
        } catch(Throwable t) {
            out += "<PRE><BR>Exception running program<BR></PRE>";
        }
        
        // Command
        if(cmd != null && cmd.length() > 0) {
            out += "<PRE><BR>" + cmd + "<BR></PRE>";
        }
        
        // Error
        if(retCode != 0) {
            out += "<PRE><BR>" +
            cmd + "BR>" +
            "The return code was <%=retVal%>.<BR></PRE>";
        }
        
        // Output
        if(output != null && output.length() > 0) {
            out += "<PRE><BR>" + convertToHtml(output) + "<BR></PRE>";
        }
        
        // Error output
        if(errOutput != null && errOutput.length() > 0) {
            out += "<PRE><BR>Error Output:<BR>" + convertToHtml(errOutput)
            + "<BR></PRE>";
        }
        
        // Error message
        if(errMsg != null && errMsg.length() > 0) {
            out += "<PRE><BR>Error Message:<BR>" + convertToHtml(errMsg) +
            "<BR></PRE>";
        }
        
        // Stack trace
        if(stackTrace != null && stackTrace.length() > 0) {
            out += "<PRE><BR>Stack Trace:<BR>" + convertToHtml(stackTrace) +
            "<BR></PRE>";
        }
        
        return out;
    }
}
