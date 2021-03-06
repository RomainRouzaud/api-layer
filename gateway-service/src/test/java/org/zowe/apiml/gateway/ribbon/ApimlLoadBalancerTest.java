/*
 * This program and the accompanying materials are made available under the terms of the
 * Eclipse Public License v2.0 which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-v20.html
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Copyright Contributors to the Zowe Project.
 */

package org.zowe.apiml.gateway.ribbon;

import com.netflix.appinfo.InstanceInfo;
import com.netflix.client.config.IClientConfig;
import com.netflix.loadbalancer.*;
import com.netflix.niws.loadbalancer.DiscoveryEnabledServer;
import com.netflix.zuul.context.RequestContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.zowe.apiml.config.ribbon.IClientConfiguration;
import org.zowe.apiml.gateway.metadata.service.LoadBalancerRegistry;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
    IClientConfiguration.class
})
class ApimlLoadBalancerTest {

    @Autowired
    IClientConfig config;

    IRule rule = new RandomRule();
    IPing ping = new DummyPing();
    ServerListUpdater serverListUpdater = mock(ServerListUpdater.class);
    ServerList<Server> serverList = mock(ServerList.class);
    ServerListFilter<Server> serverListFilter = mock(ServerListFilter.class);
    LoadBalancerRegistry loadBalancerRegistry = mock(LoadBalancerRegistry.class);
    ApimlLoadBalancer underTest;

    @BeforeEach
    void setUp() {
        RequestContext.getCurrentContext().clear();
        underTest = new ApimlLoadBalancer(config, rule, ping, serverList,
            serverListFilter, serverListUpdater, loadBalancerRegistry);
    }

    @Test
    void givenNoServerList_whenChooseServer_thenSetNothing() {
        underTest.chooseServer("anotherInstance");

        RequestContext context = RequestContext.getCurrentContext();
        assertThat(context.get(ApimlLoadBalancer.LOADBALANCED_INSTANCE_INFO_KEY), is(nullValue()));
    }

    @Test
    void givenServerList_whenChooseServer_thenSetChosenInstanceInfoToRequestContext() {
        InstanceInfo info = InstanceInfo.Builder.newBuilder()
            .setAppName("appname")
            .setInstanceId("instance")
            .build();
        underTest.addServer(new DiscoveryEnabledServer(info, true));

        underTest.chooseServer("instance");

        RequestContext context = RequestContext.getCurrentContext();
        assertThat(context.get(ApimlLoadBalancer.LOADBALANCED_INSTANCE_INFO_KEY), is(info));
    }

    @Test
    void givenUnexpectedServerImplementation_whenChooseServer_thenFailFast() {
        underTest.addServer(new Server("localhost", 69));

        assertThrows(IllegalStateException.class, () -> underTest.chooseServer("instance"));
    }


}
