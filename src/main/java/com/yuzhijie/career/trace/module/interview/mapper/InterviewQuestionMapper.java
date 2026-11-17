package com.yuzhijie.career.trace.module.interview.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.yuzhijie.career.trace.module.interview.entity.InterviewQuestion;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface InterviewQuestionMapper extends BaseMapper<InterviewQuestion> {

    @Select("""
            SELECT * FROM interview_questions
            WHERE session_id = #{sessionId}
            ORDER BY question_index, id
            """)
    List<InterviewQuestion> selectBySessionId(@Param("sessionId") Long sessionId);

    @Delete("DELETE FROM interview_questions WHERE session_id = #{sessionId}")
    int deleteBySessionId(@Param("sessionId") Long sessionId);

    @Select("""
            SELECT q.topic_summary
            FROM interview_questions q
            JOIN interview_sessions s ON s.id = q.session_id
            WHERE s.user_id = #{userId}
              AND s.status = 3
              AND s.created_at >= DATE_SUB(NOW(), INTERVAL 30 DAY)
              AND q.question_index = q.parent_index
              AND q.topic_summary IS NOT NULL
              AND q.topic_summary <> ''
            GROUP BY q.topic_summary
            ORDER BY MAX(q.created_at) DESC
            LIMIT #{limit}
            """)
    List<String> selectRecentTopics(@Param("userId") Long userId,
                                    @Param("limit") int limit);
}
