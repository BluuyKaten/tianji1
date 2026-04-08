package com.tianji.learning.utils;

import com.tianji.common.utils.JsonUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.learning.domain.po.LearningLesson;
import com.tianji.learning.domain.po.LearningRecord;
import com.tianji.learning.mapper.LearningRecordMapper;
import com.tianji.learning.service.ILearningLessonService;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;


import javax.annotation.PostConstruct;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.DelayQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RequiredArgsConstructor
@Component
public class LearningRecordDelayTaskHandler {

    private final StringRedisTemplate redisTemplate;
    private final LearningRecordMapper recordMapper;
    private final ILearningLessonService lessonService;
    private final DelayQueue<DelayTask<RecordTaskData>> queue = new DelayQueue<>();
    private final static String RECORD_KEY_TEMPLATE = "learning:record:{}";
    private static boolean begin = true;

    /* 方案二
    // CPU 核心数
    private static final int CORE_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    // 静态线程池实例，全局唯一
    private static final ExecutorService EXECUTOR = Executors.newFixedThreadPool(CORE_POOL_SIZE);
    */

    private ExecutorService delayTaskExecutor;

    @PostConstruct
    public void init(){
        //方案三
        delayTaskExecutor.submit(this::handleDelayTask);
//        方案二
//        EXECUTOR.submit(this::handleDelayTask);

//        CompletableFuture.runAsync(this::handleDelayTask);
    }

    public void destroy(){
        begin = false;
        log.debug("延迟任务停止执行！");
    }

    public void handleDelayTask(){
        while (begin){
            try {
                //1、获取到期的延迟任务
                DelayTask<RecordTaskData> task = queue.take();
                //2.查询Redis缓存
                RecordTaskData data = task.getData();
                LearningRecord record = readRecordCache(data.getLessonId(), data.getSectionId());
                if (record == null){
                    continue;
                }
                //3.比较数据，moment值
                if (Objects.equals(data.getMoment(), record.getMoment())){
                    //不一致,说明用户还在持续提交播放进度，放弃旧数据
                    continue;
                }
                //4.一致，持久化播放进度数据到数据库
                //4.1更新学习记录的moment
                record.setFinished(null);
                recordMapper.updateById(record);
                //4.2更新课表最近学习的信息
                LearningLesson lesson = new LearningLesson();
                lesson.setId(data.getLessonId());
                lesson.setLatestSectionId(data.sectionId);
                lesson.setLatestLearnTime(LocalDateTime.now());
                lessonService.updateById(lesson);
            } catch (Exception e) {
                log.error("处理延迟任务发生异常", e);
            }
        }
    }

    public void addLearningRecordDelayTask(LearningRecord record){
        //1.添加数据到Redis缓存
        writeRecordCache(record);
        //2.提交延迟任务到延迟队列DelayQueue
        queue.add(new DelayTask<>(new RecordTaskData(record),Duration.ofSeconds(10)));
    }

    public void writeRecordCache(LearningRecord record) {
        log.debug("更新学习记录的缓存数据");
        try {
            //1.数据的抓换
            String json = JsonUtils.toJsonStr(new RecordCacheData(record));
            //2.写入Redis
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, record.getLessonId());
            redisTemplate.opsForHash().put(key,record.getSectionId().toString(),json);
            //3.添加缓存过期时间
            redisTemplate.expire(key, Duration.ofMinutes(1));
        } catch (Exception e) {
            log.error("更新学习记录缓存异常",e);
        }
    }

    public LearningRecord readRecordCache(Long lessonId, Long sectionId){
        try {
            //1、读取Redis数据
            String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
            Object cacheData = redisTemplate.opsForHash().get(key, sectionId.toString());
            if (cacheData == null) {
                return null;
            }
            //2.数据检测和转换
            return JsonUtils.toBean(cacheData.toString(), LearningRecord.class);
        } catch (Exception e) {
            log.error("缓存读取异常", e);
            return null;
        }
    }

    public void cleanRecordCache(Long lessonId, Long sectionId){
        // 删除数据
        String key = StringUtils.format(RECORD_KEY_TEMPLATE, lessonId);
        redisTemplate.opsForHash().delete(key, sectionId.toString());
    }


    @Data
    @NoArgsConstructor
    private static class RecordCacheData{
        private Long id;
        private Integer Moment;
        private Boolean finished;

        public RecordCacheData(LearningRecord record) {
            this.id = record.getId();
            Moment = record.getMoment();
            this.finished = record.getFinished();
        }
    }

    @Data
    @NoArgsConstructor
    private static class RecordTaskData{
        private Long lessonId;
        private Long sectionId;
        private Integer moment;

        public RecordTaskData(LearningRecord record) {
            this.lessonId = record.getLessonId();
            this.sectionId = record.getSectionId();
            this.moment = record.getMoment();
        }
    }
}
