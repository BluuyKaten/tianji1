package com.tianji.learning.service.impl;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.tianji.api.client.user.UserClient;
import com.tianji.api.dto.user.UserDTO;
import com.tianji.common.domain.dto.PageDTO;
import com.tianji.common.exceptions.BadRequestException;
import com.tianji.common.utils.BeanUtils;
import com.tianji.common.utils.CollUtils;
import com.tianji.common.utils.StringUtils;
import com.tianji.common.utils.UserContext;
import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.domain.po.InteractionQuestion;
import com.tianji.learning.domain.po.InteractionReply;
import com.tianji.learning.domain.query.QuestionPageQuery;
import com.tianji.learning.domain.vo.QuestionVO;
import com.tianji.learning.mapper.InteractionQuestionMapper;
import com.tianji.learning.mapper.InteractionReplyMapper;
import com.tianji.learning.service.IInteractionQuestionService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

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

    private final InteractionReplyMapper replyMapper;
    private final UserClient userClient;

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

    @Override
    public PageDTO<QuestionVO> queryQuestionPage(QuestionPageQuery query) {
        // 1.参数校验，课程id和小节id不能都为空
        Long courseId = query.getCourseId();
        Long sectionId = query.getSectionId();
        if (courseId == null && sectionId == null) {
            throw new BadRequestException("课程id和小节id不能都为空");
        }
        // 2.分页查询
        Page<InteractionQuestion> page = lambdaQuery()
                .select(InteractionQuestion.class, info -> !info.getProperty().equals("description"))
                .eq(query.getOnlyMine(), InteractionQuestion::getUserId, UserContext.getUser())
                .eq(courseId != null, InteractionQuestion::getCourseId, courseId)
                .eq(sectionId != null, InteractionQuestion::getSectionId, sectionId)
                .eq(InteractionQuestion::getHidden, false)
                .page(query.toMpPageDefaultSortByCreateTimeDesc());
        List<InteractionQuestion> records = page.getRecords();
        if (CollUtils.isEmpty(records)) {
            return PageDTO.empty(page);
        }
        // 3.根据id查询提问者和最近一次回答的信息
        Set<Long> userIds = new HashSet<>();
        Set<Long> answerIds = new HashSet<>();
        // 3.1.得到问题当中的提问者id和最近一次回答的id
        for (InteractionQuestion q : records) {
            if(!q.getAnonymity()) { // 只查询非匿名的问题
                userIds.add(q.getUserId());
            }
            answerIds.add(q.getLatestAnswerId());
        }
        // 3.2.根据id查询最近一次回答
        answerIds.remove(null);
        Map<Long, InteractionReply> replyMap = new HashMap<>(answerIds.size());
        if(CollUtils.isNotEmpty(answerIds)) {
            List<InteractionReply> replies = replyMapper.selectBatchIds(answerIds);
            for (InteractionReply reply : replies) {
                replyMap.put(reply.getId(), reply);
                if(!reply.getAnonymity()){ // 匿名用户不做查询
                    userIds.add(reply.getUserId());
                }
            }
        }

        // 3.3.根据id查询用户信息（提问者）
        userIds.remove(null);
        Map<Long, UserDTO> userMap = new HashMap<>(userIds.size());
        if(CollUtils.isNotEmpty(userIds)) {
            List<UserDTO> users = userClient.queryUserByIds(userIds);
            userMap = users.stream()
                    .collect(Collectors.toMap(UserDTO::getId, u -> u));
        }

        // 4.封装VO
        List<QuestionVO> voList = new ArrayList<>(records.size());
        for (InteractionQuestion r : records) {
            // 4.1.将PO转为VO
            QuestionVO vo = BeanUtils.copyBean(r, QuestionVO.class);
            vo.setUserId(null);
            voList.add(vo);
            // 4.2.封装提问者信息
            if(!r.getAnonymity()){
                UserDTO userDTO = userMap.get(r.getUserId());
                if (userDTO != null) {
                    vo.setUserId(userDTO.getId());
                    vo.setUserName(userDTO.getName());
                    vo.setUserIcon(userDTO.getIcon());
                }
            }

            // 4.3.封装最近一次回答的信息
            InteractionReply reply = replyMap.get(r.getLatestAnswerId());
            if (reply != null) {
                vo.setLatestReplyContent(reply.getContent());
                if(!reply.getAnonymity()){// 匿名用户直接忽略
                    UserDTO user = userMap.get(reply.getUserId());
                    vo.setLatestReplyUser(user.getName());
                }

            }
        }

        return PageDTO.of(page, voList);
    }

}
