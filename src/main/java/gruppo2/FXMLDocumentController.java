package gruppo2;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
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
import gruppo2.Document;

import static gruppo2.DirectoryChecker.*;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.AnchorPane;
import javafx.stage.DirectoryChooser;

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
        String queryText = queryTf.getText();

        // Se la query è vuota, vengono mostrati tutti i documenti;
        if (queryText == null || queryText.trim().isEmpty()) {
            System.out.println("ciao");
            List<Document> allDocuments = new ArrayList<>(resultMapByDocument.keySet());
            System.out.println(allDocuments);
            tableView.setItems(FXCollections.observableArrayList(allDocuments));

            updateCollectionStatistics(allDocuments);
        } else {
            /* Altrimenti vengono mostrati solo i documenti filtrati in base alla similarità:
            - pulisce la query dalle stopwords;
            - crea un vettore dalla query e calcola la somiglianza tra la query e i documenti usando il coseno di similarità;
            - ordina i documenti in base alla somiglianza;
            - aggiorna infine la tabella per mostrare i risultati */
            String cleanedQuery = cleanAndRemoveStopwords(queryText);
            Map<String, Integer> queryVector = textToVector(cleanedQuery, "");
            corrispondenzaSimiliarita.clear();
            for (Map.Entry<Document, Map<String, Integer>> entry : resultMapByDocument.entrySet()) {
                double similarity = calculateCosineSimilarity(entry.getValue(), queryVector);
                if (similarity > 0) {
                    corrispondenzaSimiliarita.put(entry.getKey(), similarity);
                }
            }

            List<Map.Entry<Document, Double>> sortedSimilarities = corrispondenzaSimiliarita.entrySet().stream().sorted(Map.Entry.comparingByValue(Comparator.reverseOrder())).collect(Collectors.toList());
            List<Document> sortedDocuments = sortedSimilarities.stream().map(Map.Entry::getKey).collect(Collectors.toList());
            tableView.setItems(observableArrayList(sortedDocuments));
            updateCollectionStatistics(sortedDocuments);
        }
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
        // Viene creato un oggetto DirectoryChooser che permette all'utente di selezionare una cartella
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle("Seleziona una cartella");
        File selectedDirectory = directoryChooser.showDialog(null);
        addCloseRequestHandler(pane1);
        if (selectedDirectory != null) {
            System.out.println("Cartella selezionata: " + selectedDirectory.getAbsolutePath());

            // Specifica il percorso della directory da monitorare
            Path dir = Paths.get(selectedDirectory.getAbsolutePath());

            // Esegui operazioni di controllo della directory in modo asincrono
            executorService.submit(() -> {
                // Carica il percorso della directory precedente
                String previousDirPath = loadPreviousDirPath();

                List<Document> documentsToUpdate;
                if (!dir.toString().equals(previousDirPath)) {
                    // Nuova directory rilevata
                    System.out.println("Nuova directory rilevata. Salvataggio del nuovo stato.");
                    Map<String, Long> currentState = getCurrentState(dir);
                    List<Document> currentDocuments = readDocumentsFromDirectory(selectedDirectory);
                    saveCurrentState(currentState, currentDocuments);
                    saveCurrentDirPath(dir.toString());

                    // Ordina i documenti per titolo
                    documentsToUpdate = new ArrayList<>(currentDocuments);

                } else {
                    // Carica lo stato precedente della directory
                    DirectoryState previousState = loadPreviousState();

                    // Ottieni lo stato attuale della directory
                    Map<String, Long> currentState = getCurrentState(dir);

                    // Confronta gli stati e rileva le modifiche
                    checkForChanges(previousState.fileStates, currentState, previousState.documents, dir);

                    // Salva lo stato attuale per il confronto futuro
                    saveCurrentState(currentState, previousState.documents);

                    // Ordina i documenti per titolo
                    documentsToUpdate = new ArrayList<>(previousState.documents);
                }



                // Aggiorna la collezione di documenti
                Platform.runLater(() -> {
                    documents.setAll(documentsToUpdate);
                    // Passa alla vista successiva
                    pane1.setVisible(false);
                    pane2.setVisible(true);
                    createVocabularyAndVectors(documents.stream().toList());
                });
            });
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
        List<Future<Void>> futures = new ArrayList<>();

        /* Per ogni documento viene inviato un task all'executorService per:
           - pulisce il testo rimuove le stopwords;
           - aggiunge le parole del testo senza stopwords, al vocabolario;
           - creare un vettore di frequenza delle parole */
        for (Document document : documents) {
            futures.add(executorService.submit(() -> {
                String cleanedText = cleanAndRemoveStopwords(document.getDocument_text());
                String cleanedTitle = cleanAndRemoveStopwords(document.getTitle());
                addWordsToVocabulary(cleanedText + " " + cleanedTitle);
                Map<String, Integer> documentVector = textToVector(cleanedText,cleanedTitle);
                resultMapByDocument.put(document, documentVector);
                return null;
            }));
        }

        for (Future<Void> future : futures) {
            try {
                future.get();
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
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


    // Recupera il testo del documento selezionato e mostra anche le relative statistiche
    private void mostraContenutoDocumento(Document documentoSelezionato) {
        pane2.setVisible(false);
        paneDocumento.setVisible(true);
        corpoDocumento.setText(documentoSelezionato.getDocument_text());
        documentTitleLabel.setText(documentoSelezionato.getTitle());
        mostrastatisticheDocumento(documentoSelezionato);
    }


    // Chiude il documento che era stato selezionato
    @FXML
    public void chiudiDocumento() {
        paneDocumento.setVisible(false);
        pane2.setVisible(true);
    }


    // Calcola le statistiche sul documento selezionato
    @FXML
    public void mostrastatisticheDocumento(Document documentoSelezionato) {
        String testoDocumento = documentoSelezionato.getDocument_text().replaceAll("'", " ");

        int sentenceCount =  (int) Arrays.stream(testoDocumento.split("[.!?]")).filter(s -> !s.trim().isEmpty()).count();

        // Testo già pulito e senza stopwords

        Map<String, Integer> documentVector = resultMapByDocument.get(documentoSelezionato);

        // Calcolo delle statistiche
        int totalWords = testoDocumento.split("\\s").length - 1;
        int uniqueWords = documentVector.size();


        // Le 5 parole più comuni
        List<Map.Entry<String, Integer>> commonWords = documentVector.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .limit(5)
                .toList();
        for (Map.Entry<String, Integer> entry : commonWords) {

            // Divide il titolo in parole utilizzando spazi e punteggiatura come delimitatori
            String[] parole = documentoSelezionato.getTitle().split("\\W+");
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
        }

        // Creazione del messaggio di statistica
        StringBuilder statsMessage = new StringBuilder();
        statsMessage.append("Numero totale di parole: ").append(totalWords).append("\n");
        statsMessage.append("Numero di parole uniche: ").append(uniqueWords).append("\n");
        statsMessage.append("Numero di frasi: ").append(sentenceCount).append("\n");
        statsMessage.append("Le 5 parole più comuni sono:\n");
        commonWords.forEach(entry -> statsMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n"));

        if(corrispondenzaSimiliarita.get(documentoSelezionato) == null) {
            statsMessage.append("Percentuale di similaritá rispetto alla query: non definita\n");
        } else {
            double percentuale = corrispondenzaSimiliarita.get(documentoSelezionato) * 100;

            statsMessage.append("Percentuale di similaritá rispetto alla query: ").append(Math.round(percentuale)).append("%\n");
        }


        statisticheDocumentoLabel.setText(statsMessage.toString());
    }

    private void updateCollectionStatistics(List<Document> documents) {
        collectionStatisticsLabel.setText("Qui devono esserci le statistiche dell'intera collezione");
    }
}
