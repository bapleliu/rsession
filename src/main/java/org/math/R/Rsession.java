package org.math.R;

import java.io.File;
import java.io.FileFilter;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * @author richet
 */
public abstract class Rsession implements RLog {

    public static final String HEAD_TRY = "";//-try- ";
    public boolean TRY_MODE_DEFAULT = true;
    public boolean TRY_MODE = false;
    public static final String CAST_ERROR = "Cannot cast ";
    private static final String __ = "  ";
    private static final String _PACKAGE_ = "  package ";
    RLog console;
    public static final String PACKAGEINSTALLED = "Package installed.";
    public static final String PACKAGELOADED = "Package loaded.";
    static String separator = ",";

    public class RException extends Exception {

        public RException(String cause) {
            this(cause, false);
        }

        public RException(String cause, boolean details) {
            super(cause + "\nR: " + getLastLogEntry() + "\nR! " + getLastError() + (details ? "\nR: " + Arrays.asList(ls()) : ""));
        }

    }

    // <editor-fold defaultstate="collapsed" desc="Add/remove interfaces">
    List<RLog> loggers;
    public boolean debug;

    //** GLG HACK: Logging fix **//
    // No sink file (Passed to false) a lot faster not to sink the output
    boolean SINK_OUTPUT = true, SINK_MESSAGE = false;
    // GLG HACK: fixed sink file in case of multiple instances
    // (Appending the port number of the instance to file name)
    String SINK_FILE_BASE = ".Rout";
    String SINK_FILE = null;
    String lastOuput = "";

    void cleanupListeners() {
        if (loggers != null) {
            loggers.clear();
            /*while (!loggers.isEmpty()) {
             removeLogger(loggers.get(0));
             }*/
        }
        if (busy != null) {
            busy.clear();
            /*while (!busy.isEmpty()) {
             removeBusyListener(busy.get(0));
             }*/
        }
        if (updateObjects != null) {
            updateObjects.clear();
            /*while (!updateObjects.isEmpty()) {
             removeUpdateObjectsListener(updateObjects.get(0));
             }*/
        }
        if (eval != null) {
            eval.clear();
            /*while (!rawEval.isEmpty()) {
             removeEvalListener(rawEval.get(0));
             }*/
        }
    }

    public abstract boolean isAvailable();

    protected void finalize() throws Throwable {
        close();
        super.finalize();
    }

    public void addLogger(RLog l) {
        if (!loggers.contains(l)) {
            loggers.add(l);
        }
    }

    public void removeLogger(RLog l) {
        if (loggers.contains(l)) {
            l.close();
            loggers.remove(l);
        }
    }

    public void close() {
        for (RLog l : loggers) {
            l.close();
        }
    }

    List<BusyListener> busy = new LinkedList<BusyListener>();

    public void addBusyListener(BusyListener b) {
        if (!busy.contains(b)) {
            busy.add(b);
        }
    }

    public void removeBusyListener(BusyListener b) {
        if (busy.contains(b)) {
            busy.remove(b);
        }
    }

    public void setBusy(boolean bb) {
        for (BusyListener b : busy) {
            b.setBusy(bb);
        }

    }
    List<UpdateObjectsListener> updateObjects = new LinkedList<UpdateObjectsListener>();

    public void addUpdateObjectsListener(UpdateObjectsListener b) {
        if (!updateObjects.contains(b)) {
            updateObjects.add(b);
        }
    }

    public void removeUpdateObjectsListener(UpdateObjectsListener b) {
        if (updateObjects.contains(b)) {
            b.setTarget(null);
            updateObjects.remove(b);
        }
    }
    List<EvalListener> eval = new LinkedList<EvalListener>();

    public void addEvalListener(EvalListener b) {
        if (!eval.contains(b)) {
            eval.add(b);
        }
    }

    public void removeEvalListener(EvalListener b) {
        if (eval.contains(b)) {
            eval.remove(b);
        }
    }
    // </editor-fold>

