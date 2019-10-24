package com.qunar.qchat.dao;

import com.qunar.qchat.model.SendWhiteModel;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ISendMsgDao {

    void insertSelective(SendWhiteModel sendWhiteModel);

    SendWhiteModel selectByappcode(@Param("appcode") String appcode);

    SendWhiteModel selectByFromUser(@Param("fromUser") String fromUser);

    List<SendWhiteModel> selectReview();

    void deleteWhiteById(@Param("id") Integer id);

    void updateReviewFlag(@Param("review") Integer REVIEW, @Param("id") Integer id);

    SendWhiteModel selectById(@Param("id") Integer id);


}
