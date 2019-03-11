package communication.Body;

public class CreateBody implements Body {

    private String username;
    private String documentName;
    private int sectionsNumber;

    public CreateBody(String username, String documentName, int sectionsNumber) {
        this.username = username;
        this.documentName = documentName;
        this.sectionsNumber = sectionsNumber;
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

    public int getSectionsNumber() {
        return sectionsNumber;
    }

    public void setSectionsNumber(int sectionsNumber) {
        this.sectionsNumber = sectionsNumber;
    }
}
