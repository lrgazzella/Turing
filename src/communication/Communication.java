package communication;


import communication.Body.Body;
import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class Communication {

    static private Gson gson = new Gson();

    public static void sendHeader(SocketChannel sc, Header hdr) throws IOException {
        String s = gson.toJson(hdr);
        Integer len = s.length();

        System.out.println("Len hdr: " + len);

        ByteBuffer IntToSend = ByteBuffer.allocate(Integer.BYTES);
        IntToSend.putInt(len);
        IntToSend.flip();
        while(IntToSend.hasRemaining()) {
            sc.write(IntToSend);
        }

        ByteBuffer stringToSend = ByteBuffer.allocate(1024);
        stringToSend.put(s.getBytes());
        stringToSend.flip();
        while(stringToSend.hasRemaining()) {
            sc.write(stringToSend);
        }
        System.out.println("Header inviato");
    }

    public static Header receiveHeader(SocketChannel sc) throws IOException {
        ByteBuffer input = ByteBuffer.allocate(4);
        IntBuffer view = input.asIntBuffer();

        /* Leggo l'intero */
        int toRead = Integer.BYTES;
        int read = 0;
        while(toRead > 0){
            if((read = sc.read(input)) == -1){
                throw new IOException();
            }else toRead = toRead - read;
        }
        System.out.println("To read: " + toRead);
        int size = view.get(); // size è la dimensione dell'array di byte da leggere
        /* Leggo l'array di byte */
        ByteBuffer input2 = ByteBuffer.allocate(1024);
        toRead = size;
        while(toRead > 0){
            if((read = sc.read(input2)) == -1){
                throw new IOException();
            }else{
                toRead = toRead - read;
            }
        }
        input2.flip();

        JsonReader jr = new JsonReader(new StringReader(new String(input2.array())));
        jr.setLenient(true);
        return gson.fromJson(jr, Header.class); // Converto il JSON in un oggetto di tipo c
    }

    public static void sendBody(SocketChannel sc, Body b) throws IOException {
        String s = gson.toJson(b);
        System.out.println("Inizio invio body (" + s.length() + "): " + s);
        Integer len = s.length();

        ByteBuffer IntToSend = ByteBuffer.allocate(Integer.BYTES);
        IntToSend.putInt(len);
        IntToSend.flip();
        System.out.println("Inizio invio lunghezza");
        while(IntToSend.hasRemaining()) {
            //sc.write(IntToSend);
            System.out.println("Inviati byte len: " + sc.write(IntToSend));
        }
        System.out.println("Fine invio lunghezza");
        ByteBuffer stringToSend = ByteBuffer.allocate(1024);
        stringToSend.put(s.getBytes());
        stringToSend.flip();
        System.out.println("Invio messaggio vero e proprio");
        while(stringToSend.hasRemaining()) {
            System.out.println("Inviati byte msg: " + sc.write(stringToSend));
        }
        System.out.println("Fine invio messaggio vero e proprio");
    }

    public static Body receiveBody(SocketChannel sc, Class<? extends Body> c) throws IOException {
        ByteBuffer input = ByteBuffer.allocate(4);
        IntBuffer view = input.asIntBuffer();
        System.out.println("Inizio lettura length");
        /* Leggo l'intero */
        int toRead = Integer.BYTES;
        System.out.println("Devo leggere bytes: " + toRead);
        int read = 0;
        while(toRead > 0){
            if((read = sc.read(input)) == -1){
                System.out.println("Errore");
                throw new IOException();
            }else {
                toRead = toRead - read;
                if(read != 0) System.out.println("Ho letto bytes: " + read + ". Ne mancano bytes: " + toRead);
            }
        }
        int size = view.get(); // size è la dimensione dell'array di byte da leggere
        System.out.println("Fine lettura length: " + size);
        /* Leggo l'array di byte */
        System.out.println("Inizio lettura msg");
        ByteBuffer input2 = ByteBuffer.allocate(1024);
        toRead = size;
        while(toRead > 0){
            if((read = sc.read(input2)) == -1){
                System.out.println("Errore msg");
                throw new IOException();
            }else{
                toRead = toRead - read;
                if(read != 0)
                    System.out.println("Ho letto bytess: " + read + ". Ne mancano bytes: " + toRead);
            }
        }
        input2.flip();

        System.out.println("Fine lettura msg");

        JsonReader jr = new JsonReader(new StringReader(new String(input2.array())));
        jr.setLenient(true);
        return gson.fromJson(jr, c); // Converto il JSON in un oggetto di tipo c
    }

    public static void receiveFile(SocketChannel c, File f, long toRead) throws IOException, FileNotFoundException {
        ByteBuffer bb = ByteBuffer.allocate(1024);
        int bytesRead;
        FileOutputStream bout = new FileOutputStream(f);
        FileChannel sbc = bout.getChannel();

        while(toRead > 0){
            if((bytesRead = c.read(bb)) != -1){
                toRead = toRead - bytesRead;
                bb.flip();
                sbc.write(bb);
                bb.clear();
            }else throw new IOException();
        }
        sbc.close();
    }

    public static void sendFile(SocketChannel c, File f) throws IOException {
        FileChannel fChannel = FileChannel.open(f.toPath());
        ByteBuffer buff = ByteBuffer.allocate(1024);

        int bytesread;
        while((bytesread = fChannel.read(buff)) != -1){
            buff.flip();
            while (buff.hasRemaining()) {
                c.write(buff);
            }
            buff.clear();
        }
        fChannel.close();
    }

}
