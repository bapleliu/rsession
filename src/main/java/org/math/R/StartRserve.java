package org.math.R;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.logging.Level;
import org.rosuda.REngine.Rserve.RConnection;

/** helper class that consumes output of a process. In addition, it filter output of the REG command on Windows to look for InstallPath registry entry which specifies the location of R. */
class RegistryHog extends Thread {

    InputStream is;
    boolean capture;
    String installPath;

    RegistryHog(InputStream is, boolean capture) {
        this.is = is;
        this.capture = capture;
        start();
    }

    public String getInstallPath() {
        return installPath;
    }

    public void run() {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (capture) { // we are supposed to capture the output from REG command

                    int i = line.indexOf("InstallPath");
                    if (i >= 0) {
                        String s = line.substring(i + 11).trim();
                        int j = s.indexOf("REG_SZ");
                        if (j >= 0) {
                            s = s.substring(j + 6).trim();
                        }
                        installPath = s;
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
    }
}

class StreamHog extends Thread {

    InputStream is;
    boolean capture;
    StringBuffer out = new StringBuffer();

    StreamHog(InputStream is, boolean capture) {
        this.is = is;
        this.capture = capture;
        start();
    }

    public String getOutput() {
        return out.toString();
    }

    public void run() {
        //Logger.err.println("start streamhog");
        BufferedReader br = null;
        InputStreamReader isr = null;
        try {
            isr = new InputStreamReader(is);
            br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                if (capture) {
                    out.append("\n").append(line);
                } else {
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
            if (isr != null) {
                try {
                    isr.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }
        //Logger.err.println("finished streamhog");
    }
}

/** simple class that start Rserve locally if it's not running already - see mainly <code>checkLocalRserve</code> method. It spits out quite some debugging outout of the console, so feel free to modify it for your application if desired.<p>
<i>Important:</i> All applications should shutdown every Rserve that they started! Never leave Rserve running if you started it after your application quits since it may pose a security risk. Inform the user if you started an Rserve instance.
 */
public class StartRserve {

    /** R batch to check Rserve is installed
     * @param Rcmd command necessary to start R
     * @return Rserve is already installed
     */
    public static boolean isRserveInstalled(String Rcmd) {
        Process p = doInR("i=installed.packages();is.element(set=i,el='Rserve')", Rcmd, "--vanilla -q");
        if (p == null) {
            return false;
        }

        try {
            StringBuffer result = new StringBuffer();
            // we need to fetch the output - some platforms will die if you don't ...
            StreamHog error = new StreamHog(p.getErrorStream(), true);
            StreamHog output = new StreamHog(p.getInputStream(), true);
            error.join();
            output.join();

            boolean isWindows = System.getProperty("os.name") != null && System.getProperty("os.name").length() >= 7 && System.getProperty("os.name").substring(0, 7).equals("Windows");
            if (!isWindows) /* on Windows the process will never return, so we cannot wait */ {
                p.waitFor();
            }
            result.append(output.getOutput());
            result.append(error.getOutput());

            //Logger.err.println("output=\n===========\n" + result.toString() + "\n===========\n");
            if (result.toString().contains("TRUE")) {
                return true;
            } else {
                return false;
            }
        } catch (InterruptedException e) {
            return false;
        }
    }

    /** R batch to install Rserve
     * @param Rcmd command necessary to start R
     * @param http_proxy http://login:password@proxy:port string to enable internet access to rforge server
     * @return success
     */
    public static boolean installRserve(String Rcmd, String http_proxy, String repository) {
        if (repository == null || repository.length() == 0) {
            repository = Rsession.DEFAULT_REPOS;
        }
        Log.Out.println("Install Rserve from " + repository + " ... (http_proxy=" + http_proxy + ") ");
        Process p = doInR((http_proxy != null ? "Sys.setenv(http_proxy=" + http_proxy + ");" : "") + "install.packages('Rserve',repos='" + repository + "')", Rcmd, "--vanilla");
        if (p==null) {
            Log.Err.println("failed");
            return false;
        }
        int n = 5;
        while (n > 0) {
            try {
                Thread.sleep(10000 / n);
                Log.Out.print(".");
            } catch (InterruptedException ex) {
            }
            if (isRserveInstalled(Rcmd)) {
                Log.Out.print(" ok");
                return true;
            }
            n--;
        }
        Log.Err.println("failed");
        return false;
    }

    /** attempt to start Rserve. Note: parameters are <b>not</b> quoted, so avoid using any quotes in arguments
    @param todo command to execute in R
    @param Rcmd command necessary to start R
    @param rargs arguments are are to be passed to R (e.g. --vanilla -q)
    @return <code>true</code> if Rserve is running or was successfully started, <code>false</code> otherwise.
     */
    public static Process doInR(String todo, String Rcmd, String rargs/*, StringBuffer out, StringBuffer err*/) {
        Process p = null;
        try {
            String osname = System.getProperty("os.name");
            String command = null;
            if (osname != null && osname.length() >= 7 && osname.substring(0, 7).equals("Windows")) {
                command = "\"" + Rcmd + "\" -e \"" + todo + "\" " + rargs;
                p = Runtime.getRuntime().exec(command);
            } else /* unix startup */ {
                command = "echo \"" + todo + "\"|" + Rcmd + " " + rargs;
                p = Runtime.getRuntime().exec(new String[]{"/bin/sh", "-c", command});
            }
            Log.Out.println("  executing " + command);
        } catch (Exception x) {
            Log.Err.println(x.getMessage());
        }
        return p;
    }

    /** shortcut to <code>launchRserve(cmd, "--no-save --slave", "--no-save --slave", false)</code> */
    public static Process launchRserve(String cmd) {
        return launchRserve(cmd, /*null,*/ "--no-save --slave", "--no-save --slave", false);
    }

    /** attempt to start Rserve. Note: parameters are <b>not</b> quoted, so avoid using any quotes in arguments
    @param cmd command necessary to start R
    @param rargs arguments are are to be passed to R
    @param rsrvargs arguments to be passed to Rserve
    @return <code>true</code> if Rserve is running or was successfully started, <code>false</code> otherwise.
     */
    public static Process launchRserve(String cmd, /*String libloc,*/ String rargs, String rsrvargs, boolean debug) {
        Log.Out.println("Waiting for Rserve to start ...");
        Process p = doInR("library(" + /*(libloc != null ? "lib.loc='" + libloc + "'," : "") +*/ "Rserve);Rserve(" + (debug ? "TRUE" : "FALSE") + ",args='" + rsrvargs + "')", cmd, rargs);
        if (p!=null) {
            Log.Out.println("Rserve startup done, let us try to connect ...");
        } else {
            Log.Err.println("Failed to start Rserve process.");
            return null;
        }

        int attempts = 15; /* try up to 15 times before giving up. We can be conservative here, because at this point the process execution itself was successful and the start up is usually asynchronous */
        while (attempts > 0) {
            try {
                RConnection c = null;
                int port = -1;
                if (rsrvargs.contains("--RS-port")) {
                    String rsport = rsrvargs.split("--RS-port")[1].trim().split(" ")[0];
                    port = Integer.parseInt(rsport);
                    c = new RConnection("localhost", port);
                } else {
                    c = new RConnection("localhost");
                }
                Log.Out.println("Rserve is running.");
                c.close();
                return p;
            } catch (Exception e2) {
                Log.Err.println("Try failed with: " + e2.getMessage());
            }
            /* a safety sleep just in case the start up is delayed or asynchronous */
            try {
                Thread.sleep(1000);
            } catch (InterruptedException ix) {
            }

            attempts--;
        }
        return null;
    }

    /** checks whether Rserve is running and if that's not the case it attempts to start it using the defaults for the platform where it is run on. 
    This method is meant to be set-and-forget and cover most default setups. For special setups you may get more control over R with <code>launchRserve</code> instead. */
    public static boolean checkLocalRserve() {
        if (isRserveRunning()) {
            return true;
        }
        String osname = System.getProperty("os.name");
        if (osname != null && osname.length() >= 7 && osname.substring(0, 7).equals("Windows")) {
            Log.Out.println("Windows: query registry to find where R is installed ...");
            String installPath = null;
            try {
                Process rp = Runtime.getRuntime().exec("reg query HKLM\\Software\\R-core\\R");
                RegistryHog regHog = new RegistryHog(rp.getInputStream(), true);
                rp.waitFor();
                regHog.join();
                installPath = regHog.getInstallPath();
            } catch (Exception rge) {
                Log.Err.println("ERROR: unable to run REG to find the location of R: " + rge);
                return false;
            }
            if (installPath == null) {
                Log.Err.println("ERROR: canot find path to R. Make sure reg is available and R was installed with registry settings.");
                return false;
            }
            return launchRserve(installPath + "\\bin\\R.exe")!=null;
        }
        return ((launchRserve("R")!=null)
                || /* try some common unix locations of R */ ((new File("/Library/Frameworks/R.framework/Resources/bin/R")).exists() && launchRserve("/Library/Frameworks/R.framework/Resources/bin/R")!=null)
                || ((new File("/usr/local/lib/R/bin/R")).exists() && launchRserve("/usr/local/lib/R/bin/R")!=null)
                || ((new File("/usr/lib/R/bin/R")).exists() && launchRserve("/usr/lib/R/bin/R")!=null)
                || ((new File("/usr/local/bin/R")).exists() && launchRserve("/usr/local/bin/R")!=null)
                || ((new File("/sw/bin/R")).exists() && launchRserve("/sw/bin/R")!=null)
                || ((new File("/usr/common/bin/R")).exists() && launchRserve("/usr/common/bin/R")!=null)
                || ((new File("/opt/bin/R")).exists() && launchRserve("/opt/bin/R")!=null));
    }

    /** check whether Rserve is currently running (on local machine and default port).
    @return <code>true</code> if local Rserve instance is running, <code>false</code> otherwise
     */
    public static boolean isRserveRunning() {
        try {
            RConnection c = new RConnection();
            Log.Out.println("Rserve is running.");
            c.close();
            return true;
        } catch (Exception e) {
            Log.Err.println("First connect try failed with: " + e.getMessage());
        }
        return false;
    }

    /** just a demo main method which starts Rserve and shuts it down again */
    public static void main(String[] args) {
        System.out.println("result=" + checkLocalRserve());
        try {
            RConnection c = new RConnection();
            c.shutdown();
        } catch (Exception x) {
        }
    }
}
