package org.corfudb.runtime.view;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.corfudb.runtime.CorfuRuntime;
import org.corfudb.runtime.object.CorfuProxyBuilder;
import org.corfudb.runtime.object.ISMRInterface;
import org.corfudb.util.serializer.Serializers;

import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Created by mwei on 4/6/16.
 */
@Accessors(chain = true)
@Data
public class ObjectBuilder<T> {

    final CorfuRuntime runtime;

    Class<T> type;
    @Setter
    Class<? extends ISMRInterface> overlay = null;
    @Setter
    UUID streamID;
    @Setter
    String streamName;
    @Setter
    Serializers.SerializerType serializer = Serializers.SerializerType.JSON;
    @Setter
    Set<ObjectOpenOptions> options = EnumSet.noneOf(ObjectOpenOptions.class);
    @Setter(AccessLevel.NONE)
    Object[] arguments = new Object[0];

    @SuppressWarnings("unchecked")
    public <R> ObjectBuilder<R> setType(Class<R> type) {
        this.type = (Class<T>) type;
        return (ObjectBuilder<R>) this;
    }

    public ObjectBuilder<T> addOption(ObjectOpenOptions option) {
        if (options == null) {
            options = EnumSet.noneOf(ObjectOpenOptions.class);
        }
        options.add(option);
        return this;
    }

    public ObjectBuilder<T> setArguments(Object... arguments) {
        this.arguments = arguments;
        return this;
    }

    public ObjectBuilder<T> setArgumentsArray(Object[] arguments) {
        this.arguments = arguments;
        return this;
    }

    @SuppressWarnings("unchecked")
    public T open() {

        if (streamName != null) {
            streamID = CorfuRuntime.getStreamID(streamName);
        }

        // CREATE_ONLY implies no cache
        if (options.contains(ObjectOpenOptions.NO_CACHE) || options.contains(ObjectOpenOptions.CREATE_ONLY)) {
            StreamView sv = runtime.getStreamsView().get(streamID);
            return CorfuProxyBuilder.getProxy(type, overlay, sv, runtime, serializer, options, arguments);
        }

        ObjectsView.ObjectID<T, ?> oid = new ObjectsView.ObjectID(streamID, type, overlay);
        return (T) runtime.getObjectsView().objectCache.computeIfAbsent(oid, x -> {
            StreamView sv = runtime.getStreamsView().get(streamID);
            return CorfuProxyBuilder.getProxy(type, overlay, sv, runtime, serializer, options, arguments);
        });
    }


}
