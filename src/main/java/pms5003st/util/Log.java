package pms5003st.util;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Log {

	private static SimpleDateFormat time 	= new SimpleDateFormat( "HH:mm:ss" );
	private static boolean printForceOnly 	= false;

	public static void force(String message) {
		printSTEInfo( Thread.currentThread().getStackTrace()[2], message );
	}

	public static void info(String message) {
		if (!printForceOnly) {
			printSTEInfo( Thread.currentThread().getStackTrace()[2], message );
		}
	}

	public static void warn(String message) {
		if (!printForceOnly) {
			printSTEInfo( Thread.currentThread().getStackTrace()[2], "[WARN] "+ message );
		}
	}
	
	public static void warn(Exception e) {
		printSTEInfo( Thread.currentThread().getStackTrace()[2], "[EXCP] "+ e.toString() );
	}

	private static void printSTEInfo(StackTraceElement ste, String message) {
		String className 		= ste.getClassName();
		className 	= className.substring( className.lastIndexOf(".") +1, className.length());

		StringBuilder sb = new StringBuilder();
		sb.append( String.format("[%-8s][%-15s][%-20s][%-3s] ", time.format(new Date()), className, ste.getMethodName(), ste.getLineNumber()) );
		sb.append(message);

		System.out.println( sb.toString() );
	}

	public static void setPrintForceOnly(boolean value) {
		printForceOnly 	= value;
	}

}
