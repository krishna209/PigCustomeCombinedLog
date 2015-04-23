package PigCombinedLog.PigApacheLog;

import java.io.IOException;
import java.net.URI;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.hadoop.fs.Path;
import org.apache.hadoop.mapreduce.Counter;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.pig.classification.InterfaceAudience;
import org.apache.pig.classification.InterfaceStability;
import org.apache.pig.LoadCaster;
import org.apache.pig.LoadPushDown.RequiredFieldList;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigHadoopLogger;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.builtin.Utf8StorageConverter;
import org.apache.pig.data.Tuple;
import org.apache.pig.impl.logicalLayer.FrontendException;
import org.apache.pig.impl.util.UDFContext;
import org.apache.pig.tools.pigstats.PigStatusReporter;
public abstract class LoadFunc {
	public String relativeToAbsolutePath(String location, Path curDir) 
            throws IOException {      
        return getAbsolutePath(location, curDir);
    }    

    /**
     * Communicate to the loader the location of the object(s) being loaded.  
     * The location string passed to the LoadFunc here is the return value of 
     * {@link LoadFunc#relativeToAbsolutePath(String, Path)}. Implementations
     * should use this method to communicate the location (and any other information)
     * to its underlying InputFormat through the Job object.
     * 
     * This method will be called in the frontend and backend multiple times. Implementations
     * should bear in mind that this method is called multiple times and should
     * ensure there are no inconsistent side effects due to the multiple calls.
     * 
     * @param location Location as returned by 
     * {@link LoadFunc#relativeToAbsolutePath(String, Path)}
     * @param job the {@link Job} object
     * store or retrieve earlier stored information from the {@link UDFContext}
     * @throws IOException if the location is not valid.
     */
    public abstract void setLocation(String location, Job job) throws IOException;
    
    /**
     * This will be called during planning on the front end. This is the
     * instance of InputFormat (rather than the class name) because the 
     * load function may need to instantiate the InputFormat in order 
     * to control how it is constructed.
     * @return the InputFormat associated with this loader.
     * @throws IOException if there is an exception during InputFormat 
     * construction
     */
    @SuppressWarnings("unchecked")
    public abstract InputFormat getInputFormat() throws IOException;

    /**
     * This will be called on the front end during planning and not on the back 
     * end during execution.
     * @return the {@link LoadCaster} associated with this loader. Returning null 
     * indicates that casts from byte array are not supported for this loader. 
     * construction
     * @throws IOException if there is an exception during LoadCaster 
     */
    public LoadCaster getLoadCaster() throws IOException {
        return new Utf8StorageConverter();
    }

    /**
     * Initializes LoadFunc for reading data.  This will be called during execution
     * before any calls to getNext.  The RecordReader needs to be passed here because
     * it has been instantiated for a particular InputSplit.
     * @param reader {@link RecordReader} to be used by this instance of the LoadFunc
     * @param split The input {@link PigSplit} to process
     * @throws IOException if there is an exception during initialization
     */
    @SuppressWarnings("unchecked")
    public abstract void prepareToRead(RecordReader reader, PigSplit split) throws IOException;

    /**
     * Retrieves the next tuple to be processed. Implementations should NOT reuse
     * tuple objects (or inner member objects) they return across calls and 
     * should return a different tuple object in each call.
     * @return the next tuple to be processed or null if there are no more tuples
     * to be processed.
     * @throws IOException if there is an exception while retrieving the next
     * tuple
     */
    public abstract Tuple getNext() throws IOException;

    //------------------------------------------------------------------------
    
    /**
     * Join multiple strings into a string delimited by the given delimiter.
     * 
     * @param s a collection of strings
     * @param delimiter the delimiter 
     * @return a 'delimiter' separated string
     */
    public static String join(AbstractCollection<String> s, String delimiter) {
        if (s.isEmpty()) return "";
        Iterator<String> iter = s.iterator();
        StringBuffer buffer = new StringBuffer(iter.next());
        while (iter.hasNext()) {
            buffer.append(delimiter);
            buffer.append(iter.next());
        }
        return buffer.toString();
    }

