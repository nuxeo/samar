/**
 * 
 */

package fr.samar;

import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;

import org.nuxeo.ecm.core.api.ClientException;
import org.nuxeo.ecm.core.api.CoreSession;
import org.nuxeo.ecm.core.api.DocumentModel;
import org.nuxeo.ecm.core.api.DocumentModelList;
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.api.PageProvider;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.platform.semanticentities.LocalEntityService;
import org.nuxeo.ecm.platform.semanticentities.adapter.OccurrenceRelation;
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

    String userInput;
    
    CoreSession session;

    List<DocumentModel> entities = new ArrayList<DocumentModel>();

    List<AnnotatedResult> results = new ArrayList<AnnotatedResult>();

    protected long durationMilliseconds;

    public SamarRoot(@QueryParam("q") String userInput, @QueryParam("entity") List<String> entityIds,
            @Context HttpServletRequest request) throws ClientException {
        long start = System.currentTimeMillis();
        LocalEntityService entityService = Framework.getLocalService(LocalEntityService.class);
        this.userInput = userInput;
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
        userInput = NXQLQueryBuilder.sanitizeFulltextInput(userInput);
        StringBuffer sb = new StringBuffer();
        sb.append("SELECT * FROM Document WHERE ");
        if (!userInput.trim().isEmpty()) {
            sb.append(String.format("ecm:fulltext LIKE '%s'", userInput));
            sb.append(" AND ");
        }
        for (String validEntityId : validEntityIds) {
            sb.append(String.format("semantics:entities = '%s'", validEntityId));
            sb.append(" AND ");
        }
        sb.append("ecm:primaryType IN ('NewsML', 'Video')");
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
            AnnotatedResult result = new AnnotatedResult(doc);
            for (DocumentModel entity: allEntities.getCurrentPage()) {
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

    public String getUserInput() {
        return userInput;
    }

    public List<DocumentModel> getEntities() {
        return entities;
    }

    public List<AnnotatedResult> getResults() {
        return results;
    }

    public String getBaseUrl() {
        return uriInfo.getAbsolutePathBuilder().build().toASCIIString();
    }

    public Double getDuration() {
        return durationMilliseconds / 1000.;
    }
}
