package com.mskrzynski.voicecontrolpcserver;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.io.DataOutputStream;
import java.io.IOException;

class SendImage {
    SendImage(BufferedImage bufferedImage, DataOutputStream dos) {
        //zamiana otrzymanego obrazu na wersję ABGR zrozumiałą dla klienta
        BufferedImage image = new BufferedImage(bufferedImage.getWidth(), bufferedImage.getHeight(), BufferedImage.TYPE_4BYTE_ABGR);
        image.getGraphics().drawImage(bufferedImage, 0, 0, null);
        image.getGraphics().dispose();
        byte[] image_bytes = ((DataBufferByte) image.getData().getDataBuffer()).getData();

        //przesyłanie obrazu do klienta
        try {
            dos.writeInt(image.getWidth());
            dos.writeInt(image.getHeight());
            dos.write(image_bytes);
            dos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}


