package communication;

public class Body {

    private String username;
    private String documentName;
    private Integer sectionsNumber;
    private Integer sectionNumber;
    private Long bytesNumber;
    private String collaborator;
    private String other;

    public Body() {
        this.username = null;
        this.documentName = null;
        this.sectionsNumber = null;
        this.sectionNumber = null;
        this.bytesNumber = null;
        this.collaborator = null;
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

    public Integer getSectionsNumber() {
        return sectionsNumber;
    }

    public void setSectionsNumber(Integer sectionsNumber) {
        this.sectionsNumber = sectionsNumber;
    }

    public Integer getSectionNumber() {
        return sectionNumber;
    }

    public void setSectionNumber(Integer sectionNumber) {
        this.sectionNumber = sectionNumber;
    }

    public Long getBytesNumber() {
        return bytesNumber;
    }

    public void setBytesNumber(Long bytesNumber) {
        this.bytesNumber = bytesNumber;
    }

    public String getCollaborator() {
        return collaborator;
    }

    public void setCollaborator(String collaborator) {
        this.collaborator = collaborator;
    }

    public String getOther() {
        return other;
    }

    public void setOther(String other) {
        this.other = other;
    }
}
