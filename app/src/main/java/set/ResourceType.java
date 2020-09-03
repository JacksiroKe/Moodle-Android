package set;

public enum ResourceType {

    // Activities
    ASSIGNMENT("assign"),
    CHAT,
    CHOICE("choice"),
    DATABASE,
    EXTERNAL,
    FEEDBACK,
    FORUM("forum"),
    GLOSSARY,
    LESSON,
    QUIZ("quiz"),
    SCROM,
    SURVEY,
    WIKI,
    WORKSHOP,

    // Resources
    BOOK,
    FILE("resource"),
    FOLDER("folder"),
    IMS,
    LABEL("label"),
    PAGE("page"),
    URL("url"),


    UNKNOWN;

    private String id;

    ResourceType(String identifier) {
        id = identifier;
    }

    ResourceType() {
    }

    public static ResourceType getTypeFromIdentifier(String id) {
        for (ResourceType type :
                ResourceType.values()) {
            if (
                    type.id != null &&
                    type.id.toLowerCase().equals(id.toLowerCase())
            ) return type;
        }

        return UNKNOWN;
    }
}
