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
import org.nuxeo.ecm.core.api.IdRef;
import org.nuxeo.ecm.platform.query.nxql.NXQLQueryBuilder;
import org.nuxeo.ecm.webengine.jaxrs.session.SessionFactory;
import org.nuxeo.ecm.webengine.model.WebObject;
import org.nuxeo.ecm.webengine.model.impl.ModuleRoot;

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

    List<DocumentModel> documents = new ArrayList<DocumentModel>();

    public SamarRoot(@QueryParam("q") String userInput, @QueryParam("entity") List<String> entityIds,
            @Context HttpServletRequest request) throws ClientException {
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
        userInput = NXQLQueryBuilder.sanitizeFulltextInput(userInput);
        if (!userInput.trim().isEmpty() || !validEntityIds.isEmpty()) {
            StringBuffer sb = new StringBuffer();
            sb.append("SELECT * FROM Document WHERE ");
            if (!userInput.trim().isEmpty()) {
                sb.append(String.format("ecm:fulltext.dc:title LIKE '%s'", userInput));
                sb.append(" AND ");
            }
            for (String validEntityId: validEntityIds) {
                sb.append(String.format("semantic:entities = '%s'", validEntityId));
                sb.append(" AND ");
            }
            sb.append("ecm:mixinType != 'HiddenInNavigation'");
            sb.append(" AND ");
            sb.append("ecm:isCheckedInVersion = 0");
            sb.append(" AND ");
            sb.append("ecm:currentLifeCycleState != 'deleted'");
            sb.append(" LIMIT 30");
            documents.addAll(session.query(sb.toString()));
        }
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

    public List<DocumentModel> getDocuments() {
        return documents;
    }

    public String getBaseUrl() {
        return uriInfo.getAbsolutePathBuilder().path(SamarRoot.class).toString();
    }

}
