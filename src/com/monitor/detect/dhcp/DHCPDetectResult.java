package com.monitor.detect.dhcp;

import com.monitor.detect.DetectResult;

public class DHCPDetectResult extends DetectResult {
    private String serverHost;
    //The time from "DHCP Discover" request to "DHCP Offer" response（ms）
    private int discoverTime;
    // from "DHCP Request" request to "DHCP ACK" response (ms)
    private int responseTime;
    // from "DHCP Discover" request to "DHCP ACK" response (ms)
    private int totalTime;

    public DHCPDetectResult(){
        super();
        this.serverHost = "";
        this.discoverTime = -1;
        this.responseTime = -1;
        this.totalTime = -1;
    }

    public void setServerHost(String serverHost) {
        this.serverHost = serverHost;
    }

    public String getServerHost(){
        return this.serverHost;
    }

    public void setDiscoverTime(int time) {
        this.discoverTime = time;
    }

    public int getDiscoverTime(){
        return this.discoverTime;
    }

    public void setResponseTime(int time) {
        this.responseTime = time;
    }

    public int getResponseTime(){
        return this.responseTime;
    }

    public void setTotalTime(int time) {
        this.totalTime = time;
    }

    public int getTotalTime(){
        return this.totalTime;
    }
}
