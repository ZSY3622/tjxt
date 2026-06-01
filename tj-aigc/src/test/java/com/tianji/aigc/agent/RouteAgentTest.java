package com.tianji.aigc.agent;

import cn.hutool.core.lang.Assert;
import com.tianji.AIGCApplication;
import com.tianji.aigc.enums.AgentTypeEnum;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = AIGCApplication.class)
public class RouteAgentTest {
    @Resource
    private RouteAgent routeAgent;
    @Test
    public void testChat(){
        Assert.equals(this.routeAgent.process("推荐课程", "2"), AgentTypeEnum.RECOMMEND.getAgentName());
        Assert.equals(this.routeAgent.process("下单购买这个课程", "2"), AgentTypeEnum.BUY.getAgentName());
        Assert.equals(this.routeAgent.process("这个课程是多少钱", "2"), AgentTypeEnum.CONSULT.getAgentName());
        Assert.equals(this.routeAgent.process("java是什么", "2"), AgentTypeEnum.KNOWLEDGE.getAgentName());
    }

}
