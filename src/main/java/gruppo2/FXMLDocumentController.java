package gruppo2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

import java.io.*;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static gruppo2.DirectoryChecker.*;

import static javafx.collections.FXCollections.observableArrayList;


public class FXMLDocumentController implements Initializable {

    @FXML
    private Label documentTitleLabel;

    @FXML
    private AnchorPane pane1;

    @FXML
    private AnchorPane pane2;

    @FXML
    private AnchorPane paneDocumento;

    @FXML
    private AnchorPane statistics1;

    @FXML
    private TextField queryTf;

    @FXML
    private TextArea corpoDocumento;

    @FXML
    private Label statisticheDocumentoLabel;

    @FXML
    private Label collectionStatisticsLabel;

    @FXML
    private Button chiudiDocumento;

    @FXML
    private TableView<Document> tableView;

    @FXML
    private TableColumn<Document, String> titleColumn;

    @FXML
    private ProgressIndicator progressIndicator;

    private final ObservableList<Document> documents = observableArrayList();

    private static final ConcurrentMap<String, Integer> vocabolario = new ConcurrentHashMap<>();

    private static final Map<Document, Double> corrispondenzaSimiliarita = new TreeMap<>(Collections.reverseOrder());

    private static final Map<Document, Map<String, Integer>> resultMapByDocument = new ConcurrentHashMap<>();

    private List<String> stopwords;

    private final ExecutorService executorService = Executors.newFixedThreadPool(4);

