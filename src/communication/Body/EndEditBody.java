package communication.Body;

public class EndEditBody implements Body {

    private String username;
    private String documentName;
    private int sectionNumber;
    private int bytesNumber;

    public EndEditBody(String username, String documentName, int sectionNumber, int bytesNumber) {
        this.username = username;
        this.documentName = documentName;
        this.sectionNumber = sectionNumber;
        this.bytesNumber = bytesNumber;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public int getSectionNumber() {
        return sectionNumber;
    }

    public void setSectionNumber(int sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public int getBytesNumber() {
        return bytesNumber;
    }

    public void setBytesNumber(int bytesNumber) {
        this.bytesNumber = bytesNumber;
    }
}
