package com.tianji.learning.controller;


import com.tianji.learning.domain.dto.QuestionFormDTO;
import com.tianji.learning.service.IInteractionQuestionService;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

/**
 * <p>
 * 互动提问的问题表 前端控制器
 * </p>
 *
 * @author CarVak
 * @since 2026-04-09
 */
@RestController
@RequestMapping("/questions")
@RequiredArgsConstructor
public class InteractionQuestionController {

    private final IInteractionQuestionService questionService;

    @ApiOperation("新增提问")
    @PostMapping
    public void saveQuestion( @Valid @RequestBody QuestionFormDTO questionFormDTO){
        questionService.saveQuestion(questionFormDTO);
    }

    @ApiOperation("修改互动提问")
    @PutMapping("/{id}")
    public void updateQuestion(@PathVariable Long id, @RequestBody QuestionFormDTO questionFormDTO){
        questionService.updateQuestion(id,questionFormDTO);
    }
}
