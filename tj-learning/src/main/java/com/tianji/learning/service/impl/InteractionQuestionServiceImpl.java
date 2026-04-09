package com.tianji.learning.service.impl;

import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.StringUtils;
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

    @Override
    public void updateQuestion(Long id, QuestionFormDTO dto) {
        //1.校验
        //1.1手动校验部分属性
        if (StringUtils.isBlank(dto.getTitle()) ||
            StringUtils.isBlank(dto.getDescription()) || dto.getAnonymity() == null){
            throw new BadRequestException("非法参数");
        }
        //1.2校验id
        InteractionQuestion question = getById(id);
        if (question == null){
            throw new BadRequestException("非法参数");
        }
        //1.3校验用户（只能修改自己的互动问题）
        Long userId = UserContext.getUser();
        if (userId.equals(question.getUserId())) { //Long类型不能用== 比较
            throw new BadRequestException("不能修改别人的互动问题");
        }

        //2.dto转换为po
        question.setTitle(dto.getTitle());
        question.setDescription(dto.getDescription());
        question.setAnonymity(dto.getAnonymity());

        //3.修改互动问题数据
        updateById(question);
    }
}
