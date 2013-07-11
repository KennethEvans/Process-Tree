/*
 * StreamCatcher.java
 *
 * Created on October 31, 2003, 3:43 PM
 */

package runutils;

import java.io.*;

/**
 *
 * @author  evans
 */
class StreamCatcher extends Thread {
    InputStream is;
    private String buffer="";
    private boolean useBuffer=true;
    // private String lineTerminator="<BR>\n";
    private String lineTerminator="\n";
    private boolean finished=false;
    
    /** Creates a new instance of StreamCatcher */
    StreamCatcher(InputStream is) {
        this.is = is;
    }
    
    StreamCatcher(InputStream is, boolean useBuffer) {
        this.is = is;
        this.useBuffer=useBuffer;
    }
    
    public String getBuffer() {
        return buffer;
    }
    
    public void setLineTerminator(String value) {
        //String oldValue = lineTerminator;
        lineTerminator = value;
    }
    
    public String getLineTerminator() {
        return lineTerminator;
    }
    
    public boolean isFinished() {
        return finished;
    }
    
    /** Captures the output and prints it to */
    synchronized public void run() {
        buffer="";
        finished=false;
        // buffer += "Started" + lineTerminator;
        try {
            InputStreamReader isr = new InputStreamReader(is);
            BufferedReader br = new BufferedReader(isr);
            String line=null;
            while ( (line = br.readLine()) != null)
                if(useBuffer) {
                    buffer += line + lineTerminator;
                } else {
                    System.out.println(line);
                }
            // buffer += "Ended" + lineTerminator;
        } catch (IOException ioe) {
            if(useBuffer) {
                buffer += lineTerminator + "Exception: " + lineTerminator +
                ioe.getMessage();
            } else {
                ioe.printStackTrace();
            }
            // buffer += "Interrupted" + lineTerminator;
        } finally {
            finished=true;
            notifyAll();
        }
    }
}
