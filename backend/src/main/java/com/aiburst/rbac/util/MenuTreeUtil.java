package com.aiburst.rbac.util;

import com.aiburst.rbac.dto.MenuVO;
import com.aiburst.rbac.entity.SysPermission;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public final class MenuTreeUtil {

    private MenuTreeUtil() {
    }

    public static List<MenuVO> build(List<SysPermission> flat) {
        if (flat == null || flat.isEmpty()) {
            return new ArrayList<>();
        }
        Map<Long, MenuVO> map = flat.stream()
                .map(MenuTreeUtil::toVo)
                .collect(Collectors.toMap(MenuVO::getId, m -> m, (a, b) -> a));
        List<MenuVO> roots = new ArrayList<>();
        for (MenuVO node : map.values()) {
            Long pid = node.getParentId();
            if (pid == null || pid == 0) {
                roots.add(node);
            } else {
                MenuVO parent = map.get(pid);
                if (parent != null) {
                    parent.getChildren().add(node);
                } else {
                    roots.add(node);
                }
            }
        }
        sortRecursive(roots);
        return roots;
    }

    private static void sortRecursive(List<MenuVO> nodes) {
        nodes.sort(Comparator.comparing(MenuVO::getSortOrder, Comparator.nullsLast(Integer::compareTo))
                .thenComparing(MenuVO::getId));
        for (MenuVO n : nodes) {
            if (!n.getChildren().isEmpty()) {
                sortRecursive(n.getChildren());
            }
        }
    }

    private static MenuVO toVo(SysPermission p) {
        MenuVO m = new MenuVO();
        m.setId(p.getId());
        m.setParentId(p.getParentId());
        m.setPermCode(p.getPermCode());
        m.setPermName(p.getPermName());
        m.setPermType(p.getPermType());
        m.setPath(p.getPath());
        m.setComponent(p.getComponent());
        m.setIcon(p.getIcon());
        m.setSortOrder(p.getSortOrder());
        return m;
    }
}
