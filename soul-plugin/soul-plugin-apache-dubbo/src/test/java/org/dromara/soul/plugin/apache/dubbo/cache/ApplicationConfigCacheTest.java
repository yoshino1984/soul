/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dromara.soul.plugin.apache.dubbo.cache;

import lombok.SneakyThrows;
import org.dromara.soul.common.config.DubboRegisterConfig;
import org.dromara.soul.common.dto.MetaData;
import org.dromara.soul.common.enums.LoadBalanceEnum;
import org.dromara.soul.common.utils.GsonUtils;
import org.dromara.soul.plugin.apache.dubbo.cache.ApplicationConfigCache.DubboParamExtInfo;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.test.util.ReflectionTestUtils;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

/**
 * The Test Case For ApplicationConfigCache.
 *
 * @author nuo-promise
 */
@RunWith(MockitoJUnitRunner.class)
public final class ApplicationConfigCacheTest {

    private ApplicationConfigCache applicationConfigCache;

    @Before
    public void setUp() {
        applicationConfigCache = ApplicationConfigCache.getInstance();
    }

    @Test
    public void getInstance() {
        assertNotNull(this.applicationConfigCache);
    }

    @SneakyThrows
    @Test
    public void testGetSize() {
        assertNotNull(ReflectionTestUtils.invokeMethod(this.applicationConfigCache, "getSize"));
    }

    @Test
    public void testInit() {
        DubboRegisterConfig dubboRegisterConfig = mock(DubboRegisterConfig.class);
        this.applicationConfigCache.init(dubboRegisterConfig);
    }

    @Test
    public void testInitRef() {
        MetaData metaData = new MetaData();
        metaData.setPath("/test");
        assertNotNull(this.applicationConfigCache.initRef(metaData));
    }

    @Test
    public void testGet() {
        assertNotNull(this.applicationConfigCache.get("/test"));
    }

    @Test
    public void testBuild() {
        DubboParamExtInfo dubboParamExtInfo = new DubboParamExtInfo();
        dubboParamExtInfo.setVersion("2.7.5");
        dubboParamExtInfo.setGroup("Group");
        dubboParamExtInfo.setLoadbalance("Balance");
        dubboParamExtInfo.setUrl("http://192.168.55.113/dubbo");
        MetaData metaData = new MetaData();
        metaData.setRpcExt(GsonUtils.getInstance().toJson(dubboParamExtInfo));
        assertNotNull(this.applicationConfigCache.build(metaData));
    }

    @Test
    public void testInvalidate() {
        this.applicationConfigCache.invalidate("/test");
        this.applicationConfigCache.invalidateAll();
    }

    @SneakyThrows
    @Test
    public void testBuildLoadBalanceName() {
        assertThat(ReflectionTestUtils.invokeMethod(this.applicationConfigCache, "buildLoadBalanceName", LoadBalanceEnum.HASH.getName()), is("consistenthash"));
        assertThat(ReflectionTestUtils.invokeMethod(this.applicationConfigCache, "buildLoadBalanceName", LoadBalanceEnum.ROUND_ROBIN.getName()), is("roundrobin"));
    }
}
