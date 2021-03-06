package org.math.R;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import javax.script.ScriptException;
import org.apache.commons.io.FileUtils;
import static org.math.R.Rsession.HEAD_EXCEPTION;
import org.math.array.DoubleArray;
import org.renjin.primitives.matrix.Matrix;
import org.renjin.script.RenjinScriptEngine;
import org.renjin.script.RenjinScriptEngineFactory;
import org.renjin.sexp.DoubleArrayVector;
import org.renjin.sexp.DoubleVector;
import org.renjin.sexp.IntVector;
import org.renjin.sexp.ListVector;
import org.renjin.sexp.Logical;
import static org.renjin.sexp.Logical.TRUE;
import org.renjin.sexp.LogicalVector;
import org.renjin.sexp.Null;
import org.renjin.sexp.SEXP;
import org.renjin.sexp.StringArrayVector;
import org.renjin.sexp.StringVector;

/**
 *
 * @author richet
 */
public class RenjinSession extends Rsession implements RLog {

    private RenjinScriptEngine R = null;
    File wdir;
    Properties properties;

    public static RenjinSession newInstance(final RLog console, Properties properties) {
        return new RenjinSession(console, properties);
    }

    public RenjinSession(RLog console, Properties properties) {
        super(console);

        R = new RenjinScriptEngineFactory().getScriptEngine();
        if (R == null) {
            throw new RuntimeException("Renjin Script Engine not found on the classpath.");
        }

        try {
            wdir = new File(FileUtils.getTempDirectory(), "Renjin");
            wdir.mkdir();
            R.eval("setwd('" + wdir.getAbsolutePath() + "')");
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        SINK_FILE = SINK_FILE_BASE + "-renjin" + this.hashCode();

        this.properties = properties;
        if (properties != null) {
            for (String p : properties.stringPropertyNames()) {
                try {
                    rawEval("Sys.setenv(" + p + "=" + properties.getProperty(p) + ")");
                } catch (Exception ex) {
                    ex.printStackTrace();
                }
            }
        }
    }

    public RenjinSession(final PrintStream p, Properties properties) {
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
        }, properties);
    }

    private static String OS = System.getProperty("os.name").toLowerCase();

    @Override
    boolean isWindows() {
        return (OS.indexOf("win") >= 0);
    }

    @Override
    boolean isMacOSX() {
        return (OS.indexOf("mac") >= 0);
    }

    @Override
    boolean isLinux() {
        return OS.indexOf("inux") >= 0;
    }

    @Override
    public boolean silentlyVoidEval(String expression, boolean tryEval) {
        if (R == null) {
            log(HEAD_EXCEPTION + "R environment not initialized.", Level.ERROR);
            return false;
        }
        if (expression == null) {
            return false;
        }
        if (expression.trim().length() == 0) {
            return true;
        }
        for (EvalListener b : eval) {
            b.eval(expression);
        }
        SEXP e = null;
        try {
            synchronized (R) {
                if (SINK_OUTPUT) {
                    R.eval("sink('" + SINK_FILE + "',type='output')");
                }
                if (SINK_MESSAGE) {
                    R.eval("sink('" + SINK_FILE + ".m',type='message')");
                }
                if (tryEval) {
                    e = ((SEXP) R.eval("try(eval(parse(text='" + expression.replace("'", "\\'") + "')),silent=FALSE)"));
                } else {
                    e = ((SEXP) R.eval(expression));
                }
                if (SINK_OUTPUT) {
                    R.eval("sink(type='output')");
                    try {
                        lastOuput = asString(R.eval("paste(collapse='\n',readLines('" + SINK_FILE + "'))"));
                        log(lastOuput, Level.OUTPUT);
                    } catch (Exception ex) {
                        lastOuput = ex.getMessage();
                        log(lastOuput, Level.WARNING);
                    }
                    R.eval("unlink('" + SINK_FILE + "')");
                }
                if (SINK_MESSAGE) {
                    R.eval("sink(type='message')");
                    try {
                        lastOuput = asString(R.eval("paste(collapse='\n',readLines('" + SINK_FILE + ".m'))"));
                        log(lastOuput, Level.INFO);
                    } catch (Exception ex) {
                        lastOuput = ex.getMessage();
                        log(lastOuput, Level.WARNING);
                    }
                    R.eval("unlink('" + SINK_FILE + ".m')");
                }
            }
        } catch (Exception ex) {
            log(HEAD_EXCEPTION + ex.getMessage() + "\n  " + expression, Level.ERROR);
        }

        if (tryEval && e != null) {
            try {
                if (e.inherits("try-error")/*e.isString() && e.asStrings().length > 0 && e.asString().toLowerCase().startsWith("error")*/) {
                    log(HEAD_EXCEPTION + e.asString() + "\n  " + expression, Level.WARNING);
                    return false;
                }
            } catch (Exception ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  " + expression, Level.ERROR);
                return false;
            }
        }
        return true;
    }

    @Override
    public Object silentlyRawEval(String expression, boolean tryEval) {
        if (R == null) {
            log(HEAD_EXCEPTION + "R environment not initialized.", Level.ERROR);
            return null;
        }
        if (expression == null) {
            return null;
        }
        if (expression.trim().length() == 0) {
            return null;
        }
        for (EvalListener b : eval) {
            b.eval(expression);
        }
        SEXP e = null;
        try {
            synchronized (R) {
                if (SINK_OUTPUT) {
                    R.eval("sink('" + SINK_FILE + "',type='output')");
                }
                if (SINK_MESSAGE) {
                    R.eval("sink('" + SINK_FILE + ".m',type='message')");
                }
                if (tryEval) {
                    e = (SEXP) (R.eval("try(eval(parse(text='" + expression.replace("'", "\\'") + "')),silent=FALSE)"));
                } else {
                    e = (SEXP) (R.eval(expression));
                }
                if (SINK_OUTPUT) {
                    R.eval("sink(type='output')");
                    try {
                        lastOuput = asString(R.eval("paste(collapse='\n',readLines('" + SINK_FILE + "'))"));
                        log(lastOuput, Level.OUTPUT);
                    } catch (Exception ex) {
                        lastOuput = ex.getMessage();
                        log(lastOuput, Level.WARNING);
                    }
                    R.eval("unlink('" + SINK_FILE + "')");
                }
                if (SINK_MESSAGE) {
                    R.eval("sink(type='message')");
                    try {
                        lastOuput = asString(R.eval("paste(collapse='\n',readLines('" + SINK_FILE + ".m'))"));
                        log(lastOuput, Level.INFO);
                    } catch (Exception ex) {
                        lastOuput = ex.getMessage();
                        log(lastOuput, Level.WARNING);
                    }
                    R.eval("unlink('" + SINK_FILE + ".m')");
                }
            }
        } catch (Exception ex) {
            log(HEAD_EXCEPTION + ex.getMessage() + "\n  " + expression, Level.ERROR);
        }

        if (tryEval && e != null) {
            try {
                if (e.inherits("try-error")/*e.isString() && e.asStrings().length > 0 && e.asString().toLowerCase().startsWith("error")*/) {
                    log(HEAD_EXCEPTION + e.asString() + "\n  " + expression, Level.WARNING);
                    e = null;
                }
            } catch (Exception ex) {
                log(HEAD_ERROR + ex.getMessage() + "\n  " + expression, Level.ERROR);
                return null;
            }
        }
        return e;
    }

    @Override
    public boolean set(String varname, double[][] data, String... names) {
        DoubleVector[] d = new DoubleVector[data[0].length];
        for (int i = 0; i < d.length; i++) {
            d[i] = new DoubleArrayVector(DoubleArray.getColumnCopy(data, i));
        }
        ListVector l = new ListVector(d);
        //l.setAttribute(Symbols.NAMES, new StringArrayVector(names)); 
        R.put(varname, l);
        //R.put("names("+varname+")",new StringArrayVector(names));
        R.put(varname + ".names", new StringArrayVector(names));
        try {
            R.eval("names(" + varname + ") <- " + varname + ".names");
            R.eval(varname + " <- data.frame(" + varname + ")");
        } catch (ScriptException ex) {
            ex.printStackTrace();
            return false;
        }
        return true;
    }

    @Override
    public boolean set(String varname, Object var) {
        if (var instanceof double[][]) {
            double[][] dd = (double[][]) var;
            double[] d = reshapeAsRow(dd);
            R.put(varname, d);
            try {
                R.eval(varname + " <- matrix(" + varname + ",nrow=" + dd.length + ")");
            } catch (ScriptException ex) {
                ex.printStackTrace();
                return false;
            }
        } else {
            R.put(varname, var);
        }
        return true;
    }

    // http://docs.renjin.org/en/latest/library/moving-data-between-java-and-r-code.html
    @Override
    public double asDouble(Object o) throws ClassCastException {
        if (o instanceof Double) {
            return (double) o;
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asDouble] Not an SEXP object: " + o);
        }
        try {
            return ((SEXP) o).asReal();
        } catch (Exception ex) {
            throw new ClassCastException("[asDouble] Cannot cast to double " + o);
        }
    }

    @Override
    public double[] asArray(Object o) throws ClassCastException {
        if (o instanceof double[]) {
            return (double[]) o;
        }
        if (o instanceof Double) {
            return new double[]{(double) o};
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asArray] Not an SEXP object: " + o);
        }
        if (!(o instanceof DoubleVector)) {
            throw new IllegalArgumentException("[asArray] Not a DoubleVector object: " + o);
        }
        try {
            return ((DoubleVector) o).toDoubleArray();
        } catch (Exception ex) {
            throw new ClassCastException("[asArray] Cannot cast to double[] " + o);
        }
    }

    @Override
    public double[][] asMatrix(Object o) throws ClassCastException {
        if (o instanceof double[][]) {
            return (double[][]) o;
        }
        if (o instanceof double[]) {
            return t(new double[][]{(double[]) o});
        }
        if (o instanceof Double) {
            return new double[][]{{(double) o}};
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asMatrix] Not an SEXP object: " + o);
        }
        if (!(o instanceof DoubleVector)) {
            throw new IllegalArgumentException("[asMatrix] Not a DoubleVector object: " + o);
        }
        try {
            Matrix m = new Matrix((DoubleVector) o);
            double[][] mm = new double[m.getNumRows()][m.getNumCols()];
            for (int i = 0; i < mm.length; i++) {
                for (int j = 0; j < mm[i].length; j++) {
                    mm[i][j] = m.getElementAsDouble(i, j);
                }
            }
            return mm;
        } catch (Exception ex) {
            throw new ClassCastException("[asMatrix] Cannot cast to double[][] " + o);
        }
    }

    @Override
    public String asString(Object o) throws ClassCastException {
        if (o instanceof String) {
            return (String) o;
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asString] Not an SEXP object: " + o);
        }
        try {
            return ((SEXP) o).asString();
        } catch (Exception ex) {
            throw new ClassCastException("[asString] Cannot cast to String " + o);
        }
    }

    @Override
    public String[] asStrings(Object o) throws ClassCastException {
        if (o instanceof String[]) {
            return (String[]) o;
        }
        if (o instanceof String) {
            return new String[]{(String) o};
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asStrings] Not an SEXP object: " + o);
        }
        if (!(o instanceof StringVector)) {
            throw new IllegalArgumentException("[asStrings] Not a StringVector object: " + o);
        }
        try {
            int n = ((SEXP) o).length();
            String[] s = new String[n];
            for (int i = 0; i < n; i++) {
                s[i] = ((SEXP) o).getElementAsSEXP(i).asString();
            }
            return s;
        } catch (Exception ex) {
            throw new ClassCastException("[asStrings] Cannot cast to String[] " + o);
        }
    }

    @Override
    public int asInteger(Object o) throws ClassCastException {
        return asIntegers(o)[0];
    }

    @Override
    public int[] asIntegers(Object o) throws ClassCastException {
        if (o instanceof int[]) {
            return (int[]) o;
        }
        if (o instanceof Integer) {
            return new int[]{(int) o};
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asIntegers] Not an SEXP object: " + o);
        }
        if (!(o instanceof IntVector)) {
            throw new IllegalArgumentException("[asIntegers] Not a IntVector object: " + o);
        }
        try {
            return ((IntVector) o).toIntArray();
        } catch (Exception ex) {
            throw new ClassCastException("[asIntegers] Cannot cast to int[] " + o);
        }
    }

    @Override
    public boolean asLogical(Object o) throws ClassCastException {
        if (o instanceof Boolean) {
            return (boolean) o;
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asLogical] Not an SEXP object: " + o);
        }
        try {
            return ((SEXP) o).asLogical() == Logical.TRUE;
        } catch (Exception ex) {
            throw new ClassCastException("[asLogical] Cannot cast to boolean " + o);
        }
    }

    @Override
    public boolean[] asLogicals(Object o) throws ClassCastException {
        if (o instanceof boolean[]) {
            return (boolean[]) o;
        }
        if (o instanceof Boolean) {
            return new boolean[]{(boolean) o};
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asLogicals] Not an SEXP object: " + o);
        }
        if (!(o instanceof LogicalVector)) {
            throw new IllegalArgumentException("[asLogicals] Not a LogicalVector object: " + o);
        }
        try {
            int n = ((SEXP) o).length();
            boolean[] s = new boolean[n];
            for (int i = 0; i < n; i++) {
                s[i] = ((SEXP) o).getElementAsSEXP(i).asLogical()==Logical.TRUE;
            }
            return s;
        } catch (Exception ex) {
            throw new ClassCastException("[asLogicals] Cannot cast to boolean[] " + o);
        }
    }

    @Override
    public Map asList(Object o) throws ClassCastException {
        if (o instanceof Map) {
            return (Map) o;
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[asList] Not an SEXP object: " + o);
        }
        if (!(o instanceof ListVector)) {
            throw new IllegalArgumentException("[asList] Not a ListVector object: " + o);
        }
        ListVector l = (ListVector) o;
        Map m = new HashMap<String, Object>();
        for (int i = 0; i < l.length(); i++) {
            m.put(l.getName(i), cast(l.get(i).getElementAsSEXP(i)));
        }
        return m;
    }

    @Override
    public boolean isNull(Object o) {
        if (o == null) {
            return true;
        }
        if (!(o instanceof SEXP)) {
            throw new IllegalArgumentException("[isNull] Not an SEXP object: " + o);
        }
        try {
            return o instanceof Null;
        } catch (Exception ex) {
            throw new ClassCastException("[isNull] Cannot cast to Null " + o);
        }
    }

    @Override
    public String toString(Object o) {
        if (o instanceof SEXP) {
            try {
                return ((SEXP) o).toString();
            } catch (Exception ex) {
                throw new ClassCastException("[toString] Cannot toString " + o);
            }
        } else if (o.getClass().isArray()) {
            return Arrays.asList(o).toString();
        } else {
            return o.toString();
        }
    }

    @Override
    public void getFile(File localfile, String remoteFile) {
        try {
            FileUtils.copyFile(new File(wdir, remoteFile), localfile);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void deleteFile(String remoteFile) {
        try {
            FileUtils.forceDelete(new File(wdir, remoteFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    @Override
    public void putFile(File localfile, String remoteFile) {
        try {
            FileUtils.copyFile(localfile, new File(wdir, remoteFile));
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    public Object cast(Object o) throws ClassCastException {
        if (o == null) {
            return null;
        }

        if (!(o instanceof SEXP)) {
            throw new ClassCastException("[cast] Not an SEXP object");
        }

        SEXP s = (SEXP) o;

        if (s.length() != 1) {
            switch (s.getTypeName()) {
                case "logical":
                    return asLogicals(s);
                case "integer":
                    return asIntegers(s);
                case "double":
                    if (s.getAttributes().get("dim").length() == 2) {
                        return asMatrix(s);
                    }
                    return asArray(s);
                case "character":
                    return asStrings(s);
                case "list":
                    return asList(s);
                case "NULL":
                    return null;
                default:
                    throw new ClassCastException("Cannot cast " + s + " (class " + s.getImplicitClass() + ", type " + s.getTypeName() + ")");
            }
        } else {
            switch (s.getTypeName()) {
                case "logical":
                    return asLogical(s);
                case "integer":
                    return asInteger(s);
                case "double":
                    return asDouble(s);
                case "character":
                    return asString(s);
                case "list":
                    return asList(s);
                case "closure":
                    String name = "function_" + (int) Math.floor(1000 * Math.random());
                    R.put(name, s);

                    try {
                        if (((SEXP) rawEval("is.function(" + name + ")")).asLogical() == TRUE) {
                            return new Function(name);
                        }
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                case "NULL":
                    return null;
                default:
                    throw new ClassCastException("Cannot cast " + s + " (class " + s.getImplicitClass() + ", type " + s.getTypeName() + ")");

            }
        }
        //throw new ClassCastException("Cannot cast to ? " + s + " (" + s.getTypeName() + ")");
    }

    //protected Context topLevelContext = Context.newTopLevelContext();
    @Override
    public void toGraphic(File f, int width, int height, String fileformat, String... commands) {
        throw new UnsupportedOperationException("Graphics not yet available using Renjin");

        /*BufferedImage image = new BufferedImage(width, height, ColorSpace.TYPE_RGB);

         Graphics2D g2d = (Graphics2D) image.getGraphics();
         g2d.setColor(Color.WHITE);
         g2d.setBackground(Color.WHITE);
         g2d.fill(g2d.getDeviceConfiguration().getBounds());

         AwtGraphicsDevice driver = new AwtGraphicsDevice(g2d);
         topLevelContext.getSingleton(GraphicsDevices.class).setActive(new org.renjin.graphics.GraphicsDevice(driver));

         try {
         StringWriter w = new StringWriter();
         PrintWriter p = R.getSession().getStdErr();
         R.getSession().setStdErr(new PrintWriter(w));

         for (String command : commands) {
         voidEval(command);
         }
         R.getSession().setStdErr(p);
         System.err.println(w.getBuffer());
         } finally {
         try {
         FileOutputStream fos = new FileOutputStream(f);

         ImageIO.write(image, fileformat.toUpperCase(), fos);
         fos.close();
         } catch (IOException ex) {
         ex.printStackTrace();
         }
         }*/
    }

    /**
     * Get R command text output
     *
     * @param command R command returning text
     * @return String
     */
    public String print(String command) {
        StringWriter w = new StringWriter();
        try {
            PrintWriter p = R.getSession().getStdOut();
            R.getSession().setStdOut(new PrintWriter(w));
            silentlyRawEval("print(" + command + ")");
            R.getSession().setStdOut(p);
            return w.toString();//.substring(l);
        } catch (Exception ex) {
            return ex.getMessage();
        }
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    public static void main(String[] args) throws Exception {
        //args = new String[]{"install.packages('lhs',repos='\"http://cran.irsn.fr/\"',lib='.')", "1+1"};
        if (args == null || args.length == 0) {
            args = new String[10];
            for (int i = 0; i < args.length; i++) {
                args[i] = Math.random() + "+pi";
            }
        }
        RenjinSession R = new RenjinSession(new RLogSlf4j(), null);

        for (int j = 0; j < args.length; j++) {
            System.out.print(args[j] + ": ");
            System.out.println(R.cast(R.rawEval(args[j])));
        }

        R.close();
    }
}
