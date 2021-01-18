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

package org.dromara.soul.admin.service;

import com.google.common.collect.Lists;
import org.dromara.soul.admin.entity.PluginDO;
import org.dromara.soul.admin.entity.SelectorDO;
import org.dromara.soul.admin.listener.DataChangedEvent;
import org.dromara.soul.admin.mapper.PluginMapper;
import org.dromara.soul.admin.mapper.SelectorConditionMapper;
import org.dromara.soul.admin.mapper.SelectorMapper;
import org.dromara.soul.admin.query.SelectorConditionQuery;
import org.dromara.soul.admin.service.impl.UpstreamCheckService;
import org.dromara.soul.common.dto.SelectorData;
import org.dromara.soul.common.dto.convert.DivideUpstream;
import org.dromara.soul.common.enums.PluginEnum;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * Test cases for UpstreamCheckService.
 *
 * @author zendwang
 */
@RunWith(MockitoJUnitRunner.class)
public final class UpstreamCheckServiceTest {

    private static final String MOCK_SELECTOR_NAME = "mockSelectorName";

    private static final String MOCK_SELECTOR_NAME_2 = "mockSelectorName2";

    private static final String MOCK_SELECTOR_NAME_OTHER = "mockSelectorNameOther";

    private static final String MOCK_PLUGIN_ID = "mockPluginId";

    @InjectMocks
    private UpstreamCheckService upstreamCheckService;

    @Mock
    private SelectorMapper selectorMapper;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private PluginMapper pluginMapper;

    @Mock
    private SelectorConditionMapper selectorConditionMapper;

    private Map<String, List<DivideUpstream>> upstreamMap;

    @Before
    public void setUp() {
        ReflectionTestUtils.setField(upstreamCheckService, "check", Boolean.FALSE);
        ReflectionTestUtils.setField(upstreamCheckService, "scheduledTime", 10);
        //get static variable reference by reflection
        upstreamMap = (Map<String, List<DivideUpstream>>) ReflectionTestUtils.getField(UpstreamCheckService.class, "UPSTREAM_MAP");
        //mock data
        final PluginDO pluginDO = PluginDO.builder()
                .name(PluginEnum.DIVIDE.getName())
                .id(MOCK_PLUGIN_ID)
                .build();
        final SelectorDO selectorDOWithUrlError = SelectorDO.builder()
                .pluginId(MOCK_PLUGIN_ID)
                .name(MOCK_SELECTOR_NAME)
                .handle("[{\"upstreamHost\":\"localhost\",\"protocol\":\"http://\",\"upstreamUrl\":\"divide-upstream-50\",\"weight\":50}]")
                .build();
        final SelectorDO selectorDOWithUrlReachable = SelectorDO.builder()
                .pluginId(MOCK_PLUGIN_ID)
                .name(MOCK_SELECTOR_NAME_OTHER)
                .handle("[{\"upstreamHost\":\"localhost\",\"protocol\":\"http://\",\"localhost\":\"divide-upstream-60\",\"weight\":60}]")
                .build();
        final SelectorData selectorDataWithUrlError = SelectorData.builder()
                .name(MOCK_SELECTOR_NAME)
                .build();
        final SelectorData selectorDataWithUrlReachable = SelectorData.builder()
                .name(MOCK_SELECTOR_NAME)
                .build();
        //stubbing
        when(pluginMapper.selectByName(anyString())).thenReturn(pluginDO);
        when(pluginMapper.selectById(anyString())).thenReturn(pluginDO);
        when(selectorMapper.findByPluginId(anyString())).thenReturn(Lists.newArrayList(selectorDOWithUrlError, selectorDOWithUrlReachable));
        when(selectorMapper.updateSelective(any(SelectorDO.class))).thenReturn(1);
        when(selectorMapper.selectByName(anyString())).then(invocationOnMock -> {
            Object[] args = invocationOnMock.getArguments();
            if (MOCK_SELECTOR_NAME.equals(args[0])) {
                return selectorDOWithUrlError;
            } else if (MOCK_SELECTOR_NAME_OTHER.equals(args[0])) {
                return selectorDOWithUrlReachable;
            }
            return null;
        });
        when(selectorConditionMapper.selectByQuery(any(SelectorConditionQuery.class))).thenReturn(Collections.emptyList());
        doNothing().when(eventPublisher).publishEvent(any(DataChangedEvent.class));
        //spring bean creation lifecycle phase:post construct.
        upstreamCheckService.setup();
    }

    @Test
    public void testRemoveByKey() {
        UpstreamCheckService.removeByKey(MOCK_SELECTOR_NAME);
        Assert.assertFalse(upstreamMap.containsKey(MOCK_SELECTOR_NAME));
    }

    @Test
    public void testSubmitWhenSelectorNameExists() {
        final DivideUpstream divideUpstream = DivideUpstream.builder()
                .upstreamHost("localhost")
                .protocol("http://")
                .upstreamUrl("divide-upstream-60")
                .weight(60)
                .build();
        upstreamCheckService.submit(MOCK_SELECTOR_NAME, divideUpstream);
        Assert.assertEquals(2, upstreamMap.get(MOCK_SELECTOR_NAME).size());
    }

    @Test
    public void testSubmitWhenSelectorNameNotExists() {
        final DivideUpstream divideUpstream = DivideUpstream.builder()
                .upstreamUrl("divide-upstream-60")
                .weight(60)
                .build();
        upstreamCheckService.submit(MOCK_SELECTOR_NAME_OTHER, divideUpstream);
        Assert.assertTrue(upstreamMap.containsKey(MOCK_SELECTOR_NAME_OTHER));
    }

    @Test
    public void testReplace() {
        final DivideUpstream divideUpstream = DivideUpstream.builder()
                .upstreamHost("localhost")
                .build();
        final DivideUpstream divideUpstream2 = DivideUpstream.builder()
                .upstreamHost("localhost2")
                .build();
        upstreamCheckService.submit(MOCK_SELECTOR_NAME_2, divideUpstream);
        upstreamCheckService.replace(MOCK_SELECTOR_NAME_2, Collections.singletonList(divideUpstream2));
        Assert.assertEquals(1, upstreamMap.get(MOCK_SELECTOR_NAME_2).size());
        Assert.assertEquals("localhost2", upstreamMap.get(MOCK_SELECTOR_NAME_2).get(0).getUpstreamHost());
    }

    @Test
    public void testScheduled() {
        ReflectionTestUtils.invokeMethod(upstreamCheckService, "scheduled");
        Assert.assertFalse(upstreamMap.containsKey(MOCK_SELECTOR_NAME));
    }
}
