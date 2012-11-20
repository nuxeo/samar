package fr.samar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.nuxeo.common.utils.StringUtils;
import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.semanticentities.LocalEntityService;
import org.nuxeo.ecm.platform.semanticentities.adapter.OccurrenceRelation;
import org.nuxeo.ecm.platform.ui.web.util.BaseURL;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;
import org.nuxeo.runtime.api.Framework;

/**
 * The main entry point for the users to query and browse the document base.
 */
@Path("/samar")
@Produces("text/html;charset=UTF-8")
@WebObject(type = "SamarRoot")
public class SamarRoot extends ModuleRoot {

    ObjectMapper mapper = new ObjectMapper();

    String userInput;

    CoreSession session;

    List<DocumentModel> entities = new ArrayList<DocumentModel>();

    List<AnnotatedResult> results = new ArrayList<AnnotatedResult>();

    protected long durationMilliseconds;

    protected LocalEntityService entityService;

    protected final String baseURL;

    public SamarRoot(@QueryParam("q")
    String userInput, @QueryParam("entity")
    List<String> entityIds, @QueryParam("type")
    List<String> types, @Context
    HttpServletRequest request, @Context
    UriInfo uriInfo) throws ClientException {
        long start = System.currentTimeMillis();
        entityService = Framework.getLocalService(LocalEntityService.class);
        session = SessionFactory.getSession(request);
        List<String> validEntityIds = new ArrayList<String>();
        for (String entityId : entityIds) {
            IdRef entityRef = new IdRef(entityId);
            if (session.exists(entityRef)) {
                entities.add(session.getDocument(entityRef));
                validEntityIds.add(entityId);
            }
        }
        if (userInput == null) {
            userInput = "";
        }
        if (types.isEmpty()) {
            types = Arrays.asList("NewsML", "Video");
        }
        this.userInput = userInput;
        this.baseURL = BaseURL.getBaseURL(request);
        String sanitizedInput = NXQLQueryBuilder.sanitizeFulltextInput(userInput);
        // TODO: escape special chars in type and entitiy ids inputs as well
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT * FROM Document WHERE ");
        if (!sanitizedInput.isEmpty()) {
            sb.append(String.format("ecm:fulltext LIKE '%s'", sanitizedInput));
            sb.append(" AND ");
        }
        for (String validEntityId : validEntityIds) {
            sb.append(String.format("semantics:entities = '%s'", validEntityId));
            sb.append(" AND ");
        }
        sb.append(String.format("ecm:primaryType IN ('%s')",
                StringUtils.join(types, "', '")));
        sb.append(" AND ");
        sb.append("ecm:mixinType != 'HiddenInNavigation'");
        sb.append(" AND ");
        sb.append("ecm:isCheckedInVersion = 0");
        sb.append(" AND ");
        sb.append("ecm:currentLifeCycleState != 'deleted'");
        sb.append(" LIMIT 30");
        String query = sb.toString();
        DocumentModelList documents = session.query(query, 10);
        for (DocumentModel doc : documents) {
            PageProvider<DocumentModel> allEntities = entityService.getRelatedEntities(
                    session, doc.getRef(), null);

            AnnotatedResult result = new AnnotatedResult(doc, uriInfo, baseURL);
            for (DocumentModel entity : allEntities.getCurrentPage()) {
                OccurrenceRelation occurrence = entityService.getOccurrenceRelation(
                        session, doc.getRef(), entity.getRef());
                if (occurrence != null) {
                    result.addOccurrence(occurrence);
                }
            }
            results.add(result);
        }
        durationMilliseconds = System.currentTimeMillis() - start;
    }

    @GET
    public Object index() {
        return getView("index");
    }

