package fr.samar.translation;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nuxeo.common.collections.ScopeType;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.DocumentLocation;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentRef;
import org.nuxeo.ecm.core.api.UnrestrictedSessionRunner;
import org.nuxeo.ecm.core.api.event.CoreEventConstants;
import org.nuxeo.ecm.core.api.event.DocumentEventCategories;
import org.nuxeo.ecm.core.event.Event;
import org.nuxeo.ecm.core.event.EventService;
import org.nuxeo.ecm.core.event.impl.DocumentEventContext;
import org.nuxeo.ecm.core.work.AbstractWork;
import org.nuxeo.ecm.platform.commandline.executor.api.CmdParameters;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandAvailability;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandLineExecutorService;
import org.nuxeo.ecm.platform.commandline.executor.api.CommandNotAvailable;
import org.nuxeo.ecm.platform.commandline.executor.api.ExecResult;
import org.nuxeo.runtime.api.Framework;
import org.nuxeo.runtime.transaction.TransactionHelper;

/**
 * Handle the asynchronous translation of document fields using document
 * adaptors to make it possible to override text content is to be extracted for
 * each document type.
 *
 * The translation process is handled by a command line executor that works out
 * of any transactional context to avoid transaction timeout issues.
 */
public class TranslationWork extends AbstractWork {

    private static final Log log = LogFactory.getLog(TranslationWork.class);

    protected static final String EVENT_TRANSLATION_COMPLETE = "TranslationComplete";

    private static final String CATEGORY_TRANSLATION = "Translation";

    protected final DocumentLocation docLoc;

    protected final Set<String> targetLanguages = new LinkedHashSet<String>();

    public TranslationWork(DocumentLocation docLoc) {
        this.docLoc = docLoc;
    }

    public TranslationWork withTargetLanguage(String language) {
        targetLanguages.add(language);
        return this;
    }

    @Override
    public String getTitle() {
        return String.format("Translation for: %s:%s", docLoc.getServerName(),
                docLoc.getDocRef());
    }

    @Override
    public String getCategory() {
        return CATEGORY_TRANSLATION;
    }

    @Override
    public void work() throws Exception {
        setProgress(Progress.PROGRESS_INDETERMINATE);
        TranslationTask task = makeTranslationTask();
        if (task == null) {
            // nothing to do
            return;
        }

        // Release the current transaction as the following calls will be very
        // long and won't need access to any persistent transactional resources
        if (isTransactional()) {
            TransactionHelper.commitOrRollbackTransaction();
        }
        File tempFolder = File.createTempFile("nuxeo_translation_", "_tmp");
        tempFolder.delete();
        tempFolder.mkdir();
        try {
            for (String targetLanguage : targetLanguages) {
                for (Map<String, Object> subTask : task.getFieldsToTranslate()) {
                    if (isSuspending()) {
                        return;
                    }
                    String translated = performTranslation(
                            task.getSourceLanguage(),
                            targetLanguage,
                            (String) subTask.get(TranslationAdapter.PROPERTY_PATH),
                            (String) subTask.get(TranslationAdapter.TEXT),
                            (Boolean) subTask.get(TranslationAdapter.IS_FORMATTED),
                            tempFolder);
                    Map<String, Object> translationResult = new HashMap<String, Object>();
                    translationResult.put(TranslationAdapter.TEXT, translated);
                    translationResult.put(TranslationAdapter.PROPERTY_PATH,
                            subTask.get(TranslationAdapter.PROPERTY_PATH));
                    translationResult.put(TranslationTask.LANGUAGE,
                            targetLanguage);
                    task.addTranslationResult(translationResult);
                }
            }
        } finally {
            //FileUtils.deleteQuietly(tempFolder);
        }

        // Save the results back on the document in a new, short-lived
        // transaction
        if (isTransactional()) {
            TransactionHelper.startTransaction();
        }
        setStatus("saving_results");
        saveResults(task);
    }

