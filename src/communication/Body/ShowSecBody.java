package communication.Body;

public class ShowSecBody implements Body{

    private String username;
    private String documentName;
    private int sectionNumber;

    public ShowSecBody(String username, String documentName, int sectionNumber) {
        this.username = username;
        this.documentName = documentName;
        this.sectionNumber = sectionNumber;
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
}
