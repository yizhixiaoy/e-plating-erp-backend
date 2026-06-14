package com.plating.erp.iam.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.plating.erp.base.vo.MenuVo;
import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.MenuEntity;
import com.plating.erp.iam.entity.UserEntity;
import com.plating.erp.iam.mapper.MenuMapper;
import com.plating.erp.iam.mapper.UserMapper;
import com.plating.erp.iam.service.MenuService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 菜单服务
 * 负责菜单和权限的动态返回
 */
@Service
public class MenuServiceImpl implements MenuService {
    
    private static final Logger log = LoggerFactory.getLogger(MenuServiceImpl.class);
    
    private final MenuMapper menuMapper;
    private final UserMapper userMapper;
    
    public MenuServiceImpl(MenuMapper menuMapper, UserMapper userMapper) {
        this.menuMapper = menuMapper;
        this.userMapper = userMapper;
    }
    
    /**
     * 获取用户的完整菜单树和权限
     * 根据用户类型返回对应的菜单:
     * - 平台管理员(userType=0): 返回所有菜单（平台级+租户级）
     * - 租户用户(userType=1): 根据角色关联的菜单动态返回
     *
     * @return 菜单和权限数据
     */
    public Map<String, Object> getUserRoutesAndPermissions() {
        List<MenuEntity> menus;
        List<String> permissions;
        CurrentUser user = SecurityUtils.currentUser();
        log.info("获取用户菜单和权限, userId={}, tenantId={}, userType={}, roles={}",
            user.userId(), user.tenantId(), user.userType(), user.roles());
        
        if (user.isSystem()) {
            // 平台管理员(userType=0)：获取所有菜单（平台级+租户级）
            log.info("平台管理员获取全部菜单, userId={}", user.userId());
            menus = menuMapper.selectAllMenus();
            permissions = menus.stream()
                .filter(m -> "F".equals(m.getMenuType()) && m.getPerms() != null)
                .map(MenuEntity::getPerms)
                .distinct()
                .collect(Collectors.toList());
        } else {
            // 租户用户：根据角色关联获取菜单（角色菜单通过 sys_role_menu 动态分配）
            log.info("租户用户获取角色菜单, userId={}, tenantId={}", user.userId(), user.tenantId());
            menus = menuMapper.selectMenuTreeByUserId(user.userId(), user.tenantId());
            permissions = menuMapper.selectPermsByUserId(user.userId(), user.tenantId());
        }
        
        // 构建菜单树
        List<Map<String, Object>> menuTree = buildMenuTree(menus);
        
        Map<String, Object> result = new HashMap<>();
        result.put("routes", menuTree);
        result.put("permissions", permissions);
        
        log.info("用户路由和权限获取完成, userId={}, 菜单数={}, 权限数={}, 权限列表={}", 
            user.userId(), menus.size(), permissions.size(), permissions);
        
        return result;
    }
    
    /**
     * 构建菜单树
     * 将扁平菜单列表转换为树形结构
     * 注意：只构建菜单类型(M=目录, C=菜单)，按钮类型(F)不加入树结构
     */
    private List<Map<String, Object>> buildMenuTree(List<MenuEntity> menus) {
        // 按parentId分组
        Map<Long, List<MenuEntity>> parentMap = menus.stream()
            .collect(Collectors.groupingBy(MenuEntity::getParentId));
        
        // 获取根节点(parentId=0)，只取菜单类型
        List<MenuEntity> rootMenus = parentMap.getOrDefault(0L, Collections.emptyList())
            .stream()
            .filter(m -> !"F".equals(m.getMenuType()))  // 过滤掉按钮
            .toList();
        
        // 递归构建树
        return rootMenus.stream()
            .map(menu -> convertToMenuMap(menu, parentMap))
            .collect(Collectors.toList());
    }
    
