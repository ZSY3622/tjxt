package com.tianji.aigc.tools;

import cn.hutool.core.util.StrUtil;
import com.tianji.aigc.config.ToolResultHolder;
import com.tianji.aigc.constants.Constant;
import com.tianji.aigc.tools.result.CourseInfo;
import com.tianji.api.client.course.CourseClient;
import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;
import cn.hutool.core.convert.Convert;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class CourseTools {

    private final CourseClient courseClient;

    private static final String FIELD_NAME_FORMAT = "{}_{}";  // 提取格式字符串常量


    //被 @Tool 标记的方法，会变成一个“大模型可以调用的工具函数”。
    @Tool(description = Constant.Tools.QUERY_COURSE_BY_ID)
    public CourseInfo queryCourseById(@ToolParam(description = Constant.ToolParams.COURSE_ID) Long courseId, ToolContext toolContext) {
        return Optional.ofNullable(courseId)
                .map(id -> CourseInfo.of(this.courseClient.baseInfo(id, true)))
                .map(courseInfo ->{
                    //存储数据到临时容器中
                    // 存储数据的字段名
                    String field = StrUtil.format(FIELD_NAME_FORMAT,
                            StrUtil.lowerFirst(CourseInfo.class.getSimpleName()),
                            courseInfo.getId());

                    String requestId = Convert.toStr(toolContext.getContext().get(Constant.REQUEST_ID));
                    ToolResultHolder.put(requestId,field,courseInfo);
                    return courseInfo;
                })

                .orElse(null);
    }
}
