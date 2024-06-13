package gruppo2.service;

import gruppo2.Document;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

/**
 * DocumentContentService è una classe che estende Service<Void> per caricare e visualizzare il contenuto
 * di un documento in modo asincrono.
 * <p>
 * Questa classe permette di caricare il contenuto di un documento su un thread separato e di aggiornare
 * l'interfaccia utente sul thread principale per evitare blocchi dell'interfaccia durante il caricamento.
 */
public class DocumentContentService extends Service<Void> {
    private final Document document;
    private final AnchorPane pane2;
    private final AnchorPane paneDocumento;
    private final Label documentTitleLabel;
    private final TextArea corpoDocumento;

    /**
     * Costruttore per inizializzare DocumentContentService con i componenti necessari.
     *
     * @param document             il documento da caricare
     * @param pane2                il secondo pannello che sarà nascosto
     * @param paneDocumento        il pannello che sarà visualizzato
     * @param documentTitleLabel   l'etichetta per il titolo del documento
     * @param corpoDocumento       l'area di testo per il contenuto del documento
     */
    public DocumentContentService(Document document, AnchorPane pane2, AnchorPane paneDocumento, Label documentTitleLabel, TextArea corpoDocumento) {
        this.document = document;
        this.pane2 = pane2;
        this.paneDocumento = paneDocumento;
        this.documentTitleLabel = documentTitleLabel;
        this.corpoDocumento = corpoDocumento;
    }

    /**
     * Crea un task per caricare il contenuto del documento.
     *
     * @return il task per caricare il contenuto del documento
     */
    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() {
                // Carica il contenuto del documento su un thread separato
                String documentText = document.document_text();
                String documentTitle = document.title().toUpperCase();

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
    }
}
