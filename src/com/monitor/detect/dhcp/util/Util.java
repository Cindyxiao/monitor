/*
 *	This file is part of dhcp4java, a DHCP API for the Java language.
 *	(c) 2006 Stephan Hadinger
 *
 *	This library is free software; you can redistribute it and/or
 *	modify it under the terms of the GNU Lesser General Public
 *	License as published by the Free Software Foundation; either
 *	version 2.1 of the License, or (at your option) any later version.
 *
 *	This library is distributed in the hope that it will be useful,
 *	but WITHOUT ANY WARRANTY; without even the implied warranty of
 *	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *	Lesser General Public License for more details.
 *
 *	You should have received a copy of the GNU Lesser General Public
 *	License along with this library; if not, write to the Free Software
 *	Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301  USA
 */
package com.monitor.detect.dhcp.util;

import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * @author Stephan Hadinger
 * @version 1.00
 */
public final class Util {

    // Suppresses default constructor, ensuring non-instantiability.
	private Util() {
		throw new UnsupportedOperationException();
	}

    /**
     * Converts byte array to 32 bits int
     *
     * @param bytes byte array
     * @return 32 bits int
     */

    public static final int bytes2Int(byte[] bytes){
    	int len = bytes.length;
    	int intValue = 0;
    	for (int i=0; i<len; i++){
    		intValue = intValue<<8|(bytes[i]&0xFF);
    	}
    	return intValue;
    }

    /**
     * Converts byte array to string
     *
     * @param bytes byte array
     * @param separator
     * @return string, format: elem1(separator)elem2(separator)....
     */
    public static final String bytes2Str(byte[] bytes, String separator){
        StringBuilder stringBuilder = new StringBuilder();
        for (int index = 0; index < bytes.length; index++){
            stringBuilder.append(index==0?"":separator).append(((int)bytes[index]) & 0xFF);
        }

        return stringBuilder.toString();
    }
}
