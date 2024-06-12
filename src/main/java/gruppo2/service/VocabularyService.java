package gruppo2.service;

import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.List;
import java.util.Map;

import static gruppo2.FXMLDocumentController.*;

public class VocabularyService extends Service<Void> {
    private List<Document> documents;
    private Map<Document, Map<String, Integer>> resultMapByDocument;

    public VocabularyService(List<Document> documents, Map<Document, Map<String, Integer>> resultMapByDocument) {
        this.documents = documents;
        this.resultMapByDocument = resultMapByDocument;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                for (Document document : documents) {
                    String cleanedText = cleanAndRemoveStopwords(document.getDocument_text());
                    String cleanedTitle = cleanAndRemoveStopwords(document.getTitle());
                    addWordsToVocabulary(cleanedText + " " + cleanedTitle);
                    Map<String, Integer> documentVector = textToVector(cleanedText, cleanedTitle);
                    resultMapByDocument.put(document, documentVector);
                }
                return null;
            }
        };
    }


}
