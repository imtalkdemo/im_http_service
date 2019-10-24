package com.qunar.qchat.service;

import com.google.common.base.Splitter;
import com.qunar.qchat.controller.RouteMessageController;
import com.qunar.qchat.dao.ISendMsgDao;
import com.qunar.qchat.model.SendMessageParam;
import com.qunar.qchat.model.SendWhiteModel;
import com.qunar.qchat.utils.JacksonUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class SendWhieMsgService {
    @Autowired
    private ISendMsgDao iSendMsgDao;
    @Autowired
    private RouteMessageController routeMessageConttroller;
    private static final Logger LOGGER = LoggerFactory.getLogger(SendWhieMsgService.class);

    public void insertSendInfo(SendWhiteModel sendWhiteModel) {
        try {
            iSendMsgDao.insertSelective(sendWhiteModel);
        } catch (Exception e) {
            LOGGER.info("send msg statis error ", e);
        }
    }


    public void afterAgreeApllay(SendWhiteModel sendWhiteModel, String content) {
        if (sendWhiteModel == null || sendWhiteModel.getOwnerUser() == null) {
            return;
        }
        List<String> owner = Splitter.on(";").splitToList(sendWhiteModel.getOwnerUser().trim());
        List<SendMessageParam.ToEntity> toUsers = new ArrayList<>(owner.size());

        if (owner != null && owner.size() > 0) {
            for (String to : owner) {
                SendMessageParam.ToEntity t = new SendMessageParam.ToEntity();
                t.setUser(to);
                t.setHost("ejabhost1");
                toUsers.add(t);
            }
        }
        SendMessageParam sendMessageParam = new SendMessageParam();
        sendMessageParam.setSystem("qtalk_corp_servie_auth");
        sendMessageParam.setFrom("admin");
        sendMessageParam.setFromhost("ejabhost1");
        sendMessageParam.setTo(toUsers);
        sendMessageParam.setContent(content);
        sendMessageParam.setMsgtype("1");
        sendMessageParam.setType("chat");
        routeMessageConttroller.sendHttpMessage(JacksonUtils.obj2String(sendMessageParam));

    }

}