    // <editor-fold defaultstate="collapsed" desc="Conveniency String methods">
    public static String cat(double[] array) {
        if (array == null || array.length == 0) {
            return "NA";
        }

        String o = array[0] + "";
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (separator + (array[i] + ""));
            }
        }
        return o;
    }

    public static String cat(int[] array) {
        if (array == null || array.length == 0) {
            return "NA";
        }

        String o = array[0] + "";
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (separator + (array[i] + ""));
            }
        }
        return o;
    }

    public static String cat(double[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0) {
            return "NA";
        }

        String o = cat(array[0]);
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += "\n" + cat(array[i]);
            }
        }
        return o;
    }

    public static String cat(int[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0) {
            return "NA";
        }

        String o = cat(array[0]);
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += "\n" + cat(array[i]);
            }
        }
        return o;
    }

    public static String cat(Object[] array) {
        if (array == null || array.length == 0 || array[0] == null) {
            return "";
        }

        String o = array[0].toString();
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (separator + (array[i] == null ? "" : array[i].toString()));
            }
        }

        return o;
    }

    public static String cat(String sep, String[] array) {
        if (array == null || array.length == 0 || array[0] == null) {
            return "";
        }

        String o = array[0].toString();
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += (sep + (array[i] == null ? "" : array[i].toString()));
            }
        }

        return o;
    }

    public static String cat(Object[][] array) {
        if (array == null || array.length == 0 || array[0].length == 0) {
            return "NA";
        }

        String o = cat(array[0]);
        if (array.length > 1) {
            for (int i = 1; i < array.length; i++) {
                o += "\n" + cat(array[i]);
            }
        }
        return o;
    }
    // </editor-fold>

    /**
     * Map java File object to R path (as string)
     *
     * @param path java File object
     */
    public static String toRpath(File path) {
        return toRpath(path.getAbsolutePath());
    }

    /**
     * Map java path to R path (as string)
     *
     * @param path java string path
     */
    public static String toRpath(String path) {
        return path.replaceAll("\\\\", "/");
    }

    /**
     * create a new Rsession.
     *
     * @param console PrintStream for R output
     * @param serverconf RserverConf server configuration object, giving IP,
     * port, login, password, properties to pass to R (eg http_proxy or R
     * libpath)
     * @param tryLocalRServe local spawned Rsession if given remote one failed
     * to initialized
     */
    public Rsession(final RLog console) {
        this.console = console;

        loggers = new LinkedList<RLog>();
        loggers.add(console);

        // Make sink file specific to current Rserve instance
        SINK_FILE = SINK_FILE_BASE;
    }

    /**
     * create rsession using System as a logger
     */
    public Rsession(final PrintStream p) {
        this(new RLog() {

            public void log(String string, Level level) {
                if (level == Level.WARNING) {
                    p.print("(!) ");
                } else if (level == Level.ERROR) {
                    p.print("(!!) ");
                }
                p.println(string);
            }

            public void close() {
                p.close();
            }
        });
    }

    String lastmessage = "";
    int repeated = 0;

    public String getLastLogEntry() {
        return lastmessage;
    }

    private void println(String message, RLog.Level level) {
        if (level == Level.ERROR) {
            try {
                message = message + "\n R> " + getLastLogEntry();
            } catch (Exception e) {
                e.printStackTrace();
                message = message + "\n ! " + e.getMessage();
            }
        }

    }

    public void log(String message, Level level) {
        if (message != null && message.trim().length() > 0 && !message.trim().equals("\n") && level == Level.OUTPUT) {
            println(message, level);
        } else {
            if (message == null) {
                return;
            } else {
                message = message.trim();
            }
            if (message.equals(lastmessage) && repeated < 100) {
                repeated++;
            } else {
                if (repeated > 0) {
                    println("    Repeated " + repeated + " times.", level);
                    repeated = 0;
                    lastmessage = message;
                    println(message, level);
                } else {
                    lastmessage = message;
                    println(message, level);
                }
            }
        }

        for (RLog l : loggers) {
            l.log(message, level);
        }
    }

    /**
     * @return available R commands
     */
    public String[] listCommands() {
        silentlyRawEval(".keyWords <- function() {n <- length(search());result <- c();for (i in 1:n) {result <- c(result,ls(pos=i,all.names=TRUE))}; result}");
        Object rexp = silentlyRawEval(".keyWords()");
        String as[] = null;
        try {
            if (rexp != null && (as = asStrings(rexp)) != null) {
                return as;
            } else {
                return null;
            }
        } catch (Exception ex) {
            log(HEAD_ERROR + ex.getMessage() + "\n  listCommands()", Level.ERROR);
            return null;
        }
    }
    // <editor-fold defaultstate="collapsed" desc="Packages management">
    public static String DEFAULT_REPOS = "http://cran.irsn.fr/";
    public String repos = DEFAULT_REPOS;

    /**
     * @param url CRAN repository to use for packages installation (eg
     * http://cran.r-project.org)
     */
    public void setCRANRepository(String url) {
        repos = url;
    }

    /**
     * @return CRAN repository used for packages installation
     */
    public String getCRANRepository() {
        return repos;
    }
    private static String loadedpacks = "loadedpacks";

    /**
     * Check for package loaded in R environment.
     *
     * @param pack R package name
     * @return package loading status
     */
    public boolean isPackageLoaded(String pack) {
        silentlyVoidEval(loadedpacks + "<-.packages()", false);
        boolean isloaded = false;
        try {
            Object i = silentlyRawEval("is.element(set=" + loadedpacks + "[,1],el='" + pack + "')");
            if (i != null) {
                isloaded = asLogical(i);
            }
        } catch (Exception ex) {
            log(HEAD_ERROR + ex.getMessage() + "\n  isPackageLoaded(String pack=" + pack + ")", Level.ERROR);
        }
        if (isloaded) {
            log(_PACKAGE_ + pack + " is loaded.", Level.INFO);
        } else {
            log(_PACKAGE_ + pack + " is not loaded.", Level.INFO);
        }

        //silentlyEval("rm(" + loadedpacks + ")");
        return isloaded;
    }
    private static String packs = "packs";

    /**
     * Check for package installed in R environment.
     *
     * @param pack R package name
     * @param version R package version
     * @return package loading status
     */
    public boolean isPackageInstalled(String pack, String version) {
        silentlyVoidEval(packs + "<-installed.packages(noCache=TRUE)", false);
        boolean isinstalled = false;
        Object r = silentlyRawEval("is.element(set=" + packs + "[,1],el='" + pack + "')");
        try {
            if (r != null) {
                isinstalled = (asInteger(r) == 1);
            } else {
                log(HEAD_ERROR + "Could not list installed packages" + "\n  isPackageInstalled(String pack=" + pack + ", String version=" + version + ")", Level.ERROR);
            }
        } catch (Exception ex) {
            log(HEAD_ERROR + ex.getMessage() + "\n  isPackageInstalled(String pack=" + pack + ", String version=" + version + ")", Level.ERROR);
        }
        if (isinstalled) {
            log(_PACKAGE_ + pack + " is installed.", Level.INFO);
        } else {
            log(_PACKAGE_ + pack + " is not installed.", Level.INFO);
        }

        if (isinstalled && version != null && version.length() > 0) {
            try {
                isinstalled = asLogical(silentlyRawEval(packs + "['" + pack + "','Version'] == \"" + version + "\""));
            } catch (Exception ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  isPackageInstalled(String pack=" + pack + ", String version=" + version + ")", Level.ERROR);
            }
            try {
                log("    version of package " + pack + " is " + asString(silentlyRawEval(packs + "['" + pack + "','Version']")), Level.INFO);
            } catch (Exception ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  isPackageInstalled(String pack=" + pack + ", String version=" + version + ")", Level.ERROR);
            }
            if (isinstalled) {
                log(_PACKAGE_ + pack + " (" + version + ") " + " is installed.", Level.INFO);
            } else {
                log(_PACKAGE_ + pack + " (" + version + ") " + " is not installed.", Level.INFO);
            }

        }
        //silentlyEval("rm(" + packs + ")");
        return isinstalled;
    }

    /**
     * Start installation procedure of R packages
     *
     * @param pack packages to install
     * @param load automatically load packages after successfull installation
     * @return installation status
     */
    public String installPackages(String[] pack, boolean load) {
        String resall = "";
        for (String pv : pack) {
            String res = installPackage(pv, load);
            if (load) {
                if (!res.equals(PACKAGELOADED)) {
                    resall += "\n" + res;
                }
            } else {
                if (!res.equals(PACKAGEINSTALLED)) {
                    resall += "\n" + res;
                }
            }
        }
        if (resall.length() > 0) {
            return resall;
        } else {
            return load ? PACKAGELOADED : PACKAGEINSTALLED;
        }
    }

    /**
     * Start installation procedure of local R package
     *
     * @param pack package file to install
     * @param load automatically load package after successfull installation
     * @return installation status
     */
    public String installPackage(File pack, boolean load) {
        putFile(pack);
        try {
            rawEval("install.packages('" + pack.getName() + "',repos=NULL");
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        log("  request package " + pack + " install...", Level.INFO);

        String name = pack.getName();
        if (name.contains("_")) {
            name = name.substring(0, name.indexOf("_"));
        }
        if (name.contains(".")) {
            name = name.substring(0, name.indexOf("."));
        }

        if (isPackageInstalled(name, null)) {
            log(_PACKAGE_ + pack + " installation sucessfull.", Level.INFO);
            if (load) {
                return loadPackage(name);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " installation failed.", Level.ERROR);
            if (load) {
                return loadPackage(name);
            } else {
                return "Impossible to install package " + pack + " !";
            }
        }
    }

    abstract boolean isWindows();

    abstract boolean isLinux();

    abstract boolean isMacOSX();

    /**
     * Start installation procedure of local R package
     *
     * @param pack package to install
     * @param dir directory where package file (.zip, .tar.gz or .tgz) is
     * located.
     * @param load automatically load package after successfull installation
     * @return installation status
     */
    public String installPackage(final String pack, File dir, boolean load) {
        log("  trying to load package " + pack, Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " already installed.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " not yet installed.", Level.INFO);
        }

        File[] pack_files = (dir == null ? null : dir.listFiles(new FileFilter() {

            public boolean accept(File pathname) {

                if (!pathname.getName().contains(pack)) {
                    return false;
                }
                if (isWindows()) {
                    return pathname.getName().endsWith(".zip");
                }
                if (isLinux()) {
                    return pathname.getName().endsWith(".tar.gz");
                }
                if (isMacOSX()) {
                    return pathname.getName().endsWith(".tgz");
                }
                return false;
            }
        }));
        if (pack_files == null || pack_files.length == 0) {
            log("  impossible to find package " + pack + " in directory " + dir.getAbsolutePath() + " !", Level.WARNING);
            return "Impossible to find package " + pack + " in directory " + dir.getAbsolutePath() + " !";
        } else {
            log("  found package " + pack + " : " + pack_files[0].getAbsolutePath(), Level.INFO);
        }

        putFile(pack_files[0]);
        rawEval("install.packages('" + pack_files[0].getName() + "',repos=NULL)", TRY_MODE);
        log("  request package " + pack + " install...", Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " installation sucessfull.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " installation failed.", Level.ERROR);
            if (load) {
                return loadPackage(pack);
            } else {
                return "Impossible to install package " + pack + " !";
            }
        }
    }

    /**
     * Start installation procedure of CRAN R package
     *
     * @param pack package to install
     * @param load automatically load package after successfull installation
     * @return installation status
     */
    public String installPackage(String pack, boolean load) {
        log("  trying to load package " + pack, Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " already installed.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " not yet installed.", Level.INFO);
        }

        /*if (!Configuration.isWWWConnected()) {
         log("  package " + pack + " not accessible on " + repos + ": CRAN unreachable.");
         return "Impossible to get package " + pack + " from " + repos;
         }*/
        rawEval("install.packages('" + pack + "',repos='" + repos + "')", TRY_MODE);
        log("  request package " + pack + " install...", Level.INFO);

        if (isPackageInstalled(pack, null)) {
            log(_PACKAGE_ + pack + " installation sucessfull.", Level.INFO);
            if (load) {
                return loadPackage(pack);
            } else {
                return PACKAGEINSTALLED;
            }
        } else {
            log(_PACKAGE_ + pack + " installation failed.", Level.ERROR);
            if (load) {
                return loadPackage(pack);
            } else {
                return "Impossible to install package " + pack + " !";
            }
        }
    }

    /**
     * load R backage using library() command
     *
     * @param pack R package name
     * @return loading status
     */
    public String loadPackage(String pack) {
        log("  request package " + pack + " loading...", Level.INFO);
        try {
            boolean ok = asLogical(rawEval("library(" + pack + ",logical.return=T,quietly=T,verbose=F)", TRY_MODE));
            if (ok) {
                log(_PACKAGE_ + pack + " loading sucessfull.", Level.INFO);
                return PACKAGELOADED;
            } else {
                log(_PACKAGE_ + pack + " loading failed.", Level.ERROR);
                return "Impossible to load package " + pack + ": " + getLastLogEntry();
            }
        } catch (Exception ex) {
            log(_PACKAGE_ + pack + " loading failed.", Level.ERROR);
            return "Impossible to load package " + pack + ": " + ex.getLocalizedMessage();
        }

        /*rawEval("library(" + pack + ",logical.return=T)", TRY_MODE);
         log("  request package " + pack + " loading...", Level.INFO);
         if (isPackageLoaded(pack)) {
         log(_PACKAGE_ + pack + " loading sucessfull.", Level.INFO);
         return PACKAGELOADED;
         } else {
         log(_PACKAGE_ + pack + " loading failed.", Level.ERROR);
         return "Impossible to load package " + pack + ": " + getLastError();
         }*/
    }
    // </editor-fold>
    final static String HEAD_EVAL = "[eval] ";
    final static String HEAD_EXCEPTION = "[exception] ";
    final static String HEAD_ERROR = "[error] ";
    final static String HEAD_CACHE = "[cache] ";

    public String getLastOutput() {
        if (!SINK_OUTPUT) {
            return null;
        } else {
            return lastOuput;
        }
    }

    public String getLastError() {
        Object err = silentlyRawEval("geterrmessage()");
        return (err==null?"":asString(err));
    }

    /**
     * Silently (ie no log) launch R command without return value. Encapsulate
     * command in try() to cacth errors
     *
     * @param expression R expresison to evaluate
     */
    protected boolean silentlyVoidEval(String expression) {
        return silentlyVoidEval(expression, TRY_MODE_DEFAULT);
    }

    /**
     * Silently (ie no log) launch R command without return value.
     *
     * @param expression R expresison to evaluate
     * @param tryEval encapsulate command in try() to cacth errors
     */
    protected abstract boolean silentlyVoidEval(String expression, boolean tryEval);

    /**
     * Silently (ie no log) launch R command and return value. Encapsulate
     * command in try() to cacth errors.
     *
     * @param expression R expresison to evaluate
     * @return REXP R expression
     */
    protected Object silentlyRawEval(String expression) {
        return silentlyRawEval(expression, TRY_MODE_DEFAULT);
    }

    /**
     * Silently (ie no log) launch R command and return value.
     *
     * @param expression R expression to evaluate
     * @param tryEval encapsulate command in try() to cacth errors
     * @return REXP R expression
     */
    protected abstract Object silentlyRawEval(String expression, boolean tryEval);

    /**
     * Launch R command and return value.
     *
     * @param expression R expresison to evaluate
     * @param tryEval encapsulate command in try() to cacth errors
     * @return REXP R expression
     */
    protected Object rawEval(String expression, boolean tryEval) {
        log(HEAD_EVAL + (tryEval ? HEAD_TRY : "") + expression, Level.INFO);

        Object e = silentlyRawEval(expression, tryEval);

        for (UpdateObjectsListener b : updateObjects) {
            b.update();
        }

        if (e != null) {
            log(__ + toString(e), Level.INFO);
        }

        return e;
    }

    /**
     * Launch R command and return value. Encapsulate command in try() to cacth
     * errors.
     *
     * @param expression R expresison to evaluate
     * @return REXP R expression
     */
    protected Object rawEval(String expression) {
        return rawEval(expression, TRY_MODE_DEFAULT);
    }

    /**
     * Launch R command without return value.
     *
     * @param expression R expresison to evaluate
     * @param tryEval encapsulate command in try() to cacth errors
     */
    public boolean voidEval(String expression, boolean tryEval) throws RException {
        log(HEAD_EVAL + (tryEval ? HEAD_TRY : " ") + expression, Level.INFO);

        boolean done = silentlyVoidEval(expression, tryEval);
        if (!done) {
            throw new RException("Failed to evaluate " + expression);
        }

        if (done) {
            for (UpdateObjectsListener b : updateObjects) {
                b.update();
            }
        }

        return done;
    }

    /**
     * Launch R command without return value. Encapsulate command in try() to
     * cacth errors.
     *
     * @param expression R expresison to evaluate
     */
    public boolean voidEval(String expression) throws RException {
        boolean done = voidEval(expression, TRY_MODE_DEFAULT);
        if (!done) {
            throw new RException("Failed to evaluate " + expression);
        }
        return done;
    }

    public Object eval(String expression, boolean tryEval) throws RException {
        Object o = rawEval(expression, tryEval);
        if (o == null) {
            throw new RException("Failed to evaluate " + expression);
        }
        return cast(o);
    }

    public Object eval(String expression) throws RException {
        Object o = rawEval(expression);
        if (o == null) {
            throw new RException("Failed to evaluate " + expression);
        }
        return cast(o);
    }

    public class Function {

        String name;

        public Function(String name) {
            this.name = name;
        }

        public Object evaluate() throws RException {
            return eval(name + "()");
        }

        public Object evaluate(Object... args) throws RException {
            String[] x = new String[args.length];
            String arg = "";
            for (int i = 0; i < x.length; i++) {
                set(".x" + i, args[i]);
                arg = arg + ",.x" + i;
            }

            return eval(name + "(" + arg.substring(1) + ")");
        }
    }

    /**
     * delete all variables in R environment
     */
    public boolean rmAll() {
        try {
            return voidEval("rm(list=ls(all=TRUE))", TRY_MODE);
        } catch (Exception ex) {
            return false;
        }
    }

    /**
     * create a R list with given R objects
     *
     * @param vars R object names
     * @return list expression
     */
    public static String buildList(String... vars) {
        if (vars.length > 1) {
            StringBuffer b = new StringBuffer("c(");
            for (String v : vars) {
                b.append(v + ",");
            }

            return b.substring(0, b.length() - 1) + ")";
        } else {
            return vars[0];
        }
    }

    /**
     * create a R list with given R strings
     *
     * @param vars R strings
     * @return String list expression
     */
    public static String buildListString(String... vars) {
        if (vars.length > 1) {
            StringBuffer b = new StringBuffer("c(");
            for (String v : vars) {
                b.append("'" + v + "',");
            }

            return b.substring(0, b.length() - 1) + ")";
        } else {
            return "'" + vars[0] + "'";
        }
    }

    /**
     * create a R list with given R string patterns
     *
     * @param vars R string patterns
     * @return ls pattern expression
     */
    public static String buildListPattern(String... vars) {
        if (vars.length > 1) {
            StringBuffer b = new StringBuffer("c(");
            for (String v : vars) {
                b.append("ls(pattern='" + v + "'),");
            }

            return b.substring(0, b.length() - 1) + ")";
        } else {
            return "ls(pattern='" + vars[0] + "')";
        }
    }

    /**
     * loads R source file (eg ".R" file)
     *
     * @param f ".R" file to source
     */
    public void source(File f) {
        putFile(f);
        try {
            assert asLogical(rawEval("file.exists('" + f.getName() + "')", TRY_MODE));
        } catch (Exception r) {
            r.printStackTrace();
        }
        try {
            voidEval("source('" + f.getName() + "')", TRY_MODE);
        } catch (Exception ex) {
            log(ex.getMessage(), Level.ERROR);
        }
    }

    /**
     * loads R data file (eg ".Rdata" file)
     *
     * @param f ".Rdata" file to load
     */
    public void load(File f) {
        putFile(f);
        try {
            assert asLogical(rawEval("file.exists('" + f.getName() + "')", TRY_MODE));
        } catch (Exception r) {
            r.printStackTrace();
        }
        try {
            voidEval("load('" + f.getName() + "')", TRY_MODE);
        } catch (Exception ex) {
            log(ex.getMessage(), Level.ERROR);
        }
    }

    /**
     * list R variables in R env.
     *
     * @return list of R objects names
     */
    public String[] ls() {
        try {
            return asStrings(rawEval("ls()", false));
        } catch (Exception re) {
            re.printStackTrace();
            return new String[0];
        }
    }

    /**
     * list R variables in R env. matching patterns
     *
     * @param vars R object name patterns
     * @return list of R objects names
     */
    public String[] ls(String... vars) {
        if (vars == null || vars.length == 0) {
            try {
                return asStrings(rawEval("ls()", false));
            } catch (Exception re) {
                re.printStackTrace();
                return new String[0];
            }
        } else if (vars.length == 1) {
            try {
                return asStrings(rawEval(buildListPattern(vars[0]), TRY_MODE));
            } catch (Exception re) {
                re.printStackTrace();
                return new String[0];
            }
        } else {
            try {
                return asStrings(rawEval(buildListPattern(vars), TRY_MODE));
            } catch (Exception re) {
                re.printStackTrace();
                return new String[0];
            }
        }
    }

    /**
     * delete R variables in R env.
     *
     * @param vars R objects names
     */
    public boolean rm(String... vars) throws RException {
        if (vars.length == 1) {
            return voidEval("rm(" + vars[0] + ")", TRY_MODE);
        } else {
            return voidEval("rm(list=" + buildListString(vars) + ")", TRY_MODE);
        }
    }

    /**
     * delete R variables in R env. matching patterns
     *
     * @param vars R object name patterns
     */
    public boolean rmls(String... vars) throws RException {
        if (vars.length == 1) {
            return voidEval("rm(list=" + buildListPattern(vars[0]) + ")", TRY_MODE);
        } else {
            return voidEval("rm(list=" + buildListPattern(vars) + ")", TRY_MODE);
        }
    }
    public boolean SAVE_ASCII = false;

    /**
     * Save R variables in data file
     *
     * @param f file to store data (eg ".Rdata")
     * @param vars R variables to save
     */
    public void save(File f, String... vars) throws RException {
        if (vars.length == 1) {
            voidEval("save(file='" + f.getName() + "'," + vars[0] + ",ascii=" + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        } else {
            voidEval("save(file='" + f.getName() + "',list=" + buildListString(vars) + ",ascii=" + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        }
        getFile(f);
        deleteFile(f.getName());
    }

    /**
     * Save R variables in data file
     *
     * @param f file to store data (eg ".Rdata")
     * @param vars R variables names patterns to save
     */
    public void savels(File f, String... vars) throws RException {
        if (vars.length == 1) {
            voidEval("save(file='" + f.getName() + "',list=" + buildListPattern(vars[0]) + ",ascii=" + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        } else {
            voidEval("save(file='" + f.getName() + "',list=" + buildListPattern(vars) + ",ascii=" + (SAVE_ASCII ? "TRUE" : "FALSE") + ")", TRY_MODE);
        }
        try {
            Thread.sleep(1000);
        } catch (InterruptedException ex) {
        }
        getFile(f);
        deleteFile(f.getName());
    }
    final static String[] types = {"data.frame", "null", "function", "array", "integer", "character", "double"};

    /**
     *
     * @param robject R object name
     * @return R type of object
     */
    public String typeOf(String robject) {
        if (robject == null) {
            return "NULL";
        }
        for (String t : types) {
            try {
                boolean is = asLogical(silentlyRawEval("is." + t + "(" + robject + ")"));
                if (is) {
                    return t;
                }
            } catch (Exception ex) {
                log(HEAD_ERROR + "[typeOf] " + robject + " type unknown.", Level.ERROR);
                return null;
            }
        }
        return "unknown";
    }

    public final static String HEAD_SET = "[set] ";

    /**
     * delete R object in R env.
     *
     * @param varname R objects to delete
     */
    public boolean unset(String... varname) throws RException {
        return rm(varname);
    }

    /**
     * delete R object in R env.
     *
     * @param varname R objects to delete
     */
    public boolean unset(Collection varname) throws RException {
        boolean done = true;
        for (Object v : varname) {
            done = done & rm(v.toString());
        }
        return done;
    }

    /**
     * Set R object in R env.
     *
     * @param _vars R objects to set as key/values
     */
    public boolean set(Map<String, Object> _vars) throws RException {
        boolean done = true;
        for (String varname : _vars.keySet()) {
            done = done & set(varname, _vars.get(varname));
        }
        return done;
    }

    /**
     * Set R data.frame in R env.
     *
     * @param varname R list name
     * @param data numeric data in list
     * @param names names of columns
     */
    public abstract boolean set(String varname, double[][] data, String... names) throws RException;

    /**
     * Set R object in R env.
     *
     * @param varname R object name
     * @param var R object value
     */
    public abstract boolean set(String varname, Object var) throws RException;

    protected static double[] reshapeAsRow(double[][] a) {
        double[] reshaped = new double[a.length * a[0].length];
        int ir = 0;
        for (int j = 0; j < a[0].length; j++) {
            for (int i = 0; i < a.length; i++) {
                reshaped[ir] = a[i][j];
                ir++;
            }
        }
        return reshaped;
    }

    protected static double[] reshapeAsRow(Double[][] a) {
        double[] reshaped = new double[a.length * a[0].length];
        int ir = 0;
        for (int j = 0; j < a[0].length; j++) {
            for (int i = 0; i < a.length; i++) {
                reshaped[ir] = a[i][j];
                ir++;
            }
        }
        return reshaped;
    }
    
    protected double[][] t(double[][] m){
    double[][] tm = new double[m[0].length][m.length];
        for (int i = 0; i < tm.length; i++) {
            for (int j = 0; j < tm[i].length; j++) {
                tm[i][j] = m[j][i];
                
            }
        }
        return tm;
    }

    public abstract double asDouble(Object o) throws ClassCastException;

    public abstract double[] asArray(Object o) throws ClassCastException;

    public abstract double[][] asMatrix(Object o) throws ClassCastException;

    public abstract String asString(Object o) throws ClassCastException;

    public abstract String[] asStrings(Object o) throws ClassCastException;

    public abstract int asInteger(Object o) throws ClassCastException;

    public abstract int[] asIntegers(Object o) throws ClassCastException;

    public abstract boolean asLogical(Object o) throws ClassCastException;

    public abstract boolean[] asLogicals(Object o) throws ClassCastException;

    public abstract Map asList(Object o) throws ClassCastException;

    public abstract boolean isNull(Object o);

    public abstract String toString(Object o);

    public abstract Object cast(Object o) throws ClassCastException;

    /*public Object cast(Object o) {
     Object oo = o;
     try {
     oo = castStrict(o);
     } catch (ClassCastException e) {
     }
     return oo;
     }*/
    /**
     * Create a JPEG file for R graphical command output
     *
     * @param f File to store data (eg .jpg file)
     * @param width width of image
     * @param height height of image
     * @param fileformat format of image: png,tiff,jpeg,bmp
     * @param commands R command to create image (eg plot())
     */
    public void toGraphic(File f, int width, int height, String fileformat, String... commands) {
        int h = Math.abs(f.hashCode());
        try {
            set("plotfile_" + h, f.getName());
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        silentlyRawEval(fileformat + "(plotfile_" + h + ", width=" + width + ", height=" + height + ")");
        for (String command : commands) {
            silentlyVoidEval(command);
        }
        silentlyRawEval("dev.off()");
        getFile(f);
        try {
            rm("plotfile_" + h);
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        deleteFile(f.getName());
    }
    public final static String GRAPHIC_PNG = "png";
    public final static String GRAPHIC_JPEG = "jpeg";
    public final static String GRAPHIC_BMP = "bmp";
    public final static String GRAPHIC_TIFF = "tiff";

    public void toGraphic(File f, int width, int height, String... commands) {
        if (f.getName().endsWith(GRAPHIC_BMP)) {
            toBMP(f, width, height, commands);
        } else if (f.getName().endsWith(GRAPHIC_JPEG)) {
            toJPEG(f, width, height, commands);
        } else if (f.getName().endsWith(GRAPHIC_PNG)) {
            toPNG(f, width, height, commands);
        } else if (f.getName().endsWith(GRAPHIC_TIFF)) {
            toTIFF(f, width, height, commands);
        } else {
            toPNG(f, width, height, commands);
        }
    }

    public void toJPEG(File f, int width, int height, String... commands) {
        toGraphic(f, width, height, GRAPHIC_JPEG, commands);
    }

    public void toPNG(File f, int width, int height, String... commands) {
        if (isMacOSX()) {
            toGraphic(f, width, height, GRAPHIC_JPEG, commands);
        } else {
            toGraphic(f, width, height, GRAPHIC_PNG, commands);
        }
    }

    public void toBMP(File f, int width, int height, String... commands) {
        toGraphic(f, width, height, GRAPHIC_BMP, commands);
    }

    public void toTIFF(File f, int width, int height, String... commands) {
        toGraphic(f, width, height, GRAPHIC_TIFF, commands);
    }

    /**
     * Get R command text output in HTML format
     *
     * @param command R command returning text
     * @return HTML string
     */
    public String asR2HTML(String command) {
        String ret = installPackage("R2HTML", true);
        if (!ret.equals(PACKAGELOADED)) {
            return ret;
        }
        int h = Math.abs(command.hashCode());
        silentlyRawEval("HTML(file=\"htmlfile_" + h + "\", " + command + ")");
        String[] lines = null;
        try {
            lines = asStrings(silentlyRawEval("readLines(\"htmlfile_" + h + "\")"));
        } catch (Exception e) {
            return e.getMessage();
        }
        deleteFile("htmlfile_" + h);
        if (lines == null) {
            return "";
        }

        StringBuffer sb = new StringBuffer();
        for (String l : lines) {
            sb.append(l);
            sb.append("\n");
        }
        String str = sb.toString();
        str = str.replace("align= center ", "align='center'");
        str = str.replace("cellspacing=0", "cellspacing='0'");
        str = str.replace("border=1", "border='1'");
        str = str.replace("align=bottom", "align='bottom'");
        str = str.replace("class=dataframe", "class='dataframe'");
        str = str.replace("class= firstline ", "class='firstline'");
        str = str.replace("class=firstcolumn", "class='firstcolumn'");
        str = str.replace("class=cellinside", "class='cellinside'");
        str = str.replace("border=0", "border='0'");
        str = str.replace("class=captiondataframe", "class='captiondataframe'");
        str = str.replace("</td></table>", "</td></tr></table>");
        return str;
    }

    /**
     * Get R command text output in HTML format
     *
     * @param command R command returning text
     * @return HTML string
     */
    public String asHTML(String command) {
        return toHTML(print(command));
    }

    public static String toHTML(String src) {
        if (src == null) {
            return src;
        }
        src = src.replace("&", "&amp;");
        src = src.replace("\"", "&quot;");
        src = src.replace("'", "&apos;");
        src = src.replace("<", "&lt;");
        src = src.replace(">", "&gt;");
        return "<html>" + src.replace("\n", "<br/>") + "</html>";
    }

    /**
     * Get R command text output
     *
     * @param command R command returning text
     * @return String
     */
    public String print(String command) {
        try {
            String s = asString(silentlyRawEval("paste(capture.output(print(" + command + ")),collapse='\\n')"));
            return s;
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }
    final static String IO_HEAD = "[IO] ";

    /**
     * Get file from R environment to user filesystem
     *
     * @param localfile file to get (same name in R env. and user filesystem)
     */
    public void getFile(File localfile) {
        getFile(localfile, localfile.getName());
    }

    /**
     * Get file from R environment to user filesystem
     *
     * @param localfile local filesystem file
     * @param remoteFile R environment file name
     */
    public abstract void getFile(File localfile, String remoteFile);

    /**
     * delete R environment file
     *
     * @param remoteFile filename to delete
     */
    public abstract void deleteFile(String remoteFile);

    /**
     * Send user filesystem file in r environement (like data)
     *
     * @param localfile File to send
     */
    public void putFile(File localfile) {
        putFile(localfile, localfile.getName());
    }

    /**
     * Send user filesystem file in r environement (like data)
     *
     * @param localfile File to send
     * @param remoteFile filename in R env.
     */
    public abstract void putFile(File localfile, String remoteFile);

    final static String testExpression = "1+pi";
    final static double testResult = 1 + Math.PI;
    Map<String, Object> noVarsEvals = new HashMap<String, Object>();

    /**
     * Method to rawEval expression. Holds many optimizations (@see noVarsEvals)
     * and turn around for reliable usage (like engine auto restart). 1D Numeric
     * "vars" are replaced using Java replace engine instead of R one. Intended
     * to not interfer with current R env vars. Yes, it's hard-code :)
     *
     * @param expression String to evaluate
     * @param vars HashMap&lt;String, Object&gt; vars inside expression.
     * Passively overload current R env variables.
     * @return java castStrict Object Warning, UNSTABLE and high CPU cost.
     */
    public synchronized Object proxyEval(String expression, Map<String, Object> vars) throws RException {
        if (expression.length() == 0) {
            return null;
        }

        try {
            log(HEAD_CACHE + "No evaluation needed for " + expression, Level.INFO);
            return Double.parseDouble(expression);
        } catch (NumberFormatException ne) {

            if (!uses(expression, vars) && noVarsEvals.containsKey(expression)) {
                log(HEAD_CACHE + "Cached evaluation of " + expression + " in " + noVarsEvals, Level.INFO);
                return noVarsEvals.get(expression);
            }

            if (vars != null && vars.containsKey(expression)) {
                log(HEAD_CACHE + "Get evaluation of " + expression + " in " + vars, Level.INFO);
                return vars.get(expression);
            }

            Map<String, Object> clean_vars = new HashMap<String, Object>();
            String clean_expression = expression;
            if (vars != null) {
                for (String v : vars.keySet()) {
                    if (vars.get(v) instanceof Number) {
                        while (containsVar(clean_expression, v)) {
                            clean_expression = replaceVar(clean_expression, v, "(" + vars.get(v) + ")");
                        }
                        log(HEAD_CACHE + "Replacing " + v + " in " + clean_expression, Level.INFO);
                    } else {
                        if (containsVar(clean_expression, v)/*clean_expression.contains(v)*/) {
                            String newvarname = v;
                            while (ls(newvarname).length > 0) {
                                newvarname = "_" + newvarname;
                            }
                            log(HEAD_CACHE + "Renaming " + v + " by " + newvarname + " in " + clean_expression, Level.INFO);
                            while (containsVar(clean_expression, v)) {
                                clean_expression = replaceVar(clean_expression, v, newvarname);
                            }
                            clean_vars.put(newvarname, vars.get(v));
                        }
                    }
                }
            }

            if (!uses(clean_expression, clean_vars) && noVarsEvals.containsKey(clean_expression)) {
                log(HEAD_CACHE + "Cached evaluation of " + expression + " in " + noVarsEvals, Level.INFO);
                return noVarsEvals.get(clean_expression);
            }

            Object out = null;
            try {
                if (uses(clean_expression, clean_vars)) {
                    set(clean_vars);
                }
                log(HEAD_CACHE + "True evaluation of " + clean_expression + " with " + clean_vars, Level.INFO);
                out = eval(clean_expression);

                if (clean_vars.isEmpty() && out != null) {
                    log(HEAD_CACHE + "Saving result of " + clean_expression, Level.INFO);
                    noVarsEvals.put(clean_expression, out);
                }

                if (!uses(expression, vars) && out != null) {
                    log(HEAD_CACHE + "Saving result of " + expression, Level.INFO);
                    noVarsEvals.put(expression, out);
                }

            } catch (Exception e) {
                log(HEAD_CACHE + "Failed cast of " + expression, Level.INFO);
                throw new RException(CAST_ERROR + expression + ": " + e.getMessage());
            } finally {
                if (uses(clean_expression, clean_vars)) {
                    unset(clean_vars.keySet());
                }

            }

            return out;
        }
    }
    final static String AW = "((\\A)|(\\W))(";
    final static String Az = ")((\\W)|(\\z))";

    static String replaceVar(final String expr, final String var, final String val) {
        String regexp = AW + var + Az;
        Matcher m = Pattern.compile(regexp).matcher(expr);
        if (m.find()) {
            return expr.replace(m.group(), m.group().replace(var, val));
        } else {
            return expr;
        }
    }

    static boolean containsVar(final String expr, final String var) {
        String regexp = AW + var + Az;
        Matcher m = Pattern.compile(regexp).matcher(expr);
        return m.find();
    }

    static boolean areUsed(String expression, Set<String> vars) {
        for (String v : vars) {
            if (containsVar(expression, v)) {
                return true;
            }
        }
        return false;
    }

    static boolean uses(String expression, Map<String, Object> vars) {
        return vars != null && !vars.isEmpty() && areUsed(expression, vars.keySet());
    }
}
