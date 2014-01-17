monitor
=======

monitor contains detect functions of different servers, such as dhcp, http, dns, ftp ......
(there is only one dhcp server detect)

What is it
=======

Detect, DetectParameter, DetectResult in  /src/com/monitor/detect/ are base classed for defined detector.
  Detect is the main process.
  DetectParameter is the parameter for Detect, may contain parameters for different server detect.
  DetectResult contains detect results that you want.
  
Classes in /src/com/monitor/detect/ are for DHCP detect. DHCPDetect class extends from Detect and is for main DHCP detect, using DatagramSocket to send request and receive response. DHCP detect does not request parameters, so there is no DHCPDetectParameter which extends from DetectParameter. DHCPDetectResult class extends from DetectResult.

How to use
=======

1. Using DHCP detect(example code):

    DHCPDetect dhcpDetect = new DHCPDetect();
    DHCPDetectResult dhcpDetectResult = (DHCPDetectResult) dhcpDetect.doDetect(null);
    
2. Add other servers detect function
