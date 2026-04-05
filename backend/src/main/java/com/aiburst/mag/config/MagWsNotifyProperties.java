package com.aiburst.mag.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * MAG WebSocket 与业务推送相关开关。
 */
@Data
@ConfigurationProperties(prefix = "aiburst.mag.ws")
public class MagWsNotifyProperties {

    /**
     * 写入 {@code mag_alert_event} 后是否向 {@code project:{projectId}} 频道广播 {@code mag.alert.new}，供工作台实时刷新与通知。
     */
    private boolean alertBroadcast = true;
}
