package communication;

public enum OPS {
    CREATE,
    SHARE,
    SHOWDOC,
    SHOWSEC,
    LIST,
    EDIT,
    ENDEDIT,
    // OPS per le risposte
    OK,
    ALREADYEXISTS,
    NOSUCHFILE,
    NOSUCHUSER,
    ERROR,
    ALREADYINEDITING,
    CREATEASSOCIATION
}
