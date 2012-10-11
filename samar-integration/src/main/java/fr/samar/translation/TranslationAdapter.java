package fr.samar.translation;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.model.PropertyException;

public interface TranslationAdapter {

    public static final String DC_LANGUAGE = "dc:language";

    public static final String HAS_TRANSLATION = "HasTranslation";

    public static final String CATEGORY_TRANSLATION = "translation";

    public static final String TRANSLATED_FIELDS = "translation:fields";

    public static final String PROPERTY_PATH = "propertyPath";

    public static final String TEXT = "text";

    public static final String IS_FORMATTED = "isFormatted";

    TranslationTask getTranslationTask() throws PropertyException,
            ClientException;

    void setTranslationResults(TranslationTask results)
            throws PropertyException, ClientException;

}
