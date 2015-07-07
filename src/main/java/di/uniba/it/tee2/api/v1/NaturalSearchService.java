/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package di.uniba.it.tee2.api.v1;

import di.uniba.it.tee2.api.SearchServiceWrapper;
import di.uniba.it.tee2.api.ServerConfig;
import di.uniba.it.tee2.search.SearchResult;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;

/**
 *
 * @author pierpaolo
 */
@Path("v1/search/natural")
public class NaturalSearchService {

    @GET
    public Response search(@QueryParam("contextQuery") String query, @QueryParam("timeQuery") String timeQuery, @QueryParam("n") int n) {
        try {
            SearchServiceWrapper instance = SearchServiceWrapper.getInstance(ServerConfig.getInstance().getProperty("search.language"),
                    ServerConfig.getInstance().getProperty("search.index"));
            List<SearchResult> search = instance.getSearch().naturalSearch(query, timeQuery, n);
            JSONObject json = new JSONObject();
            json.put("size", search.size());
            JSONArray results = new JSONArray();
            for (SearchResult sr : search) {
                results.add(sr.toJSON());
            }
            json.put("results", results);
            return Response.ok(json.toString()).build();
        } catch (Exception ex) {
            Logger.getLogger(NaturalSearchService.class.getName()).log(Level.SEVERE, null, ex);
            return Response.serverError().build();
        }
    }
}
