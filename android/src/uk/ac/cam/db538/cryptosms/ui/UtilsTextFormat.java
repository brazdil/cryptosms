/*
 *   Copyright 2011 David Brazdil
 *
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 */
package uk.ac.cam.db538.cryptosms.ui;

import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormat;

public class UtilsTextFormat {
	
	/**
	 * Format date time.
	 *
	 * @param timeStamp the time stamp
	 * @return the string
	 */
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
