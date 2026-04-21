package com.plating.erp.iam.service.impl;

import com.plating.erp.common.security.CurrentUser;
import com.plating.erp.common.security.SecurityUtils;
import com.plating.erp.iam.entity.MenuEntity;
import com.plating.erp.iam.mapper.MenuMapper;
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
    
    public MenuServiceImpl(MenuMapper menuMapper) {
        this.menuMapper = menuMapper;
    }
    
    /**
     * 获取用户的完整菜单树和权限
     * 根据用户角色返回对应的菜单:
     * - 系统管理员(roleKey=PLATFORM_ADMIN): 返回所有平台级菜单
     * - 租户管理员(roleKey=TENANT_ADMIN): 返回平台级+租户级菜单
     * - 普通员工(roleKey=EMPLOYEE): 返回角色关联的菜单
     * 
     * @return 菜单和权限数据
     */
    public Map<String, Object> getUserRoutesAndPermissions() {
        List<MenuEntity> menus;
        List<String> permissions;
        CurrentUser user = SecurityUtils.currentUser();
        log.info("获取用户菜单和权限, userId={}, tenantId={}, roles={}", 
            user.userId(), user.tenantId(), user.roles());
        // 判断用户角色类型
        boolean isSystemAdmin = user.roles().contains("PLATFORM_ADMIN");
        boolean isTenantAdmin = user.roles().contains("TENANT_ADMIN");
        
        if (isSystemAdmin) {
            // 系统管理员（tenant_id=1）：获取所有菜单（平台级+租户级）
            log.info("系统管理员获取全部菜单, userId={}", user.userId());
            menus = menuMapper.selectAllMenus();
            permissions = menus.stream()
                .filter(m -> "F".equals(m.getMenuType()) && m.getPerms() != null)
                .map(MenuEntity::getPerms)
                .distinct()
                .collect(Collectors.toList());
        } else if (isTenantAdmin) {
            // 租户管理员：获取租户级菜单
            log.info("租户管理员获取租户菜单, userId={}, tenantId={}", user.userId(), user.tenantId());
            List<MenuEntity> platformMenus = menuMapper.selectMenuTreeByUserId(user.userId());
//            List<MenuEntity> tenantMenus = menuMapper.selectTenantMenus(user.tenantId());
            menus = new ArrayList<>(platformMenus);
//            menus.addAll(tenantMenus);
            
            permissions = menus.stream()
                .filter(m -> "F".equals(m.getMenuType()) && m.getPerms() != null)
                .map(MenuEntity::getPerms)
                .distinct()
                .collect(Collectors.toList());
        } else {
            // 普通员工：根据角色关联获取菜单
            log.info("普通员工获取角色菜单, userId={}", user.userId());
            menus = menuMapper.selectMenuTreeByUserId(user.userId());
            permissions = menuMapper.selectPermsByUserId(user.userId());
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
     * 根据ID获取菜单
     * 注意：调用方需要验证菜单是否属于当前租户
     */
    public MenuEntity getMenuById(Long id) {
        return menuMapper.selectById(id);
    }
    
    /**
     * 新增菜单
     * 注意：调用方需要设置正确的 tenant_id
     */
    public void addMenu(MenuEntity menu) {
        menuMapper.insert(menu);
        log.info("菜单新增成功, menuId={}, menuName={}, tenantId={}", 
                menu.getId(), menu.getMenuName(), menu.getTenantId());
    }
    
    /**
     * 更新菜单
     * 注意：调用方需要验证菜单是否属于当前租户
     */
    public void updateMenu(MenuEntity menu) {
        menuMapper.updateById(menu);
        log.info("菜单更新成功, menuId={}", menu.getId());
    }
    
    /**
     * 删除菜单
     * 注意：调用方需要验证菜单是否属于当前租户
     */
    public void deleteMenu(Long id) {
        menuMapper.deleteById(id);
        log.info("菜单删除成功, menuId={}", id);
    }
}
