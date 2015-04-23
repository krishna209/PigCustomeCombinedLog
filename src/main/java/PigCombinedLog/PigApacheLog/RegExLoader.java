package PigCombinedLog.PigApacheLog;

import java.io.IOException;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.InputFormat;
import org.apache.hadoop.mapreduce.Job;
import org.apache.hadoop.mapreduce.RecordReader;
import org.apache.hadoop.mapreduce.lib.input.FileInputFormat;
import org.apache.hadoop.mapreduce.lib.input.LineRecordReader;
import org.apache.hadoop.mapreduce.lib.input.TextInputFormat;
import org.apache.pig.LoadFunc;
import org.apache.pig.backend.hadoop.executionengine.mapReduceLayer.PigSplit;
import org.apache.pig.data.DataByteArray;
import org.apache.pig.data.DefaultTupleFactory;
import org.apache.pig.data.Tuple;
import org.apache.pig.data.TupleFactory;


public abstract class RegExLoader extends LoadFunc {

	  private LineRecordReader in = null;
	  abstract public Pattern getPattern();
	  @Override
	 public Tuple getNext() throws IOException {
	    Pattern pattern = getPattern();
	    Matcher matcher = pattern.matcher("");
	    TupleFactory mTupleFactory = DefaultTupleFactory.getInstance();
	   String line;
	   while (in.nextKeyValue()) {
	      Text val = in.getCurrentValue();
	      line = val.toString();
	      if (line.length() > 0 && line.charAt(line.length() - 1) == '\r') {
	        line = line.substring(0, line.length() - 1);
	      }
	   matcher = matcher.reset(line);
	      ArrayList list = new ArrayList();
	      if (matcher.find()) {
	        for (int i = 1; i <= matcher.groupCount(); i++) {
	          list.add(new DataByteArray(matcher.group(i)));
	        }
	        return mTupleFactory.newTuple(list);  
	      }
	    }
	    return null;
	  }
	  @SuppressWarnings("unchecked")
	  public InputFormat getInputFormat() throws IOException {
	      return new TextInputFormat();
	  }
	 @SuppressWarnings("unchecked")
	  @Override
	  public void prepareToRead(RecordReader reader, PigSplit split)
	         throws IOException {
	      in = (LineRecordReader) reader;
	  }
	  @Override
	 public void setLocation(String location, Job job) throws IOException {
	      FileInputFormat.setInputPaths(job, location);      
	  }

}
