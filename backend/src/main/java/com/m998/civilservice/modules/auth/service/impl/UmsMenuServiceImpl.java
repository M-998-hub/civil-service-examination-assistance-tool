package com.m998.civilservice.modules.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.m998.civilservice.modules.auth.dto.UmsMenuNode;
import com.m998.civilservice.modules.auth.mapper.UmsMenuMapper;
import com.m998.civilservice.modules.auth.model.UmsMenu;
import com.m998.civilservice.modules.auth.service.UmsMenuService;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 后台菜单管理Service实现类
 */
@Service
public class UmsMenuServiceImpl extends ServiceImpl<UmsMenuMapper,UmsMenu>implements UmsMenuService {

    /**
     * 创建菜单
     * @param umsMenu
     * @return
     */
    @Override
    public boolean create(UmsMenu umsMenu) {
        // 设置创建时间
        umsMenu.setCreateTime(new Date());
        // 自动计算菜单层级
        updateLevel(umsMenu);
        // 保存到数据库
        return save(umsMenu);
    }

    /**
     * 更新菜单层级
     *
     * 作用：方便前端展示树形结构，无需递归查询
     */
    private void updateLevel(UmsMenu umsMenu) {
        if (umsMenu.getParentId() == 0) {
            //没有父菜单时为一级菜单，level = 0
            umsMenu.setLevel(0);
        } else {
            //有父菜单时选择根据父菜单level设置
            UmsMenu parentMenu = getById(umsMenu.getParentId());
            if (parentMenu != null) {
                // 子菜单层级 = 父菜单层级 + 1
                umsMenu.setLevel(parentMenu.getLevel() + 1);
            } else {
                // 父菜单不存在（异常情况），默认为一级
                umsMenu.setLevel(0);
            }
        }
    }

    /**
     * 更新菜单信息
     * @param id
     * @param umsMenu
     * @return
     */
    @Override
    public boolean update(Long id, UmsMenu umsMenu) {
        // 设置菜单ID（告诉 MyBatis-Plus 更新哪条记录
        umsMenu.setId(id);
        // 重新计算层级（如果父级发生变化）
        updateLevel(umsMenu);
        // 执行更新
        return updateById(umsMenu);
    }

    /**
     * 分页查询菜单列表
     * @param parentId
     * @param pageSize
     * @param pageNum
     * @return
     */
    @Override
    public Page<UmsMenu> list(Long parentId, Integer pageSize, Integer pageNum) {

        // 创建分页对象
        Page<UmsMenu> page = new Page<>(pageNum,pageSize);

        // 构建查询条件
        QueryWrapper<UmsMenu> wrapper = new QueryWrapper<>();
        wrapper.lambda().eq(UmsMenu::getParentId,parentId) // 查询指定父菜单下的子菜单
                .orderByDesc(UmsMenu::getSort); // 按排序号降序

        // 执行分页查询
        return page(page,wrapper);
    }

    /**
     * 获取树形菜单列表（用于前端展示树形结构）
     * @return
     */
    @Override
    public List<UmsMenuNode> treeList() {
        // 获取所有菜单
        List<UmsMenu> menuList = list();

        // 过滤出一级菜单，并递归构建子菜单树
        List<UmsMenuNode> result = menuList.stream()
                .filter(menu -> menu.getParentId().equals(0L)) // 只取一级菜单
                .map(menu -> covertMenuNode(menu, menuList)).collect(Collectors.toList()); // 递归构建子树
        return result;
    }

    /**
     * 更新菜单的显示/隐藏状态
     *
     * 使用场景：管理员控制菜单是否显示/临时隐藏某些菜单（不删除）
     * @param id
     * @param hidden
     * @return
     */
    @Override
    public boolean updateHidden(Long id, Integer hidden) {
        // 创建菜单对象，只设置需要更新的字段
        UmsMenu umsMenu = new UmsMenu();
        umsMenu.setId(id);
        umsMenu.setHidden(hidden);

        // 执行更新（只更新 hidden 字段）
        return updateById(umsMenu);
    }

    /**
     * 将UmsMenu转化为UmsMenuNode（树形节点）并设置children属性
     */
    private UmsMenuNode covertMenuNode(UmsMenu menu, List<UmsMenu> menuList) {
        // 复制菜单属性到节点
        UmsMenuNode node = new UmsMenuNode();
        BeanUtils.copyProperties(menu, node);

        // 查找所有子菜单，递归构建子树
        List<UmsMenuNode> children = menuList.stream()
                .filter(subMenu -> subMenu.getParentId().equals(menu.getId())) //是当前菜单的子菜单
                .map(subMenu -> covertMenuNode(subMenu, menuList)) // 递归转换
                .collect(Collectors.toList());

        // 设置子节点（没子节点时为空列表）
        node.setChildren(children);
        return node;
    }
}
