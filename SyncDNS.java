
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;

public class SyncDNS
 {    
    static final int DEFAULT_PORT = 443;
    static final int TIMEOUT = 5 * 1000; // 5 seconds
    protected DataInputStream reply = null;
    protected PrintStream send = null;   
    protected SSLSocket sock = null;  
    public SyncDNS()
    {
    }
    private String ExtractNewIP(String nslookupResult)
    {
      String updatedIp = nslookupResult.substring(8);
      return updatedIp;
    }
    private boolean CompareIP(String oldIP, String newIP)
    {
      boolean ipMatch = false;
      oldIP = oldIP.trim();
      newIP= newIP.trim();
      if(oldIP.equals(newIP))
      {
         ipMatch = true;
      }
       return ipMatch;
    }
    public void CreateSocketConn(String hostName)throws UnknownHostException, IOException
    {    
      System.setProperty("networkAddress.cache.ttl","30");
      InetAddress inetAddr = InetAddress.getByName(hostName);
      SSLSocketFactory factory = (SSLSocketFactory)SSLSocketFactory.getDefault();
      sock = (SSLSocket) factory.createSocket(inetAddr, DEFAULT_PORT);
      if (sock != null)
      {
        reply = new DataInputStream(sock.getInputStream());
    	  send = new PrintStream(sock.getOutputStream());
        sock.setSoTimeout(TIMEOUT);
      }
       sock.startHandshake();
    }
    public String sendUpdateCmd(String httpReq) throws IOException 
    {
        String cmd ;
        send.println(httpReq);
        cmd = "Host: dynamicdns.park-your-domain.com";
        send.println(cmd);
        cmd = "Connection: keep-alive";
        send.println(cmd);
        send.println("");
        String res = reply.readLine();
        return res;
   }
   public boolean VerifyUpdate(String ip,String domainName)throws IOException 
   {
    Boolean updateStatus = false;
    ProcessBuilder builder ;
    Process process;  
    BufferedReader reader ;
    String line ;
    builder = new ProcessBuilder("nslookup", domainName);
    process= builder.start();
    reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
    line = reader.readLine();
    while (line != null ) 
    {
      if (line.contains("awesomesoftware.online"))
      {
        line = reader.readLine();
        String updatedIp = ExtractNewIP(line);
        updateStatus = CompareIP(ip, updatedIp);
      }
      line = reader.readLine();
    }
     return updateStatus;
   }
   public static void main (String[] args) throws IOException 
   {
      StringBuilder httpReq = new StringBuilder();
      String hostName = "dynamicdns.park-your-domain.com";
      String cmd = "GET /update?host=@&domain=";
      String httpSrvrVer= " HTTP/1.1";
      String domainName ="";
    
      Scanner scannInput = new Scanner(System.in);
      System.out.println("Enter domain name");
      domainName = scannInput.nextLine();
      System.out.println("Enter password");
      String passwd = scannInput.nextLine();
      System.out.println("Enter IP address");
      String ipAddr = scannInput.nextLine();
      httpReq.append(cmd).append(domainName).append("&password=").append(passwd).append("&ip=").append(ipAddr).append(httpSrvrVer);
      scannInput.close();
  
      SyncDNS dnsSrvr = new SyncDNS();
      try
      {
        dnsSrvr.CreateSocketConn(hostName);
        String srvrResponse = dnsSrvr.sendUpdateCmd(httpReq.toString());
        if (srvrResponse.indexOf("200") > 0 )
        {
            boolean done = false;
            while (!done)
            {
              System.out.println("..........Checking DNS for update...........");
              TimeUnit.SECONDS.sleep(5);
              if (dnsSrvr.VerifyUpdate(ipAddr, domainName))
              {
                done = true;
                System.out.println("DNS Update Successful");
              }
            }
        }
        else
        {
          System.out.println("------------------Server Response: " + srvrResponse);
        }
       
      }
      catch(Exception ee)
      {
        ee.printStackTrace(); 
      }
      dnsSrvr.sock.close();     
    }
}