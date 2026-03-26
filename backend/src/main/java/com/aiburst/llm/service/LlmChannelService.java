package com.aiburst.llm.service;

import com.aiburst.llm.catalog.LlmProviderCatalog;
import com.aiburst.llm.crypto.LlmCryptoService;
import com.aiburst.llm.dto.LlmChannelSaveRequest;
import com.aiburst.llm.dto.LlmChannelVO;
import com.aiburst.llm.entity.LlmChannel;
import com.aiburst.llm.mapper.LlmChannelMapper;
import com.aiburst.llm.model.LlmProtocol;
import com.aiburst.llm.model.LlmProviderMeta;
import com.aiburst.security.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LlmChannelService {

    private final LlmChannelMapper llmChannelMapper;
    private final LlmCryptoService llmCryptoService;
    private final LlmProviderCatalog catalog;

    public List<LlmChannelVO> listMine() {
        Long uid = SecurityUtils.currentUserId();
        return llmChannelMapper.selectByOwner(uid).stream().map(this::toVo).collect(Collectors.toList());
    }

    private LlmChannelVO toVo(LlmChannel c) {
        LlmProviderMeta meta = catalog.find(c.getProviderCode());
        return LlmChannelVO.builder()
                .id(c.getId())
                .providerCode(c.getProviderCode())
                .providerName(meta != null ? meta.getName() : c.getProviderCode())
                .protocol(c.getProtocol())
                .channelName(c.getChannelName())
                .baseUrl(c.getBaseUrl())
                .defaultModel(c.getDefaultModel())
                .extraJson(c.getExtraJson())
                .status(c.getStatus())
                .build();
    }

    @Transactional
    public void save(LlmChannelSaveRequest req) {
        Long uid = SecurityUtils.currentUserId();
        LlmProtocol protocol = catalog.resolveProtocol(req.getProviderCode());
        LlmProviderMeta meta = catalog.find(req.getProviderCode());
        String base = trimToNull(req.getBaseUrl());
        if (base == null && meta != null) {
            base = trimToNull(meta.getDefaultBaseUrl());
        }
        if (base == null) {
            throw new IllegalArgumentException("baseUrl is required");
        }
        if (req.getId() == null) {
            if (!StringUtils.hasText(req.getApiKey())) {
                throw new IllegalArgumentException("apiKey is required for new channel");
            }
            LlmChannel row = new LlmChannel();
            row.setOwnerUserId(uid);
            row.setProviderCode(req.getProviderCode().trim());
            row.setProtocol(protocol.name());
            row.setChannelName(req.getChannelName().trim());
            row.setBaseUrl(trimSlash(base));
            row.setApiKeyCipher(llmCryptoService.encrypt(req.getApiKey().trim()));
            row.setDefaultModel(trimToNull(req.getDefaultModel()));
            row.setExtraJson(trimToNull(req.getExtraJson()));
            row.setStatus(req.getStatus());
            llmChannelMapper.insert(row);
            return;
        }
        LlmChannel existing = llmChannelMapper.selectByIdAndOwner(req.getId(), uid);
        if (existing == null) {
            throw new IllegalArgumentException("channel not found");
        }
        LlmChannel row = new LlmChannel();
        row.setId(req.getId());
        row.setOwnerUserId(uid);
        row.setProviderCode(req.getProviderCode().trim());
        row.setProtocol(protocol.name());
        row.setChannelName(req.getChannelName().trim());
        row.setBaseUrl(trimSlash(base));
        row.setDefaultModel(trimToNull(req.getDefaultModel()));
        row.setExtraJson(trimToNull(req.getExtraJson()));
        row.setStatus(req.getStatus());
        if (StringUtils.hasText(req.getApiKey())) {
            row.setApiKeyCipher(llmCryptoService.encrypt(req.getApiKey().trim()));
        }
        llmChannelMapper.update(row);
    }

    @Transactional
    public void delete(Long id) {
        Long uid = SecurityUtils.currentUserId();
        llmChannelMapper.deleteByIdAndOwner(id, uid);
    }

    public LlmChannel requireOwned(Long channelId, Long ownerUserId) {
        LlmChannel c = llmChannelMapper.selectByIdAndOwner(channelId, ownerUserId);
        if (c == null) {
            throw new IllegalArgumentException("channel not found");
        }
        if (c.getStatus() == null || c.getStatus() != 1) {
            throw new IllegalArgumentException("channel disabled");
        }
        return c;
    }

    private static String trimSlash(String base) {
        String b = base.trim();
        while (b.endsWith("/")) {
            b = b.substring(0, b.length() - 1);
        }
        return b;
    }

    private static String trimToNull(String s) {
        if (!StringUtils.hasText(s)) {
            return null;
        }
        return s.trim();
    }
}