    protected TranslationTask makeTranslationTask() throws ClientException {
        final TranslationTask[] results = new TranslationTask[] { null };
        final DocumentRef docRef = docLoc.getDocRef();
        final String repositoryName = docLoc.getServerName();
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() throws ClientException {
                if (session.exists(docRef)) {
                    DocumentModel doc = session.getDocument(docRef);
                    log.debug(String.format(
                            "Collecting fields to translate for document '%s' on repository '%s'",
                            doc.getTitle(), repositoryName));
                    TranslationAdapter adapter = doc.getAdapter(
                            TranslationAdapter.class, true);
                    if (adapter != null) {
                        results[0] = adapter.getTranslationTask();
                    } else {
                        log.warn(String.format(
                                "Could not find translation adapter for '%s' with type '%s'.",
                                doc.getTitle(), doc.getType()));
                    }
                } else {
                    log.warn(String.format(
                            "Could not find document '%s' on repository '%s'",
                            docRef, repositoryName));
                }
            }
        }.runUnrestricted();
        return results[0];
    }

    protected String performTranslation(String sourceLanguage,
            String targetLanguage, String fieldName, String text,
            boolean isFormatted, File tempFolder) throws IOException,
            ClientException, CommandNotAvailable {
        setStatus("translating");
        if (text == null || text.trim().isEmpty()) {
            // Nothing to translate
            return "";
        }
        String format = isFormatted ? "xml" : "txt";
        String baseName = fieldName.replaceAll(":", "__");
        File sourceTextFile = new File(tempFolder, String.format("%s.%s",
                baseName, format));
        if (isFormatted) {
            // ensure that the input is a proper XML node by adding wrapper tag
            text = "<body>\n" + text + "\n</body>";
        }
        FileUtils.writeStringToFile(sourceTextFile, text, "UTF-8");

        String commandName = String.format("translate_%s_%s_%s",
                sourceLanguage, targetLanguage, format);
        CommandLineExecutorService executorService = Framework.getLocalService(CommandLineExecutorService.class);
        CommandAvailability ca = executorService.getCommandAvailability(commandName);
        if (!ca.isAvailable()) {
            if (ca.getInstallMessage() != null) {
                log.error(String.format("%s is not available: %s", commandName,
                        ca.getInstallMessage()));
            } else {
                log.error(ca.getErrorMessage());
            }
            return null;
        }
        CmdParameters params = new CmdParameters();
        params.addNamedParameter("inputFile", sourceTextFile);
        ExecResult execResult = executorService.execCommand(commandName, params);
        String textSample = text.substring(0, Math.min(40, text.length())) + "...";
        if (!execResult.isSuccessful()) {
            throw new RuntimeException(String.format(
                    "Error while executing '%s' on '%s':\n%s", commandName,
                    textSample, StringUtils.join(execResult.getOutput(), "\n")));
        } else {
            if (log.isTraceEnabled() && execResult != null) {
                log.trace(String.format("Output for command '%s' on '%s':\n%s",
                        commandName, textSample,
                        StringUtils.join(execResult.getOutput(), "\n")));
            }
        }
        File targetTextFile = new File(tempFolder, String.format("%s.%s.%s",
                baseName, targetLanguage, format));
        String output = FileUtils.readFileToString(targetTextFile, "UTF-8");
        if (isFormatted) {
            // remove the wrapper tag
            String[] lines = output.split("\n");
            lines = Arrays.copyOfRange(lines, 1, lines.length - 1);
            output = StringUtils.join(lines, "\n");
        }
        return output;
    }

    protected void saveResults(final TranslationTask task)
            throws ClientException {
        final DocumentRef docRef = docLoc.getDocRef();
        String repositoryName = docLoc.getServerName();
        new UnrestrictedSessionRunner(repositoryName) {
            @Override
            public void run() throws ClientException {
                if (session.exists(docRef)) {
                    DocumentModel doc = session.getDocument(docRef);
                    log.debug(String.format(
                            "Saving translation results for document '%s' on repository '%s'",
                            doc.getTitle(), repositoryName));
                    TranslationAdapter adapter = doc.getAdapter(
                            TranslationAdapter.class, true);
                    adapter.setTranslationResults(task);
                    String comment = "Automated translation from: "
                            + task.getSourceLanguage() + " to: "
                            + StringUtils.join(targetLanguages.toArray(), ", ");
                    doc.getContextData().putScopedValue(ScopeType.REQUEST,
                            "comment", comment);
                    session.saveDocument(doc);

                    // Notify transcription completion to make it possible to
                    // chain processing.
                    DocumentEventContext ctx = new DocumentEventContext(
                            session, getPrincipal(), doc);
                    ctx.setProperty(CoreEventConstants.REPOSITORY_NAME,
                            repositoryName);
                    ctx.setProperty(CoreEventConstants.SESSION_ID,
                            session.getSessionId());
                    ctx.setProperty("category",
                            DocumentEventCategories.EVENT_DOCUMENT_CATEGORY);
                    Event event = ctx.newEvent(EVENT_TRANSLATION_COMPLETE);
                    EventService eventService = Framework.getLocalService(EventService.class);
                    eventService.fireEvent(event);
                } else {
                    log.warn(String.format(
                            "Document '%s' on repository '%s' seems to have been deleted while translating.",
                            docRef, repositoryName));
                }
            }
        }.runUnrestricted();
    }

    @Override
    public int hashCode() {
        return docLoc.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TranslationWork other = (TranslationWork) obj;
        if (docLoc == null) {
            if (other.docLoc != null) {
                return false;
            }
        } else if (!docLoc.equals(other.docLoc)) {
            return false;
        }
        return true;
    }

}
