package turing;

import java.io.*;
import java.util.Date;

public class Message implements Serializable{

    private String username;
    private String text;
    private Date date;

    public Message(String username, String text, Date date) {
        this.username = username;
        this.text = text;
        this.date = date;
    }

    public static byte[] fromMessageToBytes(Message m) throws IOException {
        ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        ObjectOutputStream objStream = new ObjectOutputStream(byteStream);
        objStream.writeObject(m);
        return byteStream.toByteArray();
    }

    public static Message fromBytesToMessage(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream byteStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objStream = new ObjectInputStream(byteStream);
        return (Message) objStream.readObject();
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public Date getDate() {
        return date;
    }

    public void setDate(Date date) {
        this.date = date;
    }
}
