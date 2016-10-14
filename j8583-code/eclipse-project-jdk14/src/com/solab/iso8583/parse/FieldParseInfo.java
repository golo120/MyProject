/*
j8583 A Java implementation of the ISO8583 protocol
Copyright (C) 2007 Enrique Zamudio Lopez

This library is free software; you can redistribute it and/or
modify it under the terms of the GNU Lesser General Public
License as published by the Free Software Foundation; either
version 2.1 of the License, or (at your option) any later version.

This library is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
Lesser General Public License for more details.

You should have received a copy of the GNU Lesser General Public
License along with this library; if not, write to the Free Software
Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA
*/
package com.solab.iso8583.parse;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.text.ParseException;
import java.util.Calendar;
import java.util.Date;

import com.solab.iso8583.CustomField;
import com.solab.iso8583.IsoType;
import com.solab.iso8583.IsoValue;

/** This class contains the information needed to parse a field from a message buffer.
 * 
 * @author Enrique Zamudio
 */
public class FieldParseInfo {

	private IsoType type;
	private int length;

	/** Creates a new instance that parses a value of the specified type, with the specified length.
	 * The length is only useful for ALPHA and NUMERIC types.
	 * @param t The ISO type to be parsed.
	 * @param len The length of the data to be read (useful only for ALPHA and NUMERIC types). */
	public FieldParseInfo(IsoType t, int len) {
		if (t == null) {
			throw new IllegalArgumentException("IsoType cannot be null");
		}
		type = t;
		length = len;
	}

	/** Returns the specified length for the data to be parsed. */
	public int getLength() {
		return length;
	}

	/** Returns the data type for the data to be parsed. */
	public IsoType getType() {
		return type;
	}

