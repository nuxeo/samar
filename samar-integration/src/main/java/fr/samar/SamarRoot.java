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

    public SamarRoot(@QueryParam("q") String userInput, @QueryParam("entity") List<String> entityIds,
            @Context HttpServletRequest request) throws ClientException {
        this.userInput = userInput;
        session = SessionFactory.getSession(request);
        if (session != null) {
            for (String entityId : entityIds) {
                IdRef entityRef = new IdRef(entityId);
                if (session.exists(entityRef)) {
                    entities.add(session.getDocument(entityRef));
                }
            }
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

    public String getBaseUrl() {
        return uriInfo.getAbsolutePathBuilder().path(SamarRoot.class).toString();
    }

}
