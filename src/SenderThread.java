package com.company;
import java.net.DatagramPacket;
import java.net.DatagramSocket;

/** Bayram Muradov/21503664
 *  Waqar Ahmed/ 21503753
 *  25 december, 2019
 *  Computer Networks
 *  Programming Assignment 2
 */
public class SenderThread implements Runnable {

    private DatagramPacket packet;
    private DatagramSocket socket;
    private int timeout;

    public SenderThread(DatagramPacket packet, DatagramSocket socket, int timeout) {
        this.packet = packet;
        this.socket = socket;
        this.timeout = timeout;
    }

    public void run() {
        try {
            while (true) {
                if(Thread.interrupted()) {
                    throw new InterruptedException();
                }
                try {
                    this.socket.send(packet);
                    Thread.sleep(timeout);
                } catch (Exception e) {
                    return;
                }
            }
        } catch (InterruptedException e) {
            return;
        }
    }
}