    /**
     * Parse comma separated path strings into a string array. This method 
     * escapes commas in the Hadoop glob pattern of the given paths. 
     * 
     * This method is borrowed from 
     * {@link org.apache.hadoop.mapreduce.lib.input.FileInputFormat}. A jira
     * (MAPREDUCE-1205) is opened to make the same name method there 
     * accessible. We'll use that method directly when the jira is fixed.
     * 
     * @param commaSeparatedPaths a comma separated string
     * @return an array of path strings
     */
    public static String[] getPathStrings(String commaSeparatedPaths) {
        int length = commaSeparatedPaths.length();
        int curlyOpen = 0;
        int pathStart = 0;
        boolean globPattern = false;
        List<String> pathStrings = new ArrayList<String>();

        for (int i=0; i<length; i++) {
            char ch = commaSeparatedPaths.charAt(i);
            switch(ch) {
                case '{' : {
                    curlyOpen++;
                    if (!globPattern) {
                        globPattern = true;
                    }
                    break;
                }
                case '}' : {
                    curlyOpen--;
                    if (curlyOpen == 0 && globPattern) {
                        globPattern = false;
                    }
                    break;
                }
                case ',' : {
                    if (!globPattern) {
                        pathStrings.add(commaSeparatedPaths.substring(pathStart, i));
                        pathStart = i + 1 ;
                    }
                    break;
                }
            }
        }
        pathStrings.add(commaSeparatedPaths.substring(pathStart, length));

        return pathStrings.toArray(new String[0]);
    }
    
    /**
     * Construct the absolute path from the file location and the current
     * directory. The current directory is either of the form 
     * {code}hdfs://<nodename>:<nodeport>/<directory>{code} in Hadoop 
     * MapReduce mode, or of the form 
     * {code}file:///<directory>{code} in Hadoop local mode.
     * 
     * @param location the location string specified in the load statement
     * @param curDir the current file system directory
     * @return the absolute path of file in the file system
     * @throws FrontendException if the scheme of the location is incompatible
     *         with the scheme of the file system
     */
    public static String getAbsolutePath(String location, Path curDir) 
            throws FrontendException {
        
        if (location == null || curDir == null) {
            throw new FrontendException(
                    "location: " + location + " curDir: " + curDir);
        }
    
        URI fsUri = curDir.toUri();
        String fsScheme = fsUri.getScheme();
        if (fsScheme == null) {
            throw new FrontendException("curDir: " + curDir);           
        }
        
        fsScheme = fsScheme.toLowerCase();
        String authority = fsUri.getAuthority();
        if(authority == null) {
            authority = "";
        }
        Path rootDir = new Path(fsScheme, authority, "/");
        
        ArrayList<String> pathStrings = new ArrayList<String>();
        
        String[] fnames = getPathStrings(location);
        for (String fname: fnames) {
            // remove leading/trailing whitespace(s)
            fname = fname.trim();
            Path p = new Path(fname);
            URI uri = p.toUri();
            // if the supplied location has a scheme (i.e. uri is absolute) or 
            // an absolute path, just use it.
            if(! (uri.isAbsolute() || p.isAbsolute())) {
                String scheme = uri.getScheme();
                if (scheme != null) {
                    scheme = scheme.toLowerCase();
                }
                
                if (scheme != null && !scheme.equals(fsScheme)) {
                    throw new FrontendException("Incompatible file URI scheme: "
                            + scheme + " : " + fsScheme);               
                }            
                String path = uri.getPath();
            
                fname = (p.isAbsolute()) ? 
                            new Path(rootDir, path).toString() : 
                                new Path(curDir, path).toString();
            }
            fname = fname.replaceFirst("^file:/([^/])", "file:///$1");
            // remove the trailing /
            fname = fname.replaceFirst("/$", "");
            pathStrings.add(fname);
        }
    
        return join(pathStrings, ",");
    }
    
    /**
     * This method will be called by Pig both in the front end and back end to
     * pass a unique signature to the {@link LoadFunc}. The signature can be used
     * to store into the {@link UDFContext} any information which the 
     * {@link LoadFunc} needs to store between various method invocations in the
     * front end and back end. A use case is to store {@link RequiredFieldList} 
     * passed to it in {@link LoadPushDown#pushProjection(RequiredFieldList)} for
     * use in the back end before returning tuples in {@link LoadFunc#getNext()}.
     * This method will be call before other methods in {@link LoadFunc}
     * @param signature a unique signature to identify this LoadFunc
     */
    public void setUDFContextSignature(String signature) {
        // default implementation is a no-op
    }
    
    /**
     * Issue a warning.  Warning messages are aggregated and reported to
     * the user.
     * @param msg String message of the warning
     * @param warningEnum type of warning
     */
    public final void warn(String msg, Enum warningEnum) {
        PigHadoopLogger.getInstance().warn(this, msg, warningEnum);
    }

    /**
     * Allow a LoadFunc to specify a list of files it would like placed in the distributed 
     * cache.
     * The default implementation returns null.
     * @return A list of files
     */
    public List<String> getCacheFiles() {
        return null;
    }

    /**
     * Allow a LoadFunc to specify a list of files located locally and would like to ship to backend 
     * (through distributed cache). Check for {@link FuncUtils} for utility function to facilitate it
     * The default implementation returns null.
     * @return A list of files
     */
    public List<String> getShipFiles() {
        return null;
    }

}
