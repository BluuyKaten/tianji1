package com.tianji.learning.service.impl;

import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * <p>
 * 互动提问的问题表 服务实现类
 * </p>
 *
 * @author CarVak
 * @since 2026-04-09
 */
@Service
@RequiredArgsConstructor
public class InteractionQuestionServiceImpl extends ServiceImpl<InteractionQuestionMapper, InteractionQuestion> implements IInteractionQuestionService {

    @Override
    @Transactional
    public void saveQuestion(QuestionFormDTO questionFormDTO) {
        //1.获取登录用户
        Long userId = UserContext.getUser();
        //2.数据转换
        InteractionQuestion question = BeanUtils.copyBean(questionFormDTO, InteractionQuestion.class);
        //3.补充数据
        question.setUserId(userId);
        //4.保存问题
        save(question);
    }
}
