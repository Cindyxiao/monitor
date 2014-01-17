package com.monitor.detect.dhcp;

import com.monitor.detect.Detect;
import com.monitor.detect.DetectParameter;
import com.monitor.detect.DetectResult;
import com.monitor.detect.dhcp.util.DHCPConstants;
import com.monitor.detect.dhcp.util.DHCPPacket;
import com.monitor.detect.dhcp.util.MacAddress;
import com.monitor.detect.dhcp.util.Util;
import org.apache.log4j.Logger;

import java.io.IOException;
import java.net.*;
import java.util.Random;

/**
 * process dhcp
 */
public class DHCPDetect implements Detect {
    private final static Logger LOG = Logger.getLogger(DHCPDetect.class);
    // broadcast package ip address
    private final static String BroadcastIp = "255.255.255.255";
    // mac address of local host
    private final static byte[] LocalMacAddress = MacAddress.getMACAddress();
    // dncp server port
    private final static int DHCPServerPort = 67;
    // dncp listen port
    private final static int DHCPListenPort = 68;
    // time out of dncp discover phase
    private final static int DiscoverTimeOut = 30000;	//ms
    // time out of dhcp response phase
    private final static int ResponseTimeOut = 30000;	//ms

    // ip address of local host
    private InetAddress localIpAddress;
    // transaction id in dhcp request and response package
    private int transactionID;
    // socket for dncp process
    private DatagramSocket socket;
    // client identifier, used in discover message
    private byte[] clientIdentifier;
    // server identifier, response in offer message and filled in request message
    private byte[] serverIdentifier;
    // dhcp detect result
    private DHCPDetectResult result;

    public DHCPDetect(){
        try {
            this.localIpAddress = InetAddress.getLocalHost();
        } catch (UnknownHostException e) {
            e.printStackTrace();
        }

        serverIdentifier = null;
        // fill client identifier: HTYPE MACAddress
        clientIdentifier = new byte[1+LocalMacAddress.length];
        clientIdentifier[0] = DHCPConstants.HTYPE_ETHER;
        for (int i=0; i<LocalMacAddress.length; i++)
            clientIdentifier[i+1] = LocalMacAddress[i];

    }

    /**
     * Process detect function and return process result
     *
     * @param parameter contains the parameters requested by detection (DHCP detect need none pramater)
     * @return process result
     */
    @Override
    public DetectResult doDetect(DetectParameter parameter) {
        LOG.info("do connect of " + getDetectName() + " start ...");

        result = new DHCPDetectResult();

        try {
            start();

            // dhcp discover process
            LOG.info("DHCP discover process...");
            dhcpDiscover();

            if (result.getErrorId() == DetectResult.DETECT_SUCCESS){    // discover success
                dhcpResponse();
                if (result.getErrorId() == DetectResult.DETECT_SUCCESS){    // response success
                    result.setTotalTime(result.getDiscoverTime()+result.getResponseTime());
                }
            }

            if (serverIdentifier != null){
                result.setServerHost(Util.bytes2Str(serverIdentifier, "."));
            }
        } catch (SocketException e) {
            LOG.error("Socket exception happened.", e);
        } finally {
            close();
        }

        LOG.info("do connect of " + getDetectName() + " end ...");
        return result;  //To change body of implemented methods use File | Settings | File Templates.
    }

    /**
     * create datagram socket
     * @throws SocketException
     */
    private void start() throws SocketException {
        socket = new DatagramSocket(DHCPListenPort);
    }

    /**
     * close socket
     */
    private void close(){
        if (socket != null)
            socket.close();
    }

