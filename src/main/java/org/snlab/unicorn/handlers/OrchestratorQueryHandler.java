package org.snlab.unicorn.handlers;


import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonObject;
import javax.json.JsonReader;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.sse.Sse;
import javax.ws.rs.sse.SseEventSink;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.snlab.unicorn.adapter.ControllerAdapter;
import org.snlab.unicorn.dataprovider.QueryDataProvider;
import org.snlab.unicorn.exceptions.UnknownProtocol;
import org.snlab.unicorn.exceptions.UnknownQueryAction;
import org.snlab.unicorn.exceptions.UnknownQueryType;
import org.snlab.unicorn.model.Flow;
import org.snlab.unicorn.model.PathQueryResponseBody;
import org.snlab.unicorn.model.Query;
import org.snlab.unicorn.model.QueryItem;
import org.snlab.unicorn.model.ResourceQueryResponseBody;

public class OrchestratorQueryHandler {
    private final static Logger LOG = LoggerFactory.getLogger(OrchestratorQueryHandler.class);

    private static Map<String, OrchestratorQueryHandler> handlerMap = new HashMap<>();
    private static ObjectMapper mapper = new ObjectMapper();
    private String identifier;
    private ControllerAdapter adapter;
    private Set<String> queryIds = new HashSet<>();
    private Thread pathQueryLoop;
    private Thread resourceQueryLoop;
    private Queue<String> requiredQueryIds = new LinkedList<>();

    private OrchestratorQueryHandler(String identifier, ControllerAdapter adapter) {
        this.identifier = identifier;
        this.adapter = adapter;
    }

    public static boolean setHandler(String identifier, ControllerAdapter adapter) {
        boolean result = true;
        if (handlerMap.containsKey(identifier)) {
            LOG.info("Identifier is existing. Cannot put a new handler.");
            result = false;
        } else {
            handlerMap.put(identifier, new OrchestratorQueryHandler(identifier, adapter));
        }
        return result;
    }

    public static OrchestratorQueryHandler getHandler(String identifier) {
        return handlerMap.get(identifier);
    }

    public static Collection<OrchestratorQueryHandler> getAllHandlers() {
        return handlerMap.values();
    }

    private Query parseBody(String body) throws UnknownQueryAction, UnknownQueryType, UnknownProtocol {
        JsonReader jsonReader = Json.createReader(new StringReader(body));
        JsonObject object = jsonReader.readObject();
        Query query = new Query();

        // Get query id
        String queryId = object.getString("query-id");

        // Get query action
        String actionString = object.getString("action");
        Query.QueryAction action;
        switch (actionString) {
            case "add":
                action = Query.QueryAction.ADD;
                break;
            case "delete":
                action = Query.QueryAction.DELETE;
                break;
            case "merge":
                action = Query.QueryAction.MERGE;
                break;
            case "erase":
                action = Query.QueryAction.ERASE;
                break;
            case "new":
                action = Query.QueryAction.NEW;
                break;
            default:
                throw new UnknownQueryAction();
        }

        // Get query type
        String queryTypeString = object.getString("query-type");
        Query.QueryType queryType;
        switch (queryTypeString) {
            case "path-query":
                queryType = Query.QueryType.PATH_QUERY;
                break;
            case "resource-query":
                queryType = Query.QueryType.RESOURCE_QUERY;
                break;
            default:
                throw new UnknownQueryType();
        }

        // Get query items
        List<QueryItem> queryItems = new ArrayList<>();
        JsonArray queryDesc = object.getJsonArray("query-desc");
        for (JsonObject queryItem : queryDesc.getValuesAs(JsonObject.class)) {
            QueryItem item = new QueryItem();
            Flow flow = new Flow();
            flow.setFlowId(queryItem.getString("flow-id"));
            if (queryItem.containsKey("src-ip"))
                flow.setSrcIp(queryItem.getString("src-ip"));
            if (queryItem.containsKey("dst-ip"))
                flow.setDstIp(queryItem.getString("dst-ip"));
            if (queryItem.containsKey("dst-port"))
                flow.setDstPort(queryItem.getString("dst-port"));
            String protocol = queryItem.getString("protocol");
            switch (protocol) {
                case "tcp":
                    flow.setProtocol(Flow.Protocol.TCP);
                    break;
                case "udp":
                    flow.setProtocol(Flow.Protocol.UDP);
                    break;
                case "sctp":
                    flow.setProtocol(Flow.Protocol.SCTP);
                    break;
                default:
                    throw new UnknownProtocol();
            }
            item.setFlow(flow);
            item.setIngressPoint(queryItem.getString("ingress-point"));
            queryItems.add(item);
        }

        // Set all fileds
        query.setQueryId(queryId);
        query.setQueryDesc(queryItems);
        query.setAction(action);
        query.setQueryType(queryType);

        return query.manipulate();
    }

