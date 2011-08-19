package uk.ac.cam.db538.cryptosms.ui;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class UtilsTextFormat {
	
	public static String formatDateTime(DateTime timeStamp) {
		DateTime now = DateTime.now();
		if (timeStamp.toLocalDate().equals(now.toLocalDate()))
			// today => just time
			return timeStamp.toString(DateTimeFormat.shortTime());				
		else
			// not today => just date
			return timeStamp.toString(DateTimeFormat.shortDate());
	}
}
