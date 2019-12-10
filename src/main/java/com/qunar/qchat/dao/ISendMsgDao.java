package com.qunar.qchat.dao;

import com.qunar.qchat.model.SendWhiteModel;
import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Component;


@Mapper
public interface ISendMsgDao {

    void insertSelective(SendWhiteModel sendWhiteModel);

}
