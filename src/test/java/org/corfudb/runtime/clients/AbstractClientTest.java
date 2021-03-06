package org.corfudb.runtime.clients;

import com.google.common.collect.ImmutableMap;
import lombok.Getter;
import org.corfudb.AbstractCorfuTest;
import org.corfudb.infrastructure.AbstractServer;
import org.corfudb.infrastructure.TestServerRouter;
import org.junit.Before;

import java.util.Map;
import java.util.Set;

/**
 * Created by mwei on 12/13/15.
 */
public abstract class AbstractClientTest extends AbstractCorfuTest {

    @Getter
    TestClientRouter router;

    @Getter
    TestServerRouter serverRouter;

    @Before
    public void resetTest() {
        serverRouter = new TestServerRouter();
        router = new TestClientRouter(serverRouter);
        getServersForTest().stream().forEach(serverRouter::addServer);
        getClientsForTest().stream().forEach(router::addClient);
    }

    abstract Set<AbstractServer> getServersForTest();

    abstract Set<IClient> getClientsForTest();

    public Map<String, Object> defaultOptionsMap() {
        return new ImmutableMap.Builder<String, Object>()
                .put("--initial-token", "0")
                .put("--memory", true)
                .put("--single", false)
                .put("--max-cache", "256M")
                .put("--sync", false)
                .build();
    }
}