    private boolean isFirstClick = true;

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        titleColumn.setCellValueFactory(new PropertyValueFactory<>("title"));
        tableView.setItems(documents);
        try {
            loadStopwords();
        } catch (IOException e) {
            e.printStackTrace();
        }
        selezionaDocumento();
    }

    private void addCloseRequestHandler(Node node) {
        Stage stage = (Stage) node.getScene().getWindow();
        stage.setOnCloseRequest(new EventHandler<WindowEvent>() {
            @Override
            public void handle(WindowEvent event) {
                shutdownExecutorService();
            }
        });
    }

    private void shutdownExecutorService() {
        executorService.shutdown();
        try {
            if (!executorService.awaitTermination(60, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
        } catch (InterruptedException e) {
            executorService.shutdownNow();
        }
    }


    // Gestisce la query inserita dall'utente
    @FXML
    private void handleQuery() {
        // Crea una nuova Task per eseguire la query
        Task<List<Document>> queryTask = new Task<>() {
            @Override
            protected List<Document> call() throws Exception {
                String queryText = queryTf.getText();
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

        // Associa il ProgressIndicator al progresso della Task
        progressIndicator.progressProperty().bind(queryTask.progressProperty());
        progressIndicator.setVisible(true);

        // Aggiorna la TableView con i risultati della query al termine della Task
        queryTask.setOnSucceeded(event -> {
            List<Document> result = queryTask.getValue();
            tableView.setItems(FXCollections.observableArrayList(result));
            progressIndicator.setVisible(false);
            showCollectionStatistics(result);
        });

        queryTask.setOnFailed(event -> {
            queryTask.getException().printStackTrace();
            progressIndicator.setVisible(false);
        });

        // Esegui la Task utilizzando l'ExecutorService
        executorService.submit(queryTask);
    }



    //  Converte il testo di un documento  in un vettore di frequenze delle parole
    private Map<String, Integer> textToVector(String text, String title) {
        Map<String, Integer> vector = new TreeMap<>();
        if(!Objects.equals(title, ""))
        {
            Arrays.stream(title.split("\\s+")).forEach(word -> vector.merge(word, 2, Integer::sum));
        }
        // ogni parola nel testo diventa una chiave nella mappa e il valore associato è il numero di occorrenze di quella parola
        Arrays.stream(text.split("\\s+")).forEach(word -> vector.merge(word, 1, Integer::sum));
        return vector;
    }


    // Calcola il coseno di similarità tra due vettori: misura quanto sono simili due vettori
    private static double calculateCosineSimilarity(Map<String, Integer> vector1, Map<String, Integer> vector2) {
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;

        for (String key : vector1.keySet()) {
            int val1 = vector1.get(key);
            int val2 = vector2.getOrDefault(key, 0);

            dotProduct += val1 * val2;
            normA += Math.pow(val1, 2);
            normB += Math.pow(val2, 2);
        }

        if (normA == 0.0 || normB == 0.0) {
            return 0.0;
        } else {
            return dotProduct / (Math.sqrt(normA) * (Math.sqrt(normB)));
        }
    }


    // Carica le stopwords da un file e le inserisce in una lista
    public void loadStopwords() throws IOException {
        File stopwordsFile = new File("stopwords-it.txt");

        if (stopwordsFile.exists()) {
            this.stopwords = Files.readAllLines(stopwordsFile.toPath());
        } else {
            // Se il file non esiste, la lista di stopwords sarà vuota
            this.stopwords = new ArrayList<>();
            System.out.println("Stopwords file not found.");
        }
    }


    @FXML
    private void folderSelection(ActionEvent event) throws IOException {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");
        File selectedDirectory = directoryChooser.showDialog(null);
        addCloseRequestHandler(pane1);
        if (selectedDirectory != null) {
            System.out.println("Cartella selezionata: " + selectedDirectory.getAbsolutePath());

            Path dir = Paths.get(selectedDirectory.getAbsolutePath());

            Task<List<Document>> folderTask = new Task<>() {
                @Override
                protected List<Document> call() throws Exception {
                    String previousDirPath = loadPreviousDirPath();

                    List<Document> documentsToUpdate;
                    if (!previousDirPath.equals(dir.toString())) {
                        System.out.println("Nuova directory rilevata. Salvataggio del nuovo stato.");
                        Map<String, Long> currentState = getCurrentState(dir);
                        List<Document> currentDocuments = readDocumentsFromDirectory(selectedDirectory);
                        saveCurrentState(currentState, currentDocuments);
                        saveCurrentDirPath(dir.toString());
                        documentsToUpdate = new ArrayList<>(currentDocuments);
                    } else {
                        DirectoryState previousState = loadPreviousState();
                        Map<String, Long> currentState = getCurrentState(dir);
                        checkForChanges(previousState.fileStates, currentState, previousState.documents, dir);
                        saveCurrentState(currentState, previousState.documents);
                        documentsToUpdate = new ArrayList<>(previousState.documents);
                    }
                    return documentsToUpdate;
                }
            };

            folderTask.setOnSucceeded(event1 -> {
                List<Document> documentsToUpdate = folderTask.getValue();
                documents.setAll(documentsToUpdate);
                pane1.setVisible(false);
                pane2.setVisible(true);
                createVocabularyAndVectors(documents.stream().toList());
            });

            folderTask.setOnFailed(event1 -> {
                folderTask.getException().printStackTrace();
            });

            executorService.submit(folderTask);
        } else {
            System.out.println("Operazione annullata");
        }
    }



    private List<Document> readDocumentsFromDirectory(File directory) {
        List<Document> documentList = new ArrayList<>();
        File[] files = directory.listFiles();
        if (files != null) {
            List<Future<Document>> futures = new ArrayList<>();
            for (File file : files) {
                if (file.isFile() && file.getName().endsWith(".txt")) {
                    futures.add(executorService.submit(() -> readDocumentFromFile(file)));
                }
            }

            for (Future<Document> future : futures) {
                try {
                    Document document = future.get();
                    if (document != null) {
                        documentList.add(document);
                    }
                } catch (InterruptedException | ExecutionException e) {
                    e.printStackTrace();
                }
            }
        }
        return documentList;
    }


    // Legge un documento da un file
    private Document readDocumentFromFile(File file) {
        return getDocument(file);
    }

    static Document getDocument(File file) {
        try (Scanner scanner = new Scanner(new BufferedReader(new FileReader(file)))) {
            if (!scanner.hasNextLine()) {
                return null;
            }

            String title = scanner.nextLine();
            StringBuilder body = new StringBuilder();

            while (scanner.hasNextLine()) {
                body.append(scanner.nextLine()).append("\n");
            }

            Document document = new Document(file.getName(), title, body.toString());
            return document;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    // Crea il vocabolario e i vettori di documenti
    private void createVocabularyAndVectors(List<Document> documents) {
        Task<Void> vocabularyTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                List<Future<Void>> futures = new ArrayList<>();

                for (Document document : documents) {
                    futures.add(executorService.submit(() -> {
                        String cleanedText = cleanAndRemoveStopwords(document.getDocument_text());
                        String cleanedTitle = cleanAndRemoveStopwords(document.getTitle());
                        addWordsToVocabulary(cleanedText + " " + cleanedTitle);
                        Map<String, Integer> documentVector = textToVector(cleanedText, cleanedTitle);
                        resultMapByDocument.put(document, documentVector);
                        return null;
                    }));
                }

                for (Future<Void> future : futures) {
                    future.get();
                }
                return null;
            }
        };

        vocabularyTask.setOnSucceeded(event -> {
            // Do something after the task completes successfully, if necessary
        });

        vocabularyTask.setOnFailed(event -> {
            vocabularyTask.getException().printStackTrace();
        });

        executorService.submit(vocabularyTask);
    }



    // Pulisce il testo del documento e rimuove le stopwrods
    private String cleanAndRemoveStopwords(String text) {
        /* Pulizia:
          - sostituisce tutti gli apostrofi con spazi;
          - rimuove tutti i caratteri non alfabetici e non spazi (\p{L} è una proprietà Unicode che rappresenta tutte le lettere);
          - converte tutto il testo in minuscolo per uniformità */
        String cleanedText = text.replaceAll("'", " ").replaceAll("[^\\p{L}\\s]", " ").toLowerCase();

        /* Rimozione stopwrdos:
           - divide il testo pulito in parole, usando gli spazi come delimitatori;
           - filtra le parole, rimuovendo quelle presenti nella lista delle stopwords;
           - colleziona le parole filtrate in una lista */
        List<String> words = Stream.of(cleanedText.split("\\s+")).filter(word -> !stopwords.contains(word)).collect(Collectors.toList());

        // Unisce le parole rimanenti in una singola stringa separata da spazi.
        return String.join(" ", words);
    }


    /* Divide il testo in parole usando gli spazi come delimitatori e aggiorna il vocabolario: per ogni parola aggiorna il relativo conteggio nel
    vocabolario (aggiunge la parola al vocabolario con un conteggio di 1 se non è presente, altrimenti incrementa il conteggio di 1) */
    private void addWordsToVocabulary(String text) {
        Arrays.stream(text.split("\\s+")).forEach(parola -> vocabolario.merge(parola, 1, Integer::sum));
    }


    // Quando viene selezionato un documento con un click, viene mostrato il suo contenuto
    private void selezionaDocumento() {
        tableView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 1) {
                Document documentoSelezionato = tableView.getSelectionModel().getSelectedItem();
                if (documentoSelezionato != null) {
                    mostraContenutoDocumento(documentoSelezionato);
                }
            }
        });
    }


    private void mostraContenutoDocumento(Document documentoSelezionato) {
        Task<Void> contentTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Carica il contenuto del documento su un thread separato
                String documentText = documentoSelezionato.getDocument_text();
                String documentTitle = documentoSelezionato.getTitle();

                // Aggiorna l'interfaccia utente sul thread principale
                Platform.runLater(() -> {
                    pane2.setVisible(false);
                    paneDocumento.setVisible(true);
                    documentTitleLabel.setText(documentTitle);
                    corpoDocumento.setText(documentText); // Carica tutto il testo alla fine
                });

                return null;
            }
        };

        contentTask.setOnFailed(event -> contentTask.getException().printStackTrace());

        executorService.submit(contentTask);
    }

    // Chiude il documento che era stato selezionato
    @FXML
    public void chiudiDocumento() {
        paneDocumento.setVisible(false);
        pane2.setVisible(true);
    }


    @FXML
    public void statisticheDocumento(){
        Document documentoSelezionato = tableView.getSelectionModel().getSelectedItem();
          if (isFirstClick) {
              if (documentoSelezionato != null) {
                  mostrastatisticheDocumento(documentoSelezionato);
              }
        } else {
            statisticheDocumentoLabel.setText("");
        }
        isFirstClick = !isFirstClick;
    }

    // Calcola le statistiche sul documento selezionato
    private void mostrastatisticheDocumento(Document documentoSelezionato) {
        String testoDocumento = documentoSelezionato.getDocument_text().replaceAll("'", " ");
        Map<String, Integer> documentVector = new HashMap<>(resultMapByDocument.get(documentoSelezionato)); // Crea una copia

        // Calcolo delle statistiche
        int totalWords = testoDocumento.split("\\s").length - 1;
        int uniqueWords = documentVector.size();
        int sentenceCount = (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

        // Le 5 parole più comuni
        List<Map.Entry<String, Integer>> commonWords = documentVector.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .toList();

        // Rimuovi il conteggio delle parole del titolo dalla copia del vector
        for (Map.Entry<String, Integer> entry : commonWords) {
            String[] parole = documentoSelezionato.getTitle().split("\\W+");
            int conteggio = 0;

            for (String p : parole) {
                if (p.equalsIgnoreCase(entry.getKey())) {
                    conteggio++;
                }
            }

            if (conteggio > 0) {
                entry.setValue(entry.getValue() - conteggio);
            }
        }

        StringBuilder statsMessage = new StringBuilder();
        statsMessage.append("Numero totale di parole: ").append(totalWords).append("\n");
        statsMessage.append("Numero di parole uniche: ").append(uniqueWords).append("\n");
        statsMessage.append("Numero di frasi: ").append(sentenceCount).append("\n");
        statsMessage.append("Le 5 parole più comuni sono:\n");
        commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        if (corrispondenzaSimiliarita.get(documentoSelezionato) == null) {
            statsMessage.append("Percentuale di similaritá rispetto alla \nquery: non definita\n");
        } else {
            double percentuale = corrispondenzaSimiliarita.get(documentoSelezionato) * 100;
            statsMessage.append("Percentuale di similaritá rispetto alla query: ").append(Math.round(percentuale)).append("%\n");
        }

        statisticheDocumentoLabel.setText(String.valueOf(statsMessage));
    }



    // Calcola le statistiche sull'intera collezione di documenti
    @FXML
    private void collectionStatistics(){
        ObservableList<Document> currentDocuments = tableView.getItems();
        if (isFirstClick) {
            statistics1.setVisible(true);
            showCollectionStatistics(currentDocuments);
        } else {
            statistics1.setVisible(false);
        }
        isFirstClick = !isFirstClick;
    }

    private void showCollectionStatistics(List<Document> documents) {
        Task<String> statsTask = new Task<>() {
            @Override
            protected String call() throws Exception {
                int totalWords = 0;
                int sentenceCount = 0;
                int documentCount = documents.size();
                Map<String, Integer> globalWordCount = new HashMap<>();

                for (Document doc : documents) {
                    String testoDocumento = doc.getDocument_text().replaceAll("'", " ");

                    Map<String, Integer> documentVector = new HashMap<>(resultMapByDocument.get(doc)); // Crea una copia

                    // Aggiorna il conteggio totale delle parole e delle frasi
                    totalWords += testoDocumento.split("\\s").length - 1;
                    sentenceCount += (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

                    // Aggiorna il conteggio globale delle parole
                    for (Map.Entry<String, Integer> entry : documentVector.entrySet()) {
                        // Divide il titolo in parole utilizzando spazi e punteggiatura come delimitatori
                        String[] parole = doc.getTitle().split("\\W+");
                        int conteggio = 0;

                        // Itera attraverso le parole e conta le occorrenze
                        for (String p : parole) {
                            if (p.equalsIgnoreCase(entry.getKey())) {
                                conteggio++;
                            }
                        }

                        // Se il conteggio è maggiore di zero, aggiorna il valore dell'entry
                        if (conteggio > 0) {
                            entry.setValue(entry.getValue() - conteggio);
                        }

                        globalWordCount.put(entry.getKey(), globalWordCount.getOrDefault(entry.getKey(), 0) + entry.getValue());
                    }

                }

                // Le 5 parole più comuni nell'intera collezione
                List<Map.Entry<String, Integer>> commonWords = globalWordCount.entrySet().stream()
                        .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                        .limit(5)
                        .toList();

                // Creazione del messaggio di statistica
                StringBuilder statsMessage = new StringBuilder();
                statsMessage.append("Numero totale di parole nella collezione: ").append(totalWords).append("\n");
                statsMessage.append("Numero di frasi nella collezione: ").append(sentenceCount).append("\n");
                statsMessage.append("Numero di documenti nella collezione: ").append(documentCount).append("\n");
                statsMessage.append("Le 5 parole più comuni nella collezione sono:\n");
                commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

                return statsMessage.toString();
            }
        };

        statsTask.setOnSucceeded(event -> {
            String statsMessage = statsTask.getValue();
            collectionStatisticsLabel.setText(statsMessage);
        });

        statsTask.setOnFailed(event -> statsTask.getException().printStackTrace());

        executorService.submit(statsTask);
    }


}
