package com.company;
/** Bayram Muradov/21503664
 *  Waqar Ahmed/ 21503753
 *  25 december, 2019
 *  Computer Networks
 *  Programming Assignment 2
 */
public class Main {

    public static void main(String[] args) {

        String filename=args[0];
        int port=Integer.parseInt(args[1]);
        int windowSize= Integer.parseInt(args[2]);
        int timeout=Integer.parseInt(args[3]);
        String hostname="127.0.0.1";

        try {
            UDPSender sender = new UDPSender(hostname, port, filename, windowSize, timeout);
            sender.start();
            sender.sendPackets();
        }
        catch (Exception e) {
            e.printStackTrace();
        }

    }
}
