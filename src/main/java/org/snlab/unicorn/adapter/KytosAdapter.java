package org.snlab.unicorn.adapter;

import java.util.List;
import org.snlab.unicorn.model.odl.PathQueryResponse;
import org.snlab.unicorn.model.odl.QueryDesc;
import org.snlab.unicorn.model.odl.ResourceQueryResponse;

public class KytosAdapter implements ControllerAdapter {

    @Override
    public PathQueryResponse getAsPath(List<QueryDesc> querySet) {
        // TODO: Implement stateless unicorn path query API.
        return null;
    }

    @Override
    public ResourceQueryResponse getResource(List<QueryDesc> querySet) {
        // TODO: Implement stateless unicorn resource query API.
        return null;
    }

    @Override
    public void addAsPathListener() {
        // TODO: Inject a callback. Once receiving as path changed notification, call the method.
    }

    @Override
    public void addResourceListener() {
        // TODO: Inject a callback. Once receiving resource changed notification, call the method.
    }
}