package com.dantsu.escposprinter.connection.usb;

import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbDeviceConnection;
import android.hardware.usb.UsbEndpoint;
import android.hardware.usb.UsbInterface;
import android.hardware.usb.UsbManager;
import android.hardware.usb.UsbRequest;
import android.os.Build;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.concurrent.TimeoutException;

public class UsbInputStream extends InputStream {
    private UsbDeviceConnection usbConnection;
    private UsbInterface usbInterface;
    private UsbEndpoint usbEndpoint;


    public UsbInputStream(UsbManager usbManager, UsbDevice usbDevice) throws IOException {

        this.usbInterface = UsbDeviceHelper.findPrinterInterface(usbDevice);
        if (this.usbInterface == null) {
            throw new IOException("Unable to find USB interface.");
        }

        this.usbEndpoint = UsbDeviceHelper.findEndpointOut(this.usbInterface);
        if (this.usbEndpoint == null) {
            throw new IOException("Unable to find USB endpoint.");
        }

        this.usbConnection = usbManager.openDevice(usbDevice);
        if (this.usbConnection == null) {
            throw new IOException("Unable to open USB connection.");
        }
    }


    @Override
    public int read(byte[] b) throws IOException {
        return read(b, 0, b.length);
    }

    @Override
    public int read(byte[] bytes, int off, int len) throws IOException {
        if (this.usbInterface == null || this.usbEndpoint == null || this.usbConnection == null) {
            throw new IOException("Unable to connect to USB device.");
        }

        if (!this.usbConnection.claimInterface(this.usbInterface, true)) {
            throw new IOException("Error during claim USB interface.");
        }

        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        UsbRequest usbRequest = new UsbRequest();
        try {
            usbRequest.initialize(this.usbConnection, this.usbEndpoint);
            if (!usbRequest.queue(buffer, bytes.length)) {
                throw new IOException("Error queueing USB request.");
            }
            try {
                this.usbConnection.requestWait(16);
            } catch (TimeoutException e) {
                return 0;
            }
        } finally {
            usbRequest.close();
        }
        return buffer.capacity();
    }

    @Override
    public int read() throws IOException {
        byte b[] = new byte[1];
        read(b);
        return (int) b[0];
    }

    @Override
    public void close() throws IOException {
//        if (this.usbConnection != null) {
//            this.usbConnection.close();
//            this.usbInterface = null;
//            this.usbEndpoint = null;
//            this.usbConnection = null;
//        }
    }
}
