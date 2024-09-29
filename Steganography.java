package Steganography;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import javax.imageio.ImageIO;
import java.util.Arrays;
import java.util.Random;


public class Steganography {
    public static void main(String[] args) {
        // embedding 
        // try {
        //     Embedding embedding = new Embedding();

        //     // input qrcode image and picture image
        //     BufferedImage qrcode = ImageIO.read(new File("qrcode.png"));
        //     BufferedImage picture = ImageIO.read(new File("picture.png"));

        //     // print qrcode dimension
        //     System.out.println("QR Code Dimension: " + qrcode.getWidth() + "x" + qrcode.getHeight());
        //     // print picture dimension
        //     System.out.println("Picture Dimension: " + picture.getWidth() + "x" + picture.getHeight());

        //     embedding.qrcodeEmbedding(qrcode, picture);

        // } catch (IOException e) {
        //     e.printStackTrace();
        // }


        // extraction
        try {
            Extraction extraction = new Extraction();
            BufferedImage hiddenImage = ImageIO.read(new File("hiddenImage.png"));
            extraction.pictureExtraction(hiddenImage);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}

class Embedding {
    int[][] combination = {
        {0, 0, 0}, // 0 even
        {0, 0, 1}, // 1 odd
        {0, 1, 0}, // 2 odd
        {0, 1, 1}, // 3 even
        {1, 0, 0}, // 4 odd
        {1, 0, 1}, // 5 even
        {1, 1, 0}, // 6 even
        {1, 1, 1}  // 7 odd 
    };

    int[] even = {0, 3, 5, 6};
    int[] odd = {1, 2, 4, 7};

    public void qrcodeEmbedding(BufferedImage qrcode, BufferedImage picture) {
        BufferedImage scaledQrCode = scaledQrCode(qrcode, picture);
        try {
            ImageIO.write(scaledQrCode, "png", new File("scaledQrcode.png"));
            // print scaledQrCode dimension
            System.out.println("Scaled QR Code Dimension: " + scaledQrCode.getWidth() + "x" + scaledQrCode.getHeight());

            adjustRGB(scaledQrCode, picture);
            ImageIO.write(picture, "png", new File("hiddenImage.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private BufferedImage scaledQrCode(BufferedImage qrcode, BufferedImage picture) {
        int qrcodeWidth = qrcode.getWidth();
        int qrcodeHeight = qrcode.getHeight();
        int pictureWidth = picture.getWidth();
        int pictureHeight = picture.getHeight();

        double scaleFactor = (double) Math.min(pictureWidth, pictureHeight) / Math.max(qrcodeWidth, qrcodeHeight);

        int newQrcodeWidth = (int) (qrcodeWidth * scaleFactor);
        int newQrcodeHeight = (int) (qrcodeHeight * scaleFactor);

        Image scaledQrcode = qrcode.getScaledInstance(newQrcodeWidth, newQrcodeHeight, Image.SCALE_SMOOTH);

        BufferedImage finalImage = new BufferedImage(pictureWidth, pictureHeight, BufferedImage.TYPE_BYTE_BINARY);
        Graphics2D g2d = finalImage.createGraphics();
        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, pictureWidth, pictureHeight);

        int startx = (pictureWidth - newQrcodeWidth) / 2;
        int starty = (pictureHeight - newQrcodeHeight) / 2;

        g2d.drawImage(scaledQrcode, startx, starty, null);
        g2d.dispose();

        return finalImage;
    }

    private int getNewRGB(int qrcodePixel, int picturePixel) {
        Color qrColor = new Color(qrcodePixel);
        boolean isBlack = qrColor.equals(Color.BLACK); // black to even
        // System.out.println(isBlack);

        int r = (picturePixel >> 16) & 0xFF;
        int g = (picturePixel >> 8) & 0xFF;
        int b = picturePixel & 0xFF;

        // System.out.println(picturePixel);
        // System.out.println(Integer.toBinaryString(picturePixel));
        // System.out.printf("r : %3d - %s\n", r, Integer.toBinaryString(r));
        // System.out.printf("g : %3d - %s\n", g, Integer.toBinaryString(g));
        // System.out.printf("b : %3d - %s\n", b, Integer.toBinaryString(b));

        Random random = new Random();
        int randomIndex = random.nextInt(4);
        int[] rgbNumber;
        if(isBlack) {
            rgbNumber = combination[even[randomIndex]];
        }
        else {
            rgbNumber = combination[odd[randomIndex]];
        }
        // System.out.println(Arrays.toString(rgbNumber));

        r = (r & 0xFE) | rgbNumber[0];
        g = (g & 0xFE) | rgbNumber[1];
        b = (b & 0xFE) | rgbNumber[2];

        // System.out.printf("r : %3d - %s\n", r, Integer.toBinaryString(r));
        // System.out.printf("g : %3d - %s\n", g, Integer.toBinaryString(g));
        // System.out.printf("b : %3d - %s\n", b, Integer.toBinaryString(b));

        picturePixel = (picturePixel & 0xFF000000)
                | (r << 16) 
                | (g << 8)  
                | b;        
        // System.out.println(Integer.toBinaryString(picturePixel));
        return picturePixel;
    }

    private void adjustRGB(BufferedImage qrcode, BufferedImage picture) {
        for(int i = 0; i < qrcode.getWidth(); i++) {
            for(int j = 0; j < qrcode.getHeight(); j++) {
                int qrcodePixel = qrcode.getRGB(i, j);
                int picturePixel = picture.getRGB(i, j);
                int newRGB = getNewRGB(qrcodePixel, picturePixel);
                picture.setRGB(i, j, newRGB);
            }
        }
    }
}

class Extraction {
    private boolean getQrCodeColor(int picturePixel) {
        int rBit = (picturePixel >> 16) & 1;
        int gBit = (picturePixel >> 8) & 1;
        int bBit = picturePixel & 1;

        return (rBit + gBit + bBit) % 2 == 0 ? false : true;
    }

    public void pictureExtraction(BufferedImage picture) {
        try {
            int width = picture.getWidth();
            int height = picture.getHeight();
            BufferedImage hiddenQrCode = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    
            for(int i = 0; i < width; i++) {
                for(int j = 0; j < height; j++) {
                    int picturePixel = picture.getRGB(i, j);
                    boolean isBlack = getQrCodeColor(picturePixel);
                    int rgb = isBlack ? 0 : 255;
                    int newRGB = (rgb << 16) | (rgb << 8) | rgb;
                    hiddenQrCode.setRGB(i, j, newRGB);
                }
            }
    
            ImageIO.write(hiddenQrCode, "png", new File("hiddenQrCode.png"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
