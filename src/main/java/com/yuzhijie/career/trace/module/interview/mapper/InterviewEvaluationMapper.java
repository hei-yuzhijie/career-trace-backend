package com.yuzhijie.career.trace.module.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.interview.entity.InterviewEvaluation;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewEvaluationMapper extends BaseMapper<InterviewEvaluation> {

    @Select("""
            SELECT * FROM interview_evaluations
            WHERE session_id = #{sessionId}
            ORDER BY question_index
            """)
    List<InterviewEvaluation> selectBySessionId(@Param("sessionId") Long sessionId);

    @Insert("""
            INSERT INTO interview_evaluations
                (session_id, question_index, score, feedback, reference_answer, points)
            VALUES
                (#{sessionId}, #{questionIndex}, #{score}, #{feedback}, #{referenceAnswer}, #{points}) AS new_eval
            ON DUPLICATE KEY UPDATE
                score = new_eval.score,
                feedback = new_eval.feedback,
                reference_answer = new_eval.reference_answer,
                points = new_eval.points,
                evaluated_at = CURRENT_TIMESTAMP
            """)
    int upsert(@Param("sessionId") Long sessionId,
               @Param("questionIndex") Integer questionIndex,
               @Param("score") Integer score,
               @Param("feedback") String feedback,
               @Param("referenceAnswer") String referenceAnswer,
               @Param("points") String points);
}
