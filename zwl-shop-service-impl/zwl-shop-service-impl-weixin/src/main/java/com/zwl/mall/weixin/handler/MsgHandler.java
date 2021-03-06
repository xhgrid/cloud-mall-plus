package com.zwl.mall.weixin.handler;

import com.zwl.mall.base.Constants;
import com.zwl.mall.utils.RedisUtil;
import com.zwl.mall.utils.RegexUtils;
import com.zwl.mall.weixin.builder.TextBuilder;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;

import static me.chanjar.weixin.common.api.WxConsts.XmlMsgType;

/**
 * @author Binary Wang(https://github.com/binarywang)
 */
@Component
public class MsgHandler extends AbstractHandler {
    // 用户发送手机验证码提示
    @Value("${zwl.weixin.registration.code.message}")
    private String registrationCodeMessage;
    // 默认用户发送验证码提示
    @Value("${zwl.weixin.default.registration.code.message}")
    private String defaultRegistrationCodeMessage;
    @Autowired
    private RedisUtil redisUtil;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMessage,
                                    Map<String, Object> context, WxMpService weixinService,
                                    WxSessionManager sessionManager) {

        if (!wxMessage.getMsgType().equals(XmlMsgType.EVENT)) {
            //TODO 可以选择将消息保存到本地
        }

        //当用户输入关键词如“你好”，“客服”等，并且有客服在线时，把消息转发给在线客服
        try {
            if (StringUtils.startsWithAny(wxMessage.getContent(), "你好", "客服")
                    && weixinService.getKefuService().kfOnlineList()
                    .getKfOnlineList().size() > 0) {
                return WxMpXmlOutMessage.TRANSFER_CUSTOMER_SERVICE()
                        .fromUser(wxMessage.getToUser())
                        .toUser(wxMessage.getFromUser()).build();
            }
        } catch (WxErrorException e) {
            e.printStackTrace();
        }
        // TODO 组装回复消息
        // 1.验证关键字是否为手机号码类型
        String fromMsg = wxMessage.getContent();
        if (RegexUtils.checkMobile(fromMsg)) {
            // 如果发送消息为手机号码类型,则发送短信验证码
            int registerCode = registCode();
            String retContext = registrationCodeMessage.replaceAll("registrationCodeMessage", registerCode + "");
            // 将注册码存入redis
            redisUtil.setString(Constants.WEIXINCODE_KEY + fromMsg, registerCode + "", Constants.WEIXINCODE_TIMEOUT);
            return new TextBuilder().build(retContext, wxMessage, weixinService);

        }
        return new TextBuilder().build(defaultRegistrationCodeMessage, wxMessage, weixinService);

    }

    // 获取注册码
    private int registCode() {
        int registCode = (int) (Math.random() * 9000 + 1000);
        return registCode;
    }

}
