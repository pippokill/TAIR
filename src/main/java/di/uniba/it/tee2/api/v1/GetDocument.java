/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.api.v1;

import di.uniba.it.tee2.api.SearchServiceWrapper;
import di.uniba.it.tee2.api.ServerConfig;
import di.uniba.it.tee2.search.SearchResult;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;
import org.apache.lucene.document.Document;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author pierpaolo
 */
@Path("v1/doc/{docid}")
public class GetDocument {

    @GET
    public Response search(@PathParam("docid") String docid) {
        try {
            SearchServiceWrapper instance = SearchServiceWrapper.getInstance(ServerConfig.getInstance().getProperty("search.language"),
                    ServerConfig.getInstance().getProperty("search.index"));
            Document document = instance.getSearch().getDocument(docid);
            JSONObject json = new JSONObject();
            json.put("id", document.get("id"));
            json.put("title", document.get("title"));
            json.put("content", document.get("content"));
            return Response.ok(json.toString()).build();
        } catch (Exception ex) {
            Logger.getLogger(GetDocument.class.getName()).log(Level.SEVERE, null, ex);
            return Response.serverError().build();
        }
    }
}
