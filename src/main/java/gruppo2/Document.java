package gruppo2;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Objects;

public class Document implements Comparable<Document> {
    private final String title;
    private final String document_text;


    public Document(String title, String documentText) {
        this.title = title;

        document_text = documentText;
    }


    public String getTitle() {
        return title;
    }

    public String getDocument_text() {
        return document_text;
    }


    @Override
    public String toString() {
        return "Document{" +
                "title='" + title + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Document document = (Document) o;
        return Objects.equals(getTitle(), document.getTitle()) && Objects.equals(getDocument_text(), document.getDocument_text());

}

    @Override
    public int hashCode() {
        return Objects.hash(getTitle(), getDocument_text());
    }

    @Override
    public int compareTo(Document other) {
        int titleComparison = this.title.compareTo(other.getTitle());
        if (titleComparison != 0) {
            return titleComparison;
        } else {
            return this.document_text.compareTo(other.getDocument_text());
        }
    }
    }

