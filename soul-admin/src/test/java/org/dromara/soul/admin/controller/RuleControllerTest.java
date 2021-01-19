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

package org.dromara.soul.admin.controller;

import org.dromara.soul.admin.dto.RuleConditionDTO;
import org.dromara.soul.admin.dto.RuleDTO;
import org.dromara.soul.admin.page.CommonPager;
import org.dromara.soul.admin.page.PageParameter;
import org.dromara.soul.admin.query.RuleQuery;
import org.dromara.soul.admin.service.RuleService;
import org.dromara.soul.admin.utils.SoulResultMessage;
import org.dromara.soul.admin.vo.RuleConditionVO;
import org.dromara.soul.admin.vo.RuleVO;
import org.dromara.soul.common.utils.DateUtils;
import org.dromara.soul.common.utils.GsonUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import static org.hamcrest.core.Is.is;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Test case for RuleController.
 *
 * @author shijie666
 */
@RunWith(MockitoJUnitRunner.class)
public final class RuleControllerTest {

    private MockMvc mockMvc;

    @InjectMocks
    private RuleController ruleController;

    @Mock
    private RuleService ruleService;

    private final RuleConditionVO rCondition1 = new RuleConditionVO(
            "888", "666", "uri", "Uniform", "match", "match", "/", "/http/test/**", DateUtils.localDateTimeToString(LocalDateTime.now()), DateUtils.localDateTimeToString(LocalDateTime.now())
    );

    private final List<RuleConditionVO> rcList = new ArrayList<>(Collections.singletonList(rCondition1));

    private final RuleVO ruleVO = new RuleVO("666", "168", 0, "zero mode", "/http/test/**", true, true, 1, "{\"loadBalance\":\"random\",\"retry\":0,\"timeout\":3000}",
            rcList, DateUtils.localDateTimeToString(LocalDateTime.now()), DateUtils.localDateTimeToString(LocalDateTime.now()));

    private final PageParameter pageParameter = new PageParameter();

    private final RuleQuery tRuleQuery = new RuleQuery("168", pageParameter);

    private final CommonPager<RuleVO> commonPager = new CommonPager<>(new PageParameter(), Collections.singletonList(ruleVO));

    @Before
    public void setUp() {
        this.mockMvc = MockMvcBuilders.standaloneSetup(ruleController).build();
    }

    @Test
    public void testQueryRules() throws Exception {
        given(this.ruleService.listByPage(tRuleQuery)).willReturn(commonPager);
        String urlTemplate = "/rule?selectorId={selectorId}&currentPage={currentPage}&pageSize={pageSize}";
        this.mockMvc.perform(MockMvcRequestBuilders.get(urlTemplate, "168", 1, 12))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(SoulResultMessage.QUERY_SUCCESS)))
                .andExpect(jsonPath("$.data.dataList[0].id", is(ruleVO.getId())))
                .andReturn();
    }

    @Test
    public void testDetailRule() throws Exception {
        given(this.ruleService.findById("666")).willReturn(ruleVO);
        this.mockMvc.perform(MockMvcRequestBuilders.get("/rule/{id}", "666"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(SoulResultMessage.DETAIL_SUCCESS)))
                .andExpect(jsonPath("$.data.id", is(ruleVO.getId())))
                .andReturn();
    }

    @Test
    public void testCreateRule() throws Exception {
        RuleConditionDTO ruleConditionDTO = RuleConditionDTO.builder()
                .id("888")
                .ruleId("666")
                .paramType("uri")
                .operator("match")
                .paramName("/")
                .paramType("/http/order/save")
                .build();
        List<RuleConditionDTO> conList = new ArrayList<>();
        conList.add(ruleConditionDTO);

        RuleDTO ruleDTO = RuleDTO.builder()
                .id("666")
                .selectorId("168")
                .matchMode(0)
                .name("/http/order/save")
                .enabled(true)
                .loged(true)
                .sort(1)
                .handle("{\"loadBalance\":\"random\",\"retry\":0,\"timeout\":3000}")
                .ruleConditions(conList)
                .build();
        given(this.ruleService.createOrUpdate(ruleDTO)).willReturn(1);
        this.mockMvc.perform(MockMvcRequestBuilders.post("/rule", ruleDTO)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GsonUtils.getInstance().toJson(ruleDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(SoulResultMessage.CREATE_SUCCESS)))
                .andReturn();
    }

    @Test
    public void testUpdateRule() throws Exception {
        RuleConditionDTO ruleConditionDTO = RuleConditionDTO.builder()
                .id("888")
                .ruleId("666")
                .paramType("uri")
                .operator("match")
                .paramName("/")
                .paramValue("/http/order/update")
                .build();
        List<RuleConditionDTO> conList = new ArrayList<>();
        conList.add(ruleConditionDTO);

        RuleDTO ruleDTO = RuleDTO.builder()
                .id("666")
                .selectorId("168")
                .matchMode(0)
                .name("/http/order/update")
                .enabled(true)
                .loged(true)
                .sort(1)
                .handle("{\"loadBalance\":\"random\",\"retry\":0,\"timeout\":3000}")
                .ruleConditions(conList)
                .build();

        given(this.ruleService.createOrUpdate(ruleDTO)).willReturn(1);
        this.mockMvc.perform(MockMvcRequestBuilders.put("/rule/{id}", "666")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(GsonUtils.getInstance().toJson(ruleDTO))
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(SoulResultMessage.UPDATE_SUCCESS)))
                .andReturn();
    }

    @Test
    public void testDeleteRules() throws Exception {
        given(this.ruleService.delete(Collections.singletonList("111"))).willReturn(1);
        this.mockMvc.perform(MockMvcRequestBuilders.delete("/rule/batch")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("[\"111\"]")
                )
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message", is(SoulResultMessage.DELETE_SUCCESS)))
                .andReturn();
    }

}
