package com.tianji.learning.service.impl;


import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.course.CatalogueClient;
import com.tianji.api.client.course.CourseClient;
import com.tianji.api.dto.course.CataSimpleInfoDTO;
import com.tianji.api.dto.course.CourseFullInfoDTO;
import com.tianji.api.dto.course.CourseSimpleInfoDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.domain.query.PageQuery;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.vo.LearningLessonVO;
import com.tianji.learning.enums.LessonStatus;
import com.tianji.learning.mapper.LearningLessonMapper;
import com.tianji.learning.service.ILearningLessonService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * <p>
 * 学生课程表 服务实现类
 * </p>
 *
 * @author CarVak
 * @since 2026-03-22
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class LearningLessonServiceImpl extends ServiceImpl<LearningLessonMapper, LearningLesson> implements ILearningLessonService {


    private final CourseClient courseClient;
    private final CatalogueClient catalogueClient;


    @Override
    @Transactional
    public void addUserLessons(Long userId, List<Long> courseIds) {
        //1.查询课程有效期
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(courseIds);
        if (CollUtils.isEmpty(cInfoList) ){
            //课程不存在,无法添加
            log.error("课程信息不存在,无法添加到课表");
            return;
        }
        //2.循环遍历,处理LearningLesson数据
        List<LearningLesson> list = new ArrayList<>(cInfoList.size());
        for (CourseSimpleInfoDTO cInfo : cInfoList) {
            LearningLesson lesson = new LearningLesson();
            //2.1 获取国企时间
            Integer validDuration = cInfo.getValidDuration();
            if (validDuration != null && validDuration > 0){
                LocalDateTime now = LocalDateTime.now();
                lesson.setCreateTime(now);
                lesson.setExpireTime(now.plusMonths(validDuration));
            }
           //2.2 填充userId和courseId
            lesson.setUserId(userId);
            lesson.setCourseId(cInfo.getId());
            list.add(lesson);
        }
        //3.批量新增
        saveBatch(list);
    }

    @Override
    public PageDTO<LearningLessonVO> queryMyLessons(PageQuery query) {
        //1.获取当前登录用户
        Long userId = UserContext.getUser();
        //2.分页查询
        // select * from learning_lesson where user_id = #{userId} order by latest_learn_time limit 0,5
        Page<LearningLesson> page = lambdaQuery()
                .eq(LearningLesson::getUserId, userId) ///where user_id = #{userId}
                .page(query.toMpPage("latest_learn_time", false));
        List<LearningLesson> records = page.getRecords();
        if (CollUtils.isEmpty(records)){
            return PageDTO.empty(page);
        }
        //3.查询课程信息
        //3.1 获取课程id
        Set<Long> cIds = records.stream().map(LearningLesson::getCourseId).collect(Collectors.toSet());
        //3.2查询课程信息
        List<CourseSimpleInfoDTO> cInfoList = courseClient.getSimpleInfoList(cIds);
        if (CollUtils.isEmpty(cInfoList) ){
            //课程不存在,无法添加
            throw new BadRequestException("课程信息不存在");
        }
        //3.3 把课程集合处理成Map,key是courseId,值是course本身
        Map<Long, CourseSimpleInfoDTO> cMap = cInfoList.stream()
                .collect(Collectors.toMap(CourseSimpleInfoDTO::getId, c -> c));

        //4.封装VO返回
        List<LearningLessonVO> list = new ArrayList<>(records.size());
        //4.1循环便利,把LearningLesson(PO)转为VO
        for (LearningLesson r : records) {
            //4.2拷贝基础的属性到VO
            LearningLessonVO vo = BeanUtils.copyBean(r, LearningLessonVO.class);
            //4.3获取课程信息,填充到vo
            CourseSimpleInfoDTO cInfo = cMap.get(r.getCourseId());
            vo.setCourseName(cInfo.getName());
            vo.setCourseCoverUrl(cInfo.getCoverUrl());
            vo.setSections(cInfo.getSectionNum());
            list.add(vo);
        }

        return PageDTO.of(page,list);
    }

    @Override
    public LearningLessonVO queryNowLessons() {
        //1.首先查询用户信息
        Long userId = UserContext.getUser();
        // 2.查询正在学习的课程 select * from xx where user_id = #{userId} AND status = 1 order by latest_learn_time Desc limit 1
        @SuppressWarnings("unchecked") LearningLesson lesson = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getStatus, LessonStatus.LEARNING.getValue())
                .orderByDesc(LearningLesson::getLatestLearnTime)
                .last("limit 1")
                .one();
        if (lesson == null){
            return null;
        }
        //3.拷贝PO（数据库实体）基础属性到VO（前端视图）
        LearningLessonVO vo = BeanUtils.copyBean(lesson, LearningLessonVO.class);
        // 4.查询课程信息
        CourseFullInfoDTO cId = courseClient.getCourseInfoById(lesson.getCourseId(), false, false);
        if (cId == null){
            throw new BadRequestException("课程不存在");
        }
        vo.setCourseName(cId.getName());
        vo.setCourseCoverUrl(cId.getCoverUrl());
        vo.setSections(cId.getSectionNum());
        // 5.统计课表中的课程数量 select count(1) from xxx where user_id = #{userId}
        Integer courseAmount = lambdaQuery()
                .eq(LearningLesson::getUserId, userId)
                .count();
        vo.setCourseAmount(courseAmount);
        // 6.查询小节信息
        List<CataSimpleInfoDTO> cataInfos =
                catalogueClient.batchQueryCatalogue(CollUtils.singletonList(lesson.getLatestSectionId()));
        if (!CollUtils.isEmpty(cataInfos)){
            CataSimpleInfoDTO cataInfo  = cataInfos.get(0);
            vo.setLatestSectionName(cataInfo.getName());
            vo.setLatestSectionIndex(cataInfo.getCIndex());
        }
        return vo;
    }

    @Override
    public LearningLessonVO queryLessonByCourseId(Long courseId) {
        Long userId = UserContext.getUser();
        LearningLesson lesson = getOne(buildUserIdAndCourseIdWrapper(userId, courseId));
        // 2.查询课程信息 select * from xx where user_id = #{userId} AND course_id = #{courseId}
        if (lesson == null){
            return null;
        }

        return BeanUtils.copyBean(lesson, LearningLessonVO.class);
    }

    @Override
    public void deleteCourseFromLesson(Long userId, Long courseId) {
        if (userId == null){
            userId = UserContext.getUser();
        }
        remove(lambdaQuery()
                .eq(LearningLesson::getUserId,userId)
                .eq(LearningLesson::getCourseId,courseId));
    }

    @Override
    public Long isLessonValid(Long courseId) {
        Long userId = UserContext.getUser();
        if (userId == null){
            return null;
        }

        LearningLesson lesson = getOne(buildUserIdAndCourseIdWrapper(userId,courseId));
        if (lesson == null){
            return null;
        }
        return lesson.getId();
    }

    @Override
    public Integer countLearningLessonByCourse(Long courseId) {
        // select count(1) from xx where course_id = #{cc} AND status in (0, 1, 2)
        return lambdaQuery()
                .eq(LearningLesson::getCourseId, courseId)
                .in(LearningLesson::getStatus,
                        LessonStatus.NOT_BEGIN.getValue(),
                        LessonStatus.LEARNING.getValue(),
                        LessonStatus.FINISHED.getValue())
                .count();
    }

    private LambdaQueryWrapper<LearningLesson> buildUserIdAndCourseIdWrapper(Long userId, Long courseId) {
        return new QueryWrapper<LearningLesson>()
                .lambda()
                .eq(LearningLesson::getUserId, userId)
                .eq(LearningLesson::getCourseId, courseId);
    }
}