    /**
     * 将菜单实体转换为Map(前端路由格式)
     * 注意：只转换菜单类型，按钮类型不转换
     */
    private Map<String, Object> convertToMenuMap(MenuEntity menu, Map<Long, List<MenuEntity>> parentMap) {
        Map<String, Object> route = new HashMap<>();
        route.put("path", menu.getPath());
        route.put("name", menu.getMenuName());
        route.put("title", menu.getMenuName());
        route.put("icon", menu.getIcon());
        route.put("component", menu.getComponent());
        route.put("menuType", menu.getMenuType());
        route.put("hidden", menu.getVisible() == 0);
        // 注意：不返回 perms 字段，按钮权限在 permissions 数组中统一返回
        
        // 递归构建子菜单，过滤掉按钮类型
        List<MenuEntity> children = parentMap.getOrDefault(menu.getId(), Collections.emptyList())
            .stream()
            .filter(m -> !"F".equals(m.getMenuType()))  // 过滤掉按钮
            .toList();
        
        if (!children.isEmpty()) {
            List<Map<String, Object>> childRoutes = children.stream()
                .map(child -> convertToMenuMap(child, parentMap))
                .collect(Collectors.toList());
            route.put("children", childRoutes);
        }
        
        return route;
    }
    
    /**
     * 获取所有菜单(用于菜单管理页面)
     * 注意：此方法返回所有菜单，调用方需要根据用户类型过滤
     */
    public List<MenuEntity> getAllMenus() {
        // selectAllMenus 已使用 @InterceptorIgnore，返回所有菜单
        return menuMapper.selectAllMenus();
    }
    
    /**
     * 新增菜单
     */
    @Override
    public MenuEntity addMenu(MenuEntity menu) {
        menuMapper.insert(menu);
        log.info("菜单新增成功, menuId={}, menuName={}, tenantId={}", 
                menu.getId(), menu.getMenuName(), menu.getTenantId());
        return menu;
    }
    
    /**
     * 更新菜单
     */
    @Override
    public boolean updateMenu(MenuEntity menu) {
        int rows = menuMapper.updateById(menu);
        log.info("菜单更新成功, menuId={}, affected={}", menu.getId(), rows);
        return rows > 0;
    }
    
    /**
     * 删除菜单（逻辑删除）
     */
    @Override
    public boolean deleteMenu(Long menuId) {
        MenuEntity entity = new MenuEntity();
        entity.setId(menuId);
        entity.setDeleted(1);
        int rows = menuMapper.updateById(entity);
        log.info("菜单逻辑删除, menuId={}, affected={}", menuId, rows);
        return rows > 0;
    }

    /**
     * 根据ID获取菜单
     */
    @Override
    public MenuEntity getMenuById(Long menuId) {
        return menuMapper.selectById(menuId);
    }

    /**
     * 检查是否有子菜单
     */
    @Override
    public boolean hasChildren(Long menuId) {
        LambdaQueryWrapper<MenuEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(MenuEntity::getParentId, menuId)
               .eq(MenuEntity::getDeleted, 0);
        return menuMapper.selectCount(wrapper) > 0;
    }

    /**
     * 获取菜单管理树（用于菜单管理页面）
     * 返回完整字段的树形结构，平台管理员获取全部菜单，租户用户获取当前租户的菜单
     * 注意：包含停用(status=1)的菜单，以便管理员在页面上重新启用
     */
    @Override
    public List<MenuVo.MenuTreeNode> getMenuTreeForManagement() {
        CurrentUser user = SecurityUtils.currentUser();
        List<MenuEntity> menus;
        if (user.isSystem()) {
            menus = menuMapper.selectAllMenusForManagement();
        } else {
            menus = menuMapper.selectTenantMenusForManagement(user.tenantId());
        }

        // 构建用户名Map
        Map<Long, String> userNameMap = buildUserNameMap(menus);

        // 按parentId分组，构建树形结构（包含所有类型）
        Map<Long, List<MenuEntity>> parentMap = menus.stream()
                .collect(Collectors.groupingBy(MenuEntity::getParentId));

        List<MenuEntity> rootMenus = parentMap.getOrDefault(0L, Collections.emptyList());
        return rootMenus.stream()
                .map(menu -> convertToTreeNode(menu, parentMap, userNameMap))
                .collect(Collectors.toList());
    }

