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

public class QueryService extends Service<List<Document>> {

    private String queryText;
    private final Map<Document, Map<String, Integer>> resultMapByDocument;
    private final Map<Document, Double> corrispondenzaSimiliarita;

    public QueryService(Map<Document, Map<String, Integer>> resultMapByDocument, Map<Document, Double> corrispondenzaSimiliarita) {
        this.resultMapByDocument = resultMapByDocument;
        this.corrispondenzaSimiliarita = corrispondenzaSimiliarita;
    }

    public void setQueryText(String queryText) {
        this.queryText = queryText;
    }

    @Override
    protected Task<List<Document>> createTask() {
        return new Task<>() {
            @Override
            protected List<Document> call() throws Exception {
                List<Document> filteredDocuments;

                if (queryText == null || queryText.trim().isEmpty()) {
                    corrispondenzaSimiliarita.clear();
                    System.out.println("ciao");
                    filteredDocuments = new ArrayList<>(resultMapByDocument.keySet());
                    System.out.println(filteredDocuments);
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


