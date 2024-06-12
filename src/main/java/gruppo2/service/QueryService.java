package gruppo2.service;
import gruppo2.Document;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static gruppo2.FXMLDocumentController.*;

/**
 * QueryService è una classe che estende Service<List<Document>> per gestire le query di ricerca sui documenti.
 * <p>
 * Questa classe permette di eseguire una query sui documenti disponibili, calcolando la similarità tra i documenti
 * e la query fornita, e restituendo una lista di documenti ordinati per rilevanza.
 */
public class QueryService extends Service<List<Document>> {

    private String queryText;
    private final Map<Document, Map<String, Integer>> resultMapByDocument;
    private final Map<Document, Double> corrispondenzaSimiliarita;

    /**
     * Costruttore per inizializzare QueryService con i parametri necessari.
     *
     * @param resultMapByDocument       una mappa che contiene i conteggi delle parole per documento
     * @param corrispondenzaSimiliarita una mappa che contiene i valori di similarità per documento
     */
    public QueryService(Map<Document, Map<String, Integer>> resultMapByDocument, Map<Document, Double> corrispondenzaSimiliarita) {
        this.resultMapByDocument = resultMapByDocument;
        this.corrispondenzaSimiliarita = corrispondenzaSimiliarita;
    }

    /**
     * Imposta il testo della query.
     *
     * @param queryText il testo della query
     */
    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    /**
     * Crea un task per eseguire la query sui documenti.
     *
     * @return il task per eseguire la query sui documenti
     */
    @Override
    protected Task<List<Document>> createTask() {
        return new Task<>() {
            @Override
            protected List<Document> call() {
                List<Document> filteredDocuments;

                if (queryText == null || queryText.trim().isEmpty()) {
                    corrispondenzaSimiliarita.clear();
                    filteredDocuments = new ArrayList<>(resultMapByDocument.keySet());
                } else {
                    String cleanedQuery = cleanAndRemoveStopwords(queryText);
                    Map<String, Integer> queryVector = textToVector(cleanedQuery, "");
                    corrispondenzaSimiliarita.clear();

                    // Contatore per il progresso
                    int totalDocuments = resultMapByDocument.size();
                    int processedDocuments = 0;

                    for (Map.Entry<Document, Map<String, Integer>> entry : resultMapByDocument.entrySet()) {
                        double similarity = calculateCosineSimilarity(entry.getValue(), queryVector);
                        if (similarity > 0) {
                            corrispondenzaSimiliarita.put(entry.getKey(), similarity);
                        }

                        // Aggiorna il progresso
                        processedDocuments++;
                        updateProgress(processedDocuments, totalDocuments);
                    }

                    List<Map.Entry<Document, Double>> sortedSimilarities = corrispondenzaSimiliarita.entrySet().stream()
                            .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                            .toList();
                    filteredDocuments = sortedSimilarities.stream().map(Map.Entry::getKey).collect(Collectors.toList());
                }

                return filteredDocuments;
            }
        };
    }
}
