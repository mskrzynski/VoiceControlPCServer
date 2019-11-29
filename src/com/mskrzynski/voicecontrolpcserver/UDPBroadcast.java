package com.mskrzynski.voicecontrolpcserver;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

public class UDPBroadcast implements Runnable {

    @Override
    @SuppressWarnings("InfiniteLoopStatement")
    public void run() {
        try {
            //tworzenie socketu UDP
            DatagramSocket socketUDP = new DatagramSocket(8163);
            socketUDP.setBroadcast(true);

            while (true) {
                //odbieranie pakietu UDP od klienta
                //mały pakiet aby zminimalizować ryzyko jego utraty
                byte[] odebranyBufor = new byte[15];
                DatagramPacket pakiet = new DatagramPacket(odebranyBufor, odebranyBufor.length);
                socketUDP.receive(pakiet);

                System.out.println(getClass().getName() + "Odebrano pakiet: " + pakiet.getAddress().getHostAddress());
                System.out.println(getClass().getName() + "Wiadomość: " + new String(pakiet.getData()));

                //Sprawdzenie czy pakiet UDP od klienta jest właściwy
                String wiadomoscUDP = new String(pakiet.getData()).trim();
                if (wiadomoscUDP.equals("VC_REQUEST")) {
                    //Wysłanie odpowiedzi do klienta
                    byte[] sendData = "VC_RESPONSE".getBytes();
                    DatagramPacket sendPacket = new DatagramPacket(sendData, sendData.length, pakiet.getAddress(), pakiet.getPort());
                    socketUDP.send(sendPacket);
                    System.out.println(getClass().getName() + "Wysłano pakiet: " + sendPacket.getAddress().getHostAddress());
                }
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
    }
}
