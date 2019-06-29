package turing;

import java.io.Serializable;

public class Invitation implements Serializable { // Poichè dovrà essere inviato, implementa la classe Serializable

    private String ownerDocument;
    private String documentName;

    public Invitation(String ownerDocument, String documentName) {
        this.ownerDocument = ownerDocument;
        this.documentName = documentName;
    }

    public String getOwnerDocument() {
        return ownerDocument;
    }

    public void setOwnerDocument(String ownerDocument) {
        this.ownerDocument = ownerDocument;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }
}
