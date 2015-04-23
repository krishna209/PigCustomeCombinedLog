package PigCombinedLog.PigApacheLog;

import java.util.regex.Pattern;

public class CustomCombinedLog extends RegExLoader{
	
	private final static Pattern customCombinedLogPattern = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+?)\\s+.(\\S+?)\\s+.(\\S+)\\s+(\\S+)\\s+(\\S+?)\\s+(\\S+)\\s+([0-9]*)\\s+.(\\S+?).\\s+\"([A-Z,a-z,0-9,/,.,(,),\\s+,;,_,:]*)\"");
	public Pattern getPattern()
	{
		return customCombinedLogPattern;
	}
}
