package com.example.project_prototype.obd;

import com.example.project_prototype.exception.ConnectionException;
import com.example.project_prototype.exception.OBDTimeoutException;

import java.io.*;

import java.net.Socket;
import java.net.SocketTimeoutException;

public class OBDConnector {
  // Variables----------------------//
  private final String ip;
  private  final int port;
  private Socket socket;
  private PrintWriter writer;
  private BufferedReader reader;
  //--------------------------------//

  //hard coded ip and port//
    OBDConnector(String ip , int port){
            this.ip = ip ;
            this.port = port;
    }
    //---------------------//

  // Connection with Harrier//
    public void connect() throws ConnectionException {
      try {
        socket = new Socket(this.ip, this.port);
        socket.setSoTimeout(5000);

        writer = new PrintWriter(socket.getOutputStream(),true);
        reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));

        // ELM327 initialisation sequence — must be in this order
        sendRaw("ATZ");    // reset the chip, clears everything
        sendRaw("ATE0");   // turn echo OFF — cleaner responses
        sendRaw("ATL0");   // turn line feeds OFF
        sendRaw("ATS0");   // turn spaces OFF in responses
        sendRaw("ATSP0"); // auto-detect car protocol (CAN for Harrier)


      }catch (IOException e){
        throw new ConnectionException ("Cannot connect to ELM327 at " + ip + ":" + port +
                "\nIs your Mac connected to the V-LINK WiFi? " + e.getMessage());

      }

    }
    //----------------------------------------------------------------------------------//


  // send command/ Sends one OBD PID and returns the raw hex response string//
  public String sendCommand(String command) throws OBDTimeoutException {

      try {
        writer.print(command + "\r");
        writer.flush();

        // Read response character by character until '>' prompt
        // '>' means ELM327 is ready for next command (end of response)//
        StringBuilder response = new StringBuilder();
        int ch ;
        while((ch =reader.read()) != -1) {
          if ((char) ch == '>') break;
          response.append((char)ch);
        }
     // Clean up the raw response before returning
        return response.toString()
                .replace("\r","")     //remove carriage returns;
                .replace("\n", "")   // remove new lines;
                .trim();                              //remove trailing lines;

      }catch (SocketTimeoutException e){
           throw new OBDTimeoutException("ELM327 did not respond to: "+ command +
                   " within 5 seconds. Is the Engine running? " , command);
      }catch (IOException e){
          throw new OBDTimeoutException("Communication error: " + e.getMessage() , command);
      }
  }
  //--------------------------------------------------------------------------------------//

  // ── isConnected() ────────────────────────────────────────
  // Check if socket is alive before sending commands
  public  boolean isConnected(){
      return socket !=null && socket.isConnected() && !socket.isClosed();
  }
//-----------------------------------------------------------------------------------------//
  public void disconnect() {
    try {
          if (writer !=null) writer.close();
          if (reader !=null) reader.close();
          if (socket !=null) socket.close();
    }catch (IOException e){
      System.err.println("Error closing OBD connection: " + e.getMessage());
    }

  }

  // ── sendRaw() ────────────────────────────────────────────
  // Private helper — used only during initialisation
  // Same as sendCommand but swallows exceptions (init failures are non-critical
  private void sendRaw(String command ){
      try {
        sendCommand(command);
        Thread.sleep(100);  // small pause between init commands;

      }catch (Exception e){
        System.err.println("Init command failed: " + command);
      }
  }

}
