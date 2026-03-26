package com.tianji.learning.controller;


import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.service.ILearningLessonService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * <p>
 * 学生课程表 前端控制器
 * </p>
 *
 * @author CarVak
 * @since 2026-03-22
 */
@RestController
@RequestMapping("/lessons")
@Api(tags = "我的课表相关接口")
@RequiredArgsConstructor
public class LearningLessonController {

    private final ILearningLessonService lessonService;

    @GetMapping("/page")
    @ApiOperation("分页查询我的课表")
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query){
        return lessonService.queryMyLessons(query);
    }

    @GetMapping("/now")
    @ApiOperation("查询我正在学习的课程")
    public LearningLessonVO queryNowLessons(){
        return lessonService.queryNowLessons();
    }

    @GetMapping("/{courseId}")
    @ApiOperation("根据id查询指定课程的学习状态")
    public LearningLessonVO  queryLessonByCourseId(
            @ApiParam(name = "课程id" ,example = "1") @PathVariable("courseId") Long courseId){
        return lessonService.queryLessonByCourseId(courseId);
    }

    @DeleteMapping("/{courseId}")
    @ApiOperation("删除指定课程信息")
    public void deleteCourseFromLesson(
            @ApiParam(name = "课程id" ,example = "1") @PathVariable("courseId") Long courseId) {
        lessonService.deleteCourseFromLesson(null,courseId);
    }
}
