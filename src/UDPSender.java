package com.company;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.HashMap;

/** Bayram Muradov/21503664
 *  Waqar Ahmed/ 21503753
 *  25 december, 2019
 *  Computer Networks
 *  Programming Assignment 2
 */

public class UDPSender {
    //properties
    String filename;
    private static final int PAYLOAD = 1022; // Maximum payload of 1022 bytes
    private static final int HEADER_SIZE = 2;    // Header size of 2 bytes
    private DatagramSocket senderSocket;
    private InetAddress address;
    private int port;
    private int timeout;
    private int winSize;
    ArrayList<DatagramPacket> datagrams;

    //constructors
    public UDPSender(String hostname, int port, String filename, int winSize, int timeout) throws IOException {
        this.address = InetAddress.getByName(hostname);
        this.port = port;
        this.filename = filename;
        this.timeout=timeout;
        this.winSize=winSize;
        datagrams=new ArrayList<>();
    }

    //methods
    public void start() throws SocketException {
        this.senderSocket = new DatagramSocket();
        System.out.println(
                "socket running on " +
                        this.senderSocket.getLocalAddress().toString() + ":" +
                        this.senderSocket.getLocalPort() + "."
        );
    }

    //divides the file into packets, puts them in an ArrayList
    public void populatePackets() throws IOException {
        File file = new File(this.filename);
        FileInputStream fileStream = new FileInputStream(file);
        byte[] fileBuff = new byte[(int) file.length()];
        fileStream.read(fileBuff);
        int sequenceNum = 1;
        boolean endOfFile = false;
        for (int i = 0; i < fileBuff.length; i += PAYLOAD, ++sequenceNum) {
            byte[] message = new byte[PAYLOAD + HEADER_SIZE]; // Initialize message
            message[0] = (byte) (sequenceNum >> 8);           // Set to 1st 8 bits of sequence number
            message[1] = (byte) (sequenceNum);                // Set to 2nd 8 bits of sequence number
            if ((i + PAYLOAD) >= fileBuff.length) {
                endOfFile = true;
            } else {
                endOfFile = false;
            }
            if (endOfFile) {
                for (int j = 0; j < fileBuff.length - i; ++j)
                    message[j + 2] = fileBuff[i + j];
            } else {
                for (int j = 0; j < PAYLOAD; ++j)
                    message[j + 2] = fileBuff[i + j];
            }
            DatagramPacket dataPacket = new DatagramPacket(message, message.length, this.address, this.port);
            datagrams.add(dataPacket);
        }
        fileStream.close();
    }

    //sends datagram packets
    public void sendPackets() throws IOException {
        //fill the array
        populatePackets();
        //System.out.println(datagrams.size());
        int sendBase=winSize;
        int recBase=0;
        HashMap<Integer, Thread> table=new HashMap<>();
        int ackTracer=0;
        int ackNum=0;
        int [] winTrack = new int[winSize];
        while(true) {
            //THREAD CREATOR
            //creates senders
            if(allAcked(winTrack) || allZero(winTrack)) {
                //System.out.println("inside loop");
                for (int i = recBase; i < sendBase; i++) {
                    //start the sender threads
                    DatagramPacket curr = datagrams.get(i);
                    //create THREAD FOR each writer
                    int currSeqNum = byteToInt(curr.getData()[0], curr.getData()[1]);
                    SenderThread senderThread = new SenderThread(datagrams.get(i), senderSocket, timeout);
                    Thread t = new Thread(senderThread);
                    table.put(currSeqNum, t);
                    t.start();
                }
                //blocking thread creator
                for(int i=0; i<winSize; i++) {
                    winTrack[i]=-1;
                }
            }

            //MAIN THREAD
            //checks ack window, unblocks the thread creator
            byte[] ackData = new byte[HEADER_SIZE];
            DatagramPacket recPacket = new DatagramPacket(ackData, ackData.length);
            senderSocket.receive(recPacket);
            ackNum=byteToInt(recPacket.getData()[0], recPacket.getData()[1]);
            winTrack[(ackNum-1)%winSize]=1;
            if (inRange(ackNum, recBase, sendBase)) {
                if (table.containsKey(ackNum)) {
                    ackTracer++;
                    //System.out.println("** got ack for "+ ackNum+ " **");
                    table.get(ackNum).interrupt();
                    table.remove(ackNum);
                }
            }
            //if all acks in win. received, update the window
            if(allAcked(winTrack)) {
                recBase=recBase+winSize;
                sendBase=sendBase+winSize;
                if(sendBase>=(datagrams.size()-1)) {
                    sendBase=datagrams.size()-1;
                }
            }

            //all acks received, break
            if(ackTracer==(datagrams.size()-1)) {
                break;
            }
        }
            //send termination sequence
            int endSeq=0;
            byte[] message = new byte[PAYLOAD + HEADER_SIZE];
            message[0] = (byte) (endSeq >> 8);           // Set to 1st 8 bits of sequence number
            message[1] = (byte) (endSeq);
            DatagramPacket endPacket = new DatagramPacket(message, message.length, this.address, this.port);
            this.senderSocket.send(endPacket);
            System.out.println("**sent termination sequence**");
            this.senderSocket.close();
            System.out.println(this.filename + " successfully sent to " + this.address + ":" + this.port);
    }

    //converts bytes to int
    public static int byteToInt(byte b1, byte b2) {
        return  ((b1 << 8) | (b2 & 0xFF));
    }

    public boolean allAcked(int [] arr) {
        for(int i=0; i<winSize; i++) {
            if(arr[i]!=1) {
                return false;
            }
        }
        return true;
    }

    public boolean allZero(int [] arr) {
        for(int i=0; i<winSize; i++) {
            if(arr[i]!=0) {
                return false;
            }
        }
        return true;
    }


    //gets min. of the array
    public static int getMinValue(int[] numbers){
        int minValue = numbers[0];
        for(int i=1;i<numbers.length;i++){
            if(numbers[i] < minValue){
                minValue = numbers[i];
            }
        }
        return minValue;
    }

    //checks if the number is in range
    public static boolean inRange(int i, int minValue, int maxValue) {
        if (i >= minValue && i <= maxValue)
            return true;
        else
            return false;
    }


}
