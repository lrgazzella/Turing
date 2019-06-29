package communication;


import com.google.gson.Gson;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SocketChannel;

public class Communication {

    static private Gson gson = new Gson();

    public static void send(SocketChannel c, Packet p) throws IOException{ // Metodo che invia un Packet p nel SocketChannel c
        String s = gson.toJson(p); // Converto il pacchetto in una stringa json
        Integer len = s.length();

        /* Invio la lunghezza della string json */
        ByteBuffer IntToSend = ByteBuffer.allocate(Integer.BYTES);
        IntToSend.putInt(len);
        IntToSend.flip();
        while(IntToSend.hasRemaining()) {
            c.write(IntToSend);
        }

        /* Invio la stringa vera e propria */
        ByteBuffer stringToSend = ByteBuffer.allocate(1024);
        stringToSend.put(s.getBytes());
        stringToSend.flip();
        while(stringToSend.hasRemaining()) {
            c.write(stringToSend);
        }
    }

    public static Packet read(SocketChannel c) throws IOException{ // Metodo che legge un Packet da un SocketChannel
        ByteBuffer input = ByteBuffer.allocate(4);
        IntBuffer view = input.asIntBuffer();

        /* Leggo l'intero che rappresenta la lunghezza della stringa che dovrò leggere */
        int toRead = Integer.BYTES;
        int read = 0;
        while(toRead > 0){
            if((read = c.read(input)) == -1){
                throw new IOException();
            }else toRead = toRead - read;
        }
        int size = view.get(); // size è la dimensione dell'array di byte da leggere
        /* Leggo l'array di byte */
        ByteBuffer input2 = ByteBuffer.allocate(1024);
        toRead = size;
        while(toRead > 0){
            if((read = c.read(input2)) == -1){
                throw new IOException();
            }else{
                toRead = toRead - read;
            }
        }
        input2.flip();

        /* Converto l'array di byte appena letto in un oggetto Packet */
        JsonReader jr = new JsonReader(new StringReader(new String(input2.array())));
        jr.setLenient(true);
        return gson.fromJson(jr, Packet.class);
    }

    public static void read(SocketChannel c, File f, long toRead) throws IOException { // Legge un file di lunghezza toRead sul SocketChannel c e lo scrive su f
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

    public static void send(SocketChannel c, File f) throws IOException { // Invia il file f nel SocketChannel c
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
