package com.yuzhijie.career.trace.module.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.interview.entity.InterviewAnswer;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewAnswerMapper extends BaseMapper<InterviewAnswer> {

    @Select("""
            SELECT * FROM interview_answers
            WHERE session_id = #{sessionId}
            ORDER BY question_index
            """)
    List<InterviewAnswer> selectBySessionId(@Param("sessionId") Long sessionId);

    @Insert("""
            INSERT INTO interview_answers (session_id, question_index, answer_text)
            VALUES (#{sessionId}, #{questionIndex}, #{answerText}) AS new_answer
            ON DUPLICATE KEY UPDATE
                answer_text = new_answer.answer_text,
                answered_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("sessionId") Long sessionId,
               @Param("questionIndex") Integer questionIndex,
               @Param("answerText") String answerText);
}
