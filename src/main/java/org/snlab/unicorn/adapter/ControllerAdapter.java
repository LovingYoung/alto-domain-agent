package org.snlab.unicorn.adapter;

import java.util.List;

import org.snlab.unicorn.model.odl.PathQueryResponse;
import org.snlab.unicorn.model.odl.QueryDesc;
import org.snlab.unicorn.model.odl.ResourceQueryResponse;


/**
 * This is a general interface to adapt different SDN os platforms / controllers.
 */
public interface ControllerAdapter {

    /**
     * Implements stateless path query API.
     * @param querySet a list of query descriptor
     * @return the response of path query
     */
    public PathQueryResponse getAsPath(List<QueryDesc> querySet);

    /**
     * Implements stateless resource query API.
     * @param querySet a list of query descriptor
     * @return the response of resource query
     */
    public ResourceQueryResponse getResource(List<QueryDesc> querySet);

    /**
     * Listen as path changes for stateful path query API.
     */
    public void addAsPathListener();

    /**
     * Listen resource changes for stateful resource query API.
     */
    public void addResourceListener();
}