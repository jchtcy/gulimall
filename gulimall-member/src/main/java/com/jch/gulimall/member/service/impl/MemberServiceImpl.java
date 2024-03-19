package com.jch.gulimall.member.service.impl;

import com.jch.gulimall.member.dao.MemberLevelDao;
import com.jch.gulimall.member.entity.MemberLevelEntity;
import com.jch.gulimall.member.exception.EmailExistException;
import com.jch.gulimall.member.exception.UsernameExistException;
import com.jch.gulimall.member.vo.MemberLoginVo;
import com.jch.gulimall.member.vo.MemberRegistVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import java.util.Map;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jch.common.utils.PageUtils;
import com.jch.common.utils.Query;
import com.jch.gulimall.member.dao.MemberDao;
import com.jch.gulimall.member.entity.MemberEntity;
import com.jch.gulimall.member.service.MemberService;


@Service("memberService")
public class MemberServiceImpl extends ServiceImpl<MemberDao, MemberEntity> implements MemberService {

    @Autowired
    MemberLevelDao memberLevelDao;

    @Override
    public PageUtils queryPage(Map<String, Object> params) {
        IPage<MemberEntity> page = this.page(
                new Query<MemberEntity>().getPage(params),
                new QueryWrapper<MemberEntity>()
        );

        return new PageUtils(page);
    }

    /**
     * 注册进会员
     * @param vo
     */
    @Override
    public void regist(MemberRegistVo vo){
        MemberEntity memberEntity = new MemberEntity();

        // 设置默认等级
        MemberLevelEntity memberLevelEntity = memberLevelDao.getDefaultLevel();
        memberEntity.setLevelId(memberLevelEntity.getId());

        // 检查邮箱是否唯一
        this.checkUniqueEmail(vo.getMail());
        memberEntity.setEmail(vo.getMail());

        // 检查用户名是否唯一
        this.checkUniqueUserName(vo.getUserName());
        memberEntity.setUsername(vo.getUserName());

        // 设置密码
        BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
        String encode = passwordEncoder.encode(vo.getPassword());
        memberEntity.setPassword(encode);

        this.baseMapper.insert(memberEntity);
    }

    /**
     * 检查邮箱是否唯一
     * @param mail
     * @return
     */
    @Override
    public void checkUniqueEmail(String mail) throws EmailExistException{
        int count = this.baseMapper.selectCount(
                new QueryWrapper<MemberEntity>().eq("email", mail)
        );
        if (count > 0) {
            throw new EmailExistException();
        }
    }

    /**
     * 检查用户名是否唯一
     *
     * @param userName
     * @return
     */
    @Override
    public void checkUniqueUserName(String userName) throws UsernameExistException{
        int count = this.baseMapper.selectCount(
                new QueryWrapper<MemberEntity>().eq("username", userName)
        );
        if (count > 0) {
            throw new UsernameExistException();
        }
    }

    /**
     * 会员登录
     * @param vo
     * @return
     */
    @Override
    public MemberEntity login(MemberLoginVo vo) {
        String loginacct = vo.getLoginacct();
        String password = vo.getPassword();
        MemberEntity memberEntity = this.baseMapper.selectOne(
                new QueryWrapper<MemberEntity>().eq("username", loginacct).or().eq("email", loginacct)
        );
        if (memberEntity == null) {
            // 没有该用户
            return null;
        } else {
            String passwordDb = memberEntity.getPassword();
            BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
            boolean matches = passwordEncoder.matches(password, passwordDb);
            if (matches) {
                return memberEntity;
            } else {
                // 密码不正确
                return null;
            }
        }
    }
}
