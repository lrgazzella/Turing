package communication.Body;

public class ShowDocBody implements Body {

    private String username;
    private String documentName;

    public ShowDocBody(String username, String documentName) {
        this.username = username;
        this.documentName = documentName;
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
}