    /**
     * do dncp discover phase, send DHCP Discover request, receive DHCP offer response and parse the message
     *
     */
    private void dhcpDiscover(){
        int discoverTime = -1;

        // create discover packet
        LOG.info("create discover packet");
        byte[] discoverMessage = createDiscoverMessage();
        DatagramPacket discoverPacket = null;
        try {
            discoverPacket = new DatagramPacket(discoverMessage,
                    discoverMessage.length,
                    InetAddress.getByName(BroadcastIp),
                    DHCPServerPort);
        } catch (UnknownHostException e) {
        }

        // send discover packet
        LOG.info("begin to send discover message");
        long startTime = System.currentTimeMillis();
        try {
            socket.send(discoverPacket);
            socket.setSoTimeout(DiscoverTimeOut);
        } catch (IOException e) {
            LOG.error("failed to send discover message", e);
            result.setDiscoverTime(discoverTime);
            result.setError(DetectResult.DHCP_SEND_DISCOVER_FAILED, "Failed to send discover message");

            return;
        }

        // wait offer response message
        LOG.info("wait for DHCP offer message");
        boolean gotMessage = false;
        byte[] offerMessage = new byte[512];
        DatagramPacket offerDatagramPacket = new DatagramPacket(offerMessage, offerMessage.length);
        try {
            while (!gotMessage){
                socket.receive(offerDatagramPacket);

                // parse received message
                LOG.info("received a message and parse it");
                // charge transition id
                byte[] byteOfXID = new byte[]{offerMessage[4],offerMessage[5], offerMessage[6],offerMessage[7] };
                int offerXid = Util.bytes2Int(byteOfXID);
                LOG.info("offerXid of the received message is " + offerXid + ", and transactionID is " + transactionID);

                if (offerXid == transactionID){
                    DHCPPacket offerDHCPPacket = DHCPPacket.getPacket(offerDatagramPacket);
                    short offerMessageType = offerDHCPPacket.getOption(DHCPConstants.DHO_DHCP_MESSAGE_TYPE).getValue()[0];

                    if (offerMessageType == DHCPConstants.DHCPOFFER){   // for offer message
                        result.setError(DetectResult.DETECT_SUCCESS, "");
                        gotMessage = true;

                        // get server identifier
                        serverIdentifier = offerDHCPPacket.getOption(DHCPConstants.DHO_DHCP_SERVER_IDENTIFIER).getValue();
                        LOG.info("server identifier of offer message is: "+Util.bytes2Str(serverIdentifier, "."));
                    } else {
                        LOG.error("the received message is not offer message, the message type is "+offerMessageType);
                    }
                }
            }

            discoverTime = (int)(System.currentTimeMillis() - startTime);
        } catch(SocketTimeoutException e){
            LOG.error("Time out exception happened while wait for DHCP Offer message", e);
            result.setError(DetectResult.DHCP_RECV_OFFER_TIMEOUT, "Wait for DHCP Offer message timeout");
        } catch (IOException e) {
            LOG.error("IO exception happened while wait for DHCP Offer message", e);
            result.setError(DetectResult.DHCP_RECV_OFFER_FAILED, "Failed to receive DHCP Offer message");
        }

        result.setDiscoverTime(discoverTime);
    }

