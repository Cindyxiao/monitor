package com.monitor.detect.dhcp.util;

import org.apache.log4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

/**
 * get mac address according ip address
 */
public class MacAddress
{
    private final static Logger LOG = Logger.getLogger(MacAddress.class);

    /**
     * get local operating system name
     * @return
     */
    private static String getOsName() {
        String os = "";
        os = System.getProperty("os.name");
        return os;
    }

    /**
     * get local computer's mac address
     * The method is different between linux and windows
     * @return
     */
    public static byte[] getMACAddress() {
        String address = "";
        String os = getOsName();
        String separator = "";      // separator in mac address

        if (os.startsWith("Windows")) {     // for windows operating system
        	separator = "-";
            String command = "cmd.exe /c ipconfig /all";

            try {
                // run command in command line
                Process p = Runtime.getRuntime().exec(command);

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;

                // get mac address output line
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("Physical Address") > 0 || line.indexOf("物理地址") > 0) {
                        // analyse mac address line
                        int index = line.indexOf(":");
                        index += 2;
                        address = line.substring(index);
                        break;
                    }
                }
                br.close();
            } catch (IOException e) {
                LOG.error("Exception happened while execute command "+command, e);
            }
        } else if (os.startsWith("Linux")) {    // for linux operating system
        	separator = ":";
            String command = "/bin/sh -c ifconfig -a";

            try {
                // run command in command line
                Process p = Runtime.getRuntime().exec(command);

                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line;

                // get mac address output line
                while ((line = br.readLine()) != null) {
                    if (line.indexOf("HWaddr") > 0 || line.indexOf("物理地址") > 0 ) {
                        // analyse mac address line
                        int index = line.indexOf("HWaddr") + "HWaddr".length();
                        address = line.substring(index);
                        break;
                    }
                }
                br.close();
            } catch (IOException e) {
                LOG.error("Exception happened while execute command "+command, e);
            }
        }

        address = address.trim();
        LOG.info("mac address is "+address);

        String[] parts = address.split(separator);
        byte[] address_bytes = new byte[parts.length];
        for (int i=0; i<parts.length; i++){
        	address_bytes[i]=(byte)((short)Short.valueOf(parts[i],16));
        }

        return address_bytes;
    }
} 