package com.tianji.learning.mq;

import com.tianji.api.dto.trade.OrderBasicDTO;
import com.tianji.common.constants.MqConstants;
import com.tianji.common.utils.CollUtils;
import com.tianji.learning.service.ILearningLessonService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ExchangeTypes;
import org.springframework.amqp.rabbit.annotation.Exchange;
import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.QueueBinding;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class LessonChangesListener {

    private final ILearningLessonService lessonService;


    /*
    *   支付成功后，MQ传递添加课程信息
    * */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "Learning.lesson.pay.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_PAY_KEY
    ))
    public void ListenLessonPay(OrderBasicDTO order){
        //1.健壮性处理
        if(order == null || order.getOrderId() == null|| CollUtils.isEmpty(order.getCourseIds())){
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }
        // 2.添加课程
        log.debug("监听到用户{}的订单{},需要添加课程{}到课表中",order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.addUserLessons(order.getUserId(),order.getCourseIds());
    }

    /*
     *   退款后，MQ传递立刻移除课表中的课程
     * */
    @RabbitListener(bindings = @QueueBinding(
            value = @Queue(value = "Learning.lesson.refund.queue",durable = "true"),
            exchange = @Exchange(name = MqConstants.Exchange.ORDER_EXCHANGE, type = ExchangeTypes.TOPIC),
            key = MqConstants.Key.ORDER_REFUND_KEY
    ))
    public void ListenLessonRefund(OrderBasicDTO order){
        //1.健壮性处理
        if(order == null || order.getOrderId() == null|| CollUtils.isEmpty(order.getCourseIds())){
            log.error("接收到MQ消息有误，订单数据为空");
            return;
        }
        //2.删除课程
        log.debug("监听到用户{}的退款订单{},需要删除课程{}",order.getUserId(),order.getOrderId(),order.getCourseIds());
        lessonService.deleteCourseFromLesson(order.getUserId(),order.getCourseIds().get(0));
    }
}