    public String handle(String body) {
        Query query;
        List<QueryItem> items;
        try {
            query = parseBody(body);
            items = query.getQueryDesc();
        } catch (UnknownQueryAction | UnknownProtocol | UnknownQueryType e) {
            LOG.error("Invalid query body:", e);
            return "{\"meta\": { \"code\": \"Unknown type\"}}";
        }
        queryIds.add(query.getQueryId());
        if (items.size() == 0){
            return "{\"meta\": { \"code\": \"success\"}}";
        }

        requireQuery(query.getQueryId());
        return "{\"meta\": { \"code\": \"success\"}}";
    }

    /**
     * Play a query record with controller adapter by a given query id.
     * @param id the query id.
     * @return the query result from controller adapter.
     */
    public String doQuery(String id) {
        Query query = QueryDataProvider.getInstance().getQuery(id);
        String result = "{\"meta\": { \"code\": \"Unknown error\"}}";
        switch (query.getQueryType()) {
            case PATH_QUERY:
                result = doPathQuery(query, result);
                break;
            case RESOURCE_QUERY:
                result = doResourceQuery(query, result);
        }
        return result;
    }

    private String doPathQuery(Query query, String defaultResult) {
        PathQueryResponseBody body = adapter.getAsPath(query.getQueryDesc());
        body.setQueryId(query.getQueryId());
        try {
            defaultResult = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            LOG.error("Invalid json string:", e);
        }
        return defaultResult;
    }

    private String doResourceQuery(Query query, String defaultResult) {
        ResourceQueryResponseBody body = adapter.getResource(query.getQueryDesc());
        body.setQueryId(query.getQueryId());
        try {
            defaultResult = mapper.writeValueAsString(body);
        } catch (JsonProcessingException e) {
            LOG.error("Invalid json string:", e);
        }
        return defaultResult;
    }

    private void requireQuery(String id) {
        // TODO: unsafe without lock
        requiredQueryIds.add(id);
    }

    private Set<String> getRequiredQueries(Set<String> queries) {
        // TODO: unsafe without lock
        while (!requiredQueryIds.isEmpty()) {
            queries.add(requiredQueryIds.poll());
        }
        return queries;
    }

    public void loopForQueryUpdate(SseEventSink eventSink, Sse sse) {
        if (pathQueryLoop == null) {
            pathQueryLoop = new Thread(() -> {
                Set<String> currentQueryIds = new HashSet<>();
                while (true) {
                    currentQueryIds.clear();
                    getRequiredQueries(currentQueryIds);
                    if (adapter.ifAsPathChangedThenCleanState()) {
                        LOG.debug("As path data changed. Replay all requests.");
                        currentQueryIds.addAll(queryIds);
                    }
                    if (adapter.ifResourceChangedThenCleanState()) {
                        LOG.debug("Resource data changed. Replay all requests.");
                        currentQueryIds.addAll(queryIds);
                    }
                    for (String id : currentQueryIds) {
                        String pathQueryResponse = doQuery(id);
                        eventSink.send(sse.newEventBuilder()
                                .name(MediaType.APPLICATION_JSON)
                                .data(pathQueryResponse)
                                .build());
                    }
                }
            });
            pathQueryLoop.start();
        } else {
            LOG.info("The path query loop has been running. [handler: {}]", identifier);
        }
    }

    public void stop() {
        if (pathQueryLoop != null) {
            pathQueryLoop.interrupt();
            pathQueryLoop = null;
        }
        if (resourceQueryLoop != null) {
            resourceQueryLoop.interrupt();
            resourceQueryLoop = null;
        }
    }
}
