package com.aiburst.mag.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MagTemporalGateTest {

    @Mock
    private MagTemporalProperties properties;

    @Mock
    private MagTemporalConnectivityChecker connectivityChecker;

    @InjectMocks
    private MagTemporalGate gate;

    @Test
    void disabled_blocks() {
        when(properties.isEnabled()).thenReturn(false);
        Optional<Map<String, Object>> block = gate.blockIfAny("agentId=1");
        assertThat(block).isPresent();
        assertThat(block.get().get("accepted")).isEqualTo(false);
        assertThat((String) block.get().get("message")).contains("未启用 Temporal");
    }

    @Test
    void enabled_unreachable_blocks() {
        when(properties.isEnabled()).thenReturn(true);
        when(properties.getTarget()).thenReturn("127.0.0.1:7233");
        when(connectivityChecker.verifyReachable()).thenReturn(Optional.of("Connection refused"));
        Optional<Map<String, Object>> block = gate.blockIfAny(null);
        assertThat(block).isPresent();
        assertThat(block.get().get("temporalReachable")).isEqualTo(false);
        assertThat((String) block.get().get("message")).contains("无法连接");
    }

    @Test
    void enabled_reachable_noBlock() {
        when(properties.isEnabled()).thenReturn(true);
        when(connectivityChecker.verifyReachable()).thenReturn(Optional.empty());
        assertThat(gate.blockIfAny(null)).isEmpty();
    }
}
