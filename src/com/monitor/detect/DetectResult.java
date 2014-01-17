package com.monitor.detect;

public abstract class DetectResult {
    // error id, if no error happened set 0
    private int errorId;
    // description of error
    private String errorCode;

    public DetectResult(){
        this.errorId = DETECT_SUCCESS;
        this.errorCode = "";
    }

    public void setErrorId(int errorId){
        this.errorId = errorId;
    }

    public int getErrorId(){
        return this.errorId;
    }

    public void setErrorCode(String errorCode){
        this.errorCode = errorCode;
    }

    public String getErrorCode(){
        return this.errorCode;
    }

    public void setError(int errorId, String errorCode){
        setErrorId(errorId);
        setErrorCode(errorCode);
    }

    public static final int DETECT_SUCCESS = 0;

    // for dhcp detect
    public static final int DHCP_SEND_DISCOVER_FAILED   =   530001;
    public static final int DHCP_RECV_OFFER_TIMEOUT     =   53002;
    public static final int DHCP_RECV_OFFER_FAILED      =   53003;
    public static final int DHCP_SEND_REQUEST_FAILED    =   53004;
    public static final int DHCP_RECV_ACK_TIMEOUT       =   53005;
    public static final int DHCP_RECV_ACK_FAILED        =   53006;
    public static final int DHCP_RECV_NACK_RESPONCE     =   53007;
}
