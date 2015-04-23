package PigCombinedLog.PigApacheLog;

import java.util.regex.Pattern;

public class CustomCombinedLog extends RegExLoader{
	
	private final static Pattern customCombinedLogPattern = Pattern.compile("^(\\S+)\\s+(\\S+)\\s+(\\S+?)\\s+.([A-Z,a-z,0-9,:,-]*).\\s+.([A-Z]*)\\s+([A-Z,/,a-z,.,\\s+,0-9]*).\\s+([0-9]*)\\s+([0-9]*)\\s+.([-]).\\s+.([A-Z,a-z,/,0-9,.]*)\\s+.([A-Z,0-9,a-z,;,\\s+,_,:,.,(,),/]*).");
	public Pattern getPattern()
	{
		return customCombinedLogPattern;
	}
}