    private MenuVo.MenuTreeNode convertToTreeNode(MenuEntity menu, Map<Long, List<MenuEntity>> parentMap, Map<Long, String> userNameMap) {
        List<MenuEntity> children = parentMap.getOrDefault(menu.getId(), Collections.emptyList());
        List<MenuVo.MenuTreeNode> childNodes = children.stream()
                .map(child -> convertToTreeNode(child, parentMap, userNameMap))
                .collect(Collectors.toList());

        return new MenuVo.MenuTreeNode(
                menu.getId(),
                menu.getParentId(),
                menu.getMenuName(),
                menu.getMenuType(),
                menu.getPath(),
                menu.getComponent(),
                menu.getIcon(),
                menu.getPerms(),
                menu.getSortNo(),
                menu.getVisible(),
                menu.getStatus(),
                menu.getTenantId(),
                menu.getCreatedBy(),
                menu.getCreatedBy() != null ? userNameMap.getOrDefault(menu.getCreatedBy(), null) : null,
                menu.getCreatedAt(),
                menu.getUpdatedBy(),
                menu.getUpdatedBy() != null ? userNameMap.getOrDefault(menu.getUpdatedBy(), null) : null,
                menu.getUpdatedAt(),
                childNodes.isEmpty() ? null : childNodes
        );
    }

    private Map<Long, String> buildUserNameMap(List<MenuEntity> menus) {
        List<Long> userIds = menus.stream()
                .flatMap(m -> java.util.stream.Stream.of(m.getCreatedBy(), m.getUpdatedBy()))
                .filter(id -> id != null && id > 0)
                .distinct()
                .toList();
        if (userIds.isEmpty()) {
            return Map.of();
        }
        List<UserEntity> users = userMapper.selectBatchIds(userIds);
        return users.stream().collect(Collectors.toMap(UserEntity::getId, UserEntity::getRealName, (a, b) -> a));
    }

    /**
     * 获取菜单树（用于角色菜单分配等管理场景）
     * 返回树形结构，包含 id、label、children、menuType、perms 等字段
     * 平台管理员获取全部菜单，租户用户获取当前租户的菜单
     */
    @Override
    public List<Map<String, Object>> getMenuTree() {
        CurrentUser user = SecurityUtils.currentUser();
        List<MenuEntity> menus;
        if (user.isSystem()) {
            menus = menuMapper.selectAllMenus();
        } else {
            menus = menuMapper.selectTenantMenus(user.tenantId());
        }
        return buildMenuTreeForAssign(menus);
    }

    /**
     * 构建菜单树（用于菜单分配场景，包含按钮类型）
     */
    private List<Map<String, Object>> buildMenuTreeForAssign(List<MenuEntity> menus) {
        Map<Long, List<MenuEntity>> parentMap = menus.stream()
            .collect(Collectors.groupingBy(MenuEntity::getParentId));

        List<MenuEntity> rootMenus = parentMap.getOrDefault(0L, Collections.emptyList());
        return rootMenus.stream()
            .map(menu -> convertToAssignNode(menu, parentMap))
            .collect(Collectors.toList());
    }

    /**
     * 将菜单实体转换为分配用的树节点
     */
    private Map<String, Object> convertToAssignNode(MenuEntity menu, Map<Long, List<MenuEntity>> parentMap) {
        Map<String, Object> node = new HashMap<>();
        node.put("id", menu.getId());
        node.put("label", menu.getMenuName());
        node.put("menuType", menu.getMenuType());
        node.put("perms", menu.getPerms());
        node.put("parentId", menu.getParentId());

        List<MenuEntity> children = parentMap.getOrDefault(menu.getId(), Collections.emptyList());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childNodes = children.stream()
                .map(child -> convertToAssignNode(child, parentMap))
                .collect(Collectors.toList());
            node.put("children", childNodes);
        }

        return node;
    }
}
