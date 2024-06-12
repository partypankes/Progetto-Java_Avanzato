package gruppo2.service;

import gruppo2.Document;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.*;
import javafx.scene.layout.AnchorPane;

public class DocumentContentService extends Service<Void> {
    private Document document;
    private AnchorPane pane2;
    private AnchorPane paneDocumento;
    private Label documentTitleLabel;
    private TextArea corpoDocumento;

    public DocumentContentService(Document document, AnchorPane pane2, AnchorPane paneDocumento, Label documentTitleLabel, TextArea corpoDocumento) {
        this.document = document;
        this.pane2 = pane2;
        this.paneDocumento = paneDocumento;
        this.documentTitleLabel = documentTitleLabel;
        this.corpoDocumento = corpoDocumento;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<>() {
            @Override
            protected Void call() throws Exception {
                // Carica il contenuto del documento su un thread separato
                String documentText = document.getDocument_text();
                String documentTitle = document.getTitle();

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