    /**
     * do dncp response phase, send DHCP Request request, receive DHCP ACK response and parse the message
     *
     */
    private void dhcpResponse(){
        int responseTime = -1;

        // create request packet
        LOG.info("create request packet");
        byte[] requestMessage = createRequestMessage();
        DatagramPacket requestPacket = null;
        try {
            requestPacket = new DatagramPacket(requestMessage,
                    requestMessage.length,
                    InetAddress.getByName(BroadcastIp),
                    DHCPServerPort);
        } catch (UnknownHostException e) {
        }

        // send request packet
        LOG.info("begin to send request message");
        long startTime = System.currentTimeMillis();
        try {
            socket.send(requestPacket);
            socket.setSoTimeout(ResponseTimeOut);
        } catch (IOException e) {
            LOG.error("failed to send request message", e);
            result.setResponseTime(responseTime);
            result.setError(DetectResult.DHCP_SEND_REQUEST_FAILED, "Failed to send request message");

            return;
        }

        // wait offer response message
        LOG.info("wait for DHCP ack message");
        boolean gotMessage = false;
        byte[] ackMessage = new byte[512];
        DatagramPacket ackDatagramPacket = new DatagramPacket(ackMessage, ackMessage.length);
        try {
            while (!gotMessage){
                socket.receive(ackDatagramPacket);

                // parse received message
                LOG.info("received a message and parse it");
                // charge transition id
                byte[] byteOfXID = new byte[]{ackMessage[4],ackMessage[5], ackMessage[6],ackMessage[7] };
                int offerXid = Util.bytes2Int(byteOfXID);
                LOG.info("offerXid of the received message is " + offerXid + ", and transactionID is " + transactionID);

                if (offerXid == transactionID){
                    DHCPPacket ackDHCPPacket = DHCPPacket.getPacket(ackDatagramPacket);
                    short ackMessageType = ackDHCPPacket.getOption(DHCPConstants.DHO_DHCP_MESSAGE_TYPE).getValue()[0];

                    if (ackMessageType == DHCPConstants.DHCPACK){   // for ack message
                        result.setError(DetectResult.DETECT_SUCCESS, "");
                        gotMessage = true;
                    } else if (ackMessageType == DHCPConstants.DHCPNAK){    // for nack message
                        String errorMessage = ackDHCPPacket.getOptionAsString(DHCPConstants.DHO_DHCP_MESSAGE);

                        LOG.error("Receive dhcp nak message, "+errorMessage);
                        result.setError(DetectResult.DHCP_RECV_NACK_RESPONCE, "Receive dhcp nak message, "+errorMessage);
                        gotMessage = true;
                    } else {
                        LOG.error("the received message is not ack or nack message, the message type is "+ackMessageType);
                    }
                }
            }

            responseTime = (int)(System.currentTimeMillis() - startTime);
        } catch(SocketTimeoutException e){
            LOG.error("Time out exception happened while wait for DHCP Ack message", e);
            result.setError(DetectResult.DHCP_RECV_ACK_TIMEOUT, "Wait for DHCP ACK message timeout");
        } catch (IOException e) {
            LOG.error("IO exception happened while wait for DHCP ACK message", e);
            result.setError(DetectResult.DHCP_RECV_ACK_FAILED, "Failed to receive DHCP ACK message");
        }

        result.setResponseTime(responseTime);
    }

    /**
     * create dncp discover request message
     * @return dhcp discover packet
     */
    private byte[] createDiscoverMessage(){
        DHCPPacket discover = new DHCPPacket();
        // discover is request message
        discover.setOp(DHCPConstants.BOOTREQUEST);
        // hardware address type
        discover.setHtype(DHCPConstants.HTYPE_ETHER);
        // hardware address' length
        discover.setHlen((byte) 6);
        discover.setHops((byte) 0);
        discover.setXid((new Random()).nextInt());
        discover.setSecs((short) 0);
        // send discover using broadcast method
        discover.setFlags((short) 0x8000);
        discover.setCiaddr(localIpAddress);
        discover.setChaddr(LocalMacAddress);
        // set discover message's option field
        discover.setOptionAsByte(DHCPConstants.DHO_DHCP_MESSAGE_TYPE, DHCPConstants.DHCPDISCOVER);
        discover.setOptionRaw(DHCPConstants.DHO_DHCP_CLIENT_IDENTIFIER, clientIdentifier);

        // back up transaction identifier
        transactionID = discover.getXid();

        return discover.serialize();
    }

    /**
     * create dncp request request message
     * @return dhcp request packet
     */
    private byte[] createRequestMessage(){
        DHCPPacket request = new DHCPPacket();
        // request is request message
        request.setOp(DHCPConstants.BOOTREQUEST);
        // hardware address type
        request.setHtype(DHCPConstants.HTYPE_ETHER);
        // hardware address' length
        request.setHlen((byte) 6);
        request.setHops((byte) 0);
        request.setXid(transactionID);
        request.setSecs((short) 0);
        // send request using broadcast method
        request.setFlags((short) 0x8000);
        request.setCiaddr(localIpAddress);
        request.setChaddr(LocalMacAddress);
        // set request message's option field
        request.setOptionAsByte(DHCPConstants.DHO_DHCP_MESSAGE_TYPE, DHCPConstants.DHCPREQUEST);
        request.setOptionRaw(DHCPConstants.DHO_DHCP_CLIENT_IDENTIFIER, clientIdentifier);
        request.setOptionRaw(DHCPConstants.DHO_DHCP_SERVER_IDENTIFIER, serverIdentifier);

        return request.serialize();
    }

    /**
     * get detect name, used to indicate different detect process
     *
     * @return the name of detect
     */
    @Override
    public String getDetectName() {
        return this.getClass().getSimpleName();
    }
}