    @GET
    @Path("/suggest")
    @Produces("application/json")
    public String suggestConcept(@QueryParam("term")
    String term) throws JsonGenerationException, JsonMappingException,
            IOException, ClientException {
        List<Map<String, String>> suggestions = new ArrayList<Map<String, String>>();
        if (term != null && !term.isEmpty()) {
            String sanitizedTerm = NXQLQueryBuilder.sanitizeFulltextInput(term);
            String normalizedName = entityService.normalizeName(sanitizedTerm);
            if (!sanitizedTerm.isEmpty()) {
                StringBuffer sb = new StringBuffer();
                sb.append("SELECT * FROM Entity WHERE (");
                sb.append(String.format("ecm:fulltext.dc:title LIKE '%s*'",
                        normalizedName));
                sb.append(" OR ");
                sb.append(String.format("entity:normalizednames/* LIKE '%s%%'",
                        normalizedName));
                sb.append(") AND ");
                sb.append("ecm:mixinType != 'HiddenInNavigation'");
                sb.append(" AND ");
                sb.append("ecm:isCheckedInVersion = 0");
                sb.append(" AND ");
                sb.append("ecm:currentLifeCycleState != 'deleted'");
                sb.append(" LIMIT 10");
                String query = sb.toString();
                DocumentModelList entities = session.query(query, 10);
                for (DocumentModel entity : entities) {
                    Map<String, String> suggestion = new LinkedHashMap<String, String>();
                    suggestion.put("id", entity.getId());
                    // TODO: i18n instead
                    suggestion.put("label", entity.getTitle());
                    suggestion.put("value", entity.getTitle());
                    suggestion.put("summary", entity.getTitle());
                    suggestion.put(
                            "entityFormSelectionHTML",
                            getTemplate("entityFormSelection.ftl").arg(
                                    "entity", entity).render());
                    suggestion.put(
                            "entityFacetHTML",
                            getTemplate("entityFacet.ftl").arg("entity", entity).render());
                    suggestions.add(suggestion);
                }
            }
        }
        return mapper.writeValueAsString(suggestions);
    }

    @GET
    @Path("/samar/suggest")
    @Produces("application/json")
    // For some reason, the base URL is not always the same...
    public String suggestConceptHack(@QueryParam("term")
    String term) throws JsonGenerationException, JsonMappingException,
            IOException, ClientException {
        return suggestConcept(term);
    }

    public String getUserInput() {
        return userInput;
    }

    public List<DocumentModel> getEntities() {
        return entities;
    }

    public List<AnnotatedResult> getResults() {
        return results;
    }

    public String getCurrentQueryUrl() {
        UriBuilder b = uriInfo.getAbsolutePathBuilder();
        b.queryParam("q", userInput);
        for (DocumentModel entity : entities) {
            b.queryParam("entity", entity.getId());
        }
        return b.build().toASCIIString();
    }

    public Double getDuration() {
        return durationMilliseconds / 1000.;
    }

    @GET
    @Path("/normalizeEntityNames")
    @Produces("application/json")
    // Migration utility for entity name normalization
    public String normalizeEntityNames(@QueryParam(value = "forceUpdate")
    Boolean forceUpdate) throws JsonGenerationException, JsonMappingException,
            IOException, ClientException {
        String query = "SELECT * FROM Entity";
        int updatedCount = 0;
        int totalCount = 0;
        int last = 0;
        int batchSize = 1000;
        boolean shouldStop = false;
        if (forceUpdate == null) {
            forceUpdate = false;
        }
        while (!shouldStop) {
            DocumentModelList batch = session.query(query, null, batchSize,
                    last, false);
            for (DocumentModel entity : batch) {
                if (entityService.updateNormalizedNames(entity, forceUpdate)) {
                    updatedCount += 1;
                }
                totalCount += 1;
                session.saveDocument(entity);
                session.save();
            }
            shouldStop = batch.size() < batchSize;
            last += batch.size();
        }
        return String.format(
                "Normalized names of %d / %d entities in the repository.",
                updatedCount, totalCount);
    }

    public String getBaseURL() {
        return baseURL;
    }

    public String bigFileUrl(DocumentModel doc, String blobPropertyName, String filename) {
        return URLHelper.bigFileUrl(doc, baseURL, blobPropertyName, filename);
    }

    public String getBackofficeURL(DocumentModel doc) {
        return URLHelper.documentUrl(doc, baseURL);
    }

    public String joinNames(List<String> names) {
        if (names == null) {
            return "";
        }
        return StringUtils.join(names, ", ");
    }
}
