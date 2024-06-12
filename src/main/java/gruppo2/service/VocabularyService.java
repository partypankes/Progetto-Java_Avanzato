package gruppo2.service;

import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import java.util.List;
import java.util.Map;

import static gruppo2.FXMLDocumentController.*;

/**
 * VocabularyService è una classe che estende Service<Void> per gestire l'aggiornamento del vocabolario
 * e dei vettori di documento.
 * <p>
 * Questa classe permette di elaborare una lista di documenti, pulire il testo e i titoli dai documenti,
 * aggiungere le parole al vocabolario, e creare vettori di parole per ciascun documento.
 */
public class VocabularyService extends Service<Void> {
    private final List<Document> documents;
    private final Map<Document, Map<String, Integer>> resultMapByDocument;

    /**
     * Costruttore per inizializzare VocabularyService con i parametri necessari.
     *
     * @param documents             la lista dei documenti da elaborare
     * @param resultMapByDocument   una mappa che contiene i conteggi delle parole per documento
     */
    public VocabularyService(List<Document> documents, Map<Document, Map<String, Integer>> resultMapByDocument) {
        this.documents = documents;
        this.resultMapByDocument = resultMapByDocument;
    }

    /**
     * Crea un task per elaborare i documenti e aggiornare il vocabolario.
     *
     * @return il task per elaborare i documenti e aggiornare il vocabolario
     */
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                for (Document document : documents) {
                    String cleanedText = cleanAndRemoveStopwords(document.document_text());
                    String cleanedTitle = cleanAndRemoveStopwords(document.title());
                    addWordsToVocabulary(cleanedText + " " + cleanedTitle);
                    Map<String, Integer> documentVector = textToVector(cleanedText, cleanedTitle);
                    resultMapByDocument.put(document, documentVector);
                }
                return null;
            }
        };
    }
}