	/** Parses the character data from the buffer and returns the
	 * IsoValue with the correct data type in it. */
	public IsoValue parse(byte[] buf, int pos, CustomField custom) throws ParseException {
		if (type == IsoType.NUMERIC || type == IsoType.ALPHA) {
			if (custom == null) {
				return new IsoValue(type, new String(buf, pos, length), length, null);
			} else {
				IsoValue v = new IsoValue(type, custom.decodeField(new String(buf, pos, length)), length, custom);
				if (v.getValue() == null) {
					v = new IsoValue(type, new String(buf, pos, length), length, null);
				}
				return v;
			}
		} else if (type == IsoType.LLVAR) {
			length = ((buf[pos] - 48) * 10) + (buf[pos + 1] - 48);
			if (custom == null) {
				return new IsoValue(type, new String(buf, pos + 2, length), null);
			} else {
				IsoValue v = new IsoValue(type, custom.decodeField(new String(buf, pos + 2, length)), custom);
				if (v.getValue() == null) {
					v = new IsoValue(type, new String(buf, pos + 2, length), null);
				}
				return v;
			}
		} else if (type == IsoType.LLLVAR) {
			length = ((buf[pos] - 48) * 100) + ((buf[pos + 1] - 48) * 10) + (buf[pos + 2] - 48);
			if (custom == null) {
				return new IsoValue(type, new String(buf, pos + 3, length), null);
			} else {
				IsoValue v = new IsoValue(type, custom.decodeField(new String(buf, pos + 3, length)), custom);
				if (v.getValue() == null) {
					//problems decoding? return the string
					v = new IsoValue(type, new String(buf, pos + 3, length), null);
				}
				return v;
			}
		} else if (type == IsoType.AMOUNT) {
			byte[] c = new byte[13];
			System.arraycopy(buf, pos, c, 0, 10);
			System.arraycopy(buf, pos + 10, c, 11, 2);
			c[10] = '.';
			return new IsoValue(type, new BigDecimal(new String(c)), null);
		} else if (type == IsoType.DATE10) {
			//A SimpleDateFormat in the case of dates won't help because of the missing data
			//we have to use the current date for reference and change what comes in the buffer
			Calendar cal = Calendar.getInstance();
			//Set the month in the date
			cal.set(Calendar.MONTH, ((buf[pos] - 48) * 10) + buf[pos + 1] - 49);
			cal.set(Calendar.DATE, ((buf[pos + 2] - 48) * 10) + buf[pos + 3] - 48);
			cal.set(Calendar.HOUR_OF_DAY, ((buf[pos + 4] - 48) * 10) + buf[pos + 5] - 48);
			cal.set(Calendar.MINUTE, ((buf[pos + 6] - 48) * 10) + buf[pos + 7] - 48);
			cal.set(Calendar.SECOND, ((buf[pos + 8] - 48) * 10) + buf[pos + 9] - 48);
			if (cal.getTime().after(new Date())) {
				cal.add(Calendar.YEAR, -1);
			}
			return new IsoValue(type, cal.getTime(), null);
		} else if (type == IsoType.DATE4) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			//Set the month in the date
			cal.set(Calendar.MONTH, ((buf[pos] - 48) * 10) + buf[pos + 1] - 49);
			cal.set(Calendar.DATE, ((buf[pos + 2] - 48) * 10) + buf[pos + 3] - 48);
			if (cal.getTime().after(new Date())) {
				cal.add(Calendar.YEAR, -1);
			}
			return new IsoValue(type, cal.getTime(), null);
		} else if (type == IsoType.DATE_EXP) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR, 0);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
			cal.set(Calendar.DATE, 1);
			//Set the month in the date
			cal.set(Calendar.YEAR, cal.get(Calendar.YEAR) - (cal.get(Calendar.YEAR) % 100)
					+ ((buf[pos] - 48) * 10) + buf[pos + 1] - 48);
			cal.set(Calendar.MONTH, ((buf[pos + 2] - 48) * 10) + buf[pos + 3] - 49);
			return new IsoValue(type, cal.getTime(), null);
		} else if (type == IsoType.TIME) {
			Calendar cal = Calendar.getInstance();
			cal.set(Calendar.HOUR_OF_DAY, ((buf[pos] - 48) * 10) + buf[pos + 1] - 48);
			cal.set(Calendar.MINUTE, ((buf[pos + 2] - 48) * 10) + buf[pos + 3] - 48);
			cal.set(Calendar.SECOND, ((buf[pos + 4] - 48) * 10) + buf[pos + 5] - 48);
			return new IsoValue(type, cal.getTime(), null);
		}
		return null;
	}

	/** Parses binary data from the buffer, creating and returning an IsoValue of the configured
	 * type and length. */
	public IsoValue parseBinary(byte[] buf, int pos, CustomField custom) throws ParseException {
		if (type == IsoType.ALPHA) {

			if (custom == null) {
				return new IsoValue(type, new String(buf, pos, length), length, null);
			} else {
				IsoValue v = new IsoValue(type, custom.decodeField(new String(buf, pos, length)), length, custom);
				if (v.getValue() == null) {
					v = new IsoValue(type, new String(buf, pos, length), length, null);
				}
				return v;
			}

		} else if (type == IsoType.NUMERIC) {

			//A long covers up to 18 digits
			if (length < 19) {
				long l = 0;
				long power = 1L;
				for (int i = pos + (length / 2) + (length % 2) - 1; i >= pos; i--) {
					l += (buf[i] & 0x0f) * power;
					power *= 10L;
					l += ((buf[i] & 0xf0) >> 4) * power;
					power *= 10L;
				}
				return new IsoValue(IsoType.NUMERIC, new Long(l), length, null);
			} else {
				//Use a BigInteger
				char[] digits = new char[length];
				int start = 0;
				for (int i = pos; i < pos + (length / 2) + (length % 2); i++) {
					digits[start++] = (char)(((buf[i] & 0xf0) >> 4) + 48);
					digits[start++] = (char)((buf[i] & 0x0f) + 48);
				}
				return new IsoValue(IsoType.NUMERIC, new BigInteger(new String(digits)), length, null);
			}

		} else if (type == IsoType.LLVAR) {

			length = (((buf[pos] & 0xf0) >> 4) * 10) + (buf[pos] & 0x0f);
			if (custom == null) {
				return new IsoValue(type, new String(buf, pos + 1, length), null);
			} else {
				IsoValue v = new IsoValue(type, custom.decodeField(new String(buf, pos + 1, length)), custom);
				if (v.getValue() == null) {
					v = new IsoValue(type, new String(buf, pos + 1, length), null);
				}
				return v;
			}

		} else if (type == IsoType.LLLVAR) {

			length = ((buf[pos] & 0x0f) * 100) + (((buf[pos + 1] & 0xf0) >> 4) * 10) + (buf[pos + 1] & 0x0f);
			if (custom == null) {
				return new IsoValue(type, new String(buf, pos + 2, length), null);
			} else {
				IsoValue v = new IsoValue(type, custom.decodeField(new String(buf, pos + 2, length)), custom);
				if (v.getValue() == null) {
					v = new IsoValue(type, new String(buf, pos + 2, length), null);
				}
				return v;
			}

		} else if (type == IsoType.AMOUNT) {

			char[] digits = new char[13];
			digits[10] = '.';
			int start = 0;
			for (int i = pos; i < pos + 6; i++) {
				digits[start++] = (char)(((buf[i] & 0xf0) >> 4) + 48);
				digits[start++] = (char)((buf[i] & 0x0f) + 48);
				if (start == 10) {
					start++;
				}
			}
			return new IsoValue(IsoType.AMOUNT, new BigDecimal(new String(digits)), null);
		} else if (type == IsoType.DATE10 || type == IsoType.DATE4 || type == IsoType.DATE_EXP
				|| type == IsoType.TIME) {

			int[] tens = new int[(type.getLength() / 2) + (type.getLength() % 2)];
			int start = 0;
			for (int i = pos; i < pos + tens.length; i++) {
				tens[start++] = (((buf[i] & 0xf0) >> 4) * 10) + (buf[i] & 0x0f);
			}
			Calendar cal = Calendar.getInstance();
			if (type == IsoType.DATE10) {
				//A SimpleDateFormat in the case of dates won't help because of the missing data
				//we have to use the current date for reference and change what comes in the buffer
				//Set the month in the date
				cal.set(Calendar.MONTH, tens[0] - 1);
				cal.set(Calendar.DATE, tens[1]);
				cal.set(Calendar.HOUR_OF_DAY, tens[2]);
				cal.set(Calendar.MINUTE, tens[3]);
				cal.set(Calendar.SECOND, tens[4]);
				if (cal.getTime().after(new Date())) {
					cal.add(Calendar.YEAR, -1);
				}
				return new IsoValue(type, cal.getTime(), null);
			} else if (type == IsoType.DATE4) {
				cal.set(Calendar.HOUR, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				//Set the month in the date
				cal.set(Calendar.MONTH, tens[0] - 1);
				cal.set(Calendar.DATE, tens[1]);
				if (cal.getTime().after(new Date())) {
					cal.add(Calendar.YEAR, -1);
				}
				return new IsoValue(type, cal.getTime(), null);
			} else if (type == IsoType.DATE_EXP) {
				cal.set(Calendar.HOUR, 0);
				cal.set(Calendar.MINUTE, 0);
				cal.set(Calendar.SECOND, 0);
				cal.set(Calendar.DATE, 1);
				//Set the month in the date
				cal.set(Calendar.YEAR, cal.get(Calendar.YEAR)
						- (cal.get(Calendar.YEAR) % 100) + tens[0]);
				cal.set(Calendar.MONTH, tens[1] - 1);
				return new IsoValue(type, cal.getTime(), null);
			} else if (type == IsoType.TIME) {
				cal.set(Calendar.HOUR_OF_DAY, tens[0]);
				cal.set(Calendar.MINUTE, tens[1]);
				cal.set(Calendar.SECOND, tens[2]);
				return new IsoValue(type, cal.getTime(), null);
			}
			return new IsoValue(type, cal.getTime(), null);
		}
		return null;
	}

}
